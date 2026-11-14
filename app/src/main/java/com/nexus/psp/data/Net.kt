package com.nexus.psp.data

import android.content.Context
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Картинки из сети (значки достижений, аватары) с кэшем на диске.
 * Скачивается во временный файл и переименовывается только целиком: оборванная загрузка не застревает в кэше.
 * Для значков достижений — запасной адрес сервера. Причины неудач — только в журнал.
 */
object Net {
    private const val TAG = "KapsulaNet"

    private fun alternates(url: String): List<String> = when {
        url.startsWith("https://media.retroachievements.org/") -> listOf(url, url.replace("https://media.", "https://i."), url.replace("https://media.retroachievements.org/", "https://retroachievements.org/"))
        else -> listOf(url)
    }

    fun cached(c: Context, url: String, dir: String): File? {
        val f = File(c.cacheDir, dir + "/" + url.hashCode().toUInt().toString(16) + "_" + url.substringAfterLast('/').take(40))
        if (f.exists() && f.length() > 0) return f
        f.parentFile?.mkdirs()
        for (u in alternates(url)) {
            repeat(2) { attempt ->
                val tmp = File(f.path + ".part")
                try {
                    val con = (URL(u).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000; readTimeout = 12000; instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "KAPSULA")
                    }
                    try {
                        val code = con.responseCode
                        if (code != 200) { Log.w(TAG, "$u → HTTP $code"); return@repeat }
                        con.inputStream.use { i -> tmp.outputStream().use { i.copyTo(it) } }
                    } finally { con.disconnect() }
                    if (tmp.length() > 0 && tmp.renameTo(f)) return f
                } catch (e: Exception) {
                    Log.w(TAG, "$u (попытка ${attempt + 1}): ${e.javaClass.simpleName} ${e.message}")
                } finally { tmp.delete() }
            }
        }
        return null
    }
}
