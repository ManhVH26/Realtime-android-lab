package com.example.realtime_android_lab.socket.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.realtime_android_lab.socket.domain.NetworkMonitor
import com.example.realtime_android_lab.socket.domain.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

/**
 * BÀI 1 — DATA: hiện thực [NetworkMonitor] bằng ConnectivityManager.
 *
 * Đây là "chi tiết" theo Clean Architecture — domain khai báo interface, còn lớp này
 * mới đụng API Android. callbackFlow + awaitClose để đăng ký/gỡ callback không leak.
 *
 * [scope] quyết định vòng đời của callback: huỷ scope ⇒ `awaitClose` chạy ⇒ callback được gỡ.
 * Lớp này phải là SINGLETON theo Application — xem
 * `socketModule` (khai `single`, xem socket/di/SocketModule.kt).
 */
class AndroidNetworkMonitor(
    context: Context,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : NetworkMonitor {

    private val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)!!

    private val shared: StateFlow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkStatus.Available)
            }

            override fun onLost(network: Network) {
                // KHÔNG gửi thẳng Lost. Khi máy chuyển Wi-Fi → 4G, hệ thống bắn
                // onAvailable(mạng mới) TRƯỚC rồi mới onLost(mạng cũ); gửi Lost mù sẽ
                // kẹt trạng thái ở "mất mạng" trong khi thực tế đang online.
                // Hỏi lại hệ thống mới ra câu trả lời đúng.
                trySend(currentStatus())
            }
        }
        // registerDefaultNetworkCallback: có từ API 24 — đúng minSdk project nên không cần
        // version check. Cần quyền ACCESS_NETWORK_STATE, thiếu là SecurityException.
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        // ĐÂY LÀ CHỖ SỬA BUG NẶNG NHẤT CỦA BÀI 1.
        // stateIn + Eagerly = chốt callbackFlow lạnh thành MỘT nguồn nóng duy nhất, đăng ký
        // callback đúng một lần cho cả vòng đời app.
        //
        // Bản cũ trả thẳng callbackFlow lạnh: mỗi lần repository chờ backoff là một
        // NetworkCallback mới được đăng ký, và callback mới LUÔN nhận onAvailable ngay lập
        // tức (hành vi có tài liệu của registerDefaultNetworkCallback) ⇒ điều kiện "mạng
        // vừa quay lại" luôn đúng ⇒ backoff bị cắt về ~0ms ⇒ reconnect storm.
        //
        // Lưu ý: dùng SharingStarted.WhileSubscribed ở đây sẽ TÁI HIỆN LẠI đúng bug đó,
        // vì repository subscribe/unsubscribe liên tục giữa các lần chờ. Bắt buộc Eagerly.
        .stateIn(scope, SharingStarted.Eagerly, currentStatus())

    override fun status(): StateFlow<NetworkStatus> = shared

    /** Hỏi trạng thái mạng ngay tại thời điểm gọi — dùng làm giá trị khởi tạo của StateFlow. */
    private fun currentStatus(): NetworkStatus {
        // activeNetwork: API 23. getNetworkCapabilities: API 21.
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val usable = caps != null &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            // NET_CAPABILITY_VALIDATED (API 23): hệ thống đã xác thực mạng này thật sự ra
            // được Internet — loại được Wi-Fi captive portal (bắt sóng nhưng chưa đăng nhập),
            // thứ mà chỉ NET_CAPABILITY_INTERNET không phân biệt nổi.
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return if (usable) NetworkStatus.Available else NetworkStatus.Lost
    }
}
