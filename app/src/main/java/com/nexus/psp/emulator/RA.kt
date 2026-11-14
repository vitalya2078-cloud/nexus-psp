package com.nexus.psp.emulator

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Достижения RetroAchievements: rcheevos (rc_client) в нативной сборке LibretroDroid KAPSULA.
 * Нативная часть проверяет условия после каждого кадра; HTTP-запросы к серверу выполняет этот класс.
 * Режим «мягкий» (softcore): ускорение, сохранения и читы не блокируются.
 */
object RA {
    const val UA = "KAPSULA/0.23 (Android) rcheevos/12.5"
    private val io = Executors.newFixedThreadPool(2)

    @JvmStatic external fun nInit(bridge: Bridge)
    @JvmStatic external fun nResponse(id: Int, body: String?, status: Int)
    @JvmStatic external fun nLoginToken(user: String, token: String)
    @JvmStatic external fun nLoadGame(hash: String)
    @JvmStatic external fun nList(): String?
    @JvmStatic external fun nReset()
    @JvmStatic external fun nUnload()

    fun ensureLoaded() { try { System.loadLibrary("libretrodroid") } catch (e: Throwable) { Log.w("KapsulaRA", "load", e) } }

    /** Запрос к серверу RetroAchievements (общий для приложения и для rcheevos). */
    fun request(url: String, post: String?, contentType: String? = null): Pair<Int, String?> = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 30000
        c.setRequestProperty("User-Agent", UA)
        if (post != null) {
            c.requestMethod = "POST"; c.doOutput = true
            c.setRequestProperty("Content-Type", contentType ?: "application/x-www-form-urlencoded")
            c.outputStream.use { it.write(post.toByteArray()) }
        }
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }
        code to body
    } catch (e: Exception) { Log.w("KapsulaRA", "http", e); 0 to null }

    /** Вход по логину и паролю (из настроек приложения): токен сохраняется, пароль — нет. */
    fun login(user: String, password: String): Result<Pair<String, Int>> {
        val (code, body) = request("https://retroachievements.org/dorequest.php",
            "r=login2&u=" + java.net.URLEncoder.encode(user, "UTF-8") + "&p=" + java.net.URLEncoder.encode(password, "UTF-8"))
        if (body == null) return Result.failure(Exception(if (code == 0) "Нет связи с RetroAchievements" else "Ошибка сервера ($code)"))
        return try {
            val o = org.json.JSONObject(body)
            if (o.optBoolean("Success")) Result.success(o.getString("Token") to o.optInt("Score"))
            else Result.failure(Exception(if (o.optString("Code") == "invalid_credentials") "Неверное имя пользователя или пароль" else o.optString("Error").ifEmpty { "Не удалось войти" }))
        } catch (e: Exception) { Result.failure(Exception("Непонятный ответ сервера")) }
    }

    /** Мост для нативного кода: методы вызываются из rcheevos (сигнатуры заданы в kapsula_ra.cpp). */
    class Bridge(
        val onLogin: (Boolean, String?, String?) -> Unit,
        val onGame: (Boolean, String?, String?, Int, Int, Int, Int) -> Unit,
        val onEvent: (Int, Int, String?, String?, String?, Int) -> Unit,
    ) {
        fun http(id: Int, url: String, post: String?, contentType: String?) {
            io.execute { val (code, body) = request(url, post, contentType); nResponse(id, body, code) }
        }
        fun event(type: Int, id: Int, title: String?, desc: String?, badge: String?, points: Int) = onEvent(type, id, title, desc, badge, points)
        @Suppress("UNUSED_PARAMETER")
        fun loginDone(ok: Boolean, error: String?, token: String?, name: String?, score: Int) = onLogin(ok, error, name)
        fun gameDone(ok: Boolean, error: String?, title: String?, total: Int, unlocked: Int, points: Int, pointsUnlocked: Int) =
            onGame(ok, error, title, total, unlocked, points, pointsUnlocked)
    }

    data class Ach(val id: Int, val title: String, val desc: String, val points: Int, val unlocked: Boolean, val badge: String, val progress: String)

    fun list(): List<Ach> = (try { nList() } catch (e: Throwable) { null }).orEmpty().lines().filter { it.isNotBlank() }.mapNotNull { l ->
        val p = l.split('\t'); if (p.size < 7) null
        else Ach(p[0].toIntOrNull() ?: 0, p[1], p[2], p[3].toIntOrNull() ?: 0, p[4] == "1", p[5], p[6])
    }

    fun badgeUrl(badge: String, locked: Boolean) = "https://media.retroachievements.org/Badge/$badge${if (locked) "_lock" else ""}.png"
}
