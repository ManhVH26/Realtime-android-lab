package com.example.realtime_android_lab.socket.domain.usecase

import com.example.realtime_android_lab.socket.domain.RealtimeRepository
import com.example.realtime_android_lab.socket.domain.model.ConnectionEvent
import kotlinx.coroutines.flow.Flow

/**
 * BÀI 1 — DOMAIN use cases.
 *
 * Ở app nhỏ thế này chúng khá mỏng (chỉ gọi lại repository), nhưng vẫn giữ vì:
 * - UI phụ thuộc use case, không phụ thuộc thẳng repository ⇒ đúng chiều Clean Architecture.
 * - Là chỗ đặt business rule sau này (vd: chặn gửi khi chưa đăng nhập, log, rate-limit)
 *   mà không phải sửa ViewModel.
 * Gộp vào một file cho gọn vì mỗi cái chỉ một dòng.
 */

class ObserveConnectionUseCase(private val repository: RealtimeRepository) {
    operator fun invoke(): Flow<ConnectionEvent> = repository.observe()
}

class ConnectUseCase(private val repository: RealtimeRepository) {
    operator fun invoke(url: String) = repository.connect(url)
}

class DisconnectUseCase(private val repository: RealtimeRepository) {
    operator fun invoke() = repository.disconnect()
}

class SendMessageUseCase(private val repository: RealtimeRepository) {
    operator fun invoke(text: String): Boolean = repository.send(text)
}
