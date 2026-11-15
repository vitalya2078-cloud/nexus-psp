package com.nexus.psp.emulator

import android.app.Activity
import android.os.Build
import android.os.PerformanceHintManager
import android.util.Log
import android.view.Display
import java.io.File

/**
 * Плавность игры (только журнал, игроку ничего не показываем):
 *  - экран на время игры переключается на 60 Гц: игры выдают 60 кадров, и на 120/144 Гц движок
 *    сбивается с вертикальной синхронизации — отсюда периодические рывки;
 *  - самые занятые потоки эмуляции сообщаются системе (ADPF, Android 12+): она держит их на больших ядрах
 *    и поднимает частоты, а не отправляет на экономные ядра.
 */
class Perf(private val a: Activity) {
    private val tag = "KapsulaPerf"
    private var session: PerformanceHintManager.Session? = null
    private var last = 0L

    // ---------- частота экрана ----------
    /** Просим у системы режим экрана 60 Гц с тем же разрешением. true — такой режим есть. */
    fun request60Hz(): Boolean {
        if (Build.VERSION.SDK_INT < 23) return false
        val d = display() ?: return false
        val cur = d.mode
        val m = d.supportedModes.filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
            .minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }?.takeIf { kotlin.math.abs(it.refreshRate - 60f) < 1f } ?: return false
        if (kotlin.math.abs(cur.refreshRate - 60f) < 1f) return true
        a.window.attributes = a.window.attributes.apply { preferredDisplayModeId = m.modeId }
        Log.i(tag, "экран ${cur.refreshRate} Гц → прошу ${m.refreshRate} Гц")
        return true
    }

    fun refreshRate(): Float = display()?.refreshRate ?: 60f

    /** Частоты экрана, которые умеет телефон (для журнала плавности). */
    fun modes(): String = try { display()?.supportedModes?.map { it.refreshRate.toInt() }?.distinct()?.sorted()?.joinToString("/") ?: "?" } catch (e: Exception) { "?" }


    /** Ждём, пока экран действительно переключится (движок читает частоту один раз при запуске). */
    suspend fun await60Hz(timeoutMs: Long = 900) {
        val t0 = System.currentTimeMillis()
        while (kotlin.math.abs(refreshRate() - 60f) >= 1f && System.currentTimeMillis() - t0 < timeoutMs) kotlinx.coroutines.delay(50)
        Log.i(tag, "частота экрана при запуске: ${refreshRate()} Гц")
    }

    @Suppress("DEPRECATION")
    private fun display(): Display? = try { if (Build.VERSION.SDK_INT >= 30) a.display else a.windowManager.defaultDisplay } catch (e: Exception) { null }

    // ---------- большие ядра ----------
    /** Создать подсказку для самых занятых потоков процесса (вызывать, когда игра уже идёт). */
    fun startHints(extraTid: Int) {
        if (Build.VERSION.SDK_INT < 31 || session != null) return
        try {
            val pm = a.getSystemService(PerformanceHintManager::class.java) ?: return
            if (pm.preferredUpdateRateNanos < 0) return
            val busy = busiestThreads(3)
            val tids = (listOf(extraTid).filter { it > 0 } + busy.map { it.first }).distinct().take(4).toIntArray()
            if (tids.isEmpty()) return
            session = pm.createHintSession(tids, T.TARGET_NS)
            Log.i(tag, "подсказка производительности: " + busy.joinToString { "${it.second}(${it.first})" } + " +gl($extraTid)")
        } catch (e: Throwable) { Log.w(tag, "подсказка не создана: $e") }
    }

    /** Каждый кадр: сколько он занял на самом деле — система поднимает частоты, если не успеваем. */
    fun frame() {
        val s = session ?: return
        val now = System.nanoTime()
        val d = now - last; last = now
        if (d in 1_000_000L..200_000_000L) try { s.reportActualWorkDuration(d) } catch (e: Throwable) { }
    }

    fun close() { try { session?.close() } catch (e: Throwable) { }; session = null }

    /** Потоки процесса, больше всех занятые процессором за полсекунды: (tid, имя). */
    private fun busiestThreads(n: Int): List<Pair<Int, String>> {
        fun snap(): Map<Int, Long> = (File("/proc/self/task").listFiles() ?: emptyArray()).mapNotNull { t ->
            val tid = t.name.toIntOrNull() ?: return@mapNotNull null
            try {
                val st = File(t, "stat").readText().substringAfterLast(") ").split(' ')
                tid to (st[11].toLong() + st[12].toLong())   // utime + stime
            } catch (e: Exception) { null }
        }.toMap()
        val s1 = snap(); Thread.sleep(500); val s2 = snap()
        return s2.mapNotNull { (tid, v) -> s1[tid]?.let { tid to (v - it) } }.filter { it.second > 0 }.sortedByDescending { it.second }.take(n)
            .map { (tid, _) -> tid to (try { File("/proc/self/task/$tid/comm").readText().trim() } catch (e: Exception) { "?" }) }
    }

    object T { const val TARGET_NS = 16_666_667L }
}
