package com.nexus.psp.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nexus.psp.emulator.CoreFiles
import com.nexus.psp.emulator.GameLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** Игра магазина: только то, что разрешено распространять (лицензия или разрешение автора). */
data class StoreGame(
    val id: String, val name: String, val author: String, val category: String, val plat: String,
    val license: String, val source: String?, val site: String?, val desc: String,
    val icon: String?, val bg: String?, val url: String, val size: Long, val unpacked: Long, val sha256: String, val eboot: String,
)

/**
 * Магазин KAPSULA: каталог — файл в выпуске «store» репозитория (собирает CI из store/catalog.json),
 * копия на телефоне — чтобы магазин открывался и без сети. Установка: скачать архив, проверить SHA-256,
 * распаковать в папку игры (games/<id>/) с отметкой — игра сразу появляется в библиотеке.
 */
object StoreCatalog {
    const val URL_CATALOG = "https://github.com/vitalya2078-cloud/nexus-psp/releases/download/store/catalog.json"
    private const val MAX_AGE = 6L * 3600 * 1000

    var games by mutableStateOf<List<StoreGame>>(emptyList()); private set
    var cats by mutableStateOf<Map<String, String>>(emptyMap()); private set
    /** idle | load | ready | offline | fail */
    var state by mutableStateOf("idle"); private set
    /** Идёт установка: id → доля 0..1. */
    val progress = mutableStateMapOf<String, Float>()

    private fun cache(c: Context) = File(c.filesDir, "store/catalog.json")

    fun parse(raw: String) {
        val o = JSONObject(raw)
        val cs = o.optJSONObject("categories")
        cats = cs?.keys()?.asSequence()?.associateWith { cs.getString(it) } ?: emptyMap()
        val a = o.getJSONArray("games")
        games = (0 until a.length()).map { i ->
            val g = a.getJSONObject(i)
            fun opt(k: String) = g.optString(k).ifEmpty { null }
            StoreGame(
                g.getString("id"), g.getString("name"), g.optString("author"), g.optString("category"), g.optString("plat", "psp"),
                g.optString("license"), opt("source"), opt("site"), g.optString("desc"), opt("icon"), opt("bg"),
                g.getString("url"), g.optLong("size"), g.optLong("unpacked"), g.optString("sha256"), g.getString("eboot"),
            )
        }
    }

    /** Каталог: сначала копия с телефона, затем свежий из сети (если копия старше 6 часов или force). */
    suspend fun load(c: Context, force: Boolean = false) = withContext(Dispatchers.IO) {
        val f = cache(c)
        if (games.isEmpty() && f.exists()) try { parse(f.readText()); state = "ready" } catch (e: Exception) { }
        if (!force && f.exists() && System.currentTimeMillis() - f.lastModified() < MAX_AGE && games.isNotEmpty()) return@withContext
        if (games.isEmpty()) state = "load"
        try {
            val raw = get(URL_CATALOG).toString(Charsets.UTF_8)
            parse(raw)
            f.parentFile?.mkdirs(); f.writeText(raw)
            state = "ready"
        } catch (e: Exception) {
            if (games.isEmpty()) state = if (e is java.io.IOException) "offline" else "fail"
        }
    }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15000; readTimeout = 30000; instanceFollowRedirects = true
        setRequestProperty("User-Agent", "KAPSULA")
    }

    private fun get(url: String): ByteArray = open(url).let { c -> try { c.inputStream.use { it.readBytes() } } finally { c.disconnect() } }

    /** Папка игры на телефоне (если установлена). */
    fun dir(c: Context, g: StoreGame) = File(CoreFiles.gamesDir(c), g.id)
    fun installed(c: Context, g: StoreGame) = File(dir(c, g), g.eboot).exists() && File(dir(c, g), GameLibrary.MARK).exists()
    fun libId(g: StoreGame) = "s_" + g.id.lowercase()

    /** Скачать и установить. Ошибка — понятным текстом в исключении. */
    suspend fun install(c: Context, g: StoreGame) = withContext(Dispatchers.IO) {
        progress[g.id] = 0f
        val tmp = File(c.cacheDir, "store-${g.id}.zip")
        val dir = dir(c, g)
        try {
            if (g.unpacked > 0 && AppState.freeMb() * 1048576L < g.size + g.unpacked + 50L * 1048576) error("Мало места на телефоне")
            val con = open(g.url)
            try {
                if (con.responseCode != 200) error("Сервер магазина не ответил (${con.responseCode})")
                val total = con.contentLengthLong.takeIf { it > 0 } ?: g.size
                val md = MessageDigest.getInstance("SHA-256")
                con.inputStream.use { inp ->
                    tmp.outputStream().use { o ->
                        val buf = ByteArray(1 shl 16); var done = 0L; var last = 0L
                        while (true) {
                            val n = inp.read(buf); if (n < 0) break
                            o.write(buf, 0, n); md.update(buf, 0, n); done += n
                            if (done - last > 256 * 1024) {
                                last = done; val p = (done.toFloat() / total).coerceIn(0f, .95f)
                                progress[g.id] = p; AppState.importing = Importing(g.name, p, 1, 1)
                            }
                        }
                    }
                }
                val sum = md.digest().joinToString("") { "%02x".format(it) }
                if (g.sha256.isNotEmpty() && !sum.equals(g.sha256, true)) error("Файл повреждён при загрузке — попробуй ещё раз")
            } finally { con.disconnect() }
            // распаковка: только внутрь папки игры (защита от путей «../»)
            dir.deleteRecursively(); dir.mkdirs()
            val base = dir.canonicalPath + File.separator
            ZipInputStream(tmp.inputStream().buffered()).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    val out = File(dir, e.name)
                    if (!out.canonicalPath.startsWith(base)) continue
                    if (e.isDirectory) out.mkdirs() else { out.parentFile?.mkdirs(); out.outputStream().use { z.copyTo(it) } }
                }
            }
            if (!File(dir, g.eboot).exists()) error("В архиве нет файла игры")
            File(dir, GameLibrary.MARK).writeText(JSONObject().put("store", g.id).put("name", g.name).put("eboot", g.eboot).put("plat", g.plat).toString())
            progress[g.id] = 1f
            Covers.presetTitle(c, libId(g), g.name)
        } catch (e: Exception) {
            dir.deleteRecursively()
            throw e
        } finally {
            tmp.delete()
            progress.remove(g.id)
            AppState.importing = null
        }
    }
}

/** Установить и сразу добавить в библиотеку. Возвращает игру библиотеки. */
suspend fun StoreCatalog.installToLibrary(c: Context, g: StoreGame): Game? {
    install(c, g)
    val info = GameLibrary.dirInfo(dir(c, g)) ?: return null
    return withContext(Dispatchers.Main) { AppState.addImported(info) }
}
