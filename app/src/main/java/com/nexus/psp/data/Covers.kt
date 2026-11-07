package com.nexus.psp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.nexus.psp.emulator.Disc
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Что известно об игре сверх файла: серийный номер, название из базы, обложка, картинки из образа. */
data class Meta(val serial: String? = null, val title: String? = null, val cover: File? = null, val pic: File? = null, val icon: File? = null, val tried: Long = 0)

/**
 * Обложки и названия игр.
 * 1) Из самого образа (без интернета): серийный номер, название, значок ICON0 и широкий фон PIC1.
 * 2) Серийный номер → официальное название по открытой базе libretro-database (No-Intro).
 * 3) Название → обложка с thumbnails.libretro.com (тот же источник, что у RetroArch и Lemuroid).
 * Всё сохраняется в filesDir/meta/<id>/ и дальше работает без сети.
 */
object Covers {
    private const val TAG = "KapsulaCovers"
    private const val DB_URL = "https://raw.githubusercontent.com/libretro/libretro-database/master/metadat/no-intro/Sony%20-%20PlayStation%20Portable.dat"
    private const val THUMBS = "https://thumbnails.libretro.com/Sony%20-%20PlayStation%20Portable/Named_Boxarts/"
    private const val DB_MAX_AGE = 30L * 24 * 3600 * 1000

    fun dir(c: Context, id: String) = File(c.filesDir, "meta/$id").apply { mkdirs() }

    /** Сохранённые сведения об игре (без сети). */
    fun load(c: Context, id: String): Meta {
        val d = File(c.filesDir, "meta/$id")
        val j = try { JSONObject(File(d, "meta.json").readText()) } catch (e: Exception) { JSONObject() }
        fun f(n: String) = File(d, n).takeIf { it.exists() && it.length() > 0 }
        return Meta(j.optString("serial").ifEmpty { null }, j.optString("title").ifEmpty { null }, f("cover_user.jpg") ?: f("cover.jpg"), f("pic.png"), f("icon.png"), j.optLong("tried"))
    }

    private fun saveJson(c: Context, id: String, serial: String?, title: String?, tried: Long) {
        File(dir(c, id), "meta.json").writeText(JSONObject().put("serial", serial ?: "").put("title", title ?: "").put("tried", tried).toString())
    }

    /** Можно ли сейчас ходить в сеть за обложками (настройка «Только по Wi-Fi»). */
    fun netAllowed(c: Context, wifiOnly: Boolean): Boolean {
        val cm = c.getSystemService(ConnectivityManager::class.java) ?: return false
        return try {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) false
            else !wifiOnly || caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || !cm.isActiveNetworkMetered
        } catch (e: Exception) { false }
    }

    /**
     * Полная обработка игры: картинки из образа → название по базе → обложка из интернета.
     * Работает в фоновом потоке. Возвращает итоговые сведения.
     */
    fun process(c: Context, g: Game, online: Boolean, force: Boolean = false): Meta {
        val d = dir(c, g.id)
        val old = load(c, g.id)
        var serial = old.serial; var sfoTitle: String? = null
        // 1) из образа — один раз
        if (old.tried == 0L || force) {
            Disc.read(File(g.path))?.let { info ->
                serial = info.serial ?: serial; sfoTitle = info.title
                info.icon?.let { File(d, "icon.png").writeBytes(it) }
                info.pic?.let { File(d, "pic.png").writeBytes(it) }
            }
        }
        var title = old.title
        var tried = old.tried
        // 2–3) из интернета
        if (online && (old.cover == null || force)) {
            val db = database(c)
            val name = serial?.let { db?.bySerial?.get(it) } ?: db?.byTitle(sfoTitle ?: g.file.substringBeforeLast('.'))
            if (name != null) {
                title = cleanTitle(name)
                if (!File(d, "cover_user.jpg").exists()) download(THUMBS + thumbName(name) + ".png")?.let { saveCover(it, File(d, "cover.jpg")) }
            } else if (sfoTitle != null && title == null) title = cleanTitle(sfoTitle!!)
            tried = System.currentTimeMillis()
        } else if (title == null && sfoTitle != null) title = cleanTitle(sfoTitle!!)
        if (tried == 0L) tried = 1 // образ прочитан, сеть — позже
        saveJson(c, g.id, serial, title, tried)
        return load(c, g.id)
    }

    /** Своя обложка из галереи (важнее найденной). */
    fun setUserCover(c: Context, id: String, bytes: ByteArray): Boolean = saveCover(bytes, File(dir(c, id), "cover_user.jpg"))

    fun clearUserCover(c: Context, id: String) { File(dir(c, id), "cover_user.jpg").delete() }

    fun remove(c: Context, id: String) { File(c.filesDir, "meta/$id").deleteRecursively() }

    // ---------- база названий ----------
    class Db(val bySerial: Map<String, String>, private val names: List<String>) {
        private fun norm(s: String) = s.substringBefore('(').lowercase().replace(Regex("[^a-z0-9а-я]"), "")
        /** Поиск по названию (для CHD и образов без номера): совпадение без региона и знаков. */
        fun byTitle(t: String): String? {
            val k = norm(prettyName(t)); if (k.length < 3) return null
            return names.firstOrNull { norm(it) == k && "(Europe)" in it } ?: names.firstOrNull { norm(it) == k }
        }
    }

    @Volatile private var db: Db? = null

    @Volatile private var dbFailedAt = 0L

    private fun database(c: Context): Db? {
        db?.let { return it }
        val f = File(c.filesDir, "meta/psp-db.tsv")
        // не скачалась недавно — не пробуем снова для каждой игры (10 минут)
        val canTry = System.currentTimeMillis() - dbFailedAt > 10 * 60 * 1000
        if (canTry && (!f.exists() || System.currentTimeMillis() - f.lastModified() > DB_MAX_AGE)) {
            (download(DB_URL) ?: run { dbFailedAt = System.currentTimeMillis(); null })?.let { raw ->
                val out = StringBuilder(); var name: String? = null
                for (line in String(raw, Charsets.UTF_8).lineSequence()) {
                    val t = line.trim()
                    if (t.startsWith("name \"")) name = t.removePrefix("name \"").substringBefore('"')
                    else if (t.startsWith("serial \"") && name != null) { out.append(t.removePrefix("serial \"").substringBefore('"')).append('\t').append(name).append('\n'); name = null }
                }
                if (out.isNotEmpty()) { f.parentFile?.mkdirs(); f.writeText(out.toString()) }
            }
        }
        if (!f.exists()) return null
        val map = HashMap<String, String>(); val names = ArrayList<String>()
        f.forEachLine { l -> val i = l.indexOf('\t'); if (i > 0) { l.substring(0, i).split(',').forEach { s -> map.putIfAbsent(s.trim().uppercase(), l.substring(i + 1)) }; names.add(l.substring(i + 1)) } }
        return Db(map, names).also { db = it }
    }

    // Правило имён libretro-thumbnails: символы & * / : ` < > ? \ | заменяются на «_».
    private fun thumbName(name: String): String =
        URLEncoder.encode(name.replace(Regex("[&*/:`<>?\\\\|]"), "_"), "UTF-8").replace("+", "%20")
            .replace("%28", "(").replace("%29", ")").replace("%2C", ",").replace("%27", "'").replace("%21", "!")

    /** «Soulcalibur - Broken Destiny (Europe) (En,Fr…)» → «Soulcalibur — Broken Destiny». */
    fun cleanTitle(n: String): String = prettyName(n).replace(" - ", " — ")

    private fun download(url: String): ByteArray? = try {
        val con = URL(url).openConnection() as HttpURLConnection
        con.connectTimeout = 10000; con.readTimeout = 20000; con.setRequestProperty("User-Agent", "KAPSULA")
        try { if (con.responseCode == 200) con.inputStream.use { it.readBytes() } else null } finally { con.disconnect() }
    } catch (e: Exception) { Log.w(TAG, "download $url", e); null }

    /** Обложка → JPEG высотой до 600 px (оригиналы около 800 КБ). */
    private fun saveCover(bytes: ByteArray, out: File): Boolean = try {
        val b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw IllegalStateException("bad image")
        val k = minOf(1f, 600f / b.height)
        val s = if (k < 1f) Bitmap.createScaledBitmap(b, (b.width * k).toInt(), (b.height * k).toInt(), true) else b
        out.outputStream().use { s.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        true
    } catch (e: Exception) { Log.w(TAG, "cover", e); false }
}
