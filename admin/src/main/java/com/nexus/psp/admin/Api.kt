package com.nexus.psp.admin

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Связь с сервером админки (Supabase, функция kapsula-admin). Каждое действие проверяет сервер:
 * код аккаунта + PIN + этот телефон; 5 неверных PIN — пауза 15 минут; всё пишется в журнал.
 * На телефоне хранится только код аккаунта и случайный ID телефона; PIN — только в памяти, пока открыто приложение.
 */
object Api {
    const val URL_FN = "https://borubfgvjhntepnijxxo.supabase.co/functions/v1/kapsula-admin"

    var code = ""
        private set
    var pin = ""
    lateinit var device: String
        private set

    fun init(c: Context) {
        val sp = c.getSharedPreferences("admin", Context.MODE_PRIVATE)
        code = sp.getString("code", "") ?: ""
        device = sp.getString("device", null) ?: UUID.randomUUID().toString().also { sp.edit().putString("device", it).apply() }
    }

    fun remember(c: Context, newCode: String) {
        code = newCode.trim().uppercase()
        c.getSharedPreferences("admin", Context.MODE_PRIVATE).edit().putString("code", code).apply()
    }

    fun forget(c: Context) { code = ""; pin = ""; c.getSharedPreferences("admin", Context.MODE_PRIVATE).edit().remove("code").apply() }

    /** Ответ: data (JSONObject / JSONArray / null) или понятная ошибка. */
    class Res(val data: Any?, val error: String?) {
        val ok get() = error == null
        fun obj() = data as? JSONObject ?: JSONObject()
        fun arr() = data as? JSONArray ?: JSONArray()
    }

    /** Подменный ответ для снимков экранов в тестах (на настоящий сервер тесты не ходят). */
    var fake: ((String) -> Res)? = null

    fun call(action: String, args: JSONObject = JSONObject()): Res = fake?.invoke(action) ?: if (android.os.Build.FINGERPRINT == "robolectric") Res(null, "Тест: без сервера") else try {
        val c = URL(URL_FN).openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 10_000; c.readTimeout = 20_000
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        val body = JSONObject().put("code", code).put("pin", pin).put("device", device).put("action", action).put("args", args)
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val st = c.responseCode
        val txt = (if (st in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText() ?: ""
        val o = try { JSONObject(txt) } catch (e: Exception) { JSONObject() }
        if (o.optBoolean("ok")) Res(o.opt("data").takeIf { it != JSONObject.NULL }, null) else Res(null, words(o))
    } catch (e: Exception) { Res(null, "Нет связи с сервером. Проверь интернет") }

    /** Ошибка сервера — словами. */
    private fun words(o: JSONObject): String = when (o.optString("error")) {
        "bad_code", "unknown_code" -> "Код аккаунта не найден. Скопируй его в KAPSULA: Настройки → Аккаунт"
        "bad_pin" -> "Неверный PIN. Осталось попыток: ${o.optInt("left")}"
        "bad_pin_format" -> "PIN — от 6 до 12 цифр"
        "locked" -> "Слишком много ошибок. Подожди 15 минут"
        "not_admin" -> "У этого аккаунта нет прав администратора"
        "new_device" -> "Это новый телефон. Включи «Новый телефон» и войди с PIN"
        "bad_setup" -> "Ключ настройки не подошёл или уже использован"
        "no_player" -> "Игрок с таким ID не найден"
        "is_admin" -> "Администратора заблокировать нельзя"
        "zero" -> "Укажи количество жетонов"
        "empty" -> "Текст пустой"
        "no_item" -> "Укажи предмет"
        else -> "Сервер не ответил. Попробуй ещё раз"
    }
}
