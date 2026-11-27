package com.nexus.psp.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Уведомления KAPSULA. Сервер копит личные уведомления игрока (заявки и друзья, начисления и подарки от KAPSULA) и новости.
 * Пока приложение открыто — опрос раз в минуту (MainActivity): колокольчик + плашка, жетоны обновляются сразу.
 * Когда закрыто — Android раз в ~15 минут будит InboxJob, и новое приходит системным уведомлением.
 * Две отметки: «показано в колокольчике» (inboxAfter) и «показано системным уведомлением» (notifAfter) — ничего не теряется и не дублируется.
 */
object Inbox {
    const val CH_FRIENDS = "friends"
    const val CH_SYSTEM = "system"
    const val EXTRA_OPEN = "kapsula_open"
    private const val JOB_ID = 4207

    /** Приложение на экране — системные уведомления не нужны (всё видно внутри). */
    @Volatile var foreground = false

    private fun prefs(c: Context) = c.getSharedPreferences("kapsula_cloud", Context.MODE_PRIVATE)

    /** Запрос «что нового»; null — нет сети или аккаунта. */
    fun poll(c: Context, after: Long, newsAfter: Long): JSONObject? {
        if (Build.FINGERPRINT == "robolectric") return null
        val code = prefs(c).getString("code", "") ?: ""
        if (code.isEmpty()) return null
        return try {
            val x = URL(Cloud.URL_FN).openConnection() as HttpURLConnection
            x.requestMethod = "POST"; x.doOutput = true; x.connectTimeout = 10_000; x.readTimeout = 15_000
            x.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val body = JSONObject().put("action", "poll").put("code", code).put("after", after).put("news_after", newsAfter)
            x.outputStream.use { it.write(body.toString().toByteArray()) }
            if (x.responseCode != 200) null else JSONObject(x.inputStream.bufferedReader().readText())
        } catch (e: Exception) { null }
    }

    fun isFriend(kind: String) = kind.startsWith("friend")

    fun channels(c: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CH_FRIENDS, "Друзья", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Заявки в друзья и новые друзья" })
        nm.createNotificationChannel(NotificationChannel(CH_SYSTEM, "KAPSULA", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Новости, жетоны и подарки от KAPSULA" })
    }

    /** Системное уведомление; тап — открыть KAPSULA (для друзей — сразу «Моё → Друзья»). */
    fun notify(c: Context, id: Int, title: String, text: String, friends: Boolean, open: String? = null, pic: android.graphics.Bitmap? = null) {
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        channels(c)
        val open = Intent(c, com.nexus.psp.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_OPEN, open ?: if (friends) "friends" else "alerts")
        val pi = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val b = if (Build.VERSION.SDK_INT >= 26) android.app.Notification.Builder(c, if (friends) CH_FRIENDS else CH_SYSTEM)
            else @Suppress("DEPRECATION") android.app.Notification.Builder(c)
        val n = b.setSmallIcon(com.nexus.psp.R.drawable.ic_stat_kapsula).setContentTitle(title).setContentText(text)
            .setStyle(if (pic != null) android.app.Notification.BigPictureStyle().bigPicture(pic).setSummaryText(text) else android.app.Notification.BigTextStyle().bigText(text))
            .apply { if (pic != null) setLargeIcon(pic) }
            .setContentIntent(pi).setAutoCancel(true).build()
        try { nm.notify(id, n) } catch (e: SecurityException) { }
    }

    /** Фоновая проверка раз в ~15 минут (минимум, который даёт Android без платных push-сервисов). Переживает перезагрузку. */
    fun schedule(c: Context) {
        if (Build.FINGERPRINT == "robolectric") return
        val js = c.getSystemService(JobScheduler::class.java) ?: return
        if (js.allPendingJobs.any { it.id == JOB_ID }) return
        val info = JobInfo.Builder(JOB_ID, ComponentName(c, InboxJob::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(15 * 60 * 1000L).setPersisted(true).build()
        try { js.schedule(info) } catch (e: Exception) { }
    }

    /** Фон: всё новое — системными уведомлениями. */
    fun checkInBackground(c: Context) {
        if (foreground) return
        Update.notifyInBackground(c)
        val sp = prefs(c)
        val after = maxOf(sp.getLong("notifAfter", 0L), sp.getLong("inboxAfter", 0L))
        val newsAfter = maxOf(sp.getLong("newsNotifAfter", 0L), sp.getLong("newsAfter", 0L))
        val r = poll(c, after, newsAfter) ?: return
        var last = after; var lastNews = newsAfter
        r.optJSONArray("items")?.let { a ->
            for (i in 0 until a.length()) a.getJSONObject(i).let { o ->
                val id = o.optLong("id"); last = maxOf(last, id)
                notify(c, (id % 100000).toInt() + 1000, o.optString("title"), o.optString("body"), isFriend(o.optString("kind")))
            }
        }
        r.optJSONArray("news")?.let { a ->
            for (i in 0 until a.length()) a.getJSONObject(i).let { o ->
                val id = o.optLong("id"); lastNews = maxOf(lastNews, id)
                notify(c, (id % 100000).toInt() + 200000, "Новости KAPSULA", o.optString("text"), false)
            }
        }
        sp.edit().putLong("notifAfter", last).putLong("newsNotifAfter", lastNews).apply()
    }
}

/** Фоновая проверка уведомлений (JobScheduler). */
class InboxJob : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        Thread { try { Inbox.checkInBackground(applicationContext) } finally { jobFinished(params, false) } }.start()
        return true
    }
    override fun onStopJob(params: JobParameters?) = true
}
