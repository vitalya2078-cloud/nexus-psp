package com.nexus.psp.data

/**
 * Правило имени игрока (то же проверяет сервер, kapsula_nick_ok): 5–16 символов (у создателя — от 1),
 * буквы русские и латинские, цифры, пробел и _ - . ; без двух пробелов подряд, не одни цифры, без мата.
 * Имена могут повторяться — игрока отличает ID.
 */
object Nick {
    const val MAX = 16
    private val BAD = listOf("хуй", "хуе", "хуя", "пизд", "ебан", "ебат", "ебал", "ебл", "бляд", "блят", "мудак", "мудил", "пидор", "пидар",
        "гандон", "шлюх", "залуп", "fuck", "shit", "bitch", "cunt", "nigg", "faggot")

    private fun okChar(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c in 'а'..'я' || c in 'А'..'Я' || c == 'ё' || c == 'Ё' || c in '0'..'9' || c in " _.-"

    /** null — имя подходит (пустое тоже: будет «Игрок»), иначе — что поправить. */
    fun problem(raw: String, admin: Boolean = false): String? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        val min = if (admin) 1 else 5
        return when {
            s.length < min -> "Имя — от $min символов"
            s.length > MAX -> "Имя — до $MAX символов"
            !s.all(::okChar) -> "Только буквы, цифры, пробел и _ - ."
            "  " in s -> "Два пробела подряд не получится"
            s.all { it in "0123456789 _.-" } -> "Нужна хотя бы одна буква"
            s.lowercase().replace('ё', 'е').filter { it !in " _.-" }.let { f -> BAD.any { it in f } } -> "Такое имя не подойдёт"
            else -> null
        }
    }
}
