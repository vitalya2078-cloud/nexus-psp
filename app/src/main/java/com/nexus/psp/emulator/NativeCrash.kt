package com.nexus.psp.emulator

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Падения нативной части (ядро, графика) и «приложение не отвечает»: Java-журнал (CrashLog) их не видит.
 *  - процесс игры ставит обработчик сигналов (kapsula_crash.cpp) — он пишет место падения в crash_game.txt;
 *  - при следующем запуске главный процесс спрашивает Android о причине прошлых закрытий (Android 11+)
 *    и дописывает в журнал «не отвечает» с трассой главного потока.
 */
object NativeCrash {
    @JvmStatic external fun nInit(path: String, head: String)

    private fun head(c: Context): String {
        val ver = try { c.packageManager.getPackageInfo(c.packageName, 0).versionName } catch (e: Exception) { "?" }
        return "KAPSULA $ver · ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"
    }

    /** Процесс игры: после загрузки библиотеки эмулятора. */
    fun install(c: Context) {
        try { System.loadLibrary("libretrodroid"); nInit(File(c.filesDir, "crash_game.txt").path, head(c)) } catch (e: Throwable) { Log.w("KapsulaCrash", "init", e) }
    }

    /** Главный процесс при старте: «не отвечает» и падения, о которых сообщила система, — в журнал. */
    fun collectExitReasons(c: Context) {
        if (Build.VERSION.SDK_INT < 30) return
        try {
            val sp = c.getSharedPreferences("crash", Context.MODE_PRIVATE)
            val seen = sp.getLong("exitSeen", 0L)
            val am = c.getSystemService(ActivityManager::class.java) ?: return
            val list = am.getHistoricalProcessExitReasons(c.packageName, 0, 8).filter { it.timestamp > seen }
            for (e in list.sortedBy { it.timestamp }) {
                val proc = if (e.processName.endsWith(":game")) "game" else "main"
                val f = File(c.filesDir, "crash_$proc.txt")
                // наш обработчик уже записал это падение подробнее
                if (f.exists() && f.lastModified() >= e.timestamp - 5_000) continue
                val what = when (e.reason) {
                    ApplicationExitInfo.REASON_ANR -> "приложение не отвечало"
                    ApplicationExitInfo.REASON_CRASH_NATIVE -> "сбой эмулятора"
                    ApplicationExitInfo.REASON_LOW_MEMORY -> "системе не хватило памяти"
                    else -> continue
                }
                val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(e.timestamp))
                val trace = if (e.reason == ApplicationExitInfo.REASON_ANR) try {
                    e.traceInputStream?.bufferedReader()?.use { r -> mainThread(r.readText()) }
                } catch (x: Exception) { null } else null
                f.writeText("${head(c)}\n$date · $what (${if (proc == "game") "игра" else "главный экран"})\n\n" +
                    (e.description ?: "") + "\n\n" + (trace ?: "Подробностей система не сохранила.") )
            }
            list.maxOfOrNull { it.timestamp }?.let { sp.edit().putLong("exitSeen", it).apply() }
        } catch (e: Exception) { Log.w("KapsulaCrash", "exit reasons", e) }
    }

    /** Из трассы «не отвечает» — только главный поток: там видно, на чём он завис. */
    private fun mainThread(t: String): String {
        val i = t.indexOf("\"main\"")
        if (i < 0) return t.take(8000)
        val end = t.indexOf("\n\n", i).let { if (it < 0) t.length else it }
        return t.substring(i, end).take(8000)
    }
}
