package com.nexus.psp.data

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * «Сейчас играет» на карточке создателя: игра (отдельный процесс) сообщает серверу начало и конец сессии.
 * Только у аккаунта создателя (у остальных ничего не отправляется и не хранится). Без сети — молча пропускаем.
 */
object Presence {
    fun send(c: Context, title: String, plat: String, on: Boolean) {
        if (android.os.Build.FINGERPRINT == "robolectric") return
        val sp = c.getSharedPreferences("kapsula_cloud", Context.MODE_PRIVATE)
        val code = sp.getString("code", "") ?: ""
        if (code.isEmpty() || !sp.getBoolean("admin", false)) return
        Thread {
            try {
                val x = URL(Cloud.URL_FN).openConnection() as HttpURLConnection
                x.requestMethod = "POST"; x.doOutput = true; x.connectTimeout = 8000; x.readTimeout = 8000
                x.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val body = JSONObject().put("action", "presence").put("code", code).put("p", JSONObject().put("t", title.take(60)).put("p", plat).put("on", on))
                x.outputStream.use { it.write(body.toString().toByteArray()) }
                x.responseCode
            } catch (e: Exception) { }
        }.start()
    }
}
