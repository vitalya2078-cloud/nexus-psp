package com.nexus.psp.emulator

import com.swordfish.libretrodroid.LibretroDroid
import org.json.JSONArray
import org.json.JSONObject
import java.util.BitSet

/**
 * Автопереключение раскладок по памяти игры (своя сборка LibretroDroid: чтение RETRO_MEMORY_SYSTEM_RAM).
 * Память PSP в ядре начинается с 0x08000000; пользовательская область — с 0x08800000.
 *
 * Обучение: игрок отмечает «сейчас Пешком / Транспорт…»; при каждой отметке память сравнивается с первой
 * отметкой этой раскладки. Остаются байты, которые не меняются внутри раскладки и различаются между раскладками.
 */
object AutoSwitch {
    const val SYSTEM_RAM = 2
    const val USER_OFF = 0x00800000          // 0x08800000 − 0x08000000
    private const val SPAN = 24 * 1024 * 1024 // сколько памяти сравниваем
    private const val CHUNK = 1024 * 1024

    fun ramReady() = try { LibretroDroid.memorySize(SYSTEM_RAM) > USER_OFF } catch (e: Throwable) { false }

    private fun span() = (LibretroDroid.memorySize(SYSTEM_RAM) - USER_OFF).coerceIn(0, SPAN)

    /** Чтение памяти: смещение от 0x08000000, длина → байты (в игре — LibretroDroid; в тестах — подставная). */
    val nativeRead: (Int, Int) -> ByteArray? = { o, l -> LibretroDroid.readMemory(SYSTEM_RAM, o, l) }

    class Learner(private val n: Int = span(), private val read: (Int, Int) -> ByteArray? = nativeRead) {
        private val base = HashMap<String, ByteArray>()
        private val stable = HashMap<String, BitSet>()
        val marks = HashMap<String, Int>()

        /** Отметка «сейчас раскладка lay». Читает память кусками, чтобы не держать лишнего. */
        fun mark(lay: String): Boolean {
            if (n <= 0) return false
            val b = base[lay]
            if (b == null) {
                val all = ByteArray(n); var o = 0
                while (o < n) { val c = read(USER_OFF + o, minOf(CHUNK, n - o)) ?: return false; System.arraycopy(c, 0, all, o, c.size); o += c.size }
                base[lay] = all; stable[lay] = BitSet(n).apply { set(0, n) }
            } else {
                val st = stable.getValue(lay); var o = 0
                while (o < n) {
                    val c = read(USER_OFF + o, minOf(CHUNK, n - o)) ?: return false
                    for (i in c.indices) if (c[i] != b[o + i]) st.clear(o + i)
                    o += c.size
                }
            }
            marks[lay] = (marks[lay] ?: 0) + 1
            return true
        }

        /** Кандидаты: стабильны во всех раскладках и различаются между всеми. До 12 лучших (ближе к «флагам» 0/1). */
        fun result(): JSONObject? {
            val lays = base.keys.toList()
            if (lays.size < 2 || lays.any { (marks[it] ?: 0) < 2 }) return null
            val all = stable.getValue(lays[0]).clone() as BitSet
            lays.drop(1).forEach { all.and(stable.getValue(it)) }
            val found = ArrayList<Pair<Int, Int>>() // смещение, «оценка»
            var i = all.nextSetBit(0)
            while (i >= 0) {
                val vals = lays.map { base.getValue(it)[i].toInt() and 0xFF }
                if (vals.toSet().size == lays.size) {
                    // флаги вида 0/1 и малые значения — надёжнее случайных совпадений
                    val score = vals.max() + (if (vals.all { it <= 1 }) 0 else 50)
                    found.add(i to score)
                }
                i = all.nextSetBit(i + 1)
            }
            if (found.isEmpty()) return null
            val best = found.sortedBy { it.second }.take(12)
            val cands = JSONArray()
            best.forEach { (off, _) ->
                val v = JSONObject(); lays.forEach { v.put(it, base.getValue(it)[off].toInt() and 0xFF) }
                cands.put(JSONObject().put("o", USER_OFF + off).put("v", v))
            }
            return JSONObject().put("on", true).put("cands", cands).put("found", found.size)
        }
    }

    /** Какую раскладку показывает память сейчас (большинство адресов-кандидатов); null — не понятно. */
    fun detect(rule: JSONObject, read: (Int, Int) -> ByteArray? = nativeRead): String? {
        val c = rule.optJSONArray("cands") ?: return null
        val votes = HashMap<String, Int>()
        for (k in 0 until c.length()) {
            val o = c.getJSONObject(k)
            val b = (read(o.getInt("o"), 1) ?: continue)[0].toInt() and 0xFF
            val v = o.getJSONObject("v")
            v.keys().forEach { lay -> if (v.getInt(lay) == b) votes[lay] = (votes[lay] ?: 0) + 1 }
        }
        val top = votes.maxByOrNull { it.value } ?: return null
        // уверенность: больше половины адресов согласны
        return if (top.value * 2 > c.length()) top.key else null
    }
}
