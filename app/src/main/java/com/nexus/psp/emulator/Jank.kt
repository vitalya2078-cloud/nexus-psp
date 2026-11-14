package com.nexus.psp.emulator

import android.content.Context
import android.os.Debug
import java.io.File

/**
 * Журнал плавности: каждую секунду — сколько кадров посчитала игра (норма 60).
 * Пишется только при включённом счётчике кадров и уходит в «Отправить отчёт об ошибке» — игроку ничего не показываем.
 * Для просевших секунд: была ли сборка мусора и проверка памяти для автосмены раскладок.
 */
class Jank {
    private val t0 = System.nanoTime()
    private var gc = gcCount()
    private var secs = 0
    private var sum = 0L
    private val low = ArrayList<String>()
    private val lowAt = ArrayList<Int>()

    private fun gcCount() = try { Debug.getRuntimeStat("art.gc.gc-count")?.toLongOrNull() ?: 0L } catch (e: Throwable) { 0L }

    fun second(fps: Int, autoAt: Long) {
        secs++; sum += fps
        val g = gcCount(); val gcHit = g != gc; gc = g
        if (secs < 3 || fps >= 58 || fps <= 0) return
        val auto = autoAt != 0L && System.nanoTime() - autoAt < 1_000_000_000L
        if (low.size < 300) {
            low += "%d с: %d к/с%s%s".format((System.nanoTime() - t0) / 1_000_000_000L, fps, if (gcHit) " · сборка мусора" else "", if (auto) " · автосмена раскладки" else "")
            lowAt += secs
        }
    }

    fun save(c: Context, head: String) {
        if (secs < 5) return
        val gaps = lowAt.zipWithNext { a, b -> b - a }.filter { it > 1 }.sorted()
        val med = gaps.getOrNull(gaps.size / 2)
        val txt = buildString {
            appendLine("Плавность: $head")
            appendLine("Игра шла $secs с, в среднем ${sum / secs} к/с, просело секунд: ${low.size}${med?.let { " · обычно раз в $it с" } ?: ""}")
            low.take(150).forEach { appendLine(it) }
        }
        try { File(c.filesDir, "jank.txt").writeText(txt) } catch (e: Exception) { }
    }

    companion object {
        fun log(c: Context): String? = File(c.filesDir, "jank.txt").takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
        fun clear(c: Context) { File(c.filesDir, "jank.txt").delete() }
    }
}
