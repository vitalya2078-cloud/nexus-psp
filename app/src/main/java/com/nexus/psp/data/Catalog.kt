package com.nexus.psp.data

import androidx.compose.ui.graphics.Color

/** Консоль: короткое и полное имя, цвет, нужен ли BIOS. */
data class Platform(val id: String, val short: String, val full: String, val color: Color, val bios: Boolean, val media: String, val mediaImg: String, val conImg: String)

val PLATFORMS = listOf(
    Platform("psp", "PSP", "PlayStation Portable", Color(0xFF9DB8D6), false, "UMD-диск", "ic-umd.png", "con-psp.png"),
    Platform("ps1", "PS1", "PlayStation", Color(0xFFB8B4AE), true, "CD-диск", "ic-cd.png", "con-ps1.png"),
    Platform("ps2", "PS2", "PlayStation 2", Color(0xFF8E96C4), true, "DVD-диск", "ic-dvd.png", "con-ps2.png"),
)
val PL = PLATFORMS.associateBy { it.id }

/**
 * Игра каталога или свой файл. pal — палитра обложки (верх, низ, акцент), m — мотив процедурной обложки.
 * path != null — настоящий образ на телефоне, запускается в ядре.
 */
data class Game(
    val id: String, val t: String, val p: String, val genre: String, val year: String, val size: Int, val kind: String,
    val pal: List<Long>, val m: String, val desc: String, val feat: List<String>,
    val seed: Int = 3, val isNew: Boolean = false, val rev: Int = 0, val compat: Int = 3,
    val path: String? = null,
) {
    val real get() = path != null
    val plat get() = PL[p] ?: PLATFORMS[0]
}

private fun g(i: Int, id: String, t: String, p: String, genre: String, year: Int, size: Int, kind: String, pal: List<Long>, m: String, desc: String, feat: List<String>) =
    Game(id, t, p, genre, "$year", size, kind, pal, m, desc, feat, seed = 7 + i * 31, isNew = i % 3 == 0, rev = 40 + (i * 37) % 160, compat = if (p == "ps2" && i % 2 == 1) 2 else 3)

/** Демо-каталог: выдуманные игры (как в прототипе). Настоящий каталог — панель автора на Supabase (п. 23). */
val GAMES: List<Game> = listOf(
    g(0, "drift", "Ночной Дрифт", "psp", "Гонки", 2009, 1240, "Своя версия", listOf(0xFF2A3140, 0xFF0D1016, 0xFFD9C7A3), "sun", "Ночные гонки по бесконечному шоссе. Дрифт заряжает ускорение, трасса меняется под музыку.", listOf("24 трассы и карьера", "Мультиплеер до 4 игроков", "Фоторежим")),
    g(1, "orbit", "Орбита-9", "psp", "Стратегия", 2008, 690, "Своя версия", listOf(0xFF1F2C3A, 0xFF0B1016, 0xFFA9BCCF), "planet", "Колонизируй систему из девяти планет и свяжи их в одну торговую сеть.", listOf("Генерация систем", "Дипломатия", "Партии по 30 минут")),
    g(2, "rhythm", "Ритм Улиц", "psp", "Музыка", 2009, 520, "Демо", listOf(0xFF2B2A33, 0xFF0E0E12, 0xFFBDB6C9), "waves", "Попадай в бит на уличных площадках. Чем точнее серия, тем ярче район.", listOf("40 треков", "Редактор карт", "Дуэли")),
    g(3, "pixel", "Пиксельные Войны", "psp", "Стратегия", 2009, 310, "Сборник", listOf(0xFF2E2B26, 0xFF100F0D, 0xFFD8C9A6), "city", "Тактика на клеточных картах: заводы, армия и защита столицы.", listOf("30 миссий", "Редактор карт", "Хот-сит")),
    g(4, "ash", "Хроники Пепла", "ps1", "RPG", 1999, 640, "Сборник", listOf(0xFF3A2E25, 0xFF110D0A, 0xFFD2B48C), "hills", "Королевство под вулканическим пеплом. Собери отряд и реши, кого спасать.", listOf("60+ часов", "Три концовки", "Русский перевод")),
    g(5, "shards", "Осколки", "ps1", "Головоломка", 1998, 180, "Своя версия", listOf(0xFF1F302C, 0xFF0A100E, 0xFFA8C2B6), "shards", "Собери разбитый свет обратно, вращая и отражая лучи.", listOf("120 уровней", "Без таймеров", "Сессии по 5 минут")),
    g(6, "forest", "Тихий Лес", "ps1", "Хоррор", 2000, 560, "Своя версия", listOf(0xFF1C2A21, 0xFF090E0B, 0xFF9DB3A1), "hills", "Ночь в заповеднике, где пропал отряд лесников. Фонарь, рация и голос в эфире.", listOf("Динамический звук", "Мало ресурсов", "6–8 часов")),
    g(7, "rubezh", "Последний Рубеж", "ps2", "Экшен", 2004, 1630, "Демо", listOf(0xFF3A2523, 0xFF110B0A, 0xFFC9A396), "city", "Удерживай последние кварталы города вместе с напарником.", listOf("Кооператив", "Укрытия", "12 глав")),
    g(8, "deep", "Глубина", "ps2", "Приключения", 2005, 960, "Демо", listOf(0xFF1B2836, 0xFF080C11, 0xFF9FB3C8), "waves", "Спустись в океанский разлом и найди источник сигнала со дна.", listOf("Нелинейная карта", "Улучшения батискафа", "Атмосферный звук")),
    g(9, "fist", "Стальной Кулак", "ps2", "Файтинг", 2003, 1420, "Сборник", listOf(0xFF36202A, 0xFF100A0D, 0xFFC4A1AE), "shards", "Турнирный файтинг с 18 бойцами и глубокой системой комбо.", listOf("18 бойцов", "Тренировка", "Дуэли")),
    g(10, "courier", "Звёздный Курьер", "ps2", "Аркада", 2006, 820, "Демо", listOf(0xFF22253A, 0xFF0B0C13, 0xFFB3B8D4), "planet", "Доставляй посылки между станциями и уворачивайся от метеоритов.", listOf("Свободный полёт", "Улучшения корабля", "Таблица рекордов")),
)

val GENRES = GAMES.map { it.genre }.distinct()

data class Curated(val t: String, val d: String, val ids: List<String>)

val CURATED = listOf(
    Curated("Выбор KAPSULA", "лучшее из каталога", listOf("ash", "drift", "deep", "shards", "orbit")),
    Curated("Короткие игры на вечер", "партии до 30 минут", listOf("shards", "rhythm", "pixel", "courier", "orbit")),
    Curated("Для двоих", "ad-hoc и один экран", listOf("drift", "rubezh", "fist", "rhythm", "pixel")),
    Curated("Атмосферные приключения", "истории и миры", listOf("deep", "forest", "ash", "orbit")),
)

/** Свой файл с телефона → игра библиотеки. */
fun realGame(id: String, name: String, file: String, path: String, sizeMb: Double, plat: String) = Game(
    id, name, plat, "Свой файл", "—", maxOf(1, sizeMb.toInt()), "Из файла",
    listOf(0xFF1A1C22, 0xFF0B0C0F, 0xFF7FA7D9), "none", "Твой образ игры с телефона. Запускается в ядре PPSSPP.",
    listOf("Файл с устройства: $file", "Работает без интернета"), path = path,
)

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
