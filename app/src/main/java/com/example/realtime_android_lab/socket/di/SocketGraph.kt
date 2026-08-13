package com.example.realtime_android_lab.socket.di

import android.content.Context
import com.example.realtime_android_lab.socket.data.AndroidNetworkMonitor
import com.example.realtime_android_lab.socket.data.RealtimeRepositoryImpl
import com.example.realtime_android_lab.socket.domain.BackoffPolicy
import com.example.realtime_android_lab.socket.domain.RealtimeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * BÀI 1 — DI thủ công (lab chưa dùng Hilt).
 *
 * Vì sao phải tách ra khỏi ViewModel factory: kết nối realtime là tài nguyên SỐNG LÂU và
 * DÙNG CHUNG, vòng đời của nó là Application chứ không phải màn hình.
 *
 * Bản trước dựng repository + NetworkMonitor ngay trong `SocketDebugViewModel.factory()`.
 * Hệ quả: mỗi lần Activity bị huỷ hẳn rồi dựng lại (không phải xoay màn hình — ViewModel
 * sống qua config change) là thêm một OkHttpClient kèm thread pool + connection pool riêng,
 * và thêm một NetworkCallback không ai gỡ. Với `SharingStarted.Eagerly` ở
 * [AndroidNetworkMonitor] thì rò rỉ đó là thật, nên chỗ này bắt buộc phải có.
 *
 * Bài 2 sẽ có nhiều màn cùng dùng một kết nối — lúc đó object này được thay bằng
 * Hilt `@Singleton`, phần còn lại của code không đổi một dòng vì tất cả đều phụ thuộc
 * interface [RealtimeRepository].
 *
 * ---
 * AI SỞ HỮU VÒNG ĐỜI KẾT NỐI — câu trả lời chốt cho Bài 1:
 *
 * **App sở hữu, không phải màn hình.** Kết nối chỉ dừng khi người dùng bấm "Ngắt"
 * (`disconnect()`) hoặc process chết. ViewModel bị huỷ KHÔNG ngắt kết nối.
 *
 * Đã cân nhắc và LOẠI phương án refcount theo subscriber (`SharingStarted.WhileSubscribed`):
 * URL do người dùng gõ ở runtime, nên khi subscriber cuối rời đi rồi có người mới vào,
 * refcount không biết phải nối lại vào URL nào — trạng thái "nên kết nối tới đâu" là input
 * của người dùng, không suy ra được từ số lượng người đang xem. Thêm refcount ở đây là
 * thêm máy móc cho một bài toán không có (YAGNI).
 *
 * Cái giá phải trả và chỗ sẽ sửa: xem KDoc của `SocketDebugViewModel.onCleared`.
 */
object SocketGraph {

    /** Scope sống theo process: nuôi vòng lặp reconnect và NetworkCallback. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var instance: RealtimeRepository? = null

    fun repository(context: Context): RealtimeRepository =
        instance ?: synchronized(this) {
            instance ?: RealtimeRepositoryImpl(
                backoff = BackoffPolicy(),
                networkMonitor = AndroidNetworkMonitor(context.applicationContext, appScope),
                scope = appScope,
            ).also { instance = it }
        }
}
