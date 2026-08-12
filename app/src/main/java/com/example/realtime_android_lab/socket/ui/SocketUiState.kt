package com.example.realtime_android_lab.socket.ui

import com.example.realtime_android_lab.socket.domain.model.ConnectionState

/**
 * BÀI 1 — MVI **State**: nguồn sự thật DUY NHẤT cho màn hình, bất biến.
 *
 * Lưu ý MVI: kể cả text `url` cũng nằm trong State, View KHÔNG giữ state riêng.
 * Mọi thay đổi đi qua reducer trong ViewModel.
 */
data class SocketUiState(
    val url: String = "ws://10.0.2.2:8080/echo",
    val state: ConnectionState = ConnectionState.Disconnected,
    val retryCount: Int = 0,
    val nextDelayMs: Long = 0,
    val lastRttMs: Long? = null,
    val log: List<String> = emptyList(),
)
