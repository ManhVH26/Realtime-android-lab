package com.example.realtime_android_lab.socket.domain.model

/**
 * BÀI 1 — DOMAIN model: trạng thái mạng, trừu tượng hoá khỏi ConnectivityManager.
 * Nhờ vậy domain/data không phụ thuộc trực tiếp API Android.
 */
enum class NetworkStatus { Available, Lost }
