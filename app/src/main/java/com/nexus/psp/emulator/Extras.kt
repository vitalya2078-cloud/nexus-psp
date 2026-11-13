package com.nexus.psp.emulator

import android.content.Context
import android.net.Uri
import java.io.File
import java.net.URL
import java.util.zip.ZipInputStream

/** Чит из базы CWCheat: название и строки кода (_L адрес значение). */
data class Cheat(val name: String, val code: String, val own: Boolean = false)

/**
 * Читы: открытая база CWCheat (скачивается один раз, ~9 МБ, хранится на телефоне).
 * Ядро получает чит через libretro (retro_cheat_set) и включает его сразу, без перезапуска.
 */
object CheatDb {
    private const val URL_DB = "https://raw.githubusercontent.com/Saramagrean/CWCheat-Database-Plus-/refs/heads/master/cheat.db"
    fun file(c: Context) = File(c.filesDir, "cheat.db")
    fun ready(c: Context) = file(c).let { it.exists() && it.length() > 100_000 }

    /** Скачать базу (в фоновом потоке). progress — 0..1. */
    fun download(c: Context, progress: (Float) -> Unit): Boolean = try {
        val con = URL(URL_DB).openConnection().apply { connectTimeout = 15000; readTimeout = 30000 }
        val total = con.contentLengthLong.coerceAtLeast(1)
        val tmp = File(c.filesDir, "cheat.db.part")
        con.getInputStream().use { inp -> tmp.outputStream().use { out ->
            val buf = ByteArray(64 * 1024); var n: Int; var done = 0L
            while (inp.read(buf).also { n = it } > 0) { out.write(buf, 0, n); done += n; progress((done.toFloat() / total).coerceAtMost(1f)) }
        } }
        tmp.renameTo(file(c))
    } catch (e: Exception) { false }

    /** Читы для игры по её коду диска (ULUS-10041 или ULUS10041). */
    fun forGame(c: Context, serial: String): List<Cheat> {
        val f = file(c); if (!f.exists()) return emptyList()
        val want = serial.uppercase().replace("-", "")
        val out = ArrayList<Cheat>()
        var inGame = false; var name: String? = null; val code = StringBuilder()
        fun flush() { name?.let { if (code.isNotEmpty()) out.add(Cheat(it, code.toString().trim().trimEnd('+'))) }; name = null; code.clear() }
        f.bufferedReader(Charsets.ISO_8859_1).useLines { lines ->
            for (raw in lines) {
                val l = raw.trim()
                when {
                    l.startsWith("_S ") -> { flush(); if (inGame) break; inGame = l.substring(3).trim().uppercase().replace("-", "") == want }
                    !inGame -> {}
                    l.startsWith("_C") -> { flush(); name = l.substring(2).trimStart('0', '1').trim().ifEmpty { "Чит" } }
                    l.startsWith("_L ") && name != null -> code.append(l).append('+')
                }
            }
        }
        flush()
        return out
    }
}

/**
 * HD-текстуры: набор текстур для игры (ZIP с textures.ini) распаковывается в папку ядра PSP/TEXTURES/<код диска>.
 * Ядро подхватывает их при запуске игры (ppsspp_texture_replacement).
 */
object Textures {
    fun dir(c: Context, serial: String) = File(CoreFiles.savesDir(c), "PSP/TEXTURES/" + serial.uppercase().replace("-", ""))
    fun installed(c: Context, serial: String) = File(dir(c, serial), "textures.ini").exists()
    fun sizeMb(c: Context, serial: String): Long = dir(c, serial).walkTopDown().filter { it.isFile }.sumOf { it.length() } / 1048576

    /** Распаковать ZIP: корень набора — папка, где лежит textures.ini (или сам архив). */
    fun import(c: Context, uri: Uri, serial: String, progress: (Int) -> Unit): String? {
        val cr = c.contentResolver
        // 1) найти, где в архиве textures.ini
        var root: String? = null
        cr.openInputStream(uri)?.use { s -> ZipInputStream(s).use { z ->
            var e = z.nextEntry
            while (e != null) { if (!e.isDirectory && e.name.substringAfterLast('/').equals("textures.ini", true)) { root = e.name.substringBeforeLast('/', ""); break }; e = z.nextEntry }
        } } ?: return "Не удалось открыть файл"
        val r = root ?: return "В архиве нет textures.ini — это не набор текстур PPSSPP-формата"
        val dst = dir(c, serial); dst.deleteRecursively(); dst.mkdirs()
        var count = 0
        cr.openInputStream(uri)?.use { s -> ZipInputStream(s).use { z ->
            var e = z.nextEntry
            while (e != null) {
                val n = e.name
                if (!e.isDirectory && (r.isEmpty() || n.startsWith("$r/"))) {
                    val rel = if (r.isEmpty()) n else n.removePrefix("$r/")
                    val out = File(dst, rel)
                    if (out.canonicalPath.startsWith(dst.canonicalPath)) { out.parentFile?.mkdirs(); out.outputStream().use { z.copyTo(it) }; count++; if (count % 50 == 0) progress(count) }
                }
                e = z.nextEntry
            }
        } }
        return if (installed(c, serial)) null else "Набор не распаковался"
    }

    fun remove(c: Context, serial: String) { dir(c, serial).deleteRecursively() }
}

/** Игра по сети (ad-hoc PSP через встроенный сервер ядра). Хост: сервер у себя; гость: адрес хоста. */
object NetPlay {
    /** Настройки ядра для режима: host / join (ip) / off. */
    fun vars(mode: String, ip: String, mac: String): Map<String, String> {
        if (mode != "host" && mode != "join") return mapOf("ppsspp_enable_wlan" to "disabled")
        val m = HashMap<String, String>()
        m["ppsspp_enable_wlan"] = "enabled"
        m["ppsspp_enable_builtin_pro_ad_hoc_server"] = if (mode == "host") "enabled" else "disabled"
        if (mode == "host") m["ppsspp_change_pro_ad_hoc_server_address"] = "localhost"
        else {
            m["ppsspp_change_pro_ad_hoc_server_address"] = "IP address"
            // адрес — 12 цифр: по 3 на каждое число IPv4 (192.168.1.5 → 192 168 001 005)
            val digits = ip.split('.').joinToString("") { it.trim().padStart(3, '0') }
            digits.forEachIndexed { i, d -> m["ppsspp_pro_ad_hoc_server_address%02d".format(i + 1)] = d.toString() }
        }
        mac.filter { it.isLetterOrDigit() }.take(12).forEachIndexed { i, d -> m["ppsspp_change_mac_address%02d".format(i + 1)] = d.lowercase() }
        return m
    }

    fun validIp(ip: String) = ip.split('.').let { p -> p.size == 4 && p.all { it.toIntOrNull()?.let { n -> n in 0..255 } == true } }

    /** Постоянный MAC этого телефона для сети PSP (создаётся один раз). */
    fun mac(c: Context): String {
        val f = File(c.filesDir, "psp_mac.txt")
        f.takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.length == 12 }?.let { return it }
        val r = java.util.Random()
        // локально администрируемый адрес: второй бит первого байта = 1
        val b = ByteArray(6).also { r.nextBytes(it) }; b[0] = ((b[0].toInt() and 0xFC) or 0x02).toByte()
        return b.joinToString("") { "%02x".format(it) }.also { f.writeText(it) }
    }

    /** IP этого телефона в Wi-Fi-сети (для «Создать комнату»). */
    fun myIp(c: Context): String? = try {
        val cm = c.getSystemService(android.net.ConnectivityManager::class.java)
        cm?.getLinkProperties(cm.activeNetwork)?.linkAddresses?.map { it.address }
            ?.firstOrNull { it is java.net.Inet4Address && !it.isLoopbackAddress }?.hostAddress
    } catch (e: Exception) { null }
}
