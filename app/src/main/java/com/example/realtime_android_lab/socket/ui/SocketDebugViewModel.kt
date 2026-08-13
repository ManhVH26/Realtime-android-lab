package com.example.realtime_android_lab.socket.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.realtime_android_lab.socket.domain.ConnectionState
import com.example.realtime_android_lab.socket.domain.RealtimeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * BÀI 1 — MVI ViewModel.
 *
 * Luồng một chiều: View phát [SocketIntent] qua [onIntent] → xử lý (gọi repository) →
 * sinh [Change] → [reduce] thuần dựng [SocketUiState] mới → View vẽ lại.
 * Sự kiện một lần (toast) đi qua [effects], KHÔNG nằm trong state.
 *
 * BA nguồn đổ vào cùng một reducer: intent người dùng, trạng thái kết nối, tin nhắn đến.
 *
 * ---
 * VÌ SAO KHÔNG CÓ UseCase
 *
 * Bản trước có 5 class use case, mỗi class đúng một dòng `= repository.x()`. Chúng không
 * thêm hành vi, không thêm ràng buộc, chỉ thêm một tầng gõ tên. Chiều phụ thuộc vẫn đúng
 * mà không cần chúng: ViewModel phụ thuộc **interface [RealtimeRepository] của domain**,
 * không phụ thuộc implementation ở data.
 *
 * Nếu bị hỏi "Clean Architecture mà không có UseCase?" — trả lời: UseCase là nơi đặt
 * business rule; ở đây chưa có rule nào (không auth, không dedup, không ordering) nên nó
 * là ceremony. Rule đầu tiên xuất hiện (chặn gửi khi token hết hạn, rate-limit, dedup
 * message theo id) thì tạo đúng use case đó — không tạo trước 5 cái rỗng.
 */
class SocketDebugViewModel(
    private val repository: RealtimeRepository,
) : ViewModel() {

    /**
     * State ban đầu LẤY TỪ trạng thái thật của kết nối, không mặc định Disconnected.
     *
     * Kết nối sống theo process, ViewModel này thì không: Activity destroy hẳn rồi dựng lại
     * (đổi ngôn ngữ hệ thống, process bị kill rồi restore, back rồi vào lại màn) sẽ tạo
     * ViewModel MỚI trên một kết nối ĐANG SỐNG. Đọc `.value` ngay tại đây nên frame đầu đã
     * đúng, không nhá "chưa kết nối" rồi mới sửa.
     *
     * `url` vẫn là mặc định — URL là dữ liệu của MÀN HÌNH (người dùng gõ vào), không phải
     * trạng thái của kết nối. Kết nối đang sống có thể trỏ URL khác với ô text; lab chấp
     * nhận, app thật có một URL cấu hình duy nhất nên vấn đề này không tồn tại.
     */
    private val _state = MutableStateFlow(SocketUiState(state = repository.connectionState.value))
    val state: StateFlow<SocketUiState> = _state.asStateFlow()

    private val _effects = Channel<SocketEffect>(Channel.BUFFERED)
    val effects: Flow<SocketEffect> = _effects.receiveAsFlow()

    private var pingJob: Job? = null

    init {
        // Hai dòng riêng biệt vì hai bản chất khác nhau: trạng thái có replay, tin nhắn không.
        viewModelScope.launch { repository.connectionState.collect(::onConnectionState) }
        viewModelScope.launch { repository.messages.collect(::onMessage) }
    }

    /** Cửa vào DUY NHẤT của mọi ý định người dùng. */
    fun onIntent(intent: SocketIntent) {
        when (intent) {
            is SocketIntent.UrlChanged -> dispatch(Change.Url(intent.url))
            SocketIntent.Connect -> connect()
            SocketIntent.Disconnect -> disconnect()
            SocketIntent.SendTest -> sendTest()
            SocketIntent.Ping -> ping()
        }
    }

    // ----- xử lý intent (side effect: gọi repository) rồi phát Change -----

    private fun connect() {
        val url = _state.value.url
        dispatch(Change.Log("bắt đầu kết nối tới $url"))
        repository.connect(url)
        // KHÔNG bật ping ở đây — ping bám theo trạng thái thật, xem onConnectionState().
    }

    private fun disconnect() {
        stopPingLoop()
        repository.disconnect()
        dispatch(Change.Log("người dùng ngắt kết nối"))
    }

    private fun sendTest() {
        val ok = repository.send("hello ${_state.value.log.size}")
        if (ok) {
            dispatch(Change.Log("gửi: hello"))
        } else {
            _effects.trySend(SocketEffect.ShowMessage("Chưa kết nối, không gửi được"))
        }
    }

    /**
     * Gửi một ping mang theo mốc thời gian TRONG chính message.
     *
     * `elapsedRealtime()` chứ không phải `currentTimeMillis()`: nó đếm từ lúc boot và đơn
     * điệu tăng, không bị nhảy khi NTP chỉnh giờ hay người dùng đổi múi giờ giữa lúc đo —
     * đo khoảng thời gian thì luôn dùng đồng hồ đơn điệu.
     */
    private fun ping() {
        val sentAt = SystemClock.elapsedRealtime()
        if (repository.send("$PING_PREFIX$sentAt")) dispatch(Change.Log("ping…"))
    }

    private fun startPingLoop() {
        if (pingJob?.isActive == true) return // đang chạy rồi thì thôi
        pingJob = viewModelScope.launch {
            while (isActive) {
                delay(PING_EVERY_MS)
                ping()
            }
        }
    }

    private fun stopPingLoop() {
        pingJob?.cancel()
        pingJob = null
    }

    // Tên tham số là `newState` để không che mất property `state` của ViewModel.
    private fun onConnectionState(newState: ConnectionState) {
        // Ping bám theo TRẠNG THÁI THẬT, không theo nút bấm.
        // Bản cũ bật ping ngay lúc bấm "Kết nối" và không bao giờ tắt: vào Failed
        // (route /policy) hay đang Reconnecting vẫn ping đều 10s/lần vô ích.
        if (newState == ConnectionState.Connected) startPingLoop() else stopPingLoop()

        dispatch(Change.State(newState))
        dispatch(Change.Log("state: ${newState.label()}"))
    }

    private fun onMessage(text: String) {
        val sentAt = text.takeIf { it.startsWith(PING_PREFIX) }
            ?.removePrefix(PING_PREFIX)
            ?.toLongOrNull()

        if (sentAt == null) {
            dispatch(Change.Log("nhận: $text"))
            return
        }

        // RTT tính từ mốc NẰM TRONG chính message trả về.
        // Bản cũ trừ theo một biến `lastPingSentAt` bị ghi đè bởi mọi ping — khi có 2 ping
        // bay đồng thời (route /slow trễ 2s, hoặc bấm Ping tay xen với ping tự động) thì
        // pong của ping thứ nhất bị trừ theo mốc của ping thứ hai ⇒ RTT ra số vô nghĩa
        // (thậm chí âm). Nhét mốc vào payload là cách chuẩn, cũng là cách WebRTC/RTCP làm.
        val rtt = SystemClock.elapsedRealtime() - sentAt
        dispatch(Change.Rtt(rtt))
        dispatch(Change.Log("pong ← RTT = ${rtt}ms"))
    }

    // ----- reducer: (State, Change) -> State, THUẦN, là chỗ duy nhất tạo state mới -----

    private fun dispatch(change: Change) = _state.update { reduce(it, change) }

    private fun reduce(s: SocketUiState, change: Change): SocketUiState = when (change) {
        is Change.Url -> s.copy(url = change.url)
        is Change.State -> s.copy(
            state = change.state,
            retryCount = (change.state as? ConnectionState.Reconnecting)?.attempt ?: s.retryCount,
            nextDelayMs = (change.state as? ConnectionState.Reconnecting)?.nextDelayMs ?: 0,
        )
        is Change.Rtt -> s.copy(lastRttMs = change.rttMs)
        is Change.Log -> s.copy(log = (s.log + change.line).takeLast(MAX_LOG_LINES))
    }

    /** Thay đổi từng phần của state — đầu vào của reducer. */
    private sealed interface Change {
        data class Url(val url: String) : Change
        data class State(val state: ConnectionState) : Change
        data class Rtt(val rttMs: Long) : Change
        data class Log(val line: String) : Change
    }

    /**
     * CHỈ dừng vòng ping — KHÔNG ngắt kết nối.
     *
     * Bản trước gọi `disconnect()` ở đây, và nó tự phủ định lý do khai repository là `single`
     * trong `socketModule`:
     * dựng repository thành singleton theo Application để kết nối sống lâu hơn màn hình, rồi
     * lại giết nó ngay khi màn hình chết. Khi có HAI màn dùng chung MỘT kết nối thì hỏng
     * thật: pop màn A ⇒ onCleared ⇒ disconnect ⇒ màn B đứt kết nối.
     *
     * Còn ping thì ĐÚNG là của màn hình: nó chỉ để đo RTT cho màn debug này, không phải
     * keep-alive (keep-alive là ping/pong tầng WebSocket do OkHttp làm, ở tầng data). pingJob
     * nằm trong viewModelScope nên tự chết cùng ViewModel; gọi tường minh để ý định hiện rõ.
     *
     * Đánh đổi đang chấp nhận: rời màn hình mà chưa bấm "Ngắt" thì vòng reconnect vẫn chạy
     * nền tới khi process chết (xấu nhất 1 lần thử mỗi 30s do backoff có trần). Với lab thì
     * đây còn là tính năng — background app rồi xem nó nối lại. App thật phải buộc kết nối
     * vào vòng đời PROCESS foreground (ProcessLifecycleOwner) hoặc vào phiên đăng nhập.
     */
    override fun onCleared() {
        super.onCleared()
        stopPingLoop()
    }

    companion object {
        private const val PING_EVERY_MS = 10_000L
        private const val MAX_LOG_LINES = 100

        /** Tiền tố đánh dấu message dùng để đo RTT (server route /echo trả nguyên văn). */
        private const val PING_PREFIX = "PING:"

        // Không còn `factory(context)`: việc dựng ViewModel đã chuyển sang Koin
        // (`viewModelOf(::SocketDebugViewModel)` trong socketModule). Nhờ vậy class này không
        // còn biết gì về Context — nó chỉ nhận đúng thứ nó cần qua constructor, và test có thể
        // `new` nó thẳng với một RealtimeRepository giả, không cần Koin lẫn Android.
    }
}
