package com.nexus.psp.data

import androidx.compose.ui.graphics.Color

/** Консоль: короткое и полное имя, цвет, работает ли уже в KAPSULA, нужен ли BIOS. */
data class Platform(
    val id: String, val short: String, val full: String, val color: Color, val bios: Boolean,
    val media: String, val mediaImg: String, val conImg: String, val ready: Boolean,
)

val PLATFORMS = listOf(
    Platform("psp", "PSP", "PlayStation Portable", Color(0xFF9DB8D6), false, "UMD-диск", "ic-umd.png", "con-psp.png", ready = true),
    Platform("ps1", "PS1", "PlayStation", Color(0xFFB8B4AE), true, "CD-диск", "ic-cd.png", "con-ps1.png", ready = true),
    Platform("ps2", "PS2", "PlayStation 2", Color(0xFF8E96C4), true, "DVD-диск", "ic-dvd.png", "con-ps2.png", ready = false),
)
val PL = PLATFORMS.associateBy { it.id }

/**
 * Игра библиотеки — свой образ на телефоне (path). pal — палитра обложки (верх, низ, акцент), m — мотив
 * процедурной обложки; у своих файлов подбираются по названию, поэтому у каждой игры своё лицо.
 */
data class Game(
    val id: String, val t: String, val p: String, val size: Int, val file: String, val path: String,
    val pal: List<Long>, val m: String, val seed: Int,
) {
    val plat get() = PL[p] ?: PLATFORMS[0]
    val format get() = file.substringAfterLast('.', "").uppercase()
}

/** Палитры и мотивы обложек (из прототипа). */
private val PALETTES = listOf(
    listOf(0xFF2A3140, 0xFF0D1016, 0xFFD9C7A3), listOf(0xFF1F2C3A, 0xFF0B1016, 0xFFA9BCCF), listOf(0xFF2B2A33, 0xFF0E0E12, 0xFFBDB6C9),
    listOf(0xFF2E2B26, 0xFF100F0D, 0xFFD8C9A6), listOf(0xFF3A2E25, 0xFF110D0A, 0xFFD2B48C), listOf(0xFF1F302C, 0xFF0A100E, 0xFFA8C2B6),
    listOf(0xFF1C2A21, 0xFF090E0B, 0xFF9DB3A1), listOf(0xFF3A2523, 0xFF110B0A, 0xFFC9A396), listOf(0xFF1B2836, 0xFF080C11, 0xFF9FB3C8),
    listOf(0xFF36202A, 0xFF100A0D, 0xFFC4A1AE), listOf(0xFF22253A, 0xFF0B0C13, 0xFFB3B8D4),
)
private val MOTIFS = listOf("sun", "planet", "waves", "city", "hills", "shards")

/** Красивое название из имени файла: без расширения, кодов региона и подчёркиваний. */
fun prettyName(raw: String): String =
    raw.replace(Regex("\\[[^]]*]|\\([^)]*\\)"), " ").replace('_', ' ').replace(Regex("\\s+"), " ").trim().ifEmpty { raw }

fun realGame(id: String, name: String, file: String, path: String, sizeMb: Double, plat: String): Game {
    val h = (name.lowercase().hashCode() and 0x7fffffff)
    return Game(id, prettyName(name), plat, maxOf(1, sizeMb.toInt()), file, path, PALETTES[h % PALETTES.size], MOTIFS[(h / 7) % MOTIFS.size], 7 + h % 997)
}

/** Пример игры для превью экранных кнопок (не попадает в библиотеку). */
val PREVIEW_GAME = Game("preview", "", "psp", 0, "", "", PALETTES[7], "city", 224)

// ---------- форматирование ----------

fun size(mb: Int): String = if (mb >= 1000) String.format("%.1f ГБ", mb / 1024.0).replace('.', ',') else "$mb МБ"

fun hrs(min: Int): String {
    val h = min / 60; val m = min % 60
    return if (h > 0) "$h ч" + (if (m > 0) " $m мин" else "") else "$m мин"
}

fun plural(n: Int, a: String, b: String, c: String): String {
    val m = n % 10; val h = n % 100
    return "$n " + when { m == 1 && h != 11 -> a; m in 2..4 && (h < 12 || h > 14) -> b; else -> c }
}
