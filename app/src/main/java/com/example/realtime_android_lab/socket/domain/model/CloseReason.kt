package com.example.realtime_android_lab.socket.domain.model

/**
 * BÀI 1 — DOMAIN model: lý do một lần kết nối bị đứt, để quyết định nối lại hay dừng.
 * Case "token hết hạn giữa phiên": server đóng bằng close code riêng (4001) ⇒ dừng hẳn.
 */
sealed interface CloseReason {
    data class ServerClose(val code: Int, val reason: String) : CloseReason
    data class NetworkFailure(val message: String) : CloseReason

    fun isFatal(): Boolean = this is ServerClose && code == FATAL_CLOSE_CODE

    companion object {
        /** Close code do server dùng để nói "đừng nối lại nữa". */
        const val FATAL_CLOSE_CODE = 4001
    }
}
