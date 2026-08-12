package com.example.realtime_android_lab.socket.ui

/**
 * BÀI 1 — MVI **Intent**: mọi ý định của người dùng đi qua đây (một cửa vào duy nhất
 * `onIntent`). View không gọi thẳng use case — nó chỉ phát Intent.
 */
sealed interface SocketIntent {
    data class UrlChanged(val url: String) : SocketIntent
    data object Connect : SocketIntent
    data object Disconnect : SocketIntent
    data object SendTest : SocketIntent
    data object Ping : SocketIntent
}
