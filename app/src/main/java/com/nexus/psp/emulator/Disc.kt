package com.nexus.psp.emulator

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Inflater

/**
 * Чтение образа PSP без эмулятора: серийный номер диска (ULES-01298), название и картинки игры
 * (ICON0 — значок, PIC1 — широкий фон). Поддержка: ISO, CSO (сжатый ISO, CISO v1), PBP (EBOOT).
 * CHD пока не читаем — для него обложка ищется по названию файла.
 */
object Disc {

    data class Info(val serial: String?, val title: String?, val icon: ByteArray?, val pic: ByteArray?)

    fun read(file: File): Info? = try {
        when (file.extension.lowercase()) {
            "iso" -> IsoSource(RandomAccessFile(file, "r")).use { s -> if (isPsx(s)) fromPsx(s) else fromIso(s) }
            "cso" -> CsoSource(RandomAccessFile(file, "r")).use { fromIso(it) }
            "pbp" -> fromPbp(file)
            "bin", "img" -> RawSource.open(file)?.use { fromPsx(it) }
            "cue" -> cueTracks(file).firstOrNull()?.let { RawSource.open(it) }?.use { fromPsx(it) }
            else -> null
        }
    } catch (e: Exception) { null }

    // ---------- консоль по файлу ----------
    /**
     * PSP или PS1: CSO/ELF/PRX — PSP; CUE/M3U/BIN/IMG — PS1; ISO — по метке тома («PSP GAME» / «PLAYSTATION»);
     * PBP — PS1-игры в формате PSP («PSISOIMG» в данных); CHD — CD-образ (PS1) или DVD (PSP).
     */
    fun platform(file: File): String = try {
        when (file.extension.lowercase()) {
            "cue", "m3u", "bin", "img" -> "ps1"
            "iso" -> IsoSource(RandomAccessFile(file, "r")).use { if (isPsx(it)) "ps1" else "psp" }
            "pbp" -> if (pbpIsPs1(file)) "ps1" else "psp"
            "chd" -> if (chdIsCd(file)) "ps1" else "psp"
            else -> "psp"
        }
    } catch (e: Exception) { "psp" }

    private fun isPsx(s: Sectors): Boolean {
        val pvd = s.read(16, 1)
        return String(pvd, 1, 5, Charsets.US_ASCII) == "CD001" && String(pvd, 8, 32, Charsets.US_ASCII).trim().startsWith("PLAYSTATION")
    }

    /** PS1 на PSP (PSN): в DATA.PSAR лежит «PSISOIMG» (один диск) или «PSTITLEIMG» (несколько). */
    private fun pbpIsPs1(f: File): Boolean = RandomAccessFile(f, "r").use { r ->
        val h = ByteArray(40); r.readFully(h)
        if (h[1] != 'P'.code.toByte() || h[2] != 'B'.code.toByte()) return false
        val psar = le32(h, 8 + 7 * 4).toLong()
        if (psar <= 0 || psar + 10 > r.length()) return false
        val m = ByteArray(10); r.seek(psar); r.readFully(m)
        val t = String(m, Charsets.US_ASCII)
        t.startsWith("PSISOIMG") || t.startsWith("PSTITLEIMG")
    }

    /** CHD v5: в метаданных есть дорожки CD (CHT2/CHTR/CHCD) — значит, это компакт-диск. */
    private fun chdIsCd(f: File): Boolean = RandomAccessFile(f, "r").use { r ->
        val h = ByteArray(124); r.readFully(h)
        if (String(h, 0, 8, Charsets.US_ASCII) != "MComprHD") return false
        val ver = be32(h, 12)
        var off = if (ver >= 5) be64(h, 48) else if (ver >= 3) be64(h, 36) else return false
        var n = 0
        while (off > 0 && off < r.length() && n++ < 64) {
            val m = ByteArray(16); r.seek(off); r.readFully(m)
            val tag = String(m, 0, 4, Charsets.US_ASCII)
            if (tag == "CHT2" || tag == "CHTR" || tag == "CHCD" || tag == "CHGD") return true
            if (tag == "DVD ") return false
            off = be64(m, 8)
        }
        false
    }

    /** Файлы дорожек из CUE (рядом с ним). */
    fun cueTracks(cue: File): List<File> = try {
        cue.readLines().mapNotNull { l ->
            val t = l.trim()
            if (!t.startsWith("FILE", true)) null
            else {
                val name = if ('"' in t) t.substringAfter('"').substringBefore('"') else t.removePrefix("FILE").trim().substringBeforeLast(' ')
                File(cue.parentFile, name.substringAfterLast('/').substringAfterLast('\\'))
            }
        }
    } catch (e: Exception) { emptyList() }

    /** Список дисков из M3U. */
    fun m3uItems(m3u: File): List<File> = try {
        m3u.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.map { File(m3u.parentFile, it.substringAfterLast('/').substringAfterLast('\\')) }
    } catch (e: Exception) { emptyList() }

    private fun be32(b: ByteArray, o: Int) = ((b[o].toInt() and 0xFF) shl 24) or ((b[o + 1].toInt() and 0xFF) shl 16) or ((b[o + 2].toInt() and 0xFF) shl 8) or (b[o + 3].toInt() and 0xFF)
    private fun be64(b: ByteArray, o: Int) = (be32(b, o).toLong() shl 32) or (be32(b, o + 4).toLong() and 0xFFFFFFFFL)

    /** PS1: номер из SYSTEM.CNF («BOOT = cdrom:\\SLUS_005.94;1» → SLUS-00594). */
    private fun fromPsx(s: Sectors): Info {
        val pvd = s.read(16, 1)
        require(String(pvd, 1, 5, Charsets.US_ASCII) == "CD001") { "not iso9660" }
        val root = Entry(le32(pvd, 156 + 2).toLong(), le32(pvd, 156 + 10), true)
        val cnf = file(s, root, "SYSTEM.CNF")?.let { String(it, Charsets.US_ASCII) }
        val boot = cnf?.lineSequence()?.firstOrNull { it.trim().startsWith("BOOT", true) }?.substringAfter('=')?.trim()
        val exe = boot?.substringAfterLast('\\')?.substringAfterLast(':')?.substringBefore(';')
        return Info(normSerial(exe), null, null, null)
    }

    /**
     * Хеш игры для RetroAchievements — тот же алгоритм, что rc_hash_psp в rcheevos:
     * ISO/CSO — MD5 от содержимого PSP_GAME/PARAM.SFO, затем PSP_GAME/SYSDIR/EBOOT.BIN (каждый не больше 64 МБ);
     * PBP — MD5 всего файла.
     */
    fun raHash(file: File): String? = try {
        val md = java.security.MessageDigest.getInstance("MD5")
        when (file.extension.lowercase()) {
            "pbp" -> file.inputStream().use { inp -> val buf = ByteArray(1 shl 16); var n: Int; while (inp.read(buf).also { n = it } > 0) md.update(buf, 0, n) }
            "iso", "cso" -> (if (file.extension.equals("iso", true)) IsoSource(RandomAccessFile(file, "r")) else CsoSource(RandomAccessFile(file, "r"))).use { s ->
                val pvd = s.read(16, 1)
                require(String(pvd, 1, 5, Charsets.US_ASCII) == "CD001")
                val root = Entry(le32(pvd, 156 + 2).toLong(), le32(pvd, 156 + 10), true)
                for (path in listOf("PSP_GAME/PARAM.SFO", "PSP_GAME/SYSDIR/EBOOT.BIN")) {
                    var e: Entry = root
                    for (part in path.split('/')) e = find(s, e, part) ?: error("нет $path")
                    var left = minOf(e.size, 64 * 1024 * 1024); var lba = e.lba
                    while (left > 0) {
                        val cnt = minOf(256, (left + 2047) / 2048)
                        val b = s.read(lba, cnt); val take = minOf(left, b.size)
                        md.update(b, 0, take); left -= take; lba += cnt
                    }
                }
            }
            else -> error("не PSP-образ")
        }
        md.digest().joinToString("") { "%02x".format(it) }
    } catch (e: Exception) { null }

    // ---------- источник секторов по 2048 байт ----------
    private interface Sectors : Closeable { fun read(lba: Long, count: Int): ByteArray }

    private class IsoSource(val f: RandomAccessFile) : Sectors {
        override fun read(lba: Long, count: Int): ByteArray { val b = ByteArray(count * 2048); f.seek(lba * 2048); f.readFully(b); return b }
        override fun close() = f.close()
    }

    /** CISO v1: заголовок 24 байта, таблица смещений блоков; старший бит — блок не сжат; данные — raw deflate. */
    /** Сырой образ CD (BIN/IMG, 2352 байта на сектор): данные режима 1 — со смещения 16, режима 2 (PS1) — с 24. ISO по 2048 — как есть. */
    private class RawSource(val f: RandomAccessFile, val size: Int, val off: Int) : Sectors {
        override fun read(lba: Long, count: Int): ByteArray {
            val out = ByteArray(count * 2048); val sec = ByteArray(size)
            for (i in 0 until count) { f.seek((lba + i) * size); f.readFully(sec); System.arraycopy(sec, off, out, i * 2048, 2048) }
            return out
        }
        override fun close() = f.close()
        companion object {
            fun open(file: File): RawSource? {
                val r = RandomAccessFile(file, "r")
                if (r.length() % 2352 != 0L) return RawSource(r, 2048, 0)
                val h = ByteArray(16); r.seek(16L * 2352); r.readFully(h)
                return RawSource(r, 2352, if (h[15].toInt() == 1) 16 else 24)
            }
        }
    }

    private class CsoSource(val f: RandomAccessFile) : Sectors {
        private val blockSize: Int; private val align: Int; private val index: IntArray
        init {
            val h = ByteArray(24); f.seek(0); f.readFully(h)
            require(String(h, 0, 4, Charsets.US_ASCII) == "CISO") { "not cso" }
            val bb = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN)
            val total = bb.getLong(8); blockSize = bb.getInt(16); align = h[21].toInt() and 0xFF
            val n = ((total + blockSize - 1) / blockSize).toInt()
            val raw = ByteArray((n + 1) * 4); f.seek(24); f.readFully(raw)
            val ib = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN); index = IntArray(n + 1) { ib.getInt(it * 4) }
        }
        private fun block(i: Int): ByteArray {
            val a = index[i]; val b = index[i + 1]
            val plain = a and 0x80000000.toInt() != 0
            val pos = (a.toLong() and 0x7FFFFFFF) shl align; val end = (b.toLong() and 0x7FFFFFFF) shl align
            val len = (end - pos).toInt().coerceAtLeast(0)
            val buf = ByteArray(if (plain) blockSize else len); f.seek(pos); f.readFully(buf, 0, minOf(buf.size, (f.length() - pos).toInt()))
            if (plain) return buf
            val inf = Inflater(true); inf.setInput(buf); val out = ByteArray(blockSize)
            try { inf.inflate(out) } finally { inf.end() }
            return out
        }
        override fun read(lba: Long, count: Int): ByteArray {
            val out = ByteArray(count * 2048); var o = 0
            var off = lba * 2048; val end = off + count * 2048L
            while (off < end) {
                val bi = (off / blockSize).toInt(); val inBlock = (off % blockSize).toInt()
                val blk = block(bi); val n = minOf(blk.size - inBlock, (end - off).toInt())
                System.arraycopy(blk, inBlock, out, o, n); o += n; off += n
            }
            return out
        }
        override fun close() = f.close()
    }

    // ---------- ISO 9660 ----------
    private fun le32(b: ByteArray, o: Int) = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)

    private data class Entry(val lba: Long, val size: Int, val dir: Boolean)

    private fun find(s: Sectors, dir: Entry, name: String): Entry? {
        val data = s.read(dir.lba, (dir.size + 2047) / 2048)
        var p = 0
        while (p < data.size) {
            val len = data[p].toInt() and 0xFF
            if (len == 0) { p = (p / 2048 + 1) * 2048; continue }
            val nl = data[p + 32].toInt() and 0xFF
            val n = String(data, p + 33, nl, Charsets.US_ASCII).substringBefore(';')
            if (n.equals(name, ignoreCase = true)) return Entry(le32(data, p + 2).toLong(), le32(data, p + 10), (data[p + 25].toInt() and 2) != 0)
            p += len
        }
        return null
    }

    private fun file(s: Sectors, root: Entry, path: String, max: Int = 4 shl 20): ByteArray? {
        var cur: Entry = root
        for (part in path.split('/')) cur = find(s, cur, part) ?: return null
        if (cur.dir || cur.size > max) return null
        return s.read(cur.lba, (cur.size + 2047) / 2048).copyOf(cur.size)
    }

    private fun fromIso(s: Sectors): Info {
        val pvd = s.read(16, 1)
        require(String(pvd, 1, 5, Charsets.US_ASCII) == "CD001") { "not iso9660" }
        val root = Entry(le32(pvd, 156 + 2).toLong(), le32(pvd, 156 + 10), true)
        val umd = file(s, root, "UMD_DATA.BIN")?.let { String(it, Charsets.US_ASCII).substringBefore('|').trim() }
        val sfo = file(s, root, "PSP_GAME/PARAM.SFO")?.let { sfo(it) } ?: emptyMap()
        return Info(
            normSerial(umd ?: sfo["DISC_ID"]), sfo["TITLE"],
            file(s, root, "PSP_GAME/ICON0.PNG"), file(s, root, "PSP_GAME/PIC1.PNG"),
        )
    }

    // ---------- PBP (EBOOT): заголовок с 8 смещениями ----------
    private fun fromPbp(f: File): Info? = RandomAccessFile(f, "r").use { r ->
        val h = ByteArray(40); r.readFully(h)
        if (h[1] != 'P'.code.toByte() || h[2] != 'B'.code.toByte() || h[3] != 'P'.code.toByte()) return null
        val off = IntArray(8) { le32(h, 8 + it * 4) }
        fun part(i: Int): ByteArray? {
            val a = off[i].toLong(); val b = off[i + 1].toLong(); val n = (b - a).toInt()
            if (n <= 0 || n > (4 shl 20)) return null
            return ByteArray(n).also { r.seek(a); r.readFully(it) }
        }
        val s = part(0)?.let { sfo(it) } ?: emptyMap()
        Info(normSerial(s["DISC_ID"]), s["TITLE"], part(1), part(4))
    }

    // ---------- PARAM.SFO ----------
    private fun sfo(b: ByteArray): Map<String, String> {
        if (b.size < 20 || b[1] != 'P'.code.toByte()) return emptyMap()
        val keyT = le32(b, 8); val dataT = le32(b, 12); val n = le32(b, 16)
        val out = HashMap<String, String>()
        for (i in 0 until n) {
            val e = 20 + i * 16
            val ko = (b[e].toInt() and 0xFF) or ((b[e + 1].toInt() and 0xFF) shl 8)
            val fmt = (b[e + 2].toInt() and 0xFF) or ((b[e + 3].toInt() and 0xFF) shl 8)
            val len = le32(b, e + 4); val doff = le32(b, e + 12)
            var ke = keyT + ko; while (ke < b.size && b[ke] != 0.toByte()) ke++
            val key = String(b, keyT + ko, ke - keyT - ko, Charsets.US_ASCII)
            if (fmt == 0x0204 || fmt == 0x0004) out[key] = String(b, dataT + doff, len, Charsets.UTF_8).trimEnd('\u0000').trim()
        }
        return out
    }

    /** «ULES01298» / «ULES-01298» → «ULES-01298». */
    private fun normSerial(s: String?): String? {
        val t = s?.uppercase()?.replace(Regex("[^A-Z0-9]"), "") ?: return null
        return if (t.length >= 9) t.substring(0, 4) + "-" + t.substring(4, 9) else null
    }
}
