package com.nexus.psp

import com.nexus.psp.data.ago
import org.junit.Assert.assertEquals
import org.junit.Test

/** «Сколько прошло» в уведомлениях. */
class AgoTest {
    private val now = java.time.LocalDateTime.of(2026, 11, 15, 18, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun t(min: Long) = ago(now - min * 60_000, now)

    @Test fun texts() {
        assertEquals("только что", t(0))
        assertEquals("1 минуту назад", t(1))
        assertEquals("5 минут назад", t(5))
        assertEquals("22 минуты назад", t(22))
        assertEquals("10 часов назад", t(600))
        assertEquals("вчера", t(60 * 20))
        assertEquals("3 дня назад", t(60 * 24 * 3))
        assertEquals("на прошлой неделе", t(60 * 24 * 9))
        assertEquals("3 недели назад", t(60 * 24 * 22))
        assertEquals("месяц назад", t(60 * 24 * 40))
        assertEquals("5 месяцев назад", t(60 * 24 * 155))
        assertEquals("год назад", t(60 * 24 * 400))
        assertEquals("ранее", ago(0, now))
    }
}
