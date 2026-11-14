package com.nexus.psp.emulator

import android.content.Context
import android.os.Debug
import java.io.File

/**
 * Журнал рывков: кадры, которые пришли позже 25 мс (при 60 к/с норма — 16,7 мс).
 * Пишется только при включённом счётчике кадров и уходит в «Отправить отчёт об ошибке» — игроку ничего не показываем.
 * Для каждого рывка: когда, сколько длился, была ли в этот момент сборка мусора и проверка памяти для автосмены раскладок.
 */
class Jank {
    private val t0 = System.nanoTime()
    private var last = 0L
    private var gc = gcCount()
    private var frames = 0L
    private val spikes = ArrayList<String>()
    private val at = ArrayList<Long>()

    private fun gcCount() = try { Debug.getRuntimeStat("art.gc.gc-count")?.toLongOrNull() ?: 0L } catch (e: Throwable) { 0L }

    fun frame(autoAt: Long) {
        val now = System.nanoTime(); frames++
        val dt = now - last; last = now
        if (dt !in 25_000_000L..1_000_000_000L || frames < 120) return
        val g = gcCount(); val gcHit = g != gc; gc = g
        val auto = autoAt != 0L && now - autoAt < 150_000_000L
        if (spikes.size < 300) {
            spikes += "%.1f с: %d мс%s%s".format((now - t0) / 1e9, dt / 1_000_000, if (gcHit) " · сборка мусора" else "", if (auto) " · автосмена раскладки" else "")
            at += now
        }
    }

    fun save(c: Context, head: String) {
        if (spikes.isEmpty()) { File(c.filesDir, "jank.txt").delete(); return }
        val gaps = at.zipWithNext { a, b -> (b - a) / 1e9 }.sorted()
        val med = gaps.getOrNull(gaps.size / 2)
        val secs = (System.nanoTime() - t0) / 1e9
        val txt = buildString {
            appendLine("Плавность: $head")
            appendLine("Игра шла %.0f с, кадров %d, рывков %d%s".format(secs, frames, spikes.size, med?.let { " · обычно раз в %.1f с".format(it) } ?: ""))
            spikes.take(120).forEach { appendLine(it) }
        }
        try { File(c.filesDir, "jank.txt").writeText(txt) } catch (e: Exception) { }
    }

    companion object {
        fun log(c: Context): String? = File(c.filesDir, "jank.txt").takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
        fun clear(c: Context) { File(c.filesDir, "jank.txt").delete() }
    }
}
