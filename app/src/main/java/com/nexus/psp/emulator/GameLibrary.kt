package com.nexus.psp.emulator

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/** Свои образы игр пользователя: копия в папке приложения (Android/data/…/files/games). */
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
        dirGames(c).forEach { a.put(it) }
        val files = CoreFiles.gamesDir(c).listFiles()?.filter { it.isFile && it.extension.lowercase() in EXT && !it.name.endsWith(".part") } ?: emptyList()
        val hidden = files.flatMap { parts(it) }.map { it.name.lowercase() }.toSet()
        files.filter { it.name.lowercase() !in hidden }.sortedBy { it.name.lowercase() }.forEach { f -> a.put(info(f)) }
        return a.toString()
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
        // Разные SAF-провайдеры по-разному реализуют OpenableColumns и могут
        // выбрасывать исключение для documentUri, построенного из treeUri.
        try {
            c.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cur ->
                if (cur.moveToFirst()) {
                    val name = cur.getString(0)?.takeIf { it.isNotBlank() }
                    if (name != null) return name to (if (cur.isNull(1)) -1L else cur.getLong(1))
                }
            }
        } catch (_: Exception) { }
        // Для SAF берём имя из documentId (например, primary:Games/title.iso),
        // а не из последнего сегмента URI, который у некоторых провайдеров — числовой ID.
        val documentName = try {
            if (android.provider.DocumentsContract.isDocumentUri(c, uri))
                android.provider.DocumentsContract.getDocumentId(uri).substringAfterLast('/').substringAfterLast(':')
            else null
        } catch (_: Exception) { null }
        val fallback = documentName?.takeIf { it.contains('.') }
            ?: Uri.decode(uri.lastPathSegment.orEmpty()).substringAfterLast('/').substringAfterLast(':').takeIf { it.contains('.') }
            ?: "game.iso"
        return fallback to -1L
    }

    /**
     * Копирует поток во временный файл и проверяет размер, если источник его сообщил.
     * 100% прогресса показываем только после успешной проверки.
     */
    internal fun copyStreamToFile(input: InputStream, target: File, expectedSize: Long, onProgress: (Double) -> Unit): Long {
        var copied = 0L
        try {
            FileOutputStream(target, false).use { output ->
                val buffer = ByteArray(1 shl 20)
                var lastPercent = -1
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) {
                        val one = input.read()
                        if (one < 0) break
                        output.write(one)
                        copied++
                    } else {
                        output.write(buffer, 0, count)
                        copied += count
                    }
                    if (expectedSize > 0) {
                        val percent = (copied * 100 / expectedSize).toInt().coerceIn(0, 99)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent / 100.0)
                        }
                    }
                }
                output.fd.sync()
            }
            if (copied == 0L) throw IOException("Выбранный файл пустой")
            if (expectedSize > 0 && copied != expectedSize) {
                throw IOException("Файл скопирован не полностью: получено $copied из $expectedSize байт")
            }
            onProgress(1.0)
            return copied
        } catch (e: Exception) {
            target.delete()
            throw e
        }
    }

    /** Копирует файл; исходная версия в библиотеке сохраняется до успешной проверки новой копии. */
    fun importFile(c: Context, uri: Uri, onProgress: (Double) -> Unit): File {
        val (name0, total) = displayName(c, uri)
        val name = name0.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        require(File(name).extension.lowercase() in EXT) { "Такой файл KAPSULA не откроет. Нужен образ игры: ISO, CSO, CHD, PBP или CUE с BIN" }
        val dir = CoreFiles.gamesDir(c)
        val out = File(dir, name)
        val part = File(dir, "$name.part")
        val backup = File(dir, ".$name.kapsula-backup")
        try {
            // Некоторые SAF-провайдеры не отдают InputStream напрямую,
            // но предоставляют читаемый файловый дескриптор.
            val input = try { c.contentResolver.openInputStream(uri) } catch (_: Exception) { null }
                ?: try {
                    c.contentResolver.openFileDescriptor(uri, "r")?.let {
                        android.os.ParcelFileDescriptor.AutoCloseInputStream(it)
                    }
                } catch (_: Exception) { null }
                ?: throw IOException("Не удалось открыть файл через Android SAF. Проверь доступ к выбранной папке.")
            input.use { copyStreamToFile(it, part, total, onProgress) }

            // Восстанавливаем резервную копию, если предыдущий запуск прервался между переименованиями.
            if (backup.exists()) {
                if (!out.exists()) {
                    if (!backup.renameTo(out)) throw IOException("Не удалось восстановить прежнюю копию игры")
                } else if (!backup.delete()) {
                    throw IOException("Не удалось очистить временную резервную копию")
                }
            }

            val hadOriginal = out.exists()
            if (hadOriginal && !out.renameTo(backup)) {
                throw IOException("Не удалось сохранить прежнюю копию игры")
            }
            if (!part.renameTo(out)) {
                if (hadOriginal && backup.exists()) backup.renameTo(out)
                throw IOException("Не удалось сохранить новую копию игры")
            }
            if (backup.exists()) backup.delete()
            return out
        } catch (e: Exception) {
            part.delete()
            throw e
        }
    }

    /** Удаляет игру вместе с её частями (дорожки CUE, диски M3U). */
    fun delete(c: Context, path: String): Boolean {
        val f = File(path)
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
