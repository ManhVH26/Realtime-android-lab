package com.example.realtime_android_lab.socket.ui

/**
 * BÀI 1 — MVI **Effect**: sự kiện MỘT LẦN (toast, điều hướng…) — thứ không được nằm
 * trong State vì nếu để trong State nó sẽ phát lại khi recompose / xoay màn hình.
 * Gửi qua Channel, View thu một lần rồi thôi.
 */
sealed interface SocketEffect {
    data class ShowMessage(val text: String) : SocketEffect
}
