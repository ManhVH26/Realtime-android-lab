package com.example.realtime_android_lab.socket.di

import android.app.Application
import android.content.Context
import okhttp3.OkHttpClient
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify
import kotlin.random.Random

/**
 * BÀI 1 — kiểm graph DI.
 *
 * VÌ SAO TEST NÀY LÀ BẮT BUỘC, KHÔNG PHẢI "CÓ THÌ TỐT"
 *
 * Koin phân giải lúc CHẠY. Thêm một tham số vào constructor của `RealtimeRepositoryImpl` mà
 * quên khai binding thì **build vẫn xanh**, app chỉ chết khi mở màn hình. Bản `SocketGraph`
 * viết tay trước đây được compiler kiểm 100% — bỏ nó mà không có lưới này là đi lùi.
 *
 * `verify()` đọc chữ ký constructor của từng definition bằng reflection và đối chiếu với
 * những kiểu module khai. Nó KHÔNG khởi tạo object thật ⇒ chạy được trên JVM thuần, không cần
 * Robolectric, không cần Android.
 *
 * Cái nó bắt được (chính là lỗi hay xảy ra nhất):
 *   thêm param vào constructor → quên khai binding → test đỏ ngay, không phải đợi crash.
 *
 * Cái nó KHÔNG bắt được: lỗi bên trong thân lambda `single { ... }` (verify không chạy lambda).
 * Muốn phủ nốt phần đó thì phải khởi tạo thật bằng `checkModules` + Robolectric — chưa đáng
 * cho graph 6 node.
 */
class SocketModuleTest {

    /**
     * [extraTypes] = danh sách trắng các kiểu Koin KHÔNG cần tự phân giải:
     * - `Context` / `Application`: do `androidContext()` nạp lúc `startKoin`, không có trong module.
     * - `Long` / `Double` / `Random`: tham số có GIÁ TRỊ MẶC ĐỊNH của `BackoffPolicy`.
     * - `OkHttpClient.Builder`: constructor nội bộ của OkHttpClient.
     *
     * Nếu `verify()` báo thiếu một kiểu nào khác, cân nhắc kỹ trước khi nhét vào đây: phần lớn
     * trường hợp đó là binding THẬT SỰ bị thiếu, và whitelist nó đi là làm test mất tác dụng.
     */
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `moi phu thuoc trong socketModule deu phan giai duoc`() {
        socketModule.verify(
            extraTypes = listOf(
                Context::class,
                Application::class,
                Long::class,
                Double::class,
                Random::class,
                OkHttpClient.Builder::class,
            ),
        )
    }
}
