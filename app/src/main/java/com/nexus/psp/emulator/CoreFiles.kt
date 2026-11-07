package com.nexus.psp.emulator

import android.content.Context
import android.util.Log
import java.io.File
import java.util.zip.ZipInputStream

/** Папки ядра и распаковка системных файлов PPSSPP (шрифты, flash0, compat.ini) из assets. */
object CoreFiles {
    private const val TAG = "KapsulaCore"
    private const val SYSTEM_ZIP = "ppsspp/PPSSPP.zip"

    fun systemDir(c: Context) = File(c.filesDir, "system").apply { mkdirs() }
    fun savesDir(c: Context) = File(c.filesDir, "saves").apply { mkdirs() }
    fun statesDir(c: Context) = File(c.filesDir, "states").apply { mkdirs() }
    fun gamesDir(c: Context) = File(c.getExternalFilesDir(null) ?: c.filesDir, "games").apply { mkdirs() }

    /** Распаковывает PPSSPP.zip в system/ один раз (и заново, если архив в APK поменялся). */
    fun ensureSystem(c: Context): Boolean = try {
        val sys = systemDir(c)
        val stamp = File(sys, ".kapsula-ppsspp")
        val want = (try { c.assets.openFd(SYSTEM_ZIP).use { it.length.toString() } } catch (e: Exception) { "?" }) + ":" + appVersion(c)
        if (stamp.exists() && stamp.readText() == want && File(sys, "PPSSPP").isDirectory) true
        else {
            val base = sys.canonicalPath + File.separator
            ZipInputStream(c.assets.open(SYSTEM_ZIP).buffered()).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    val out = File(sys, e.name)
                    if (!out.canonicalPath.startsWith(base)) continue // защита от путей «../» в архиве
                    if (e.isDirectory) out.mkdirs() else { out.parentFile?.mkdirs(); out.outputStream().use { z.copyTo(it) } }
                }
            }
            stamp.writeText(want)
            true
        }
    } catch (e: Exception) { Log.e(TAG, "ensureSystem", e); false }

    private fun appVersion(c: Context): String =
        try { androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(c.packageManager.getPackageInfo(c.packageName, 0)).toString() } catch (e: Exception) { "0" }
}
