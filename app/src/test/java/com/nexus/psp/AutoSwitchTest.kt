package com.nexus.psp

import com.nexus.psp.emulator.AutoSwitch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** Обучение автопереключения на подставной памяти: находит флаг «в машине», игнорирует таймеры и шум. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutoSwitchTest {
    private val n = 64 * 1024
    private val ram = ByteArray(AutoSwitch.USER_OFF + n)
    private val read: (Int, Int) -> ByteArray? = { o, l -> ram.copyOfRange(o, o + l) }
    private val flag = AutoSwitch.USER_OFF + 12345
    private val rnd = Random(7)

    /** Кадр игры: шум в части памяти (таймеры, анимация), флаг — по состоянию. */
    private fun frame(inCar: Boolean) {
        repeat(3000) { ram[AutoSwitch.USER_OFF + rnd.nextInt(n)] = rnd.nextInt(256).toByte() }
        ram[flag] = if (inCar) 1 else 0
    }

    @Test fun learnsFlagAndDetects() {
        val l = AutoSwitch.Learner(n, read)
        frame(false); l.mark("foot"); frame(true); l.mark("drive")
        frame(false); l.mark("foot"); frame(true); l.mark("drive")
        frame(false); l.mark("foot"); frame(true); l.mark("drive")
        val r = l.result()
        assertNotNull(r)
        frame(true); assertEquals("drive", AutoSwitch.detect(r!!, read))
        frame(false); assertEquals("foot", AutoSwitch.detect(r, read))
    }

    @Test fun needsTwoMarksEach() {
        val l = AutoSwitch.Learner(n, read)
        frame(false); l.mark("foot"); frame(true); l.mark("drive")
        assertNull(l.result())
    }
}
