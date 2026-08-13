package com.example.realtime_android_lab

import android.app.Application
import com.example.realtime_android_lab.socket.di.socketModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Điểm khởi động container Koin.
 *
 * Phải là Application chứ không phải Activity: container giữ các singleton sống theo PROCESS
 * (kết nối WebSocket, NetworkCallback). Khởi tạo ở Activity thì chúng chết theo màn hình —
 * đúng cái sai mà `SocketGraph` sinh ra để tránh.
 *
 * `androidContext()` nạp sẵn hai binding `Context` và `Application` vào container; nhờ vậy
 * `singleOf(::AndroidNetworkMonitor)` phân giải được tham số `Context` mà không phải khai gì thêm.
 *
 * `androidLogger(Level.ERROR)`: Koin mặc định log mọi lần phân giải (Level.INFO) — ồn và tốn
 * lúc chạy thật. Chỉ cần thấy lỗi. Muốn soi graph khi debug thì tạm đổi sang `Level.DEBUG`.
 */
class RealtimeLabApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@RealtimeLabApp)
            modules(socketModule)
        }
    }
}
