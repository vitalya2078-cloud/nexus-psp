package com.nexus.psp.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Обновление без браузера: служба качает APK сама, прогресс — в уведомлении («34 из 56 МБ») и в приложении (UpdateService.progress).
 * Обрыв связи — продолжает с того же места (Range). GitHub недоступен — пробует зеркало (сайт / GitVerse).
 * Скачалось — открывается установщик Android (если KAPSULA на экране) или уведомление «Установить».
 */
class UpdateService : Service() {
    companion object {
        private const val TAG = "KapsulaUpdate"
        private const val CH = "update"
        private const val NID = 300002
        const val ACTION_CANCEL = "com.nexus.psp.update.CANCEL"
        /** Прогресс для приложения: null — не качаем; 0..100 — проценты; -1 — размер неизвестен. */
        var progress by mutableStateOf<Int?>(null)
        @Volatile private var cancel = false

        fun start(c: Context) {
            if (progress != null) { AppState.say("Обновление уже качается — смотри уведомление"); return }
            val i = Intent(c, UpdateService::class.java)
            try { if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i) else c.startService(i) }
            catch (e: Exception) { Log.w(TAG, "start", e); Update.openInBrowser(c) }
        }

        fun file(c: Context) = File(c.cacheDir, "update/kapsula.apk")

        /** Установщик Android для скачанного файла. */
        fun installIntent(c: Context): Intent {
            val uri: Uri = FileProvider.getUriForFile(c, c.packageName + ".share", file(c))
            return Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private val nm by lazy { getSystemService(NotificationManager::class.java) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) { cancel = true; return START_NOT_STICKY }
        channel()
        val n = note("Готовлю загрузку…", 0, 0, true)
        if (Build.VERSION.SDK_INT >= 29) startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else startForeground(NID, n)
        if (progress != null) return START_NOT_STICKY
        progress = -1; cancel = false
        Thread { run() }.start()
        return START_NOT_STICKY
    }

    private fun channel() {
        if (Build.VERSION.SDK_INT >= 26) nm?.createNotificationChannel(NotificationChannel(CH, "Обновление KAPSULA", NotificationManager.IMPORTANCE_LOW).apply { description = "Загрузка новой версии" })
    }

    private fun note(text: String, max: Int, cur: Int, indet: Boolean, done: PendingIntent? = null): Notification {
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CH) else @Suppress("DEPRECATION") Notification.Builder(this)
        b.setSmallIcon(if (done != null) com.nexus.psp.R.drawable.ic_stat_kapsula else android.R.drawable.stat_sys_download)
            .setContentTitle(if (done != null) "KAPSULA скачана" else "Качаю KAPSULA ${Update.pendingVer(this) ?: ""}".trim())
            .setContentText(text).setOnlyAlertOnce(true)
        if (done != null) b.setContentIntent(done).setAutoCancel(true)
        else {
            b.setOngoing(true).setProgress(max, cur, indet)
            val ci = PendingIntent.getService(this, 1, Intent(this, UpdateService::class.java).setAction(ACTION_CANCEL), PendingIntent.FLAG_IMMUTABLE)
            @Suppress("DEPRECATION") b.addAction(0, "Отмена", ci)
        }
        return b.build()
    }

    private fun mb(b: Long) = String.format(java.util.Locale.US, "%.1f", b / 1048576.0).replace('.', ',')

    private fun run() {
        val f = file(this); f.parentFile?.mkdirs()
        val part = File(f.path + ".part")
        val urls = Update.downloadUrls(this)
        var ok = false; var last = ""
        for (u in urls) {
            for (attempt in 0 until 4) {
                if (cancel) break
                val r = fetch(u, part)
                if (r == null) { ok = true; break }
                last = r; Log.w(TAG, "$u: $r"); Thread.sleep(2000L * (attempt + 1))
            }
            if (ok || cancel) break
        }
        if (cancel) { part.delete(); finish(null, "Загрузка отменена"); return }
        if (!ok) { finish(null, "Не скачалось: $last. Попробуй позже или скачай с сайта"); return }
        // проверка: это APK KAPSULA, а не страница ошибки
        val info = try { packageManager.getPackageArchiveInfo(part.path, 0) } catch (e: Exception) { null }
        if (info?.packageName != packageName) { part.delete(); finish(null, "Файл повреждён — попробуй ещё раз"); return }
        f.delete(); part.renameTo(f)
        val pi = PendingIntent.getActivity(this, 2, installIntent(this), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        finish(pi, "Нажми, чтобы установить. Игры и сохранения останутся")
        // KAPSULA на экране — сразу окно установки
        if (Inbox.foreground) try { startActivity(installIntent(this)) } catch (e: Exception) { Log.w(TAG, "install", e) }
    }

    /** Скачать (с продолжением). null — готово, иначе причина. */
    private fun fetch(u: String, part: File): String? = try {
        val have = if (part.exists()) part.length() else 0L
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 30_000; c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "KAPSULA")
        if (have > 0) c.setRequestProperty("Range", "bytes=$have-")
        val code = c.responseCode
        if (code !in listOf(200, 206)) "сервер ответил $code"
        else {
            val resume = code == 206
            val total = (if (resume) have else 0L) + c.contentLengthLong.coerceAtLeast(0L)
            var done = if (resume) have else 0L
            var shown = -1; var t = 0L
            c.inputStream.use { inp -> FileOutputStream(part, resume).use { out ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    if (cancel) return "отменено"
                    val n = inp.read(buf); if (n < 0) break
                    out.write(buf, 0, n); done += n
                    val p = if (total > 0) (done * 100 / total).toInt() else -1
                    val now = System.currentTimeMillis()
                    if (p != shown && now - t > 400) {
                        shown = p; t = now; progress = p
                        nm?.notify(NID, note(if (total > 0) "${mb(done)} из ${mb(total)} МБ" else "${mb(done)} МБ", 100, p.coerceAtLeast(0), p < 0))
                    }
                }
            } }
            if (total > 0 && done < total) "оборвалось на ${mb(done)} МБ" else null
        }
    } catch (e: Exception) { e.message ?: e.javaClass.simpleName }

    private fun finish(done: PendingIntent?, text: String) {
        progress = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        nm?.notify(NID, note(text, 0, 0, false, done ?: PendingIntent.getActivity(this, 3,
            Intent(this, com.nexus.psp.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)))
        if (done == null) android.os.Handler(android.os.Looper.getMainLooper()).post { AppState.say(text) }
        stopSelf()
    }
}
