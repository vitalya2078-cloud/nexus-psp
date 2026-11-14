package com.nexus.psp.data

import android.content.Context
import java.io.File

/**
 * Журнал падений — только на телефоне, никуда не отправляется.
 * Последняя ошибка каждого процесса (главный экран, игра) сохраняется в files/crash_<процесс>.txt,
 * её можно посмотреть и скопировать в «Настройки → О приложении».
 */
object CrashLog {
    fun install(c: Context, proc: String) {
        val f = File(c.filesDir, "crash_$proc.txt")
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val ver = try { c.packageManager.getPackageInfo(c.packageName, 0).versionName } catch (x: Exception) { "?" }
                f.writeText("KAPSULA $ver · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.RELEASE}\n" +
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()) + " · поток ${t.name}\n\n" + e.stackTraceToString().take(12000))
            } catch (x: Exception) { }
            prev?.uncaughtException(t, e)
        }
    }

    /** Последняя ошибка (самая свежая из процессов) или null. */
    fun last(c: Context): File? = listOf("main", "game").map { File(c.filesDir, "crash_$it.txt") }.filter { it.exists() }.maxByOrNull { it.lastModified() }

    fun clear(c: Context) { listOf("main", "game").forEach { File(c.filesDir, "crash_$it.txt").delete() } }
}
