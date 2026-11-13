package com.nexus.psp.emulator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemSearchTest {
    private val span = 4 * 1024 * 1024
    private val ram = ByteArray(AutoSwitch.USER_OFF + span)
    private val read: (Int, Int) -> ByteArray? = { o, l -> if (o + l <= ram.size) ram.copyOfRange(o, o + l) else null }
    private fun put32(off: Int, v: Int) { for (i in 0..3) ram[off + i] = (v ushr (8 * i)).toByte() }
    private fun put16(off: Int, v: Int) { ram[off] = v.toByte(); ram[off + 1] = (v ushr 8).toByte() }

    @Test fun narrowsMoneyToOneAddress() {
        val money = AutoSwitch.USER_OFF + 0x123450
        put32(money, 500)
        listOf(0x1000, 0x2000, 0x3000).forEach { put32(AutoSwitch.USER_OFF + it, 500) } // шум с тем же числом
        val s = MemSearch(read, span)
        assertEquals(4, s.first(500))
        put32(money, 750); put32(AutoSwitch.USER_OFF + 0x2000, 900)
        assertEquals(1, s.next("eq", 750))
        assertEquals(money, s.cands!![0])
        assertEquals(listOf(money to 750), s.current())
    }

    @Test fun filtersByDirection() {
        val hp = AutoSwitch.USER_OFF + 0x40
        put16(hp, 100); put16(AutoSwitch.USER_OFF + 0x80, 100); put16(AutoSwitch.USER_OFF + 0xC0, 100)
        val s = MemSearch(read, span).apply { size = 2 }
        assertEquals(3, s.first(100))
        put16(hp, 80); put16(AutoSwitch.USER_OFF + 0x80, 120)
        assertEquals(1, s.next("dec"))
        assertEquals(hp, s.cands!![0])
        assertEquals(1, s.next("same"))
    }

    @Test fun cwcheatFormat() {
        assertEquals("_L 0x20123450 0x000002EE", MemSearch.cwcheat(AutoSwitch.USER_OFF + 0x123450, 4, 750))
        assertEquals("_L 0x10000040 0x00000050", MemSearch.cwcheat(AutoSwitch.USER_OFF + 0x40, 2, 80))
        assertEquals("_L 0x00000001 0x000000FF", MemSearch.cwcheat(AutoSwitch.USER_OFF + 1, 1, 255))
        assertTrue(MemSearch.pspAddr(AutoSwitch.USER_OFF + 0x40) == "0x08800040")
    }
}
