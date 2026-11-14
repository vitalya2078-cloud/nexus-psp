package com.nexus.psp.emulator

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Свои образы игр пользователя: копия в папке приложения (Android/data/…/files/games). */
object GameLibrary {
    /** PSP: ISO, CSO, CHD, PBP, ELF, PRX. PS1: CUE + BIN, BIN, IMG, CHD, PBP, M3U (несколько дисков). */
    val EXT = setOf("iso", "cso", "chd", "pbp", "elf", "prx", "cue", "bin", "img", "m3u")

    /** Части игры, которые не показываются отдельно: дорожки из CUE и диски из M3U. */
    private fun parts(f: File): List<File> = when (f.extension.lowercase()) {
        "cue" -> Disc.cueTracks(f)
        "m3u" -> Disc.m3uItems(f).flatMap { listOf(it) + parts(it) }
        else -> emptyList()
    }

    fun list(c: Context): String {
        val a = JSONArray()
        val files = CoreFiles.gamesDir(c).listFiles()?.filter { it.isFile && it.extension.lowercase() in EXT && !it.name.endsWith(".part") } ?: emptyList()
        val hidden = files.flatMap { parts(it) }.map { it.name.lowercase() }.toSet()
        files.filter { it.name.lowercase() !in hidden }.sortedBy { it.name.lowercase() }.forEach { f -> a.put(info(f)) }
        return a.toString()
    }

    /** Платформа считается по содержимому один раз и запоминается рядом (по размеру и времени файла). */
    private val platCache = HashMap<String, String>()

    fun info(f: File): JSONObject {
        val key = f.path + ":" + f.length() + ":" + f.lastModified()
        val plat = platCache.getOrPut(key) { Disc.platform(f) }
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
        require(File(name).extension.lowercase() in EXT) { "Формат не поддерживается: нужен ISO, CSO, CHD, PBP, CUE + BIN или M3U" }
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
        check(part.renameTo(out)) { "Не удалось сохранить файл" }
        return out
    }

    /** Удаляет игру вместе с её частями (дорожки CUE, диски M3U). */
    fun delete(c: Context, path: String): Boolean {
        val f = File(path)
        if (f.parentFile?.canonicalPath != CoreFiles.gamesDir(c).canonicalPath) return false
        parts(f).forEach { p -> if (p.parentFile?.canonicalPath == f.parentFile?.canonicalPath) p.delete() }
        return f.delete()
    }
}
