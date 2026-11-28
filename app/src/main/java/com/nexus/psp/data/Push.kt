package com.nexus.psp.data

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

/**
 * Push-уведомления (Firebase Cloud Messaging). Сервер (функция push) присылает только данные — уведомление показывает
 * само приложение: открыта KAPSULA (меню) — плашка и колокольчик сразу; закрыта или идёт игра — системное уведомление.
 * Без сервисов Google на телефоне всё работает как раньше — фоновой проверкой раз в ~15 минут (Inbox.schedule).
 */
object Push {
    private const val TAG = "KapsulaPush"
    private fun prefs(c: Context) = c.getSharedPreferences("kapsula_cloud", Context.MODE_PRIVATE)

    /** При запуске: подписка на новости и токен телефона (отправится на сервер, когда будет аккаунт). */
    fun init(c: Context) {
        try {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { t -> if (!t.isNullOrBlank()) token(c, t) }
            FirebaseMessaging.getInstance().subscribeToTopic("news")
            tips(AppState.S.pushTips)
        } catch (e: Throwable) { Log.w(TAG, "push недоступен: ${e.message}") }
    }

    /** Советы и Сейвик (тема tips, ~3 раза в день): подписка по настройке. */
    fun tips(on: Boolean) {
        try {
            if (on) FirebaseMessaging.getInstance().subscribeToTopic("tips") else FirebaseMessaging.getInstance().unsubscribeFromTopic("tips")
        } catch (e: Throwable) { Log.w(TAG, "push недоступен: ${e.message}") }
    }

    /** Новый токен — запомнить и отправить на сервер. */
    fun token(c: Context, t: String) {
        val sp = prefs(c)
        if (sp.getString("pushToken", null) != t) sp.edit().putString("pushToken", t).putBoolean("pushSent", false).apply()
        flush(c)
    }

    /** Отправить токен, если ещё не отправлен и аккаунт уже есть (вызывается и после входа). */
    fun flush(c: Context) {
        val sp = prefs(c)
        val t = sp.getString("pushToken", null) ?: return
        if (sp.getBoolean("pushSent", false) || !Cloud.ready || Cloud.code.isEmpty()) return
        Thread {
            val (st, _) = Cloud.call(JSONObject().put("action", "push_token").put("code", Cloud.code).put("token", t))
            if (st == 200) sp.edit().putBoolean("pushSent", true).apply()
        }.start()
    }

    /** Пришёл push. В открытом меню — сразу перечитать «что нового» (плашка, колокольчик, баланс);
     *  иначе — системное уведомление и отметка, чтобы фоновая проверка не показала его второй раз. */
    fun received(c: Context, d: Map<String, String>) {
        val id = d["id"]?.toLongOrNull() ?: return
        val src = d["src"] ?: "inbox"
        // совет (tip) или свой пуш создателя всем (msg): своя нумерация, в колокольчик не попадают
        if (src == "tip" || src == "msg") {
            // приложение могло быть закрыто (AppState не поднят) — тогда решает подписка на тему
            if (src == "tip" && runCatching { !AppState.S.pushTips }.getOrDefault(false)) return
            val sp = prefs(c); val key = if (src == "tip") "tipAfter" else "msgAfter"
            if (id <= sp.getLong(key, 0L)) return
            sp.edit().putLong(key, id).apply()
            val title = d["title"].orEmpty(); val body = d["body"].orEmpty()
            if (Inbox.foreground) {
                android.os.Handler(android.os.Looper.getMainLooper()).post { AppState.sayAs(if (src == "tip") "wave" else "letter", if (body.isEmpty()) title else "$title · $body") }
                return
            }
            val pic = d["image"]?.takeIf { it.startsWith("https://") }?.let { picture(it) }
            Inbox.notify(c, (id % 100000).toInt() + (if (src == "tip") 300000 else 400000), title.ifBlank { "KAPSULA" }, body, false, open = "home", pic = pic)
            return
        }
        if (Inbox.foreground) { Thread { try { Cloud.pollNow(c) } catch (e: Exception) { } }.start(); return }
        val sp = prefs(c)
        val pic = d["image"]?.takeIf { it.startsWith("https://") }?.let { picture(it) }
        if (src == "news") {
            if (id <= maxOf(sp.getLong("newsNotifAfter", 0L), sp.getLong("newsAfter", 0L))) return
            Inbox.notify(c, (id % 100000).toInt() + 200000, d["title"].orEmpty().ifBlank { "Новости KAPSULA" }, d["body"].orEmpty(), false, pic = pic)
            sp.edit().putLong("newsNotifAfter", id).apply()
        } else {
            if (id <= maxOf(sp.getLong("notifAfter", 0L), sp.getLong("inboxAfter", 0L))) return
            Inbox.notify(c, (id % 100000).toInt() + 1000, d["title"].orEmpty(), d["body"].orEmpty(), Inbox.isFriend(d["kind"].orEmpty()), pic = pic)
            sp.edit().putLong("notifAfter", id).apply()
        }
    }
}

/** Картинка для уведомления (адрес https из push): до 2 МБ, уменьшается до 1024 px по ширине. */
private fun picture(url: String): android.graphics.Bitmap? = try {
    val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    c.connectTimeout = 8000; c.readTimeout = 10000
    val bytes = c.inputStream.use { it.readBytes() }
    if (bytes.size > 2 * 1024 * 1024) null else {
        val o = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var k = 1; while (o.outWidth / (k * 2) >= 1024) k *= 2
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, android.graphics.BitmapFactory.Options().apply { inSampleSize = k })
    }
} catch (e: Exception) { null }

/** Служба Firebase: новый токен и входящие push. */
class KapsulaFcm : FirebaseMessagingService() {
    override fun onNewToken(token: String) = Push.token(applicationContext, token)
    override fun onMessageReceived(m: RemoteMessage) = Push.received(applicationContext, m.data)
}
