package com.nexus.psp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar

/**
 * Оформление: аватары, рамки, баннеры, фоны главного экрана, титулы.
 * Каждый предмет — бесплатный, открывается за игру (условие) или покупается за жетоны.
 * Платного за деньги пока нет. Всё хранится только на телефоне.
 */
object Cos {
    enum class Kind(val title: String) { AV("Аватары"), FR("Рамки"), BN("Баннеры"), WP("Фоны"), TT("Титулы") }

    /** Условие «за игру»: что считается, сколько нужно, как назвать. */
    data class Need(val what: String, val n: Int, val text: String)

    /** id — имя файла в assets/img/look (кроме титулов и особых значений); price > 0 — за жетоны; need — за игру. */
    data class Item(val id: String, val kind: Kind, val name: String, val price: Int = 0, val need: Need? = null) {
        val free get() = price == 0 && need == null
        val asset get() = "look/$id.webp"
    }

    private fun lvl(n: Int) = Need("level", n, "Уровень $n")
    private fun hours(n: Int) = Need("play", n * 60, "$n ч в играх")
    private fun con(p: String, h: Int, name: String) = Need("con:$p", h * 60, "$h ч на $name")
    private fun games(n: Int) = Need("games", n, "$n игр в библиотеке")
    private fun cons(n: Int) = Need("consoles", n, "Игры на $n консолях")
    private fun streak(n: Int) = Need("streak", n, "$n дней подряд")
    private fun saves(n: Int) = Need("saves", n, "$n сохранений")
    /** Подарок: открывается только кодом подарка (вещь приходит в Tokens.owned с сервера). */
    private fun gift(why: String) = Need("gift", 1, "Подарок $why")

    val ITEMS: List<Item> = listOf(
        // ---------- аватары ----------
        Item("av_ghost", Kind.AV, "Призрак"), Item("av_robot", Kind.AV, "Робот"), Item("av_cat", Kind.AV, "Кот"),
        Item("av_fox", Kind.AV, "Лис"), Item("av_astro", Kind.AV, "Космонавт"), Item("av_slime", Kind.AV, "Слайм"),
        Item("av_bunny", Kind.AV, "Зайка"), Item("av_panda", Kind.AV, "Панда"),
        Item("av_ninja", Kind.AV, "Ниндзя", 150), Item("av_knight", Kind.AV, "Рыцарь", 150), Item("av_pilot", Kind.AV, "Пилот", 150),
        Item("av_skull", Kind.AV, "Череп", 200), Item("av_samurai", Kind.AV, "Самурай", 200), Item("av_rocket", Kind.AV, "Ракета", 150),
        Item("av_dragon", Kind.AV, "Дракон", 250), Item("av_joystick", Kind.AV, "Джойстик", 150), Item("av_heart", Kind.AV, "Сердце", 150),
        Item("av_mech", Kind.AV, "Мех", 200), Item("av_owl", Kind.AV, "Сова-маг", 200), Item("av_hood", Kind.AV, "Тень", 200),
        Item("av_helmet", Kind.AV, "Гонщик", 150), Item("av_moon", Kind.AV, "Луна", 200), Item("av_golem", Kind.AV, "Голем", 250),
        Item("av_capsule", Kind.AV, "Капсула", 250),
        Item("av_s_knight", Kind.AV, "Сейвик-рыцарь", need = lvl(3)), Item("av_s_pilot", Kind.AV, "Сейвик-пилот", need = hours(5)),
        Item("av_s_wizard", Kind.AV, "Сейвик-маг", 300), Item("av_s_racer", Kind.AV, "Сейвик-гонщик", need = hours(10)),
        Item("av_s_ninja", Kind.AV, "Сейвик-ниндзя", 300), Item("av_s_astro", Kind.AV, "Сейвик-космонавт", need = cons(3)),
        Item("av_s_pirate", Kind.AV, "Сейвик-пират", 300), Item("av_s_samurai", Kind.AV, "Сейвик-самурай", need = hours(25)),
        Item("av_s_detective", Kind.AV, "Сейвик-сыщик", need = games(10)), Item("av_s_rock", Kind.AV, "Сейвик-рокер", 350),
        Item("av_s_champ", Kind.AV, "Сейвик-чемпион", need = streak(7)), Item("av_s_king", Kind.AV, "Сейвик-король", need = lvl(10)),
        // ---------- рамки ----------
        Item("", Kind.FR, "Без рамки"), Item("fr_ivory", Kind.FR, "Слоновая кость"),
        Item("fr_bronze", Kind.FR, "Бронза", need = lvl(3)), Item("fr_silver", Kind.FR, "Серебро", need = lvl(6)),
        Item("fr_gold", Kind.FR, "Золото", need = lvl(10)), Item("fr_platinum", Kind.FR, "Платина", need = lvl(15)),
        Item("fr_hourglass", Kind.FR, "Песочные часы", need = hours(10)), Item("fr_collector", Kind.FR, "Коллекционер", need = games(15)),
        Item("fr_handheld", Kind.FR, "Портативная", need = con("psp", 5, "PSP")), Item("fr_8bit", Kind.FR, "8 бит", need = con("nes", 3, "Денди")),
        Item("fr_16bit", Kind.FR, "16 бит", need = con("md", 3, "Mega Drive")), Item("fr_neon", Kind.FR, "Аркада", need = con("neogeo", 2, "Neo Geo")),
        Item("fr_disc", Kind.FR, "Диск", need = con("ps1", 5, "PS1")), Item("fr_legend", Kind.FR, "Легенда", need = hours(100)),
        Item("fr_creator", Kind.FR, "Пиксельная корона", need = Need("admin", 1, "Только у создателя")),
        Item("fr_pixel", Kind.FR, "Пиксели", 300), Item("fr_leaf", Kind.FR, "Листья", 300),
        Item("fr_laurel", Kind.FR, "Лавры", 500), Item("fr_flame", Kind.FR, "Пламя", 500), Item("fr_crown", Kind.FR, "Корона", 800),
        // подарочные: не продаются, приходят подарком (сайт, код подарка) — Need("gift")
        Item("fr_pioneer", Kind.FR, "Первопроходец", need = gift("первым игрокам KAPSULA")), Item("fr_winter", Kind.FR, "Зима", need = gift("к Новому году")),
        Item("fr_pumpkin", Kind.FR, "Тыквенная ночь", need = gift("на Хеллоуин")), Item("fr_birthday", Kind.FR, "День рождения", need = gift("в день рождения KAPSULA")),
        // ---------- баннеры профиля (g0…g5 — прежние градиенты, game — обложка любимой игры) ----------
        Item("g0", Kind.BN, "Ночь"), Item("g1", Kind.BN, "Закат"), Item("g2", Kind.BN, "Океан"), Item("g3", Kind.BN, "Лес"),
        Item("g4", Kind.BN, "Аврора"), Item("g5", Kind.BN, "Графит"), Item("game", Kind.BN, "Любимая игра"),
        Item("bn_creator", Kind.BN, "Зал славы", need = Need("admin", 1, "Только у создателя")),
        Item("bn_city", Kind.BN, "Ночной город"), Item("bn_space", Kind.BN, "Космос"),
        Item("bn_arcade", Kind.BN, "Аркадный зал", 400), Item("bn_room", Kind.BN, "Уютная комната", 400),
        Item("bn_forest", Kind.BN, "Лес на закате", need = streak(14)), Item("bn_reef", Kind.BN, "Подводные руины", need = hours(50)),
        // ---------- фоны главного экрана ----------
        Item("none", Kind.WP, "Без фона"), Item("game", Kind.WP, "Обложка игры"), Item("photo", Kind.WP, "Своё фото"),
        Item("wp_grid", Kind.WP, "Горизонт"), Item("wp_aurora", Kind.WP, "Сияние"),
        Item("wp_rain", Kind.WP, "Дождливая улица", 500), Item("wp_shop", Kind.WP, "Магазин игр", 600),
        Item("wp_attic", Kind.WP, "Чердак", 600), Item("wp_islands", Kind.WP, "Парящие острова", need = lvl(8)),
        // ---------- титулы ("" — по уровню) ----------
        Item("", Kind.TT, "По уровню"),
        Item("tt_night", Kind.TT, "Ночной игрок", need = hours(10)), Item("tt_collector", Kind.TT, "Коллекционер", need = games(20)),
        Item("tt_marathon", Kind.TT, "Марафонец", need = hours(50)), Item("tt_saver", Kind.TT, "Мастер сохранений", need = saves(50)),
        Item("tt_traveler", Kind.TT, "Путешественник", need = cons(5)), Item("tt_loyal", Kind.TT, "Верный игрок", need = streak(14)),
        Item("tt_legend", Kind.TT, "Легенда KAPSULA", need = lvl(15)),
        Item("tt_creator", Kind.TT, "Создатель KAPSULA", need = Need("admin", 1, "Только у создателя")),
        Item("tt_retro", Kind.TT, "Ретро-душа", 200), Item("tt_ninja", Kind.TT, "Кнопочный ниндзя", 200), Item("tt_keeper", Kind.TT, "Хранитель картриджей", 300),
    )

    fun of(kind: Kind) = ITEMS.filter { it.kind == kind }
    fun find(kind: Kind, id: String) = ITEMS.firstOrNull { it.kind == kind && it.id == id }

    // ---------- прогресс игрока ----------
    /** Сколько набрано по условию сейчас. */
    fun progress(what: String): Int {
        val lib = AppState.lib
        return when {
            // наибольшее из наигранного на телефоне и записанного на сервере (новый телефон после восстановления)
            what == "level" -> com.nexus.psp.ui.kit.Look.level(xp())
            what == "play" -> maxOf(lib.values.sumOf { it.play }, Cloud.stat("play"))
            what == "games" -> maxOf(AppState.byId.size, Cloud.stat("games"))
            what == "consoles" -> playedConsoles().size
            what == "streak" -> maxOf(AppState.streak(), Cloud.stat("streak"))
            what == "saves" -> maxOf(lib.values.sumOf { it.saves }, Cloud.stat("saves"))
            what == "admin" -> if (Cloud.isAdmin) 1 else 0
            what == "gift" -> 0 // подарок — только в Tokens.owned
            what.startsWith("con:") -> { val p = what.substringAfter(':'); maxOf(lib.entries.filter { AppState.byId[it.key]?.p == p }.sumOf { it.value.play }, Cloud.consoleMinutes(p)) }
            else -> 0
        }
    }
    fun xp() = AppState.lib.values.let { l -> maxOf(l.sumOf { it.play } * 2 + l.sumOf { it.saves } * 30, Cloud.stat("play") * 2 + Cloud.stat("saves") * 30) }
    fun playedConsoles() = AppState.lib.entries.filter { it.value.play > 0 }.mapNotNull { AppState.byId[it.key]?.p }.toSet() + Cloud.consoles()

    /** Доступен ли предмет: бесплатный, куплен или выполнено условие. */
    fun has(it: Item) = it.free || it.id in Tokens.owned || (it.need != null && progress(it.need.what) >= it.need.n)

    /** Фильтры экрана «Оформление». */
    enum class Filter(val title: String) { ALL("Все"), FREE("Бесплатные"), PLAY("За игру"), TOKENS("За жетоны"), MINE("Мои") }
    /** Особые предметы (создателя) показываются только тем, у кого они есть. */
    fun visible(it: Item) = it.need?.what != "admin" || has(it)
    fun pass(it: Item, f: Filter) = visible(it) && when (f) {
        Filter.ALL -> true; Filter.FREE -> it.free; Filter.PLAY -> it.need != null; Filter.TOKENS -> it.price > 0; Filter.MINE -> has(it)
    }
}

/**
 * Жетоны — внутриигровая валюта. Только зарабатываются (за деньги не продаются).
 * Начисление — в главном окне, по уже посчитанным цифрам (время идёт, только пока игра не на паузе).
 */
object Tokens {
    private lateinit var sp: SharedPreferences
    var balance by mutableIntStateOf(0)
        private set
    var owned by mutableStateOf<Set<String>>(emptySet())
        private set

    // правила
    const val PER_15_MIN = 5
    const val PLAY_DAY_CAP = 60
    const val LEVEL_UP = 100
    const val NEW_CONSOLE = 30
    const val NEW_GAME = 5
    const val NEW_GAME_DAY_CAP = 50
    const val WELCOME = 100

    /** «жетон / жетона / жетонов» по числу. */
    fun word(n: Int): String { val a = n % 100; val b = n % 10; return if (a in 11..14) "жетонов" else when (b) { 1 -> "жетон"; 2, 3, 4 -> "жетона"; else -> "жетонов" } }

    fun init(c: Context) {
        sp = c.getSharedPreferences("kapsula_tokens", Context.MODE_PRIVATE)
        balance = sp.getInt("balance", 0)
        owned = sp.getStringSet("owned", emptySet())!!.toSet()
    }
    private val ready get() = ::sp.isInitialized

    private fun today(): String { val c = Calendar.getInstance(); return "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)) }
    private fun yesterday(): String { val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_MONTH, -1); return "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)) }

    /** Дневная серия заходов (для награды за вход). */
    fun visitStreak() = if (ready) sp.getInt("vStreak", 0) else 0

    /**
     * Начислить всё заработанное с прошлого раза. Ответ — строки «за что» (для уведомления), пусто — ничего.
     * Первый запуск: прежние часы не превращаются в жетоны задним числом — вместо этого приветственные 100.
     */
    fun earn(): List<Pair<String, Int>> {
        if (!ready) return emptyList()
        val out = ArrayList<Pair<String, Int>>()
        val e = sp.edit()
        val day = today()
        val play = AppState.lib.values.sumOf { it.play }
        val level = com.nexus.psp.ui.kit.Look.level(Cos.xp())
        val consoles = Cos.playedConsoles()
        val games = AppState.byId.size
        if (!sp.getBoolean("started", false)) {
            e.putBoolean("started", true).putInt("pMin", play).putInt("lvl", level).putStringSet("cons", consoles).putInt("games", games)
            out += "Добро пожаловать" to WELCOME
        } else {
            // время в игре: 5 за 15 минут, не больше 60 в день
            val credited = sp.getInt("pMin", 0)
            val chunks = (play - credited).coerceAtLeast(0) / 15
            if (chunks > 0) {
                val used = if (sp.getString("pDay", "") == day) sp.getInt("pDayN", 0) else 0
                val give = minOf(chunks * PER_15_MIN, (PLAY_DAY_CAP - used).coerceAtLeast(0))
                if (give > 0) out += "Время в игре" to give
                e.putInt("pMin", credited + chunks * 15).putString("pDay", day).putInt("pDayN", used + give)
            }
            // новый уровень
            val lv = sp.getInt("lvl", level)
            if (level > lv) { out += "Уровень $level" to LEVEL_UP * (level - lv); e.putInt("lvl", level) }
            // первая игра на новой консоли
            val old = sp.getStringSet("cons", emptySet())!!
            val fresh = consoles - old
            if (fresh.isNotEmpty()) { out += "Новая консоль" to NEW_CONSOLE * fresh.size; e.putStringSet("cons", old + fresh) }
            // новые игры в библиотеке
            val g0 = sp.getInt("games", games)
            if (games > g0) {
                val used = if (sp.getString("gDay", "") == day) sp.getInt("gDayN", 0) else 0
                val give = minOf((games - g0) * NEW_GAME, (NEW_GAME_DAY_CAP - used).coerceAtLeast(0))
                if (give > 0) out += "Новые игры" to give
                e.putString("gDay", day).putInt("gDayN", used + give)
            }
            e.putInt("games", maxOf(games, g0))
        }
        // заход в день: 10, дальше +5 за каждый день подряд, до 50
        if (sp.getString("vDay", "") != day) {
            val n = if (sp.getString("vDay", "") == yesterday()) sp.getInt("vStreak", 0) + 1 else 1
            out += (if (n > 1) "Заход $n-й день подряд" else "Заход сегодня") to minOf(50, 10 + (n - 1) * 5)
            e.putString("vDay", day).putInt("vStreak", n)
        }
        val sum = out.sumOf { it.second }
        balance += sum
        e.putInt("balance", balance).apply()
        return out
    }

    /**
     * Состояние с сервера — главное: баланс, купленное. Счётчики «уже начислено» на телефоне подтягиваются к текущим цифрам,
     * чтобы без сети те же минуты не начислились второй раз.
     */
    /** Баланс с сервера (сундук, начисления) — без остального пересчёта. */
    fun serverBalance(bal: Int) { if (!ready) return; balance = bal; sp.edit().putInt("balance", bal).apply() }

    fun fromServer(bal: Int, own: Set<String>, visit: Int) {
        if (!ready) return
        balance = bal; owned = own
        val lvl = com.nexus.psp.ui.kit.Look.level(Cos.xp())
        sp.edit().putInt("balance", bal).putStringSet("owned", own).putBoolean("started", true)
            .putInt("pMin", AppState.lib.values.sumOf { it.play }).putInt("lvl", lvl).putStringSet("cons", Cos.playedConsoles())
            .putInt("games", AppState.byId.size).putString("vDay", today()).putInt("vStreak", visit).apply()
    }

    /** Купить за жетоны. false — не хватает. */
    fun buy(item: Cos.Item): Boolean {
        if (!ready || item.price <= 0 || item.id in owned) return item.id in owned
        if (balance < item.price) return false
        balance -= item.price
        owned = owned + item.id
        sp.edit().putInt("balance", balance).putStringSet("owned", owned).apply()
        Cloud.pendingBuy(item.id) // сервер спишет по своей цене при следующей синхронизации
        return true
    }
}
