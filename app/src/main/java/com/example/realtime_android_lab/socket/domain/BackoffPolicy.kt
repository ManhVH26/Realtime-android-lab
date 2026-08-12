package com.example.realtime_android_lab.socket.domain

import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * BÀI 1 — DOMAIN: quy tắc tính thời gian chờ giữa các lần reconnect.
 *
 * Đây là BUSINESS RULE thuần (không mạng, không thời gian hệ thống) nên nằm ở domain
 * và unit test được — xem BackoffPolicyTest.
 *
 * Công thức = exponential backoff + FULL JITTER:
 *   ceiling(attempt) = min(cap, base * factor^(attempt-1))
 *   delay            = random trong [0, ceiling]
 *
 * Jitter để chống thundering herd: cả vùng mất mạng rồi có lại cùng lúc thì hàng triệu
 * client sẽ reconnect đúng cùng một giây và đấm sập gateway; rải ngẫu nhiên làm mỏng cú đấm.
 *
 * RESET attempt về 0 khi kết nối thành công KHÔNG nằm ở đây — nó ở nơi giữ vòng lặp
 * (repository), vì policy chỉ là hàm tính số.
 */
class BackoffPolicy(
    private val baseMs: Long = 500L,
    private val capMs: Long = 30_000L,
    private val factor: Double = 2.0,
    private val random: Random = Random.Default,
) {
    /** Trần chờ của [attempt] (chưa cộng jitter). Tách ra để test phần xác định. */
    fun ceilingFor(attempt: Int): Long {
        require(attempt >= 1) { "attempt phải >= 1" }
        val exp = baseMs.toDouble() * factor.pow(attempt - 1)
        return min(capMs.toDouble(), exp).toLong().coerceAtLeast(1L)
    }

    /** Thời gian chờ thực tế cho lần thử thứ [attempt], đã rải jitter trong [0, ceiling]. */
    fun nextDelay(attempt: Int): Long = random.nextLong(ceilingFor(attempt) + 1)
}
