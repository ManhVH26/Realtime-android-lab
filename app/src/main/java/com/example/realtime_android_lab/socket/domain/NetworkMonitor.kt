package com.example.realtime_android_lab.socket.domain

import com.example.realtime_android_lab.socket.domain.model.NetworkStatus
import kotlinx.coroutines.flow.Flow

/**
 * BÀI 1 — DOMAIN: cổng trừu tượng để quan sát mạng.
 *
 * Domain khai báo INTERFACE này (dependency inversion); tầng data mới hiện thực bằng
 * ConnectivityManager. Nhờ vậy repository chỉ nói chuyện với domain, không đụng Android.
 *
 * Quy ước: [status] chỉ phát khi có THAY ĐỔI (Available/Lost), không replay giá trị hiện tại,
 * để repository dùng nó cắt ngắn backoff đúng lúc mạng vừa quay lại.
 */
interface NetworkMonitor {
    fun status(): Flow<NetworkStatus>
}
