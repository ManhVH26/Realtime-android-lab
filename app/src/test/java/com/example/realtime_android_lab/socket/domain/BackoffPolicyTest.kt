package com.example.realtime_android_lab.socket.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * BÀI 1 — test cho quy tắc backoff (domain). Vì [BackoffPolicy] là hàm thuần nên
 * test được mà không cần chạy socket hay đợi thời gian thật.
 */
class BackoffPolicyTest {

    @Test
    fun `tran tang gap doi cho toi khi cham cap`() {
        val backoff = BackoffPolicy(baseMs = 500, capMs = 30_000, factor = 2.0)
        assertEquals(500L, backoff.ceilingFor(1))
        assertEquals(1_000L, backoff.ceilingFor(2))
        assertEquals(2_000L, backoff.ceilingFor(3))
        assertEquals(4_000L, backoff.ceilingFor(4))
        // 500 * 2^9 = 256_000 nhưng bị chặn ở cap 30_000
        assertEquals(30_000L, backoff.ceilingFor(10))
    }

    @Test
    fun `delay khong bao gio vuot tran va khong am`() {
        val backoff = BackoffPolicy(baseMs = 500, capMs = 30_000, random = Random(42))
        repeat(1000) { i ->
            val attempt = (i % 12) + 1
            val delay = backoff.nextDelay(attempt)
            assertTrue(
                "delay=$delay phải trong [0, ${backoff.ceilingFor(attempt)}]",
                delay in 0..backoff.ceilingFor(attempt),
            )
        }
    }
}
