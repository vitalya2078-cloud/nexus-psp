package com.nexus.psp.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Проверка обновлений без магазина: раз в 12 часов смотрим последний выпуск на GitHub
 * (в его названии — версия, её пишет сборка). Новее установленной — уведомление «Вышла KAPSULA X».
 */
object Update {
    const val APK = "https://github.com/vitalya2078-cloud/nexus-psp/releases/download/latest/kapsula.apk"
    private const val API = "https://api.github.com/repos/vitalya2078-cloud/nexus-psp/releases/tags/latest"
    private const val EVERY = 12 * 3600_000L

    /** Версия из названия выпуска: «KAPSULA 0.36.4» → 0.36.4. */
    fun versionOf(title: String): String? = Regex("""(\d+)\.(\d+)(?:\.(\d+))?""").find(title)?.value

    /** a новее b (0.36.10 > 0.36.9). */
    fun newer(a: String, b: String): Boolean {
        val x = a.split('.').map { it.toIntOrNull() ?: 0 }; val y = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) { val p = x.getOrElse(i) { 0 }; val q = y.getOrElse(i) { 0 }; if (p != q) return p > q }
        return false
    }

    /** Последняя версия на GitHub (null — нет сети или выпуск без версии). */
    fun latest(): String? = try {
        val c = URL(API).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        c.setRequestProperty("Accept", "application/vnd.github+json"); c.setRequestProperty("User-Agent", "KAPSULA")
        if (c.responseCode != 200) null else versionOf(JSONObject(c.inputStream.bufferedReader().readText()).optString("name"))
    } catch (e: Exception) { null }

    /** Проверить (force — сразу, по кнопке). Ответ: новая версия или null. */
    fun check(c: Context, force: Boolean = false): String? {
        val sp = c.getSharedPreferences("kapsula", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!force && now - sp.getLong("updChecked", 0L) < EVERY) return null
        val v = latest() ?: return null
        sp.edit().putLong("updChecked", now).apply()
        val cur = com.nexus.psp.BuildConfig.VERSION_NAME
        return v.takeIf { newer(it, cur) }
    }

    /** Уведомление о новой версии — один раз на версию. */
    fun announce(c: Context, v: String) {
        val sp = c.getSharedPreferences("kapsula", Context.MODE_PRIVATE)
        if (sp.getString("updSaid", "") == v) return
        sp.edit().putString("updSaid", v).apply()
        AppState.alerts.add(0, Alert("Вышла KAPSULA $v", "Нажми, чтобы скачать. Ставится поверх — игры и сохранения останутся", true, G))
        AppState.save()
    }

    /** Метка уведомления об обновлении (вместо id игры). */
    const val G = "update"

    fun download(c: Context) = try {
        c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(APK)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) { AppState.say("Не получилось открыть браузер") }
}
