package com.example.realtime_android_lab.socket.domain

import com.example.realtime_android_lab.socket.domain.model.ConnectionEvent
import kotlinx.coroutines.flow.Flow

/**
 * BÀI 1 — DOMAIN: hợp đồng của "một kết nối realtime bền bỉ".
 *
 * Đây là ranh giới Clean Architecture: UI và use case chỉ biết interface này, KHÔNG biết
 * bên dưới là OkHttp WebSocket hay cái gì khác. Muốn đổi sang XMPP/gRPC chỉ cần thay
 * implementation ở tầng data, domain và ui không đổi một dòng.
 *
 * Kết nối là STATEFUL và sống lâu: [connect]/[disconnect] điều khiển vòng đời, [observe]
 * là dòng sự kiện, [send] đẩy tin ra kết nối đang sống.
 */
interface RealtimeRepository {
    /** Bắt đầu (hoặc khởi động lại) kết nối tới [url]; tự reconnect khi đứt. */
    fun connect(url: String)

    /** Ngắt hẳn, dừng vòng reconnect. */
    fun disconnect()

    /** Dòng sự kiện trạng thái + tin nhắn. Thu ở tầng UI. */
    fun observe(): Flow<ConnectionEvent>

    /** Gửi text qua kết nối đang sống; trả false nếu chưa kết nối. */
    fun send(text: String): Boolean
}
