package com.nexus.psp.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import com.nexus.psp.emulator.Disc
import com.nexus.psp.emulator.GameLibrary
import java.io.File

/**
 * Доступ к файлам игрока — два способа, работают вместе:
 *  - «Доступ ко всем файлам» (Android 11+) или разрешение на память (Android 8–10): виден весь телефон;
 *  - папки, которые игрок выбрал в системном окне (разрешение запоминается навсегда).
 * Узлы проводника и сканера — обычные файлы или документы выбранных папок.
 */
sealed class FNode {
    abstract val name: String
    abstract val isDir: Boolean
    abstract val size: Long
    abstract val uri: Uri
    abstract val key: String
    val ext get() = name.substringAfterLast('.', "").lowercase()

    class F(val file: File) : FNode() {
        override val name: String get() = file.name
        override val isDir get() = file.isDirectory
        override val size get() = if (file.isFile) file.length() else 0L
        override val uri: Uri get() = Uri.fromFile(file)
        override val key get() = file.path
    }

    class D(val tree: Uri, val docId: String, override val name: String, override val isDir: Boolean, override val size: Long) : FNode() {
        override val uri: Uri get() = DocumentsContract.buildDocumentUriUsingTree(tree, docId)
        override val key get() = tree.toString() + "#" + docId
    }
}

/** Место в проводнике: весь телефон, «Загрузки», карта памяти, выбранная папка. */
class Place(val title: String, val node: FNode, val icon: String)

object Storage {
    /** Виден весь телефон. */
    fun allFiles(c: Context): Boolean = try {
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else c.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) { false }

    /** Папки, которые игрок выбрал сам. */
    fun trees(c: Context): List<Uri> = c.contentResolver.persistedUriPermissions.filter { it.isReadPermission }.map { it.uri }
        .filter { DocumentsContract.isTreeUri(it) }

    fun forget(c: Context, tree: Uri) { try { c.contentResolver.releasePersistableUriPermission(tree, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { } }

    fun hasAny(c: Context) = allFiles(c) || trees(c).isNotEmpty()

    /** Места для левой колонки проводника. */
    fun places(c: Context): List<Place> {
        val out = ArrayList<Place>()
        if (allFiles(c)) {
            // своя папка KAPSULA — первой: игры из неё появляются в библиотеке сами
            GameLibrary.folder().takeIf { it.isDirectory }?.let { out += Place(GameLibrary.FOLDER, FNode.F(it), "folder") }
            val root = Environment.getExternalStorageDirectory()
            out += Place("Телефон", FNode.F(root), "phone")
            File(root, Environment.DIRECTORY_DOWNLOADS).takeIf { it.isDirectory }?.let { out += Place("Загрузки", FNode.F(it), "dl") }
            // карты памяти: корень тома из пути «…/Android/data/<пакет>/files»
            c.getExternalFilesDirs(null).drop(1).filterNotNull().mapNotNull { it.path.substringBefore("/Android/").let { p -> File(p).takeIf { f -> f.isDirectory } } }
                .forEach { out += Place("Карта памяти", FNode.F(it), "sd") }
        }
        trees(c).forEach { t ->
            val id = DocumentsContract.getTreeDocumentId(t)
            out += Place(Uri.decode(id).substringAfterLast(':').substringAfterLast('/').ifEmpty { "Папка" }, FNode.D(t, id, "", true, 0), "folder")
        }
        return out
    }

    /** Содержимое папки: сначала папки, потом файлы; скрытые и служебные Android — не показываем. */
    fun list(c: Context, n: FNode): List<FNode> {
        val raw: List<FNode> = when (n) {
            is FNode.F -> n.file.listFiles()?.map { FNode.F(it) } ?: emptyList()
            is FNode.D -> try {
                val kids = DocumentsContract.buildChildDocumentsUriUsingTree(n.tree, n.docId)
                val cols = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE)
                val out = ArrayList<FNode>()
                c.contentResolver.query(kids, cols, null, null, null)?.use { cur ->
                    while (cur.moveToNext()) {
                        val id = cur.getString(0) ?: continue; val name = cur.getString(1) ?: continue
                        out += FNode.D(n.tree, id, name, cur.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR, if (cur.isNull(3)) 0 else cur.getLong(3))
                    }
                }
                out
            } catch (e: Exception) { emptyList() }
        }
        return raw.filter { !it.name.startsWith(".") && !(it.isDir && it.name == "Android" && n is FNode.F && n.file == Environment.getExternalStorageDirectory()) }
            .sortedWith(compareBy<FNode>({ !it.isDir }, { it.name.lowercase() }))
    }

    /**
     * Итог проверки файла для проводника и поиска:
     *  - [Game] — содержимое похоже на игру, консоль определена;
     *  - [Unsure] — образ диска, который не удалось проверить до конца (не прочитался, нет привычной метки).
     *    Его можно добавить: эмулятор откроет файл сам, а при неудаче запуска будет понятная причина;
     *  - [No] — не игра, why — почему (показываем игроку вместо общего «не игра»).
     */
    sealed class Check {
        data class Game(val plat: String) : Check()
        data class Unsure(val plat: String, val why: String) : Check()
        data class No(val why: String) : Check()
    }

    const val NOT_SUPPORTED = "Такой формат KAPSULA не открывает"

    /** Консоль по расширению — когда содержимое прочитать не удалось. */
    fun platformByExt(ext: String): String = when (ext) {
        "cue", "m3u", "bin", "img" -> "ps1"
        "iso", "chd", "pbp", "cso" -> "psp"
        else -> PLATFORMS.firstOrNull { ext in it.exts }?.id ?: "psp"
    }

    /** Проверка файла: похож ли он на игру и для какой консоли. */
    fun check(c: Context, n: FNode, kids: List<FNode>? = null): Check {
        if (n.isDir || n.ext !in GameLibrary.EXT) return Check.No(NOT_SUPPORTED)
        val disc = n.ext in Disc.DISC_EXT
        fun unread(why: String) = if (disc) Check.Unsure(platformByExt(n.ext), why) else Check.No(why)
        // содержимое должно быть похоже на игру: README.md, описания и случайные .bin не проходят
        val head: ByteArray = when (n) {
            is FNode.F -> if (!n.file.canRead()) return unread("Нет доступа к файлу — проверим при запуске") else Disc.head(n.file)
            is FNode.D -> readHead(c, n) ?: return unread("Файл не удалось прочитать — проверим при запуске")
        }
        when (Disc.verdict(n.ext, n.size, head)) {
            Disc.Verdict.OK -> {}
            Disc.Verdict.TEXT -> return Check.No("Это текстовый файл, а не образ игры")
            Disc.Verdict.SMALL -> return Check.No("Файл слишком маленький для игры")
            Disc.Verdict.NO_MARK -> return unread("Внутри не нашлась метка диска. Если игра не запустится — образ повреждён")
        }
        return Check.Game(platformOf(c, n, kids, verified = true) ?: platformByExt(n.ext))
    }

    /** Начало документа выбранной папки: сначала поток, если провайдер его не даёт — файловый дескриптор. */
    private fun readHead(c: Context, n: FNode): ByteArray? {
        fun read(i: java.io.InputStream): ByteArray { val b = ByteArray(Disc.HEAD); var o = 0; while (o < b.size) { val r = i.read(b, o, b.size - o); if (r < 0) break; o += r }; return b.copyOf(o) }
        return try { c.contentResolver.openInputStream(n.uri)?.use { read(it) } } catch (e: Exception) { null }
            ?: try { c.contentResolver.openFileDescriptor(n.uri, "r")?.let { pfd -> android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { read(it) } } } catch (e: Exception) { null }
    }

    /** Консоль файла: у обычных файлов — по содержимому, у документов папок — по расширению. null — не игра. */
    fun platformOf(c: Context, n: FNode, kids: List<FNode>? = null, verified: Boolean = false): String? {
        if (n.isDir || n.ext !in GameLibrary.EXT) return null
        if (!verified) return (check(c, n, kids) as? Check.Game)?.plat
        if (n is FNode.F) return Disc.platform(n.file)
        // файл из выбранной папки: читаем содержимое через дескриптор — так же, как обычный файл
        val target = if (n.ext == "cue") kids?.firstOrNull { k -> !k.isDir && cueParts(c, n).firstOrNull()?.equals(k.name, true) == true } ?: n else n
        val ext = if (target === n) n.ext else target.ext
        if (n.ext == "cue" && target === n) return "ps1"
        return byDescriptor(c, target, ext) ?: platformByExt(n.ext)
    }

    /** Консоль файла-документа: открываем его и читаем как обычный файл (/proc/self/fd). null — не получилось. */
    private fun byDescriptor(c: Context, n: FNode, ext: String): String? = try {
        c.contentResolver.openFileDescriptor(n.uri, "r")?.use { pfd ->
            val f = java.io.File("/proc/self/fd/" + pfd.fd)
            if (!f.canRead()) null else Disc.platformOf(f, if (ext == "cue") "bin" else ext)
        }
    } catch (e: Exception) { null }

    /** Имена дорожек из CUE (читается и у обычных файлов, и у документов). */
    fun cueParts(c: Context, n: FNode): List<String> = try {
        c.contentResolver.openInputStream(n.uri)?.bufferedReader()?.readLines()?.mapNotNull { l ->
            val t = l.trim(); if (!t.startsWith("FILE", true)) null
            else (if ('"' in t) t.substringAfter('"').substringBefore('"') else t.removePrefix("FILE").trim().substringBeforeLast(' ')).substringAfterLast('/').substringAfterLast('\\')
        } ?: emptyList()
    } catch (e: Exception) { emptyList() }

    // ---------- BIOS среди найденного ----------
    private val BIOS_NAMES = setOf("aes.zip", "neogeo.zip", "lynxboot.img", "dc_boot.bin", "saturn_bios.bin")

    /** Похоже на BIOS: известное имя или подходящий размер. Какая консоль — уточняется при подключении. */
    fun maybeBios(n: FNode): String? {
        val nm = n.name.lowercase()
        return when {
            nm in setOf("aes.zip", "neogeo.zip") -> "neogeo"
            nm == "lynxboot.img" || n.size == 512L && nm.endsWith(".img") -> "lynx"
            nm == "dc_boot.bin" || (n.size == 2L * 1024 * 1024 && nm.contains("dc")) -> "dc"
            nm.contains("saturn") && n.size == 512L * 1024 -> "saturn"
            n.size == 512L * 1024 && (nm.startsWith("scph") || nm.contains("psx") || nm.contains("ps1")) -> "ps1"
            n.size == 4L * 1024 * 1024 && (nm.startsWith("scph") || nm.contains("ps2")) -> "ps2"
            else -> null
        }
    }
}
