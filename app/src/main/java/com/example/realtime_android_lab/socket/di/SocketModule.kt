package com.example.realtime_android_lab.socket.di

import com.example.realtime_android_lab.socket.data.AndroidNetworkMonitor
import com.example.realtime_android_lab.socket.data.RealtimeRepositoryImpl
import com.example.realtime_android_lab.socket.domain.BackoffPolicy
import com.example.realtime_android_lab.socket.domain.NetworkMonitor
import com.example.realtime_android_lab.socket.domain.RealtimeRepository
import com.example.realtime_android_lab.socket.ui.SocketDebugViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * BÀI 1 — DI bằng Koin. Thay cho `object SocketGraph` (service locator viết tay).
 *
 * ---
 * VÌ SAO KẾT NỐI PHẢI LÀ `single` (singleton theo Application)
 *
 * Kết nối realtime là tài nguyên SỐNG LÂU và DÙNG CHUNG; vòng đời của nó là Application,
 * không phải màn hình. Bản đầu tiên dựng repository ngay trong ViewModel factory ⇒ mỗi lần
 * Activity bị huỷ hẳn rồi dựng lại là thêm một OkHttpClient (kèm thread pool + connection
 * pool) và thêm một NetworkCallback không ai gỡ. Với `SharingStarted.Eagerly` ở
 * [AndroidNetworkMonitor] thì rò rỉ đó là thật.
 *
 * ---
 * AI SỞ HỮU VÒNG ĐỜI KẾT NỐI — câu trả lời chốt cho Bài 1
 *
 * **App sở hữu, không phải màn hình.** Kết nối chỉ dừng khi người dùng bấm "Ngắt"
 * (`disconnect()`) hoặc process chết. ViewModel bị huỷ KHÔNG ngắt kết nối.
 *
 * Đã cân nhắc và LOẠI phương án refcount theo subscriber (`SharingStarted.WhileSubscribed`):
 * URL do người dùng gõ ở runtime, nên khi subscriber cuối rời đi rồi có người mới vào,
 * refcount không biết phải nối lại vào URL nào — "nên kết nối tới đâu" là input của người
 * dùng, không suy ra được từ số lượng người đang xem. Thêm refcount là thêm máy móc cho một
 * bài toán không có (YAGNI). Cái giá phải trả ghi ở KDoc của `SocketDebugViewModel.onCleared`.
 *
 * ---
 * KOIN LÀ SERVICE LOCATOR, KHÔNG PHẢI DI THẬT — biết trước để trả lời phỏng vấn
 *
 * Koin không tiêm phụ thuộc lúc biên dịch; nó tra cứu theo KIỂU lúc chạy. Hệ quả: thiếu một
 * binding thì app **crash lúc mở màn**, không phải lỗi build như Hilt/Dagger. Đổi lại: không
 * codegen, không KSP, không Gradle plugin — nên không có rủi ro tương thích với AGP 9.
 *
 * Lưới an toàn bắt buộc đi kèm: `SocketModuleTest` kiểm graph trong unit test. Không có test
 * đó thì đổi từ `SocketGraph` (compiler kiểm 100%) sang Koin là một đánh đổi XẤU.
 *
 * ---
 * VÌ SAO TRỘN HAI KIỂU KHAI BÁO
 *
 * - `singleOf(::Foo)` = constructor DSL: Koin đọc chữ ký constructor bằng reflection và tự
 *   `get()` từng tham số. Ngắn, và `verify()` mới soi được vào nó.
 * - `single { ... }` = lambda tường minh: dùng khi cần một BIỂU THỨC chứ không phải constructor
 *   thuần (gọi factory method, hoặc muốn giữ tham số mặc định thay vì bắt Koin phân giải
 *   `Long`/`Double`/`Random` của [BackoffPolicy]).
 */
val socketModule = module {

    /**
     * Scope sống theo process: nuôi vòng lặp reconnect và NetworkCallback.
     *
     * Bind thẳng kiểu framework `CoroutineScope` vào container là chấp nhận được ở lab một
     * scope. Có scope thứ hai (vd scope riêng cho DB ở Bài 2) là đụng ngay — lúc đó phải
     * dùng qualifier `named("appScope")` hoặc bọc thành một kiểu riêng.
     */
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    /** Dùng tham số mặc định (base 500ms, cap 30s, factor 2.0) nên không dùng constructor DSL. */
    single { BackoffPolicy() }

    /** OkHttpClient đến từ factory method (đã cấu hình `pingInterval`), không từ constructor. */
    single { RealtimeRepositoryImpl.defaultClient() }

    /**
     * `bind` để phần còn lại của app CHỈ thấy interface của domain.
     * Đây chính là ranh giới Clean Architecture, giờ được khai báo tường minh ở một chỗ:
     * muốn thay bằng fake trong test thì `loadKoinModules` đè lên đúng dòng này.
     */
    singleOf(::AndroidNetworkMonitor) bind NetworkMonitor::class
    singleOf(::RealtimeRepositoryImpl) bind RealtimeRepository::class

    /**
     * `viewModelOf` lấy từ `org.koin.core.module.dsl` (module koin-core-viewmodel) chứ KHÔNG
     * phải `org.koin.androidx.viewmodel.dsl`. Koin 4.x có cả hai; bản core mới là bản dùng
     * chung với `koinViewModel()` của `org.koin.compose.viewmodel` mà màn hình đang gọi.
     * Lẫn hai họ này là nguồn lỗi "No definition found for ViewModel" kinh điển.
     */
    viewModelOf(::SocketDebugViewModel)
}
