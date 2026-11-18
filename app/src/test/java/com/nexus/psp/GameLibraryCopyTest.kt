package com.nexus.psp

import com.nexus.psp.emulator.GameLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

/** Проверки безопасного копирования больших игровых образов. */
class GameLibraryCopyTest {
    private fun tempFile(): File = File.createTempFile("kapsula-import-", ".part").apply { delete() }

    @Test fun copiesCompleteStreamAndFinishesProgressAtOne() {
        val target = tempFile()
        val bytes = ByteArray(128 * 1024) { (it % 251).toByte() }
        val progress = mutableListOf<Double>()

        val copied = GameLibrary.copyStreamToFile(
            ByteArrayInputStream(bytes), target, bytes.size.toLong(), progress::add
        )

        assertEquals(bytes.size.toLong(), copied)
        assertTrue(target.exists())
        assertEquals(bytes.toList(), target.readBytes().toList())
        assertEquals(1.0, progress.last(), 0.0)
        target.delete()
    }

    @Test fun rejectsTruncatedStreamAndRemovesPartialFile() {
        val target = tempFile()
        val error = org.junit.Assert.assertThrows(IOException::class.java) {
            GameLibrary.copyStreamToFile(ByteArrayInputStream(ByteArray(64)), target, 128L) {}
        }

        assertTrue(error.message!!.contains("не полностью"))
        assertFalse(target.exists())
    }

    @Test fun rejectsEmptyStream() {
        val target = tempFile()
        org.junit.Assert.assertThrows(IOException::class.java) {
            GameLibrary.copyStreamToFile(ByteArrayInputStream(ByteArray(0)), target, -1L) {}
        }
        assertFalse(target.exists())
    }

    @Test fun unknownSourceSizeDoesNotBlockCompleteCopy() {
        val target = tempFile()
        val bytes = "unknown-size-source".toByteArray()
        val copied = GameLibrary.copyStreamToFile(
            ByteArrayInputStream(bytes), target, -1L
        ) {}
        assertEquals(bytes.size.toLong(), copied)
        assertEquals(bytes.toList(), target.readBytes().toList())
        target.delete()
    }
}
