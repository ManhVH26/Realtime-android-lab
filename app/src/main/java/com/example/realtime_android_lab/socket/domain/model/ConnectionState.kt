package com.example.realtime_android_lab.socket.domain.model

/**
 * BÀI 1 — DOMAIN model: trạng thái kết nối mà tầng UI quan tâm.
 * Thuần Kotlin, không biết OkHttp/Android là gì (đúng nguyên tắc Clean Architecture:
 * domain là lớp trong cùng, không phụ thuộc framework).
 */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState

    /** Đang chờ nối lại: [attempt] = lần thử thứ mấy, [nextDelayMs] = còn chờ bao lâu. */
    data class Reconnecting(val attempt: Int, val nextDelayMs: Long) : ConnectionState

    /** Dừng hẳn — server chủ động đuổi (vd token bị thu hồi). Không tự nối lại. */
    data class Failed(val reason: String) : ConnectionState

    fun label(): String = when (this) {
        Disconnected -> "chưa kết nối"
        Connecting -> "đang kết nối…"
        Connected -> "đã kết nối"
        is Reconnecting -> "nối lại lần $attempt (chờ ${nextDelayMs}ms)"
        is Failed -> "hỏng: $reason"
    }
}
