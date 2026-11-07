package com.nexus.psp.emulator

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Свои образы игр пользователя: копия в папке приложения (Android/data/…/files/games). */
object GameLibrary {
    val EXT = setOf("iso", "cso", "chd", "pbp", "elf", "prx")

    fun list(c: Context): String {
        val a = JSONArray()
        CoreFiles.gamesDir(c).listFiles()?.filter { it.isFile && it.extension.lowercase() in EXT && !it.name.endsWith(".part") }
            ?.sortedBy { it.name.lowercase() }
            ?.forEach { f -> a.put(info(f)) }
        return a.toString()
    }

    fun info(f: File): JSONObject = JSONObject()
        .put("id", idOf(f.name)).put("name", f.nameWithoutExtension).put("file", f.name)
        .put("path", f.absolutePath).put("size", f.length() / (1024.0 * 1024.0)).put("plat", "psp")

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
        require(File(name).extension.lowercase() in EXT) { "Формат не поддерживается: нужен ISO, CSO, CHD или PBP" }
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

    fun delete(c: Context, path: String): Boolean {
        val f = File(path)
        return f.parentFile?.canonicalPath == CoreFiles.gamesDir(c).canonicalPath && f.delete()
    }
}
