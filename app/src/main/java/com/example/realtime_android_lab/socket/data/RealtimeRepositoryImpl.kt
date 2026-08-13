package com.example.realtime_android_lab.socket.data

import com.example.realtime_android_lab.socket.domain.BackoffPolicy
import com.example.realtime_android_lab.socket.domain.NetworkMonitor
import com.example.realtime_android_lab.socket.domain.RealtimeRepository
import com.example.realtime_android_lab.socket.domain.CloseReason
import com.example.realtime_android_lab.socket.domain.ConnectionState
import com.example.realtime_android_lab.socket.domain.NetworkStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * BÀI 1 — DATA: hiện thực [RealtimeRepository] bằng OkHttp WebSocket.
 *
 * Đây là lớp DUY NHẤT trong tính năng biết OkHttp tồn tại. Toàn bộ logic khó nằm ở đây:
 * - Vòng lặp reconnect: mở kết nối → chờ tới khi chết → tính backoff → chờ → thử lại.
 * - RESET số lần thử về 0 khi kết nối thành công (onOpen).
 * - Cắt ngắn thời gian chờ backoff khi mạng CHUYỂN từ mất sang có ([awaitBackoff]).
 * - Ping/pong tầng WS ([pingInterval]) để lộ half-open + giữ NAT mapping sống.
 *
 * Bốn cơ chế chống race, đọc kỹ vì đây là phần hay bị hỏi:
 * 1. [generation] — con dấu thế hệ, loại bỏ sự kiện đến muộn của vòng lặp đã bị thay thế.
 * 2. [currentSocket] / [openSocket] là AtomicReference + `compareAndSet`, không bao giờ
 *    `set(null)` mù, vì dọn dẹp của vòng lặp cũ có thể chạy SAU khi vòng lặp mới đã bắt đầu.
 * 3. [openSocket] chỉ được gán trong `onOpen` — tách "socket đã tạo" khỏi "socket đã bắt tay xong".
 * 4. [lifecycleLock] — bọc CẶP (đọc generation, ghi state) thành một khối nguyên tử, và bảo
 *    vệ [loopJob]. Xem [publish] để biết vì sao chỉ `if (gen == generation.get())` là chưa đủ.
 *
 * Kết nối stateful, sống lâu ⇒ lớp này phải là singleton theo Application, xem
 * `socketModule` (khai `single`, xem socket/di/SocketModule.kt).
 */
class RealtimeRepositoryImpl(
    private val backoff: BackoffPolicy,
    private val networkMonitor: NetworkMonitor,
    private val client: OkHttpClient = defaultClient(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : RealtimeRepository {

    /**
     * Trạng thái là GIÁ TRỊ, không phải sự kiện ⇒ StateFlow (luôn có `.value`, replay cho
     * collector mới). Nhờ vậy ViewModel dựng lại đọc được ngay trạng thái thật.
     */
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /**
     * Tin nhắn là SỰ KIỆN ⇒ SharedFlow không replay.
     * `extraBufferCapacity` để callback của OkHttp (chạy trên thread riêng) `tryEmit` không rớt.
     */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val messages: Flow<String> = _messages.asSharedFlow()

    /**
     * Con dấu thế hệ. Mỗi [connect]/[disconnect] tăng 1; sự kiện mang dấu cũ bị bỏ.
     * Không có nó thì: bấm "Ngắt" đúng lúc `onOpen` của vòng lặp cũ vừa chạy trên thread
     * OkHttp ⇒ UI kẹt ở "đã kết nối" dù kết nối đã bị huỷ.
     */
    private val generation = AtomicLong(0)

    /**
     * Bảo vệ hai thứ đi liền nhau: con dấu thế hệ và [loopJob].
     *
     * KHÔNG giữ lock này khi gọi vào OkHttp (`ws.cancel()`) — `cancel()` có thể gọi
     * `onFailure` ngay trên thread hiện tại, và nếu listener nào đó sau này gọi [publish]
     * thì thành tự deadlock. Mọi lời gọi OkHttp đều nằm NGOÀI khối synchronized.
     */
    private val lifecycleLock = Any()

    /** Socket đã TẠO (có thể chưa bắt tay xong) — giữ để cancel khi ngắt. */
    private val currentSocket = AtomicReference<WebSocket?>(null)

    /** Socket đã BẮT TAY XONG (sau onOpen) — chỉ cái này mới được phép [send]. */
    private val openSocket = AtomicReference<WebSocket?>(null)

    /** Chỉ đọc/ghi trong `synchronized(lifecycleLock)`. */
    private var loopJob: Job? = null

    override fun connect(url: String) {
        synchronized(lifecycleLock) {
            val gen = generation.incrementAndGet()
            loopJob?.cancel()
            loopJob = scope.launch { runConnectionLoop(url, gen) }
        }
        // Không tự phát Connecting ở đây — vòng lặp phát, để state luôn đi ra từ MỘT nguồn.
    }

    override fun disconnect() {
        // Ghi state NGAY trong lock cùng lúc với tăng generation: mọi publish của vòng lặp
        // cũ hoặc bị chặn (gen cũ) hoặc phải xếp hàng sau, không thể ghi đè Disconnected.
        val dying = synchronized(lifecycleLock) {
            generation.incrementAndGet()
            loopJob?.cancel()
            loopJob = null
            _connectionState.value = ConnectionState.Disconnected
            currentSocket.getAndSet(null)
        }
        // compareAndSet chứ không set(null) mù: chỉ xoá đúng socket mình vừa lấy ra.
        openSocket.compareAndSet(dying, null)
        dying?.cancel() // gọi NGOÀI lock, xem KDoc của lifecycleLock
    }

    /**
     * Gửi text; trả false nếu chưa bắt tay xong.
     *
     * Vì sao kiểm tra [openSocket] chứ không phải [currentSocket]: OkHttp cho gọi `send()`
     * ngay sau `newWebSocket()` và trả TRUE (nó xếp message vào hàng đợi, chờ handshake).
     * Nếu chỉ kiểm tra "socket != null" thì lúc đang CONNECTING vẫn trả true — sai với
     * hợp đồng "trả false nếu chưa kết nối" mà UI đang tin tưởng.
     */
    override fun send(text: String): Boolean = openSocket.get()?.send(text) ?: false

    private suspend fun runConnectionLoop(url: String, gen: Long) {
        /*
         * AtomicInteger chứ không phải `var attempt` thường: giá trị này được GHI trên thread
         * của OkHttp (trong onOpen) và ĐỌC trên dispatcher của vòng lặp. Với plain var,
         * JMM không đảm bảo thread đọc thấy giá trị thread kia vừa ghi ⇒ backoff có thể
         * không bao giờ reset dù kết nối đã thành công.
         * Biến cục bộ của mỗi lần chạy vòng lặp, nên không cần là field của class.
         */
        val attempt = AtomicInteger(0)

        try {
            while (currentCoroutineContext().isActive) {
                publish(gen, ConnectionState.Connecting)

                val reason = connectOnce(
                    url = url,
                    gen = gen,
                    onOpen = {
                        attempt.set(0) // RESET backoff khi kết nối thành công
                        publish(gen, ConnectionState.Connected)
                    },
                    onMessage = { text -> emitMessage(gen, text) },
                )

                // Server chủ động đuổi (token bị thu hồi…) hoặc URL sai ⇒ dừng hẳn.
                if (reason.isFatal()) {
                    publish(gen, ConnectionState.Failed(reason.describe()))
                    break
                }

                val n = attempt.incrementAndGet()
                val waitMs = backoff.nextDelay(n)
                publish(gen, ConnectionState.Reconnecting(n, waitMs))
                awaitBackoff(waitMs)
            }
        } catch (e: CancellationException) {
            throw e // huỷ là chuyện bình thường, phải để nó lan tiếp
        } catch (t: Throwable) {
            // Chốt chặn cuối: KHÔNG để exception thoát ra scope.
            // SupervisorJob KHÔNG nuốt lỗi — nó chỉ chặn lỗi lan sang coroutine anh em.
            // Lỗi vẫn bay lên uncaught handler và giết app.
            publish(gen, ConnectionState.Failed(t.message ?: t::class.java.simpleName))
        }
    }

    /**
     * Chờ [waitMs], NHƯNG cắt ngắn nếu mạng vừa chuyển từ mất → có.
     *
     * `dropWhile { Available }` là mấu chốt của cả hàm: nếu ngay lúc này đang có mạng
     * (server tự đá mình ra chứ không phải rớt sóng) thì bỏ qua giá trị hiện tại và chỉ
     * tỉnh dậy khi thật sự đi qua chuỗi Lost → Available. Không có `dropWhile`, hàm này
     * trả về tức thì mọi lúc và backoff coi như không tồn tại.
     *
     * Nếu đang mất mạng sẵn thì `dropWhile` không bỏ gì cả, và ta chờ đúng khoảnh khắc
     * mạng quay lại — đây mới là hành vi mong muốn: reconnect chủ động, không ngồi đợi
     * hết 30 giây backoff trong khi Wi-Fi đã lên từ lâu.
     */
    private suspend fun awaitBackoff(waitMs: Long) {
        withTimeoutOrNull(waitMs) {
            networkMonitor.status()
                .dropWhile { it == NetworkStatus.Available }
                .first { it == NetworkStatus.Available }
        }
    }

    /**
     * Mở ĐÚNG MỘT kết nối, suspend tới khi nó đóng/hỏng, trả về lý do.
     * Bắc cầu callback OkHttp sang coroutine bằng [CompletableDeferred].
     */
    private suspend fun connectOnce(
        url: String,
        gen: Long,
        onOpen: () -> Unit,
        onMessage: (String) -> Unit,
    ): CloseReason {
        // `Request.Builder().url()` NÉM IllegalArgumentException nếu chuỗi không parse được
        // (ô URL là text tự do, người dùng gõ gì cũng được). Bắt tại đây và coi là lỗi chí tử.
        // Trước đây exception này thoát ra scope và làm chết app.
        // Lưu ý: OkHttp tự quy đổi ws:// → http:// và wss:// → https:// nên cả 4 scheme đều hợp lệ.
        val request = try {
            Request.Builder().url(url).build()
        } catch (_: IllegalArgumentException) {
            return CloseReason.InvalidUrl(url)
        }

        val closed = CompletableDeferred<CloseReason>()

        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // claimSocket chứ KHÔNG set() mù: onOpen chạy trên thread OkHttp và hoàn toàn
                // có thể nổ MUỘN, sau khi vòng lặp này đã bị thay thế. set() mù khi đó ghi đè
                // socket đang sống của vòng lặp mới bằng một socket sắp bị cancel ⇒ send()
                // trả false vĩnh viễn dù màn hình hiển thị "đã kết nối".
                claimSocket(gen, openSocket, webSocket) // từ đây send() mới được phép
                onOpen()
            }

            override fun onMessage(webSocket: WebSocket, text: String) = onMessage(text)

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                openSocket.compareAndSet(webSocket, null)
                // Trả lại close frame để bắt tay đóng đúng chuẩn RFC 6455.
                // OkHttp sẽ gọi onClosed với code CỦA PEER (vd 4001), không phải code mình gửi —
                // nhờ vậy isFatal() vẫn nhận ra được lệnh "đừng nối lại".
                webSocket.close(NORMAL_CLOSE, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                openSocket.compareAndSet(webSocket, null)
                closed.complete(CloseReason.ServerClose(code, reason))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Half-open lộ ra ở đây: chỉ khi ping/pong timeout OkHttp mới báo onFailure.
                openSocket.compareAndSet(webSocket, null)
                closed.complete(CloseReason.NetworkFailure(t.message ?: "unknown"))
            }
        })

        claimSocket(gen, currentSocket, ws)
        return try {
            closed.await()
        } finally {
            // compareAndSet chứ KHÔNG set(null) mù.
            // connect() huỷ vòng lặp cũ rồi launch vòng lặp mới NGAY; `cancel()` là bất đồng bộ
            // nên khối finally này của vòng lặp cũ hoàn toàn có thể chạy SAU khi vòng lặp mới
            // đã gán socket của nó. set(null) mù sẽ xoá mất socket đang sống ⇒ send() trả
            // false vĩnh viễn và disconnect() không cancel được gì.
            openSocket.compareAndSet(ws, null)
            currentSocket.compareAndSet(ws, null)
            ws.cancel() // dọn kết nối khi vòng lặp bị huỷ hoặc kết nối đã chết
        }
    }

    // ----- phát ra ngoài, có kiểm tra con dấu thế hệ -----

    /**
     * Chỗ DUY NHẤT ghi [_connectionState].
     *
     * Vì sao phải synchronized mà không chỉ `if (gen == generation.get()) value = state`:
     * đó là hai lệnh rời rạc. Thread A (vòng lặp cũ) đọc generation thấy khớp, bị hệ điều
     * hành cắt ngang; thread B chạy disconnect() (tăng generation, ghi Disconnected); A tỉnh
     * lại và ghi Connected lên trên ⇒ UI kẹt "đã kết nối" sau khi đã ngắt. Đúng con bug mà
     * con dấu thế hệ ra đời để diệt, chỉ dịch xuống nhỏ hơn một tầng.
     * Bọc cặp (đọc, ghi) vào cùng lock mà disconnect()/connect() dùng thì cửa sổ đó biến mất.
     */
    private fun publish(gen: Long, state: ConnectionState) {
        synchronized(lifecycleLock) {
            if (gen == generation.get()) _connectionState.value = state
        }
    }

    /**
     * Gán socket vào [ref] CHỈ KHI vòng lặp gọi nó còn là vòng lặp hiện hành.
     *
     * Dùng chung [lifecycleLock] với [publish] vì cùng một lý do: `if (còn hiện hành) rồi set`
     * là hai lệnh rời, không bọc lại thì thread cũ có thể lọt qua bước kiểm tra rồi mới ghi
     * đè lên kết quả của thread mới.
     *
     * An toàn deadlock: hàm này KHÔNG gọi vào OkHttp, và mọi lời gọi OkHttp
     * (`ws.cancel()`) đều nằm ngoài lock — xem KDoc của [lifecycleLock].
     */
    private fun claimSocket(gen: Long, ref: AtomicReference<WebSocket?>, ws: WebSocket) {
        synchronized(lifecycleLock) {
            if (gen == generation.get()) ref.set(ws)
        }
    }

    /**
     * Tin nhắn KHÔNG cần lock: nó không phải state, ghi trễ cũng không làm sai giá trị nào.
     * Kiểm tra generation ở đây chỉ để không đẩy tin của kết nối đã bị thay thế lên UI.
     */
    private fun emitMessage(gen: Long, text: String) {
        if (gen == generation.get()) _messages.tryEmit(text)
    }

    companion object {
        private const val NORMAL_CLOSE = 1000

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }
}
