package com.nexus.psp

import com.nexus.psp.data.Net
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Значки достижений: прямой адрес, второй сервер и запасной путь через Supabase. */
class NetTest {
    @Test fun badgeHasFallbacks() {
        val u = "https://media.retroachievements.org/Badge/12345_lock.png"
        val l = Net.alternates(u)
        assertEquals(u, l[0])
        assertEquals("https://i.retroachievements.org/Badge/12345_lock.png", l[1])
        assertTrue(l[2].startsWith("https://borubfgvjhntepnijxxo.supabase.co/functions/v1/img?u=https%3A%2F%2Fmedia.retroachievements.org%2FBadge%2F12345_lock.png"))
    }

    @Test fun otherHostsGoDirect() {
        assertEquals(listOf("https://example.com/a.png"), Net.alternates("https://example.com/a.png"))
    }
}
