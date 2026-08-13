package com.example.realtime_android_lab.socket.ui

import com.example.realtime_android_lab.socket.domain.ConnectionState

/**
 * BÀI 1 — HỢP ĐỒNG MVI của màn hình: State + Intent + Effect, gộp một file.
 *
 * Ba thứ này chỉ có nghĩa khi đi cùng nhau (thêm một Intent thường kéo theo sửa State), nên
 * tách thành 3 file chỉ tạo việc nhảy tab. Đây cũng là convention "Contract" phổ biến.
 */

/**
 * **State**: nguồn sự thật DUY NHẤT cho màn hình, bất biến.
 * Lưu ý MVI: kể cả text `url` cũng nằm trong State, View KHÔNG giữ state riêng.
 */
data class SocketUiState(
    val url: String = "wss://realtime-ws-lab.onrender.com/echo",
    val state: ConnectionState = ConnectionState.Disconnected,
    val retryCount: Int = 0,
    val nextDelayMs: Long = 0,
    val lastRttMs: Long? = null,
    val log: List<String> = emptyList(),
)

/**
 * **Intent**: mọi ý định của người dùng đi qua đây (một cửa vào duy nhất `onIntent`).
 * View không gọi thẳng repository — nó chỉ phát Intent.
 */
sealed interface SocketIntent {
    data class UrlChanged(val url: String) : SocketIntent
    data object Connect : SocketIntent
    data object Disconnect : SocketIntent
    data object SendTest : SocketIntent
    data object Ping : SocketIntent
}

/**
 * **Effect**: sự kiện MỘT LẦN (toast, điều hướng…) — thứ không được nằm trong State vì để
 * trong State nó sẽ phát lại khi recompose / xoay màn hình. Gửi qua Channel, View thu một lần.
 */
sealed interface SocketEffect {
    data class ShowMessage(val text: String) : SocketEffect
}

/**
 * Ánh xạ [ConnectionState] (domain) sang chữ hiển thị.
 *
 * Vì sao ở `ui` chứ không phải trong chính domain model: chuỗi tiếng Việt là chuyện TRÌNH BÀY.
 * Để `label()` trong domain nghĩa là lớp trong cùng gánh trách nhiệm của lớp ngoài cùng — và
 * ngày phải làm đa ngôn ngữ thì lại đi sửa domain (hoặc tệ hơn: nhét `Context`/`R.string` vào
 * domain, phá sạch ranh giới). Đúng chi tiết interviewer hay soi khi hỏi "Clean Architecture
 * của bạn sạch đến đâu".
 *
 * Bước tiếp cho đúng chuẩn app thật: đổi sang `stringResource(...)`; lab giữ chuỗi cứng cho gọn.
 */
fun ConnectionState.label(): String = when (this) {
    ConnectionState.Disconnected -> "chưa kết nối"
    ConnectionState.Connecting -> "đang kết nối…"
    ConnectionState.Connected -> "đã kết nối"
    is ConnectionState.Reconnecting -> "nối lại lần $attempt (chờ ${nextDelayMs}ms)"
    is ConnectionState.Failed -> "hỏng: $reason"
}
