package com.example.realtime_android_lab.socket.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * BÀI 1 — DOMAIN: cổng trừu tượng để quan sát mạng.
 *
 * Domain khai báo INTERFACE này (dependency inversion); tầng data mới hiện thực bằng
 * ConnectivityManager. Nhờ vậy repository chỉ nói chuyện với domain, không đụng Android.
 *
 * HỢP ĐỒNG:
 * - Trả về [StateFlow] NÓNG, DÙNG CHUNG: gọi [status] bao nhiêu lần cũng chỉ có ĐÚNG MỘT
 *   callback hệ thống được đăng ký.
 * - `.value` luôn là trạng thái mạng HIỆN TẠI (có replay), không phải "chỉ phát khi đổi".
 *
 * Vì sao BẮT BUỘC nóng: nếu trả Flow lạnh thì mỗi collector đăng ký một NetworkCallback mới
 * — mà ConnectivityManager bắn `onAvailable` NGAY tại thời điểm đăng ký cho default network
 * đang có. Bên tiêu thụ vì thế luôn tưởng "mạng vừa quay lại" dù mạng chưa hề đứt, và
 * backoff bị cắt về ~0ms ⇒ reconnect storm. Bên tiêu thụ tự lọc ra ĐÚNG chuyển dịch
 * Lost → Available (xem `awaitBackoff` ở tầng data — cố ý viết dạng text, không dùng KDoc
 * link, để domain không tham chiếu ngược xuống data kể cả trong tài liệu).
 */
interface NetworkMonitor {
    fun status(): StateFlow<NetworkStatus>
}

/** Trạng thái mạng, trừu tượng hoá khỏi ConnectivityManager. */
enum class NetworkStatus { Available, Lost }
