package com.example.realtime_android_lab.socket.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * BÀI 1 — DOMAIN: hợp đồng của "một kết nối realtime bền bỉ" + từ vựng của nó.
 *
 * Gộp interface và các type nó dùng vào một file: chúng thay đổi cùng nhau, tách ra 4 file
 * chỉ làm phải mở 4 tab để đọc một khái niệm.
 *
 * Đây là ranh giới Clean Architecture: UI chỉ biết interface này, KHÔNG biết bên dưới là
 * OkHttp WebSocket hay gì khác. Đổi sang XMPP/gRPC chỉ cần thay implementation ở tầng data.
 * File này KHÔNG được import OkHttp/Android — kiểm bằng danh sách import ở trên.
 *
 * ---
 * VÌ SAO HAI DÒNG RIÊNG (bản trước gộp làm một `observe(): Flow<ConnectionEvent>`)
 *
 * - Trạng thái = GIÁ TRỊ HIỆN TẠI, ai subscribe lúc nào cũng phải đọc được sự thật ngay
 *   ⇒ cần replay ⇒ [StateFlow].
 * - Tin nhắn = SỰ KIỆN TRÔI QUA, không được phát lại ⇒ SharedFlow không replay.
 *
 * Gộp vào một Flow thì buộc chọn MỘT semantics và bên kia sai. Bản cũ chọn không replay,
 * hậu quả thật: Activity destroy hẳn rồi dựng lại ⇒ ViewModel mới hiển thị "chưa kết nối"
 * trong khi socket vẫn Connected. Chữa bằng replay = 1 thì tin nhắn cũ bị phát lại mỗi lần
 * vào màn — sai kiểu khác.
 *
 * ---
 * VÒNG ĐỜI: kết nối KHÔNG thuộc màn hình. Chỉ [disconnect] hoặc process chết mới dừng nó.
 * ViewModel bị huỷ KHÔNG được ngắt — lý do ở `SocketDebugViewModel.onCleared`.
 */
interface RealtimeRepository {

    /** Trạng thái HIỆN TẠI. Đọc được ngay qua `.value`; collector mới nhận tức thì. */
    val connectionState: StateFlow<ConnectionState>

    /** Tin nhắn đến. KHÔNG replay — subscribe muộn thì không nhận lại tin cũ. */
    val messages: Flow<String>

    /** Bắt đầu (hoặc khởi động lại) kết nối tới [url]; tự reconnect khi đứt. */
    fun connect(url: String)

    /** Ngắt hẳn, dừng vòng reconnect. Chỉ gọi khi NGƯỜI DÙNG chủ động ngắt. */
    fun disconnect()

    /** Gửi text qua kết nối đang sống; trả false nếu chưa bắt tay xong. */
    fun send(text: String): Boolean
}

/** Trạng thái kết nối mà UI quan tâm. Thuần Kotlin, không biết framework nào. */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState

    /** Đang chờ nối lại: [attempt] = lần thử thứ mấy, [nextDelayMs] = còn chờ bao lâu. */
    data class Reconnecting(val attempt: Int, val nextDelayMs: Long) : ConnectionState

    /** Dừng hẳn — không tự nối lại (server đuổi, URL sai…). */
    data class Failed(val reason: String) : ConnectionState
}

/**
 * Lý do một lần kết nối bị đứt, để quyết định nối lại hay dừng.
 * Case "token hết hạn giữa phiên": server đóng bằng close code riêng (4001) ⇒ dừng hẳn.
 */
sealed interface CloseReason {
    data class ServerClose(val code: Int, val reason: String) : CloseReason
    data class NetworkFailure(val message: String) : CloseReason

    /** URL không parse được — nối lại bao nhiêu lần cũng vô ích. */
    data class InvalidUrl(val url: String) : CloseReason

    /**
     * Có nên DỪNG HẲN thay vì nối lại không.
     * `when` đủ nhánh (exhaustive) để sau này thêm một loại CloseReason mới là compiler bắt
     * phải quyết định nó fatal hay không, không im lặng coi như retry được.
     */
    fun isFatal(): Boolean = when (this) {
        is ServerClose -> code == FATAL_CLOSE_CODE
        is InvalidUrl -> true
        is NetworkFailure -> false
    }

    /** Mô tả ngắn để đưa lên UI/log. */
    fun describe(): String = when (this) {
        is ServerClose -> "server đóng $code" + if (reason.isBlank()) "" else " ($reason)"
        is NetworkFailure -> "lỗi mạng: $message"
        is InvalidUrl -> "URL không hợp lệ: $url"
    }

    companion object {
        /**
         * Close code server dùng để nói "đừng nối lại nữa".
         * Dải 4000–4999 là private-use theo RFC 6455 §7.4.2 — ứng dụng tự định nghĩa, không
         * đụng dải chuẩn (1000 normal, 1006 abnormal, 1011 internal error).
         */
        const val FATAL_CLOSE_CODE = 4001
    }
}
