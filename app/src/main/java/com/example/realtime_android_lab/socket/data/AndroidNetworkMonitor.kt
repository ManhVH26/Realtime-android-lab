package com.example.realtime_android_lab.socket.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.example.realtime_android_lab.socket.domain.NetworkMonitor
import com.example.realtime_android_lab.socket.domain.model.NetworkStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * BÀI 1 — DATA: hiện thực [NetworkMonitor] bằng ConnectivityManager.
 *
 * Đây là "chi tiết" theo Clean Architecture — domain khai báo interface, còn lớp này
 * mới đụng API Android. callbackFlow + awaitClose để đăng ký/gỡ callback không leak.
 */
class AndroidNetworkMonitor(context: Context) : NetworkMonitor {

    private val cm = context.getSystemService(ConnectivityManager::class.java)!!

    override fun status(): Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkStatus.Available)
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus.Lost)
            }
        }
        // registerDefaultNetworkCallback có từ API 24 — đúng minSdk project, không cần version check.
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
