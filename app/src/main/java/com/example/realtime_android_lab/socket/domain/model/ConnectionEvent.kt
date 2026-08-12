package com.example.realtime_android_lab.socket.domain.model

/**
 * BÀI 1 — DOMAIN model: sự kiện thô mà repository phát ra cho tầng trên.
 */
sealed interface ConnectionEvent {
    data class StateChanged(val state: ConnectionState) : ConnectionEvent
    data class Message(val text: String) : ConnectionEvent
}
