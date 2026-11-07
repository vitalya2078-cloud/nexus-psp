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
data class Meta(
    val serial: String? = null, val title: String? = null, val cover: File? = null, val pic: File? = null, val icon: File? = null, val tried: Long = 0,
    /** Скриншот из игры и титульный экран (libretro Named_Snaps / Named_Titles). */
    val snap: File? = null, val titleImg: File? = null,
    /** Основной цвет обложки (ARGB) — для подсветки фона, если картинок нет. */
    val color: Int? = null,
    /** Когда искали скриншоты (0 — ещё не искали). */
    val extra: Long = 0,
) {
    /** Фон игры: картинка из образа → титульный экран → скриншот. */
    val backdrop get() = pic ?: titleImg ?: snap
    /** Все кадры для вкладки «Скриншоты». */
    val shots get() = listOfNotNull(snap, titleImg, pic)
}

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
    private const val SNAPS = "https://thumbnails.libretro.com/Sony%20-%20PlayStation%20Portable/Named_Snaps/"
    private const val TITLES = "https://thumbnails.libretro.com/Sony%20-%20PlayStation%20Portable/Named_Titles/"
    private const val DB_MAX_AGE = 30L * 24 * 3600 * 1000

    fun dir(c: Context, id: String) = File(c.filesDir, "meta/$id").apply { mkdirs() }

    /** Сохранённые сведения об игре (без сети). */
    fun load(c: Context, id: String): Meta {
        val d = File(c.filesDir, "meta/$id")
        val j = try { JSONObject(File(d, "meta.json").readText()) } catch (e: Exception) { JSONObject() }
        fun f(n: String) = File(d, n).takeIf { it.exists() && it.length() > 0 }
        return Meta(
            j.optString("serial").ifEmpty { null }, j.optString("title").ifEmpty { null }, f("cover_user.jpg") ?: f("cover.jpg"), f("pic.png"), f("icon.png"), j.optLong("tried"),
            f("snap.jpg"), f("title.jpg"), if (j.has("color")) j.optInt("color") else null, j.optLong("extra"),
        )
    }

    private fun saveJson(c: Context, id: String, serial: String?, title: String?, tried: Long, color: Int?, extra: Long) {
        val o = JSONObject().put("serial", serial ?: "").put("title", title ?: "").put("tried", tried).put("extra", extra)
        if (color != null) o.put("color", color)
        File(dir(c, id), "meta.json").writeText(o.toString())
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
        var extra = old.extra
        // 2–3) из интернета: обложка, а также скриншот и титульный экран для фона
        val needCover = old.cover == null || force
        val needExtra = old.extra == 0L || force
        if (online && (needCover || needExtra)) {
            val db = database(c)
            val name = serial?.let { db?.bySerial?.get(it) } ?: db?.byTitle(sfoTitle ?: g.file.substringBeforeLast('.'))
            if (name != null) {
                title = cleanTitle(name)
                if (needCover && !File(d, "cover_user.jpg").exists()) download(THUMBS + thumbName(name) + ".png")?.let { saveImage(it, File(d, "cover.jpg"), 600, byWidth = false) }
                if (needExtra) {
                    download(SNAPS + thumbName(name) + ".png")?.let { saveImage(it, File(d, "snap.jpg"), 960, byWidth = true) }
                    download(TITLES + thumbName(name) + ".png")?.let { saveImage(it, File(d, "title.jpg"), 960, byWidth = true) }
                }
            } else if (sfoTitle != null && title == null) title = cleanTitle(sfoTitle!!)
            tried = System.currentTimeMillis(); extra = System.currentTimeMillis()
        } else if (title == null && sfoTitle != null) title = cleanTitle(sfoTitle!!)
        if (tried == 0L) tried = 1 // образ прочитан, сеть — позже
        // основной цвет обложки (или картинки из образа) — для подсветки фона
        val src = File(d, "cover_user.jpg").takeIf { it.exists() } ?: File(d, "cover.jpg").takeIf { it.exists() } ?: File(d, "pic.png").takeIf { it.exists() }
        val color = if (force || old.color == null) src?.let { dominant(it) } ?: old.color else old.color
        saveJson(c, g.id, serial, title, tried, color, extra)
        return load(c, g.id)
    }

    /** Своя обложка из галереи (важнее найденной). */
    fun setUserCover(c: Context, id: String, bytes: ByteArray): Boolean {
        val f = File(dir(c, id), "cover_user.jpg")
        val ok = saveImage(bytes, f, 600, byWidth = false)
        if (ok) { val m = load(c, id); saveJson(c, id, m.serial, m.title, m.tried, dominant(f) ?: m.color, m.extra) }
        return ok
    }

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

    /** Картинка → JPEG: обложка — высотой до 600 px (оригиналы около 800 КБ), фоны — шириной до 960 px. */
    private fun saveImage(bytes: ByteArray, out: File, max: Int, byWidth: Boolean): Boolean = try {
        val b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw IllegalStateException("bad image")
        val side = if (byWidth) b.width else b.height
        val k = minOf(1f, max.toFloat() / side)
        val s = if (k < 1f) Bitmap.createScaledBitmap(b, (b.width * k).toInt(), (b.height * k).toInt(), true) else b
        out.outputStream().use { s.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        true
    } catch (e: Exception) { Log.w(TAG, "image", e); false }

    /**
     * Основной цвет картинки: среднее по насыщенным пикселям уменьшенной копии,
     * приглушённое под тёмную палитру KAPSULA. Без сторонних библиотек.
     */
    fun dominant(f: File): Int? = try {
        val o = BitmapFactory.Options().apply { inSampleSize = 8 }
        val b = BitmapFactory.decodeFile(f.path, o) ?: throw IllegalStateException()
        val sm = Bitmap.createScaledBitmap(b, 24, 24, true)
        var r = 0.0; var g = 0.0; var bl = 0.0; var w = 0.0
        val hsv = FloatArray(3)
        for (y in 0 until 24) for (x in 0 until 24) {
            val p = sm.getPixel(x, y)
            android.graphics.Color.colorToHSV(p, hsv)
            val wt = (hsv[1] * hsv[2]).toDouble().let { it * it } + .02 // насыщенные и светлые — важнее
            r += android.graphics.Color.red(p) * wt; g += android.graphics.Color.green(p) * wt; bl += android.graphics.Color.blue(p) * wt; w += wt
        }
        val c = android.graphics.Color.rgb((r / w).toInt(), (g / w).toInt(), (bl / w).toInt())
        android.graphics.Color.colorToHSV(c, hsv)
        hsv[1] = minOf(hsv[1], .55f); hsv[2] = hsv[2].coerceIn(.45f, .85f)
        android.graphics.Color.HSVToColor(hsv)
    } catch (e: Exception) { null }
}
