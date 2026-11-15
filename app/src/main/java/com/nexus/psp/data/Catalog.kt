package com.nexus.psp.data

import androidx.compose.ui.graphics.Color

/**
 * Консоль: имена, цвет, ядро (lib<core>_libretro_android.so), форматы, BIOS.
 * bios: 0 — не нужен, 1 — по желанию, 2 — нужен. analog — есть стики (иначе стик экрана работает как крестовина).
 * exp — экспериментально (нужен мощный телефон). family — к какой группе относится на экране консолей.
 */
data class Platform(
    val id: String, val short: String, val full: String, val color: Color, val biosLevel: Int,
    val media: String, val mediaImg: String, val conImg: String, val ready: Boolean = true,
    val core: String = "", val exts: List<String> = emptyList(), val analog: Boolean = false, val exp: Boolean = false,
    val family: String = "", val biosHint: String = "",
) {
    val bios get() = biosLevel > 0
    val coreLib get() = "lib${core}_libretro_android.so"
}

val PLATFORMS = listOf(
    Platform("psp", "PSP", "PSP", Color(0xFF9DB8D6), 0, "UMD-диск", "ic-umd.png", "con-psp.png", core = "ppsspp", exts = listOf("iso", "cso", "chd", "pbp", "elf", "prx"), analog = true, family = "Sony"),
    Platform("ps1", "PS1", "PS1", Color(0xFFB8B4AE), 1, "CD-диск", "ic-cd.png", "con-ps1.png", core = "pcsx_rearmed", exts = listOf("cue", "bin", "img", "chd", "pbp", "m3u", "iso"), family = "Sony",
        biosHint = "Работает и без него. Свой BIOS — чуть лучше совместимость"),
    Platform("ps2", "PS2", "PS2", Color(0xFF8E96C4), 2, "DVD-диск", "ic-dvd.png", "con-ps2.png", core = "armsx2", exts = listOf("iso", "chd", "cso", "zso"), analog = true, exp = true, family = "Sony",
        biosHint = "Нужен BIOS с твоей PS2 (например scph39001.bin)"),
    // Денди (Famicom): без логотипов и названий Nintendo в интерфейсе — своя картинка приставки
    Platform("nes", "Денди", "Денди (Famicom)", Color(0xFFD98A8A), 0, "Картридж", "ic-cd.png", "con-dendy.png", core = "fceumm", exts = listOf("nes", "unf", "unif"), family = "Денди"),
    Platform("md", "Mega Drive", "Sega Mega Drive", Color(0xFF9AA3B5), 0, "Картридж", "ic-cd.png", "con-md.png", core = "clownmdemu", exts = listOf("md", "gen", "smd", "bin"), family = "Sega"),
    Platform("sms", "Master System", "Sega Master System", Color(0xFFB59A9A), 0, "Картридж", "ic-cd.png", "con-sms.png", core = "gearsystem", exts = listOf("sms", "sg"), family = "Sega"),
    Platform("gg", "Game Gear", "Sega Game Gear", Color(0xFF8FA7A0), 0, "Картридж", "ic-cd.png", "con-gg.png", core = "gearsystem", exts = listOf("gg"), family = "Sega"),
    Platform("dc", "Dreamcast", "Sega Dreamcast", Color(0xFFD9A07C), 1, "GD-диск", "ic-cd.png", "con-dc.png", core = "flycast", exts = listOf("gdi", "cdi", "chd"), analog = true, family = "Sega",
        biosHint = "Работает и без него. Свой BIOS: dc_boot.bin"),
    Platform("saturn", "Saturn", "Sega Saturn", Color(0xFF9DA2B0), 1, "CD-диск", "ic-cd.png", "con-saturn.png", core = "yabasanshiro", exts = listOf("cue", "chd", "iso", "ccd", "mds"), exp = true, family = "Sega",
        biosHint = "Лучше со своим BIOS: saturn_bios.bin"),
    Platform("pce", "PC Engine", "NEC PC Engine", Color(0xFFC7A6D6), 0, "HuCard", "ic-cd.png", "con-pce.png", core = "geargrafx", exts = listOf("pce", "sgx"), family = "NEC и SNK"),
    Platform("neogeo", "Neo Geo", "SNK Neo Geo", Color(0xFFD9B36C), 2, "Картридж", "ic-cd.png", "con-neogeo.png", core = "geolith", exts = listOf("neo"), family = "NEC и SNK",
        biosHint = "Нужен BIOS: aes.zip или neogeo.zip"),
    Platform("ngp", "Neo Geo Pocket", "SNK Neo Geo Pocket", Color(0xFF8FB3C8), 0, "Картридж", "ic-cd.png", "con-ngp.png", core = "mednafen_ngp", exts = listOf("ngp", "ngc"), family = "NEC и SNK"),
    Platform("ws", "WonderSwan", "Bandai WonderSwan", Color(0xFFA9C79B), 0, "Картридж", "ic-cd.png", "con-ws.png", core = "mednafen_wswan", exts = listOf("ws", "wsc", "pc2"), family = "Bandai и Atari"),
    Platform("a26", "Atari 2600", "Atari 2600", Color(0xFFC79B7C), 0, "Картридж", "ic-cd.png", "con-a26.png", core = "stella", exts = listOf("a26"), family = "Bandai и Atari"),
    Platform("a78", "Atari 7800", "Atari 7800", Color(0xFFB3A27C), 0, "Картридж", "ic-cd.png", "con-a26.png", core = "prosystem", exts = listOf("a78"), family = "Bandai и Atari"),
    Platform("lynx", "Lynx", "Atari Lynx", Color(0xFF9CB0A8), 2, "Картридж", "ic-cd.png", "con-lynx.png", core = "gearlynx", exts = listOf("lnx", "lyx"), family = "Bandai и Atari",
        biosHint = "Нужен BIOS: lynxboot.img"),
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
