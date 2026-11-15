package com.nexus.psp.emulator

import android.content.Context
import java.io.File
import java.io.RandomAccessFile

/**
 * Почему игра не запустилась. Эмулятор сообщает только «не открылось», поэтому KAPSULA сама проверяет файл
 * (доступ, формат, дорожки CUE, целостность, чья это игра) и читает журнал эмулятора.
 * Причина — понятным текстом на экране ошибки и в «Отчёт об ошибке» (launch_fail.txt).
 */
object LaunchCheck {
    /** text — что случилось; asPlat — игра другой консоли: предложить «Запустить как …». */
    data class Why(val text: String, val asPlat: String? = null)

    fun diagnose(file: File, plat: String, coreLog: List<String> = emptyList()): Why {
        val pl = com.nexus.psp.data.PL[plat]
        val name = pl?.short ?: plat
        val ext = file.extension.lowercase()
        if (!file.exists()) return Why("Файл игры пропал. Похоже, его удалили или переместили")
        if (!file.canRead()) return Why("Нет доступа к файлу. Добавь игру заново через «Добавить игру»")
        // дорожки CUE и диски M3U должны лежать рядом
        val missing = when (ext) {
            "cue" -> Disc.cueTracks(file).filter { !it.exists() }
            "m3u" -> Disc.m3uItems(file).filter { !it.exists() }
            else -> emptyList()
        }
        if (missing.isNotEmpty()) return Why("Не хватает файла «${missing.first().name}» — положи его в ту же папку, что и «${file.name}»")
        // формат, который эта консоль не открывает
        val okExt = pl?.exts.orEmpty() + (if (plat == "ps1") listOf("cue", "bin", "img", "m3u") else emptyList())
        if (pl != null && ext !in okExt) {
            val other = Disc.platformOf(file)?.takeIf { it != plat && com.nexus.psp.data.PL[it]?.exts?.contains(ext) == true }
            return Why("$name не открывает файлы .$ext. Подходят: ${okExt.distinct().joinToString(", ") { it.uppercase() }}", other)
        }
        // внутри не игра (переименованный файл, страница вместо образа, обрыв в самом начале)
        if (!Disc.plausible(ext, file.length(), Disc.head(file))) return Why("Внутри файла не образ игры. Возможно, он повреждён, не докачался или переименован")
        // образ короче, чем записано в нём самом
        short(file, ext)?.let { (have, need) -> return Why("Файл неполный — похоже, не докачался (${mb(have)} из ${mb(need)}). Скачай игру заново") }
        // по содержимому — игра другой консоли
        val real = Disc.platformOf(file)
        if (real != null && real != plat && ext in setOf("iso", "cso", "chd", "bin", "cue", "img", "pbp") && strong(file, ext)) {
            val rn = com.nexus.psp.data.PL[real]?.short ?: real
            return Why("Похоже, это игра для $rn, а запускается как $name", real)
        }
        // журнал эмулятора
        fromLog(coreLog, plat)?.let { return Why(it) }
        return Why("Эмулятор $name не смог открыть эту игру. Возможно, образ повреждён или эта игра пока не поддерживается")
    }

    /** Подсказка под причиной (если консоль не предложили сменить сами). */
    const val HINT = "Не та консоль? Её можно сменить в карточке игры: «Для какой консоли игра»"

    /** Консоль по файлу определена надёжно (по метке диска, а не «по умолчанию»). */
    private fun strong(f: File, ext: String): Boolean = when (ext) {
        "iso", "cso", "bin", "img", "cue" -> true   // метка тома / заголовок Sega прочитаны: plausible уже проверил CD001
        "pbp" -> true
        "chd" -> Disc.platformOf(f) != "psp"            // маленький DVD-образ без подсказок — не уверены
        else -> false
    }

    private fun mb(b: Long) = if (b >= 1L shl 30) "%.1f ГБ".format(b / 1073741824.0) else "${b / 1048576} МБ"

    /** (есть, нужно), если образ ISO 9660 или CSO обрезан. */
    private fun short(f: File, ext: String): Pair<Long, Long>? = try {
        RandomAccessFile(f, "r").use { r ->
            when (ext) {
                "iso" -> {
                    if (r.length() % 2352 == 0L) return null   // сырой образ — размер тома в другом масштабе
                    val pvd = ByteArray(2048); r.seek(16L * 2048); r.readFully(pvd)
                    if (String(pvd, 1, 5, Charsets.US_ASCII) != "CD001") return null
                    val blocks = le32(pvd, 80).toLong() and 0xFFFFFFFFL
                    val need = blocks * 2048
                    // небольшой запас: некоторые программы записи обрезают хвост из пустых секторов
                    if (need > 0 && r.length() < need - 2L * 1024 * 1024) r.length() to need else null
                }
                "cso" -> {
                    val h = ByteArray(24); r.readFully(h)
                    val total = (0..7).fold(0L) { a, i -> a or ((h[8 + i].toLong() and 0xFF) shl (8 * i)) }
                    val bs = le32(h, 16).toLong(); val align = h[21].toInt() and 0xFF
                    if (bs <= 0 || total <= 0) return null
                    val n = (total + bs - 1) / bs
                    val idx = ByteArray(4); r.seek(24 + n * 4); r.readFully(idx)
                    val end = (le32(idx, 0).toLong() and 0x7FFFFFFF) shl align
                    if (end > r.length()) r.length() to end else null
                }
                else -> null
            }
        }
    } catch (e: Exception) { null }

    private fun le32(b: ByteArray, o: Int) = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)

    /** Понятная причина из сообщений эмулятора (null — не узнали). */
    fun fromLog(lines: List<String>, plat: String): String? {
        val t = lines.joinToString("\n").lowercase()
        val name = com.nexus.psp.data.PL[plat]?.short ?: plat
        return when {
            "bios" in t && ("not found" in t || "missing" in t || "invalid" in t || "failed" in t || "couldn't" in t || "could not" in t) ->
                "Эмулятор $name не принял BIOS. Нужен BIOS с твоей приставки: Настройки → BIOS"
            "out of memory" in t || "failed to allocate" in t || "cannot allocate" in t || "bad_alloc" in t ->
                "Телефону не хватило памяти для этой игры. Закрой другие приложения и попробуй снова"
            "unsupported" in t || "not supported" in t -> "Эмулятор $name не поддерживает этот образ"
            "no such file" in t || "failed to open" in t || "cannot open" in t || "could not open" in t ->
                "Эмулятор не смог прочитать файл игры. Добавь игру заново через «Добавить игру»"
            else -> null
        }
    }

    /** Последние сообщения эмулятора из журнала своего процесса. */
    fun coreLog(): List<String> = try {
        val p = ProcessBuilder("logcat", "-d", "-v", "brief", "-t", "800", "--pid=" + android.os.Process.myPid()).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readLines(); p.waitFor()
        out.filter { "Libretro Core" in it || "libretrodroid" in it }.takeLast(40)
    } catch (e: Exception) { emptyList() }

    /** Записать причину и журнал — уходит в «Отчёт об ошибке». */
    fun save(c: Context, game: String, plat: String, why: Why, log: List<String>) = try {
        File(c.filesDir, "launch_fail.txt").writeText("Игра: $game ($plat)\nПричина: ${why.text}\n" + log.joinToString("\n") + "\n")
    } catch (e: Exception) { }

    fun saved(c: Context): String? = File(c.filesDir, "launch_fail.txt").takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
}
