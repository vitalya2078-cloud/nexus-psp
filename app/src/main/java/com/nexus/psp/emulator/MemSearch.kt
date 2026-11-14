package com.nexus.psp.emulator

/**
 * Искатель значений (как ArtMoney): ищет в памяти игры число и сужает поиск по изменениям.
 * Найденный адрес превращается в код CWCheat (_L), который ядро записывает каждый кадр — «заморозка».
 */
class MemSearch(private val read: (Int, Int) -> ByteArray? = AutoSwitch.nativeRead, private val span: Int = 24 * 1024 * 1024) {
    /** Размер значения в байтах: 1, 2 или 4 (младший байт первым, как на PSP). */
    var size = 4
    var cands: IntArray? = null; private set
    private var olds: IntArray = IntArray(0)
    var overflow = false; private set
    val count get() = cands?.size ?: 0

    private fun mask() = when (size) { 1 -> 0xFF; 2 -> 0xFFFF; else -> -1 }
    private fun at(b: ByteArray, i: Int): Int = when (size) {
        1 -> b[i].toInt() and 0xFF
        2 -> (b[i].toInt() and 0xFF) or ((b[i + 1].toInt() and 0xFF) shl 8)
        else -> (b[i].toInt() and 0xFF) or ((b[i + 1].toInt() and 0xFF) shl 8) or ((b[i + 2].toInt() and 0xFF) shl 16) or ((b[i + 3].toInt() and 0xFF) shl 24)
    }

    /** Вся пользовательская память одним массивом (читается кусками по 1 МБ). */
    private fun snapshot(): ByteArray? {
        val n = span; val all = ByteArray(n); var o = 0
        while (o < n) { val c = read(AutoSwitch.USER_OFF + o, minOf(1 shl 20, n - o)) ?: break; System.arraycopy(c, 0, all, o, c.size); o += c.size; if (c.isEmpty()) break }
        return if (o == 0) null else all.copyOf(o)
    }

    /** Первый поиск: все адреса, где сейчас лежит value. Больше 2 млн совпадений — храним первые и просим уточнить. */
    fun first(value: Int): Int {
        val m = snapshot() ?: return 0
        val v = value and mask(); val out = IntArrayBuilder(); val ov = IntArrayBuilder()
        var i = 0
        while (i + size <= m.size) {
            if (at(m, i) == v) { out.add(AutoSwitch.USER_OFF + i); ov.add(v); if (out.size >= 2_000_000) { overflow = true; break } }
            i += size
        }
        cands = out.toArray(); olds = ov.toArray()
        return count
    }

    /** Сужение: eq — стало value; inc / dec — выросло / уменьшилось; same / changed — не изменилось / изменилось. */
    fun next(mode: String, value: Int = 0): Int {
        val c = cands ?: return 0
        val m = snapshot() ?: return count
        val v = value and mask(); val out = IntArrayBuilder(); val ov = IntArrayBuilder()
        for (k in c.indices) {
            val i = c[k] - AutoSwitch.USER_OFF; if (i < 0 || i + size > m.size) continue
            val now = at(m, i); val was = olds[k]
            val ok = when (mode) {
                "eq" -> now == v
                "inc" -> (now.toLong() and 0xFFFFFFFFL) > (was.toLong() and 0xFFFFFFFFL)
                "dec" -> (now.toLong() and 0xFFFFFFFFL) < (was.toLong() and 0xFFFFFFFFL)
                "same" -> now == was
                else -> now != was
            }
            if (ok) { out.add(c[k]); ov.add(now) }
        }
        cands = out.toArray(); olds = ov.toArray(); overflow = false
        return count
    }

    /** Текущие значения первых n адресов. */
    fun current(n: Int = 30): List<Pair<Int, Int>> = (cands ?: IntArray(0)).take(n).mapNotNull { o -> read(o, size)?.let { b -> o to at(b, 0) } }

    fun reset() { cands = null; olds = IntArray(0); overflow = false }

    companion object {
        /** Код CWCheat: запись значения по адресу (смещение от 0x08800000), тип по размеру: 0 — байт, 1 — 2 байта, 2 — 4 байта. */
        fun cwcheat(offset: Int, size: Int, value: Int): String {
            val a = offset - AutoSwitch.USER_OFF
            val type = when (size) { 1 -> 0; 2 -> 1; else -> 2 }
            val v = when (size) { 1 -> value and 0xFF; 2 -> value and 0xFFFF; else -> value }
            return "_L 0x%X%07X 0x%08X".format(type, a and 0x0FFFFFFF, v)
        }
        /** Адрес в памяти PSP для показа игроку. */
        fun pspAddr(offset: Int) = "0x%08X".format(0x08000000 + offset)
    }
}

/** Растущий массив Int без упаковки. */
class IntArrayBuilder {
    private var a = IntArray(1024); var size = 0; private set
    fun add(v: Int) { if (size == a.size) a = a.copyOf(a.size * 2); a[size++] = v }
    fun toArray() = a.copyOf(size)
}
