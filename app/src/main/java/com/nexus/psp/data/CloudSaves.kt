package com.nexus.psp.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.nexus.psp.emulator.CoreFiles
import com.nexus.psp.emulator.GameLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Облачные сохранения: только сейвы игр (не снимки состояния — они по 20–40 МБ).
 *  - PSP: папки PSP/SAVEDATA/<код диска>* одним zip, ключ psp_<код диска> — одинаковый на любом телефоне;
 *  - остальные консоли: файл <имя образа>.srm, ключ srm_<хэш имени>.
 * На сервере до 2 МБ на файл и 20 МБ на игрока. Сравнение — по SHA-256 содержимого:
 *  «база» — что было в облаке при прошлой сверке. Изменилось только на телефоне — загружаем, только в облаке — скачиваем,
 *  и там и там — спрашиваем игрока, молча ничего не затираем.
 */
object CloudSaves {
    private const val TAG = "KapsulaSaves"
    const val MAX = 2 * 1024 * 1024
    private const val ZIP_TIME = 315_532_800_000L // 1980-01-01: одинаковый zip для одинаковых файлов
    private lateinit var sp: SharedPreferences

    /** После игры — сверить при возвращении на главный экран. */
    @Volatile var dirty = false

    fun init(c: Context) { if (!::sp.isInitialized) sp = c.getSharedPreferences("kapsula_csaves", Context.MODE_PRIVATE) }

    private fun savesDir(c: Context) = CoreFiles.savesDir(c)
    private fun pspDir(c: Context) = File(savesDir(c), "PSP/SAVEDATA")

    /** Код диска PSP без дефиса, ULUS10041 (null — не знаем). */
    fun serialOf(g: Game): String? = AppState.meta[g.id]?.serial?.uppercase()?.filter { it.isLetterOrDigit() }?.takeIf { it.length >= 9 }

    /** Ключ сейва в облаке (null — для этой игры облако не умеет). */
    fun keyOf(g: Game): String? = if (g.p == "psp") serialOf(g)?.let { "psp_" + it.lowercase() }
        else "srm_" + sha(GameLibrary.key(g.path).toByteArray()).take(24)

    /** Содержимое сейва на телефоне (null — сейва нет). */
    fun local(c: Context, g: Game): ByteArray? = try {
        if (g.p == "psp") {
            val s = serialOf(g) ?: return null
            val dirs = pspDir(c).listFiles { f -> f.isDirectory && f.name.uppercase().startsWith(s) }?.sortedBy { it.name } ?: emptyList()
            val files = dirs.flatMap { d -> d.walkTopDown().filter { it.isFile }.map { d to it } }.sortedBy { it.second.path }
            if (files.isEmpty()) null else {
                val bo = ByteArrayOutputStream()
                ZipOutputStream(bo).use { z ->
                    for ((d, f) in files) {
                        val name = d.name + "/" + f.relativeTo(d).invariantSeparatorsPath
                        z.putNextEntry(ZipEntry(name).apply { time = ZIP_TIME }); f.inputStream().use { it.copyTo(z) }; z.closeEntry()
                    }
                }
                bo.toByteArray()
            }
        } else File(savesDir(c), GameLibrary.key(g.path) + ".srm").takeIf { it.isFile && it.length() > 0 }?.readBytes()
    } catch (e: Exception) { Log.w(TAG, "local", e); null }

    /** Записать скачанный сейв на телефон. */
    fun write(c: Context, g: Game, data: ByteArray): Boolean = try {
        if (g.p == "psp") {
            val s = serialOf(g) ?: return false
            val root = pspDir(c).apply { mkdirs() }
            val canon = root.canonicalPath + File.separator
            ZipInputStream(ByteArrayInputStream(data)).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    // только папки этой игры и без выхода за SAVEDATA
                    val out = File(root, e.name)
                    if (e.isDirectory || !e.name.uppercase().startsWith(s) || !out.canonicalPath.startsWith(canon)) continue
                    out.parentFile?.mkdirs(); out.outputStream().use { z.copyTo(it) }
                }
            }
            true
        } else { File(savesDir(c), GameLibrary.key(g.path) + ".srm").writeBytes(data); true }
    } catch (e: Exception) { Log.w(TAG, "write", e); false }

    fun sha(b: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    private fun base(key: String): String? = sp.getString(key, null)
    private fun setBase(key: String, s: String) { sp.edit().putString(key, s).apply() }

    /** Что делать с одной игрой. */
    enum class Plan { NONE, UPLOAD, DOWNLOAD, ASK }

    /** Решение по трём суммам: телефон, облако, база (что было в облаке при прошлой сверке). */
    fun plan(local: String?, remote: String?, base: String?): Plan = when {
        local == null && remote == null -> Plan.NONE
        local == remote -> Plan.NONE
        remote == null -> Plan.UPLOAD
        local == null -> Plan.DOWNLOAD
        remote == base -> Plan.UPLOAD     // поменялось только на телефоне
        local == base -> Plan.DOWNLOAD    // поменялось только в облаке
        else -> Plan.ASK                  // поменялось и там и там (или первая сверка на новом телефоне)
    }

    data class Report(val up: Int, val down: Int, val kept: Int, val failed: Int, val error: String? = null) {
        val text get() = error ?: when {
            up + down == 0 && failed == 0 -> "Сохранения уже совпадают с облаком"
            else -> listOfNotNull(
                up.takeIf { it > 0 }?.let { "загружено: $it" }, down.takeIf { it > 0 }?.let { "скачано: $it" },
                failed.takeIf { it > 0 }?.let { "не вышло: $it" }).joinToString(", ").replaceFirstChar { it.uppercase() }
        }
    }

    /** Сверить сейвы всех игр (или одной). Конфликты — вопрос игроку. Зовётся с главного потока. */
    suspend fun sync(c: Context, onlyId: String? = null, quiet: Boolean = false): Report {
        init(c)
        if (!AppState.S.cloud) return Report(0, 0, 0, 0, "Облачные сохранения выключены")
        if (Cloud.code.isEmpty()) return Report(0, 0, 0, 0, "Аккаунт ещё создаётся — попробуй через минуту")
        val remote = withContext(Dispatchers.IO) { list() } ?: return Report(0, 0, 0, 0, "Нет связи с сервером. Проверь интернет")
        val games = AppState.byId.values.filter { (onlyId == null || it.id == onlyId) && !it.missing }
        var up = 0; var down = 0; var kept = 0; var failed = 0
        for (g in games) {
            val key = keyOf(g) ?: continue
            val bytes = withContext(Dispatchers.IO) { local(c, g) }
            if (bytes != null && bytes.size > MAX) { failed++; continue }
            val ls = bytes?.let { sha(it) }
            val rs = remote[key]
            var p = plan(ls, rs, base(key))
            if (ls != null && ls == rs) setBase(key, ls)
            if (p == Plan.ASK) {
                if (quiet && onlyId == null) { kept++; continue }
                val a = AppState.askUser("Сохранения «${g.t}»", "Сохранения изменились и на этом телефоне, и в облаке. Какие оставить? Другие будут заменены.",
                    "Оставить с телефона", "Взять из облака")
                p = when (a) { 0 -> Plan.UPLOAD; 1 -> Plan.DOWNLOAD; else -> Plan.NONE }
                if (p == Plan.NONE) { kept++; continue }
            }
            when (p) {
                Plan.UPLOAD -> if (withContext(Dispatchers.IO) { put(key, bytes!!) }?.let { setBase(key, it); true } == true) up++ else failed++
                Plan.DOWNLOAD -> {
                    val got = withContext(Dispatchers.IO) { get(key) }
                    if (got != null && sha(got) == rs && withContext(Dispatchers.IO) { write(c, g, got) }) { setBase(key, rs); down++ } else failed++
                }
                else -> {}
            }
        }
        return Report(up, down, kept, failed)
    }

    // ---------- сеть ----------
    private val offline get() = android.os.Build.FINGERPRINT == "robolectric"

    /** Опись облака: ключ → SHA-256 (null — нет связи). */
    private fun list(): Map<String, String>? {
        if (offline) return null
        val (st, r) = Cloud.call(JSONObject().put("action", "saves").put("code", Cloud.code))
        if (st != 200 || r == null) return null
        val a = r.optJSONArray("items") ?: return emptyMap()
        return (0 until a.length()).associate { a.getJSONObject(it).let { o -> o.optString("key") to o.optString("sha") } }
    }

    private fun conn(op: String, key: String): HttpURLConnection = (URL(Cloud.URL_FN + "?op=" + op).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"; connectTimeout = 10_000; readTimeout = 30_000
        setRequestProperty("x-kapsula-code", Cloud.code); setRequestProperty("x-kapsula-key", key)
    }

    /** Загрузить; ответ — SHA-256, который записал сервер (null — не вышло). */
    private fun put(key: String, data: ByteArray): String? = try {
        val c = conn("put", key)
        c.doOutput = true; c.setRequestProperty("Content-Type", "application/octet-stream"); c.setFixedLengthStreamingMode(data.size)
        c.outputStream.use { it.write(data) }
        if (c.responseCode != 200) { Log.w(TAG, "put ${c.responseCode}"); null }
        else JSONObject(c.inputStream.bufferedReader().readText()).optString("sha").takeIf { it == sha(data) }
    } catch (e: Exception) { Log.w(TAG, "put", e); null }

    private fun get(key: String): ByteArray? = try {
        val c = conn("get", key)
        c.doOutput = true; c.setFixedLengthStreamingMode(0); c.outputStream.close()
        if (c.responseCode != 200) null else c.inputStream.use { i -> i.readBytes().takeIf { it.size <= MAX } }
    } catch (e: Exception) { Log.w(TAG, "get", e); null }
}
