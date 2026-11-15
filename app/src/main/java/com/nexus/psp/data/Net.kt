package com.nexus.psp.data

import android.content.Context
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Semaphore

/**
 * Картинки из сети (значки достижений, аватары) с кэшем на диске.
 * Скачивается во временный файл и переименовывается только целиком: оборванная загрузка не застревает в кэше.
 * Не больше 4 загрузок разом (сервер не принимает пачку за бота). Картинки RetroAchievements идут с подписью
 * приложения, как и запросы rcheevos; если сервер не отдал — через наш запасной адрес (функция img в Supabase).
 * Причины неудач — в журнал и в файл net_fail.txt (уходит в отчёт об ошибке), игроку не показываем.
 */
object Net {
    private const val TAG = "KapsulaNet"
    const val UA_RA = "KAPSULA/0.31 (Android) rcheevos/12.5"
    private const val PROXY = "https://borubfgvjhntepnijxxo.supabase.co/functions/v1/img?u="
    private val gate = Semaphore(4)

    private fun isRa(url: String) = url.startsWith("https://media.retroachievements.org/") || url.startsWith("https://i.retroachievements.org/")

    /** Адреса по очереди: прямой, второй сервер RetroAchievements, запасной через Supabase. */
    internal fun alternates(url: String): List<String> = if (!isRa(url)) listOf(url) else listOf(
        url,
        url.replace("https://media.", "https://i."),
        PROXY + URLEncoder.encode(url, "UTF-8"),
    ).distinct()

    fun cached(c: Context, url: String, dir: String): File? {
        val f = File(c.cacheDir, dir + "/" + url.hashCode().toUInt().toString(16) + "_" + url.substringAfterLast('/').take(40))
        if (f.exists() && f.length() > 0) return f
        f.parentFile?.mkdirs()
        val errors = ArrayList<String>()
        gate.acquire()
        try {
            if (f.exists() && f.length() > 0) return f
            for (u in alternates(url)) {
                val tmp = File(f.path + ".part")
                try {
                    val con = (URL(u).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000; readTimeout = 12000; instanceFollowRedirects = true
                        setRequestProperty("User-Agent", if (isRa(url)) UA_RA else "KAPSULA")
                    }
                    try {
                        val code = con.responseCode
                        if (code != 200) { errors += "${host(u)} HTTP $code"; continue }
                        con.inputStream.use { i -> tmp.outputStream().use { i.copyTo(it) } }
                    } finally { con.disconnect() }
                    if (tmp.length() > 0 && tmp.renameTo(f)) return f
                    errors += "${host(u)} пустой ответ"
                } catch (e: Exception) {
                    errors += "${host(u)} ${e.javaClass.simpleName}: ${e.message}"
                } finally { tmp.delete() }
            }
        } finally { gate.release() }
        fail(c, url, errors)
        return null
    }

    private fun host(u: String) = if (u.startsWith(PROXY)) "запасной" else u.substringAfter("://").substringBefore('/')

    private fun fail(c: Context, url: String, errors: List<String>) {
        val line = "${java.text.SimpleDateFormat("dd.MM HH:mm:ss", java.util.Locale.US).format(java.util.Date())} ${url.substringAfterLast('/')}: ${errors.joinToString("; ")}"
        Log.w(TAG, line)
        try {
            val log = File(c.filesDir, "net_fail.txt")
            val old = if (log.exists()) log.readLines().takeLast(19) else emptyList()
            log.writeText((old + line).joinToString("\n") + "\n")
        } catch (e: Exception) { }
    }

    /** Последние сбои загрузки картинок — для отчёта об ошибке. */
    fun failLog(c: Context): String? = File(c.filesDir, "net_fail.txt").takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
    fun clearFailLog(c: Context) { File(c.filesDir, "net_fail.txt").delete() }
}
