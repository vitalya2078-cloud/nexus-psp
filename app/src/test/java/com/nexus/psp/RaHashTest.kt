package com.nexus.psp

import com.nexus.psp.emulator.Disc
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Хеш игры для RetroAchievements совпадает с rcheevos (rc_hash_generate_from_file, RC_CONSOLE_PSP) на тестовом образе. */
class RaHashTest {
    @Test fun matchesRcheevos() {
        val iso = File(javaClass.classLoader!!.getResource("ra-hash-test.iso")!!.toURI())
        // значение получено самим rcheevos v12 (gcc + rc_hash.c) на этом же файле
        assertEquals("a59ba64bae47d6c0d56800c3112193e1", Disc.raHash(iso))
    }
}
