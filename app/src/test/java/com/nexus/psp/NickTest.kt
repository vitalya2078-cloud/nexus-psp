package com.nexus.psp

import com.nexus.psp.data.Nick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Правило имени — то же, что на сервере (kapsula_nick_ok). */
class NickTest {
    @Test fun valid() {
        listOf("Виталий", "Neo_X-1.2", "Себастьян", "Котик 2000", "  Пробел  ", "").forEach { assertNull(it, Nick.problem(it)) }
    }
    @Test fun invalid() {
        listOf("Вит", "12345", "два  пробела", "emoji😀", "ОченьДлинныйНикнейм17", "Пи з дец", "a@b#c!").forEach { assertNotNull(it, Nick.problem(it)) }
    }
    @Test fun creatorShort() {
        assertNull(Nick.problem("Вит", admin = true))
        assertEquals("Имя — от 5 символов", Nick.problem("Вит"))
    }
}
