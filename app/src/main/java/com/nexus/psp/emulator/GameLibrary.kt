package com.nexus.psp.emulator

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Свои образы игр пользователя: копия в папке приложения (Android/data/…/files/games)
 * или игра на месте — в папке KAPSULA и других папках телефона (без копирования, нужен доступ ко всем файлам).
 */
object GameLibrary {
    /** PSP: ISO, CSO, CHD, PBP, ELF, PRX. PS1: CUE + BIN, BIN, IMG, CHD, PBP, M3U (несколько дисков). */
    val EXT: Set<String> = com.nexus.psp.data.PLATFORMS.flatMap { it.exts }.toSet() + setOf("cue", "bin", "img", "m3u")

    /** Консоль, выбранная игроком вручную (если автоматически угадали не так): имя файла → консоль. */
    private fun overrides(c: Context) = File(CoreFiles.gamesDir(c), ".kapsula-plat.json")
    fun setPlatform(c: Context, fileName: String, plat: String) {
        val o = try { JSONObject(overrides(c).readText()) } catch (e: Exception) { JSONObject() }
        overrides(c).writeText(o.put(fileName, plat).toString())
    }
    private var ov: JSONObject? = null

    /** Части игры, которые не показываются отдельно: дорожки из CUE и диски из M3U. */
    private fun parts(f: File): List<File> = when (f.extension.lowercase()) {
        "cue" -> Disc.cueTracks(f)
        "m3u" -> Disc.m3uItems(f).flatMap { listOf(it) + parts(it) }
        else -> emptyList()
    }

    /** Ключ игры для имён сохранений: имя файла, а у игр-папок (EBOOT.PBP) — имя папки игры. */
    fun key(path: String): String {
        val f = File(path)
        if (!f.name.equals("EBOOT.PBP", true)) return f.nameWithoutExtension
        var d = f.parentFile
        while (d != null && !File(d, MARK).exists()) d = d.parentFile
        // без отметки магазина — как раньше (сохранения прежних версий не теряются)
        return if (d == null) f.nameWithoutExtension else "dir_" + d.name
    }

    /** Отметка папки игры из магазина: что запускать и как называется. */
    const val MARK = ".kapsula.json"

    /** Игры-папки (из магазина): папка с отметкой и EBOOT внутри. */
    private fun dirGames(c: Context): List<JSONObject> =
        CoreFiles.gamesDir(c).listFiles()?.filter { it.isDirectory && File(it, MARK).exists() }?.mapNotNull { dirInfo(it) } ?: emptyList()

    /** Сведения об игре-папке для библиотеки (null — папка повреждена). */
    fun dirInfo(d: File): JSONObject? = try {
        val m = JSONObject(File(d, MARK).readText())
        val eboot = File(d, m.getString("eboot"))
        if (!eboot.exists()) null
        else JSONObject().put("id", "s_" + d.name.lowercase()).put("name", m.optString("name", d.name)).put("file", d.name + ".pbp")
            .put("path", eboot.absolutePath).put("size", d.walkTopDown().filter { it.isFile }.sumOf { it.length() } / (1024.0 * 1024.0))
            .put("plat", m.optString("plat", "psp")).put("store", m.optString("store"))
    } catch (e: Exception) { null }

    fun list(c: Context): String {
        ov = try { JSONObject(overrides(c).readText()) } catch (e: Exception) { JSONObject() }
        val a = JSONArray()
        // один id — одна игра: копия в папке приложения важнее такой же игры на месте
        val ids = HashSet<String>()
        fun add(o: JSONObject) { if (ids.add(o.getString("id"))) a.put(o) }
        dirGames(c).forEach { add(it) }
        val files = CoreFiles.gamesDir(c).listFiles()?.filter { it.isFile && it.extension.lowercase() in EXT && !it.name.endsWith(".part") } ?: emptyList()
        val hidden = files.flatMap { parts(it) }.map { it.name.lowercase() }.toSet()
        files.filter { it.name.lowercase() !in hidden }.sortedBy { it.name.lowercase() }.forEach { f -> add(info(f)) }
        linked(c).forEach { add(it) }
        return a.toString()
    }

    // ---------- игры на месте: папка KAPSULA и файлы, добавленные без копирования ----------

    /** Имя своей папки в корне памяти телефона. */
    const val FOLDER = "KAPSULA"

    /** Папка KAPSULA: игры, положенные сюда, появляются в библиотеке сами (нужен «Доступ ко всем файлам»). */
    fun folder(): File = File(android.os.Environment.getExternalStorageDirectory(), FOLDER)

    /** Создаёт папку KAPSULA, если есть доступ к файлам. true — папка на месте. */
    fun ensureFolder(c: Context): Boolean = try {
        com.nexus.psp.data.Storage.allFiles(c) && folder().let { it.isDirectory || it.mkdirs() }
    } catch (e: Exception) { false }

    fun inFolder(f: File): Boolean = f.absolutePath.startsWith(folder().absolutePath + File.separator)

    /** Игры на месте: путь → {p: путь, plat: консоль, mb: размер} (размер и консоль — чтобы показать игру, когда файла нет). */
    private fun linksFile(c: Context) = File(c.filesDir, "links.json")

    private fun readLinks(c: Context): LinkedHashMap<String, JSONObject> {
        val m = LinkedHashMap<String, JSONObject>()
        try {
            val a = JSONArray(linksFile(c).readText())
            for (i in 0 until a.length()) a.optJSONObject(i)?.let { o -> o.optString("p").takeIf { it.isNotEmpty() }?.let { m[it] = o } }
        } catch (e: Exception) { }
        return m
    }

    private fun writeLinks(c: Context, m: Map<String, JSONObject>) {
        try {
            val f = linksFile(c); val tmp = File(f.path + ".tmp")
            tmp.writeText(JSONArray(m.values.toList()).toString())
            if (!tmp.renameTo(f)) { f.writeText(tmp.readText()); tmp.delete() }
        } catch (e: Exception) { }
    }

    private fun entry(f: File, inf: JSONObject = info(f)) = JSONObject().put("p", f.absolutePath).put("plat", inf.optString("plat", "psp")).put("mb", inf.optDouble("size", 0.0))

    /** Игра на месте (из папки KAPSULA или выбранная в проводнике): лежит в общей памяти, не в папке приложения. */
    fun isLinked(c: Context, path: String): Boolean = readLinks(c).containsKey(path)

    /** Удаление такой игры только убирает её из библиотеки: файл чужой папки остаётся на месте. Папка KAPSULA — наша, оттуда удаляем. */
    fun keepsFile(c: Context, path: String): Boolean = isLinked(c, path) && !inFolder(File(path))

    /** Добавить игру без копирования — играть прямо из того места, где лежит файл. */
    @Synchronized
    fun link(c: Context, f: File): JSONObject {
        require(f.isFile) { "Файл «${f.name}» не найден" }
        require(f.extension.lowercase() in EXT) { "Такой файл KAPSULA не откроет. Нужен образ игры: ISO, CSO, CHD, PBP или CUE с BIN" }
        if (ov == null) ov = try { JSONObject(overrides(c).readText()) } catch (e: Exception) { JSONObject() }
        val m = readLinks(c)
        val inf = info(f)
        m[f.absolutePath] = entry(f, inf)
        writeLinks(c, m)
        return inf.put("linked", true)
    }

    /** Проверка «похоже на игру» для файлов из папки KAPSULA (по путям, размеру и времени — один раз). */
    private val folderOk = HashMap<String, Boolean>()

    /** Игры в папке KAPSULA и её подпапках (до 3 уровней); дорожки CUE и диски M3U — не отдельными играми. */
    private fun folderGames(dir: File): List<File> {
        val all = ArrayList<File>()
        fun walk(d: File, depth: Int) {
            if (depth > 3) return
            d.listFiles()?.forEach { f ->
                if (f.name.startsWith(".")) return@forEach
                if (f.isDirectory) walk(f, depth + 1)
                else if (f.extension.lowercase() in EXT && !f.name.endsWith(".part")) all += f
            }
        }
        walk(dir, 0)
        val hidden = all.flatMap { parts(it) }.map { it.absolutePath }.toSet()
        return all.filter { f ->
            f.absolutePath !in hidden && folderOk.getOrPut(f.path + ":" + f.length() + ":" + f.lastModified()) {
                val v = Disc.verdict(f.extension.lowercase(), f.length(), Disc.head(f))
                v == Disc.Verdict.OK || (v == Disc.Verdict.NO_MARK && f.extension.lowercase() in Disc.DISC_EXT)
            }
        }
    }

    /**
     * Игры на месте для библиотеки. Новые файлы папки KAPSULA добавляются сами. Файла нет на месте —
     * игра остаётся с отметкой missing (карта памяти вынута, нет доступа, файл перенесли): время в игре и
     * избранное не теряются. Исключение — файл исчез из доступной папки KAPSULA: его удалили, убираем и запись.
     */
    @Synchronized
    private fun linked(c: Context): List<JSONObject> {
        val m = readLinks(c)
        var changed = false
        val access = com.nexus.psp.data.Storage.allFiles(c)
        val dir = folder()
        val folderReady = access && dir.isDirectory
        if (folderReady) folderGames(dir).forEach { f -> if (!m.containsKey(f.absolutePath)) { m[f.absolutePath] = entry(f); changed = true } }
        val present = m.keys.map { File(it) }.filter { it.isFile }
        val hidden = present.flatMap { parts(it) }.map { it.absolutePath }.toSet()
        val out = ArrayList<JSONObject>()
        val it = m.entries.iterator()
        while (it.hasNext()) {
            val (p, e) = it.next()
            val f = File(p)
            when {
                f.isFile -> {
                    if (f.absolutePath in hidden) continue
                    val inf = info(f).put("linked", true)
                    if (e.optString("plat") != inf.optString("plat") || e.optDouble("mb") != inf.optDouble("size")) { e.put("plat", inf.optString("plat")).put("mb", inf.optDouble("size")); changed = true }
                    out += inf
                }
                folderReady && inFolder(f) -> { it.remove(); changed = true }
                else -> out += JSONObject().put("id", idOf(f.name)).put("name", f.nameWithoutExtension).put("file", f.name)
                    .put("path", f.absolutePath).put("size", e.optDouble("mb", 0.0)).put("plat", ov?.optString(f.name)?.takeIf { s -> s.isNotEmpty() } ?: e.optString("plat", "psp"))
                    .put("linked", true).put("missing", true)
            }
        }
        if (changed) writeLinks(c, m)
        return out
    }

    /** Платформа считается по содержимому один раз и запоминается рядом (по размеру и времени файла). */
    private val platCache = HashMap<String, String>()

    fun info(f: File): JSONObject {
        val key = f.path + ":" + f.length() + ":" + f.lastModified()
        val plat = ov?.optString(f.name)?.takeIf { it.isNotEmpty() } ?: platCache.getOrPut(key) { Disc.platform(f) }
        val size = f.length() + parts(f).sumOf { it.length() }
        return JSONObject()
            .put("id", idOf(f.name)).put("name", f.nameWithoutExtension).put("file", f.name)
            .put("path", f.absolutePath).put("size", size / (1024.0 * 1024.0)).put("plat", plat)
    }

    /** Недостающие файлы игры (CUE без своих BIN) — их нужно добавить вместе с ним. */
    fun missingParts(f: File): List<String> = parts(f).filter { !it.exists() }.map { it.name }

    fun idOf(name: String) = "f_" + name.lowercase().replace(Regex("[^a-z0-9а-яё]+"), "_").trim('_').take(40)

    fun displayName(c: Context, uri: Uri): Pair<String, Long> {
        c.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cur ->
            if (cur.moveToFirst()) return (cur.getString(0) ?: "game.iso") to (if (cur.isNull(1)) -1L else cur.getLong(1))
        }
        return (uri.lastPathSegment ?: "game.iso") to -1L
    }

    /** Копирует файл; onProgress — доля 0..1. Возвращает итоговый файл. */
    fun importFile(c: Context, uri: Uri, onProgress: (Double) -> Unit): File {
        val (name0, total) = displayName(c, uri)
        val name = name0.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        require(File(name).extension.lowercase() in EXT) { "Такой файл KAPSULA не откроет. Нужен образ игры: ISO, CSO, CHD, PBP или CUE с BIN" }
        val dir = CoreFiles.gamesDir(c)
        val out = File(dir, name)
        val part = File(dir, "$name.part")
        c.contentResolver.openInputStream(uri)!!.use { inp ->
            part.outputStream().use { o ->
                val buf = ByteArray(1 shl 20); var done = 0L; var last = -1
                while (true) {
                    val n = inp.read(buf); if (n < 0) break
                    o.write(buf, 0, n); done += n
                    if (total > 0) { val pct = (done * 100 / total).toInt(); if (pct != last) { last = pct; onProgress(done.toDouble() / total) } }
                }
            }
        }
        if (out.exists()) out.delete()
        check(part.renameTo(out)) { "Не сохранилось, попробуй ещё раз файл" }
        return out
    }

    /** Удаляет игру вместе с её частями (дорожки CUE, диски M3U). */
    fun delete(c: Context, path: String): Boolean {
        val f = File(path)
        // игра на месте: из папки KAPSULA удаляем файл, из других папок — только запись в библиотеке
        synchronized(this) {
            val m = readLinks(c)
            if (m.containsKey(path)) {
                if (inFolder(f) && f.exists()) {
                    parts(f).forEach { p -> if (p.parentFile?.absolutePath == f.parentFile?.absolutePath) p.delete() }
                    if (!f.delete()) return false
                }
                m.remove(path); writeLinks(c, m)
                return true
            }
        }
        // игра-папка из магазина — удаляется целиком
        val root = CoreFiles.gamesDir(c).canonicalPath
        var d = f.parentFile
        while (d != null && d.parentFile?.canonicalPath != root) d = d.parentFile
        if (d != null && d.canonicalPath != root && File(d, MARK).exists()) return d.deleteRecursively()
        if (f.parentFile?.canonicalPath != CoreFiles.gamesDir(c).canonicalPath) return false
        parts(f).forEach { p -> if (p.parentFile?.canonicalPath == f.parentFile?.canonicalPath) p.delete() }
        return f.delete()
    }
}
