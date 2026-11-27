package com.nexus.psp.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar

/**
 * Секреты KAPSULA: маленькие пасхалки по всему приложению. Нашёл — Сейвик радуется, секрет появляется в «Моё → Секреты».
 * Хранятся локально (настройка eggs: id через запятую). Ненайденные показываются загадкой-подсказкой.
 */
object Secrets {
    class Egg(val id: String, val title: String, val hint: String, val pose: String, val text: String)

    val ALL = listOf(
        Egg("tickle", "Щекотун", "Сейвик ужасно боится щекотки", "tickle", "Хи-хи-хи! Хватит, сдаюсь!"),
        Egg("owl", "Сова", "Загляни сюда глубокой ночью", "sleep", "Ты чего не спишь? Ну ладно, поиграем тихонько"),
        Egg("lark", "Жаворонок", "Кто рано встаёт…", "stretch", "Доброе утро! Ты сегодня раньше солнца"),
        Egg("midnight", "Ровно полночь", "Ровно в 00:00", "spin", "Тыква не появилась. Зато есть я!"),
        Egg("dev", "Разработчик", "Посчитай до семи у версии KAPSULA", "detective", "Ты нашёл режим разработчика! …Шучу, его нет"),
        Egg("konami", "Код Конами", "↑↑↓↓←→←→ B A — на геймпаде", "party", "30 жизней! Ну, почти"),
        Egg("newyear", "С Новым годом!", "Загляни в новогодние праздники", "newyear", "С Новым годом! Пусть сейвы не теряются"),
        Egg("halloween", "Хэллоуин", "Конец октября, страшилки", "halloween", "Бу! Испугался? Это я, Сейвик"),
        Egg("spring", "Весна", "Первая неделя весны", "spring", "Весна! Пора отряхнуть картриджи от пыли"),
        Egg("summer", "Лето", "Первая неделя лета", "summer", "Лето! Самое время для портативных игр"),
        Egg("autumn", "Осень", "Первая неделя осени", "autumn", "Осень — пледы, чай и длинные RPG"),
        Egg("bday", "День рождения KAPSULA", "14 ноября", "bday", "Сегодня день рождения KAPSULA! Спасибо, что ты с нами"),
        Egg("fri13", "Пятница, 13-е", "Самая невезучая пятница", "hide", "Сегодня сохраняйся почаще. На всякий случай"),
        Egg("glutton", "Обжора", "Покорми сытого Сейвика", "eat", "Больше не могу… Ну, может, ещё кусочек"),
        Egg("alarm", "Будильник", "Разбуди Сейвика", "shock", "А? Что? Я не сплю, я сохраняюсь!"),
        Egg("bestie", "Лучший друг", "Много-много раз поиграй с Сейвиком", "heart", "Ты мой самый лучший друг!"),
        Egg("perfect", "Идеальный уход", "Сделай Сейвика полностью счастливым", "trophy2", "Сыт, чист, весел и бодр. Жизнь удалась!"),
        Egg("visits", "Постоянный гость", "Заходи в уголок Сейвика разные дни", "letter", "Я каждый день жду, когда ты зайдёшь"),
        Egg("namesake", "Тёзка", "Назовись так же, как маскот", "wow", "Эй, это же моё имя!"),
        Egg("selfsearch", "Ищу себя", "Поищи маскота в поиске", "magnifier", "Я тут! Нашёлся"),
        Egg("k8", "Тайна KAPSULA-8", "Поищи консоль, которой нет", "whisper", "Тсс… KAPSULA-8 ещё собирается. Скоро"),
        Egg("film", "Киноман", "Пощёлкай зерном плёнки", "wink", "Щёлк-щёлк! Как в старом кинотеатре"),
        Egg("thorough", "Дотошный", "Загляни во все разделы настроек", "checklist", "Ты прочитал все настройки. Уважаю!"),
        Egg("week", "Неделя вместе", "Заходи семь дней подряд", "hugcase", "Целая неделя вместе!"),
        Egg("collector", "Коллекционер", "Собери десять игр", "sleepcases", "Полка уже ломится от коробок"),
        Egg("marathon", "Марафонец", "Наиграй 10 часов в одну игру", "trophy", "Десять часов в одной игре! Легенда"),
    )

    /** Найденные (порядок — по времени находки). */
    var found by mutableStateOf<List<String>>(emptyList()); private set

    fun load() { found = AppState.S.eggs.split(',').filter { it.isNotBlank() } }

    fun has(id: String) = id in found

    /** Секрет найден: Сейвик сообщает об этом (один раз). */
    fun find(id: String) {
        if (has(id)) return
        val e = ALL.firstOrNull { it.id == id } ?: return
        found = found + id
        AppState.S.eggs = found.joinToString(",")
        AppState.sayAs(e.pose, "Секрет «${e.title}» · ${e.text}")
        Telemetry.count("egg", id)
    }

    /** Проверки по времени и цифрам — при каждом входе в приложение. */
    fun check(now: Calendar = Calendar.getInstance()) {
        val h = now.get(Calendar.HOUR_OF_DAY); val m = now.get(Calendar.MINUTE)
        val d = now.get(Calendar.DAY_OF_MONTH); val mon = now.get(Calendar.MONTH) + 1; val wd = now.get(Calendar.DAY_OF_WEEK)
        when {
            h == 0 && m == 0 -> find("midnight")
            h in 2..4 -> find("owl")
            h == 5 -> find("lark")
        }
        when {
            (mon == 12 && d >= 25) || (mon == 1 && d <= 7) -> find("newyear")
            mon == 10 && d >= 25 || mon == 11 && d <= 2 -> find("halloween")
            mon == 11 && d == 14 -> find("bday")
            mon == 3 && d <= 7 -> find("spring")
            mon == 6 && d <= 7 -> find("summer")
            mon == 9 && d <= 7 -> find("autumn")
        }
        if (d == 13 && wd == Calendar.FRIDAY) find("fri13")
        if (AppState.S.nick.trim().equals("сейвик", true) || AppState.S.nick.trim().equals("savik", true)) find("namesake")
        if (Tokens.visitStreak() >= 7) find("week")
        if (AppState.byId.size >= 10) find("collector")
        if (AppState.lib.values.any { it.play >= 600 }) find("marathon")
    }

    /** Поза Сейвика на главной по сезону (если сегодня праздник) — null в обычный день. */
    fun seasonPose(now: Calendar = Calendar.getInstance()): String? {
        val d = now.get(Calendar.DAY_OF_MONTH); val mon = now.get(Calendar.MONTH) + 1
        return when {
            (mon == 12 && d >= 25) || (mon == 1 && d <= 7) -> "newyear"
            mon == 10 && d >= 25 || mon == 11 && d <= 2 -> "halloween"
            mon == 11 && d == 14 -> "bday"
            else -> null
        }
    }

    /** Поиск: секретные слова. */
    fun search(q: String) {
        val t = q.trim().lowercase()
        if (t == "сейвик" || t == "savik" || t == "saveik") find("selfsearch")
        if (t.replace(" ", "").let { it == "kapsula-8" || it == "капсула-8" || it == "kapsula8" || it == "капсула8" || it == "k8" }) find("k8")
    }

    // ---- счётчики внутри сессии ----
    private var tickles = ArrayList<Long>()
    /** Касание Сейвика в его уголке: 8 касаний за 5 секунд — щекотка. */
    fun tickle() {
        val now = System.currentTimeMillis()
        tickles.add(now); tickles.removeAll { now - it > 5000 }
        if (tickles.size >= 8) find("tickle")
    }

    private var grainFlips = 0
    fun grainFlip() { if (++grainFlips >= 6) find("film") }

    private var verTaps = 0; private var verAt = 0L
    fun versionTap() {
        val now = System.currentTimeMillis()
        if (now - verAt > 2500) verTaps = 0
        verAt = now
        if (++verTaps >= 7) find("dev")
    }

    private val seenTabs = HashSet<String>()
    fun settingsTab(id: String, all: Int) { seenTabs.add(id); if (seenTabs.size >= all) find("thorough") }

    // код Конами: вверх, вверх, вниз, вниз, влево, вправо, влево, вправо, B, A
    private val KONAMI = listOf("u", "u", "d", "d", "l", "r", "l", "r", "b", "a")
    private val keys = ArrayList<String>()
    /** Нажатие кнопки геймпада/клавиатуры в меню. */
    fun key(k: String) {
        keys.add(k); while (keys.size > KONAMI.size) keys.removeAt(0)
        if (keys == KONAMI) { keys.clear(); find("konami") }
    }
}
