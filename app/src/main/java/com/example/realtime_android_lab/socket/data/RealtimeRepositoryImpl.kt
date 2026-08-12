package com.example.realtime_android_lab.socket.data

import com.example.realtime_android_lab.socket.domain.BackoffPolicy
import com.example.realtime_android_lab.socket.domain.NetworkMonitor
import com.example.realtime_android_lab.socket.domain.RealtimeRepository
import com.example.realtime_android_lab.socket.domain.model.CloseReason
import com.example.realtime_android_lab.socket.domain.model.ConnectionEvent
import com.example.realtime_android_lab.socket.domain.model.ConnectionState
import com.example.realtime_android_lab.socket.domain.model.NetworkStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

/**
 * BÀI 1 — DATA: hiện thực [RealtimeRepository] bằng OkHttp WebSocket.
 *
 * Đây là lớp DUY NHẤT trong tính năng biết OkHttp tồn tại. Toàn bộ logic khó nằm ở đây:
 * - Vòng lặp reconnect: mở kết nối → chờ tới khi chết → tính backoff → chờ → thử lại.
 * - RESET số lần thử về 0 khi kết nối thành công (onOpen).
 * - Cắt ngắn thời gian chờ backoff nếu MẠNG vừa quay lại ([networkMonitor]) ⇒ reconnect chủ động.
 * - Ping/pong tầng WS ([pingInterval]) để lộ half-open + giữ NAT mapping sống.
 *
 * Kết nối stateful nên tự giữ một [scope] riêng; [connect]/[disconnect] điều khiển vòng đời.
 * (App thật sẽ để lớp này là @Singleton qua Hilt; ở lab thì tạo tay trong ViewModel factory.)
 */
class RealtimeRepositoryImpl(
    private val backoff: BackoffPolicy,
    private val networkMonitor: NetworkMonitor,
    private val client: OkHttpClient = defaultClient(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : RealtimeRepository {

    // Buffer để callback của OkHttp (chạy trên thread riêng) tryEmit không bị rớt.
    private val events = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 64)

    @Volatile
    private var currentSocket: WebSocket? = null
    private var loopJob: Job? = null

    override fun observe(): Flow<ConnectionEvent> = events.asSharedFlow()

    override fun connect(url: String) {
        loopJob?.cancel()
        loopJob = scope.launch { runConnectionLoop(url) }
    }

    override fun disconnect() {
        loopJob?.cancel()
        loopJob = null
        currentSocket?.cancel()
        currentSocket = null
        events.tryEmit(ConnectionEvent.StateChanged(ConnectionState.Disconnected))
    }

    override fun send(text: String): Boolean = currentSocket?.send(text) ?: false

    private suspend fun runConnectionLoop(url: String) {
        var attempt = 0
        events.emit(ConnectionEvent.StateChanged(ConnectionState.Connecting))

        while (currentCoroutineContext().isActive) {
            val reason = connectOnce(
                url = url,
                onOpen = {
                    attempt = 0 // RESET backoff khi kết nối thành công
                    events.tryEmit(ConnectionEvent.StateChanged(ConnectionState.Connected))
                },
                onMessage = { text -> events.tryEmit(ConnectionEvent.Message(text)) },
            )

            // Server chủ động đuổi (token bị thu hồi…) ⇒ dừng hẳn.
            if (reason.isFatal()) {
                events.emit(ConnectionEvent.StateChanged(ConnectionState.Failed("server yêu cầu dừng")))
                break
            }

            attempt++
            val waitMs = backoff.nextDelay(attempt)
            events.emit(ConnectionEvent.StateChanged(ConnectionState.Reconnecting(attempt, waitMs)))

            // Chờ tối đa waitMs, NHƯNG nếu mạng vừa quay lại thì cắt chờ và thử ngay.
            // status() chỉ phát khi đổi trạng thái nên nếu mạng ổn định sẽ chờ đủ waitMs.
            withTimeoutOrNull(waitMs) {
                networkMonitor.status().first { it == NetworkStatus.Available }
            }
        }
    }

    /**
     * Mở ĐÚNG MỘT kết nối, suspend tới khi nó đóng/hỏng, trả về lý do.
     * Bắc cầu callback OkHttp sang coroutine bằng [CompletableDeferred].
     */
    private suspend fun connectOnce(
        url: String,
        onOpen: () -> Unit,
        onMessage: (String) -> Unit,
    ): CloseReason {
        val closed = CompletableDeferred<CloseReason>()
        val request = Request.Builder().url(url).build()

        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = onOpen()

            override fun onMessage(webSocket: WebSocket, text: String) = onMessage(text)

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(NORMAL_CLOSE, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                closed.complete(CloseReason.ServerClose(code, reason))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Half-open lộ ra ở đây: chỉ khi ping/pong timeout OkHttp mới báo onFailure.
                closed.complete(CloseReason.NetworkFailure(t.message ?: "unknown"))
            }
        })

        currentSocket = ws
        return try {
            closed.await()
        } finally {
            currentSocket = null
            ws.cancel() // dọn kết nối khi vòng lặp bị hủy hoặc kết nối đã chết
        }
    }

    companion object {
        private const val NORMAL_CLOSE = 1000

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }
}
