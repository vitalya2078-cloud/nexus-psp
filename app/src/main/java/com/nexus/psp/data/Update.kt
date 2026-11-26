package com.nexus.psp.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Проверка обновлений без магазина. Спрашиваем обе площадки и берём самую новую версию:
 * сначала GitVerse (открывается без VPN, оттуда же и скачивание), затем GitHub (если зеркало отстало или недоступно).
 * Версия — в названии выпуска («KAPSULA 0.44.5»), её пишет сборка.
 * Автоматически — при открытии KAPSULA и в фоне (InboxJob), не чаще раза в 6 часов; новая версия — колокольчик + плашка,
 * а если KAPSULA закрыта — системное уведомление (тап — скачать). Каждая версия объявляется один раз.
 */
object Update {
    const val APK = "https://github.com/vitalya2078-cloud/nexus-psp/releases/download/latest/kapsula.apk"
    private const val API = "https://api.github.com/repos/vitalya2078-cloud/nexus-psp/releases/tags/latest"
    private const val GV_API = "https://gitverse.ru/api/repos/kapsula/KAPSULA/releases?limit=1"
    /** Сайт KAPSULA (GitVerse Pages): версия и APK рядом со страницей — без VPN и без лимита GitVerse для гостей (с 0.50). */
    private const val SITE = "https://kapsula.gitverse.site/"
    private const val EVERY = 6 * 3600_000L

    /** Версия и откуда её скачать. */
    class Rel(val ver: String, val url: String, val mirror: Boolean)

    /** Версия из названия выпуска: «KAPSULA 0.36.4» → 0.36.4. */
    fun versionOf(title: String): String? = Regex("""(\d+)\.(\d+)(?:\.(\d+))?""").find(title)?.value

    /** a новее b (0.36.10 > 0.36.9). */
    fun newer(a: String, b: String): Boolean {
        val x = a.split('.').map { it.toIntOrNull() ?: 0 }; val y = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) { val p = x.getOrElse(i) { 0 }; val q = y.getOrElse(i) { 0 }; if (p != q) return p > q }
        return false
    }

    private fun get(url: String): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        c.setRequestProperty("Accept", "application/json"); c.setRequestProperty("User-Agent", "KAPSULA")
        if (c.responseCode != 200) null else c.inputStream.bufferedReader().readText()
    } catch (e: Exception) { null }

    /** Зеркало GitVerse: последний выпуск и прямая ссылка на APK (без входа). */
    fun fromGitVerse(): Rel? = try {
        val r = JSONObject(get(GV_API) ?: "").optJSONArray("data")?.optJSONObject(0)
        val v = r?.let { versionOf(it.optString("title")) ?: versionOf(it.optString("tagName")) }
        val a = r?.optJSONArray("attachments")?.let { arr -> (0 until arr.length()).map { arr.getJSONObject(it) }.firstOrNull { it.optString("name").endsWith(".apk") } }
        if (v != null && a != null) Rel(v, a.optString("downloadUrl"), true) else null
    } catch (e: Exception) { null }

    /** Сайт: latest.json → версия и ссылка на APK (обычно kapsula.apk рядом). */
    fun fromSite(): Rel? = try {
        val o = JSONObject(get(SITE + "latest.json") ?: "")
        val v = versionOf(o.optString("ver")); val u = o.optString("url")
        if (v != null && u.isNotEmpty()) Rel(v, if (u.startsWith("http")) u else SITE + u, true) else null
    } catch (e: Exception) { null }

    fun fromGitHub(): Rel? = try {
        versionOf(JSONObject(get(API) ?: "").optString("name"))?.let { Rel(it, APK, false) }
    } catch (e: Exception) { null }

    /** Самая новая из двух площадок (при равных — зеркало: качается без VPN). */
    fun latestRel(): Rel? {
        if (android.os.Build.FINGERPRINT == "robolectric") return null
        // сайт первым: с выпусков GitVerse гостям качать не дают (429), сайт отдаёт APK без ограничений
        val gv = fromSite() ?: fromGitVerse(); val gh = fromGitHub()
        return when {
            gv == null -> gh
            gh == null -> gv
            newer(gh.ver, gv.ver) -> gh
            else -> gv
        }
    }

    /** Последняя версия (null — нет сети). */
    fun latest(c: Context? = null): String? = latestRel()?.also { r -> c?.let { remember(it, r) } }?.ver

    private fun prefs(c: Context) = c.getSharedPreferences("kapsula", Context.MODE_PRIVATE)
    private fun remember(c: Context, r: Rel) { prefs(c).edit().putString("updLatest", r.ver).putString("updUrl", r.url).apply() }

    /** Проверить (force — сразу, по кнопке). Ответ: новая версия или null. Работает и из фона (без экрана). */
    fun check(c: Context, force: Boolean = false): String? {
        val sp = prefs(c)
        val now = System.currentTimeMillis()
        if (!force && now - sp.getLong("updChecked", 0L) < EVERY) return pending(c)
        val r = latestRel() ?: return pending(c)
        sp.edit().putLong("updChecked", now).apply()
        remember(c, r)
        return pending(c)
    }

    /** Известная новая версия (уже найденная проверкой, в т. ч. фоновой) или null. */
    fun pending(c: Context): String? = prefs(c).getString("updLatest", null)?.takeIf { newer(it, com.nexus.psp.BuildConfig.VERSION_NAME) }

    /** Колокольчик — один раз на версию. */
    fun announce(c: Context, v: String): Boolean {
        val sp = prefs(c)
        if (sp.getString("updSaid", "") == v) return false
        sp.edit().putString("updSaid", v).apply()
        AppState.alerts.add(0, Alert("Вышла KAPSULA $v", "Нажми, чтобы скачать. Ставится поверх — игры и сохранения останутся", true, G))
        AppState.save()
        return true
    }

    /** Фон: системное уведомление — один раз на версию. */
    fun notifyInBackground(c: Context) {
        val v = check(c) ?: return
        val sp = prefs(c)
        if (sp.getString("updNotified", "") == v || sp.getString("updSaid", "") == v) return
        sp.edit().putString("updNotified", v).apply()
        Inbox.notify(c, 300001, "Вышла KAPSULA $v", "Нажми, чтобы скачать. Ставится поверх — игры и сохранения останутся", friends = false, open = "update")
    }

    /** Метка уведомления об обновлении (вместо id игры). */
    const val G = "update"

    /** Скачать: ссылка с площадки, где нашлась новая версия (GitVerse — без VPN), иначе GitHub. */
    fun download(c: Context) = try {
        val url = prefs(c).getString("updUrl", null)?.takeIf { it.startsWith("https://") } ?: APK
        c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) { AppState.say("Не получилось открыть браузер") }
}
