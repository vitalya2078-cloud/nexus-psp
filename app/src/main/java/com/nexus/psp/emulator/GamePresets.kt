package com.nexus.psp.emulator

import org.json.JSONObject

/**
 * Встроенные раскладки популярных игр PSP. Игра узнаётся по названию (имя файла или название из базы дисков),
 * поэтому одна запись работает для всех регионов. Только раскладки кнопок — никаких файлов игр.
 *
 * Точность:
 *  - GTA: Vice City Stories — сверено с таблицей управления PSP (WikiGTA, раскладка 1): пешком, транспорт, вертолёт.
 *  - GTA: Liberty City Stories — та же схема, что у VCS (предшественник на том же движке; отдельной таблицы не нашли).
 *  - Tekken, Soulcalibur, PES/Winning Eleven — общепринятая схема серий на PlayStation.
 *  - Остальные — шаблон жанра (самая частая схема PSP); игрок подправляет в редакторе.
 */
object GamePresets {
    private class P(val name: String, val keys: List<String>, val genre: String, val extra: (JSONObject) -> Unit = {}, val lays: List<LaySpec>? = null,
                    val plats: Set<String> = SONY)

    /** PSP, PS1 и PS2 — одни и те же кнопки (× ○ □ △, L, R), жанровые раскладки подходят всем трём. */
    private val SONY = setOf("psp", "ps1", "ps2")

    /**
     * Игра ретро-консоли: её родной джойстик, но кнопки подписаны действиями игры. acts: кнопка RetroPad → (значок, подпись).
     * Схемы — из руководств игр (кнопки консоли, не экрана; сверены с раскладкой ядра в ControlLayout.NAMES).
     */
    class Retro(val plat: String, val acts: Map<String, Pair<String, String>>)
    private val RETRO = HashMap<String, Retro>()
    private fun r(name: String, plat: String, keys: List<String>, genre: String, vararg acts: Pair<String, Pair<String, String>>): P {
        val id = "r_" + plat + "_" + norm(name).take(24)
        RETRO[id] = Retro(plat, acts.toMap())
        return P(name, keys, genre, lays = one(id), plats = setOf(plat))
    }
    private infix fun String.on(a: Pair<String, String>) = this to a

    /** Раскладка игры ретро-консоли (основа "r_…"): джойстик консоли с подписями действий. */
    fun retroLayout(base: String): List<Btn>? {
        val r = RETRO[base] ?: return null
        return ControlLayout.layout("touch", r.plat).map { b ->
            val a = r.acts[b.key ?: b.id] ?: return@map b
            if (b.w != null) b.copy(label = a.second) else b.copy(icon = a.first, cap = a.second, label = null)
        }
    }

    /** Своя раскладка игры (GameLayouts): одна или несколько с переключением. */
    private fun one(base: String, name: String = "Основная") = listOf(LaySpec("main", name, base))

    private fun icons(vararg p: Pair<String, String>): (JSONObject) -> Unit = { o ->
        val m = o.optJSONObject("keyIcon") ?: JSONObject().also { o.put("keyIcon", it) }
        p.forEach { (k, v) -> m.put(k, v) }
    }

    private val LIST: List<P> = listOf(
        // экшены с транспортом
        P("GTA: Vice City Stories", listOf("vicecitystories"), "action"),
        P("GTA: Liberty City Stories", listOf("libertycitystories"), "action"),
        P("GTA: Chinatown Wars", listOf("chinatownwars"), "action"),
        P("Driver 76", listOf("driver76"), "action"),
        P("The Godfather: Mob Wars", listOf("godfathermobwars", "godfather"), "action"),
        P("Pursuit Force", listOf("pursuitforce"), "race"),
        // файтинги
        P("Tekken", listOf("tekken"), "fight", lays = one("g_tekken", "Бой")),
        P("Soulcalibur", listOf("soulcalibur", "soulcalibur"), "fight", icons("fight:psp:aft" to "sword", "fight:psp:afl" to "sword", "fight:psp:afb" to "block", "fight:psp:afr" to "kick")),
        P("Mortal Kombat", listOf("mortalkombat"), "fight"),
        P("Street Fighter", listOf("streetfighter"), "fight"),
        P("Darkstalkers", listOf("darkstalkers", "vampirechronicle"), "fight"),
        P("Dragon Ball Z", listOf("dragonball"), "fight"),
        P("Naruto", listOf("naruto"), "fight"),
        P("Bleach", listOf("bleach"), "fight"),
        P("Dissidia Final Fantasy", listOf("dissidia"), "fight", lays = one("g_dissidia", "Бой")),
        P("WWE SmackDown vs. Raw", listOf("smackdown", "wwe"), "fight"),
        P("Fight Night Round 3", listOf("fightnight"), "fight"),
        P("BlazBlue", listOf("blazblue"), "fight"),
        P("The King of Fighters", listOf("kingoffighters"), "fight"),
        P("Guilty Gear", listOf("guiltygear"), "fight"),
        P("UFC Undisputed", listOf("ufc"), "fight"),
        P("Marvel vs. Capcom / Capcom Classics", listOf("marvelsuperheroes"), "fight"),
        // слэшеры и приключения
        P("God of War", listOf("godofwar"), "slasher", lays = one("g_gow", "Бой")),
        P("Assassin's Creed: Bloodlines", listOf("assassinscreed"), "slasher", lays = one("g_ac")),
        P("Prince of Persia", listOf("princeofpersia"), "slasher"),
        P("Tomb Raider", listOf("tombraider"), "slasher"),
        P("Kingdom Hearts: Birth by Sleep", listOf("kingdomhearts"), "slasher", lays = one("g_khbbs")),
        P("Crisis Core: Final Fantasy VII", listOf("crisiscore"), "slasher"),
        P("Dante's Inferno", listOf("dantesinferno"), "slasher"),
        P("Star Wars: The Force Unleashed", listOf("forceunleashed"), "slasher"),
        P("X-Men Origins: Wolverine", listOf("wolverine"), "slasher"),
        P("Spider-Man", listOf("spiderman"), "slasher"),
        P("Thor: God of Thunder", listOf("thorgodofthunder"), "slasher"),
        P("Marvel Ultimate Alliance", listOf("ultimatealliance"), "slasher"),
        P("Dynasty Warriors", listOf("dynastywarriors", "shinsangokumusou"), "slasher", lays = one("g_musou", "Бой")),
        P("Warriors Orochi", listOf("warriorsorochi", "musououochi"), "slasher", lays = one("g_musou", "Бой")),
        P("Monster Hunter", listOf("monsterhunter"), "slasher", lays = one("g_mh", "Охота")),
        P("Gods Eater", listOf("godeater", "godseater"), "slasher"),
        P("Phantasy Star Portable", listOf("phantasystar"), "slasher"),
        P("Ben 10", listOf("ben10"), "slasher"),
        P("The Warriors", listOf("thewarriors"), "slasher"),
        P("Silent Hill", listOf("silenthill"), "slasher"),
        P("Resident Evil / Biohazard", listOf("residentevil", "biohazard"), "shooter"),
        // гонки
        P("Need for Speed", listOf("needforspeed"), "race"),
        P("Burnout", listOf("burnout"), "race"),
        P("Midnight Club", listOf("midnightclub"), "race"),
        P("Gran Turismo", listOf("granturismo"), "race"),
        P("Ridge Racer", listOf("ridgeracer"), "race"),
        P("WipEout", listOf("wipeout"), "race", lays = one("g_wipeout", "Гонка")),
        P("MotorStorm", listOf("motorstorm"), "race"),
        P("Cars", listOf("cars", "tachki"), "race"),
        P("Ferrari Challenge", listOf("ferrari"), "race"),
        P("Colin McRae / DiRT", listOf("colinmcrae", "dirt2"), "race"),
        P("Sega Rally", listOf("segarally"), "race"),
        P("OutRun 2006", listOf("outrun"), "race"),
        P("Juiced", listOf("juiced"), "race"),
        P("Tokyo Xtreme Racer / Kaido", listOf("tokyoxtreme", "kaido", "shutokou"), "race"),
        P("ModNation Racers", listOf("modnation"), "race"),
        P("Crash Tag Team Racing", listOf("crashtagteam"), "race"),
        P("Twisted Metal: Head-On", listOf("twistedmetal"), "race"),
        P("MX vs. ATV", listOf("mxvsatv"), "race"),
        P("Formula One", listOf("formulaone", "f12006", "f12009"), "race"),
        P("TOCA Race Driver", listOf("toca"), "race"),
        // шутеры
        P("Metal Gear Solid", listOf("metalgear"), "shooter"),
        P("Syphon Filter", listOf("syphonfilter"), "shooter"),
        P("SOCOM", listOf("socom"), "shooter"),
        P("Killzone: Liberation", listOf("killzone"), "shooter"),
        P("Resistance: Retribution", listOf("resistance"), "shooter"),
        P("Star Wars Battlefront", listOf("battlefront"), "shooter"),
        P("Medal of Honor", listOf("medalofhonor"), "shooter"),
        P("Call of Duty", listOf("callofduty"), "shooter"),
        P("Ghost Recon", listOf("ghostrecon"), "shooter"),
        P("Rainbow Six", listOf("rainbowsix"), "shooter"),
        P("Splinter Cell", listOf("splintercell"), "shooter"),
        P("Army of Two: The 40th Day", listOf("armyoftwo"), "shooter"),
        P("Coded Arms", listOf("codedarms"), "shooter"),
        P("Brothers in Arms", listOf("brothersinarms"), "shooter"),
        P("The 3rd Birthday", listOf("3rdbirthday", "thirdbirthday"), "shooter"),
        P("Ratchet & Clank: Size Matters", listOf("ratchet"), "shooter"),
        P("Gun Showdown", listOf("gunshowdown"), "shooter"),
        // полёты
        P("Ace Combat", listOf("acecombat"), "flight"),
        P("After Burner: Black Falcon", listOf("afterburner"), "flight"),
        // спорт
        P("Pro Evolution Soccer", listOf("proevolution", "pes20", "winningeleven", "worldsoccer"), "sport"),
        P("FIFA", listOf("fifa"), "sport"),
        P("NBA", listOf("nba"), "sport"),
        P("Madden NFL", listOf("madden"), "sport"),
        P("NHL", listOf("nhl"), "sport"),
        P("Virtua Tennis", listOf("virtuatennis"), "sport"),
        P("Top Spin", listOf("topspin"), "sport"),
        P("Everybody's Golf / Hot Shots Golf", listOf("everybodysgolf", "hotshotsgolf", "minnanogolf"), "sport"),
        P("Tiger Woods PGA Tour", listOf("tigerwoods"), "sport"),
        // платформеры
        P("LocoRoco", listOf("locoroco"), "platform", lays = one("g_loco")),
        P("Daxter", listOf("daxter"), "platform"),
        P("LittleBigPlanet", listOf("littlebigplanet"), "platform"),
        P("Crash Bandicoot", listOf("crash"), "platform"),
        P("Mega Man", listOf("megaman", "rockman"), "platform", lays = one("g_megaman")),
        P("Castlevania", listOf("castlevania", "akumajou"), "platform"),
        P("Rayman", listOf("rayman"), "platform"),
        P("SpongeBob", listOf("spongebob"), "platform"),
        P("LEGO", listOf("lego"), "platform"),
        P("Toy Story 3", listOf("toystory"), "platform"),
        P("Sonic Rivals", listOf("sonic"), "platform"),
        P("Jak / Secret Agent Clank", listOf("secretagentclank"), "platform"),
        // RPG
        P("Final Fantasy", listOf("finalfantasy"), "rpg"),
        P("Persona", listOf("persona"), "rpg"),
        P("Star Ocean", listOf("starocean"), "rpg"),
        P("Tales of", listOf("talesof"), "rpg"),
        P("Valkyrie Profile", listOf("valkyrieprofile"), "rpg"),
        P("Valkyria Chronicles", listOf("valkyria", "senjounovalkyria"), "rpg"),
        P("Jeanne d'Arc", listOf("jeannedarc"), "rpg"),
        P("Disgaea", listOf("disgaea"), "rpg"),
        P("Ys", listOf("ysseven", "ysthe", "ysi", "ysvs"), "rpg"),
        P("Lunar: Silver Star Harmony", listOf("lunar"), "rpg"),
        P("Breath of Fire", listOf("breathoffire"), "rpg"),
        P("Wild Arms XF", listOf("wildarms"), "rpg"),
        P("Dungeon Siege / Untold Legends", listOf("untoldlegends", "dungeonsiege"), "slasher"),
        // ритм и головоломки — обычный джойстик (кнопки игры — сами фигуры)
        P("Patapon", listOf("patapon"), "pad", lays = one("g_patapon", "Барабаны")),
        P("Lumines", listOf("lumines"), "pad"),
        P("Lemmings", listOf("lemmings"), "pad"),
        P("Puzzle Quest", listOf("puzzlequest"), "pad"),
        P("Hatsune Miku: Project DIVA", listOf("projectdiva", "hatsunemiku"), "pad"),
        P("DJ Max", listOf("djmax"), "pad"),

        // ---------- PS1 и PS2: те же кнопки, что у PSP — жанровые раскладки ----------
        P("GTA: San Andreas", listOf("sanandreas"), "action", plats = setOf("ps2")),
        P("GTA: Vice City", listOf("vicecity", "grandtheftautovice"), "action", plats = setOf("ps2")),
        P("GTA III", listOf("grandtheftautoiii", "gta3", "gtaiii"), "action", plats = setOf("ps2")),
        P("Bully", listOf("bully", "canisCanem"), "action", plats = setOf("ps2")),
        P("Driver", listOf("driver"), "action", plats = setOf("ps1", "ps2")),
        P("Spyro", listOf("spyro"), "platform", plats = setOf("ps1", "ps2")),
        P("Ape Escape", listOf("apeescape", "sarugetchu"), "platform", plats = setOf("ps1", "ps2")),
        P("Jak and Daxter", listOf("jakand", "jakii", "jak3", "jakx"), "platform", plats = setOf("ps2")),
        P("Ratchet & Clank", listOf("ratchetclank", "ratchetandclank"), "shooter", plats = setOf("ps2")),
        P("Sly Cooper", listOf("slycooper", "slyraccoon"), "platform", plats = setOf("ps2")),
        P("Devil May Cry", listOf("devilmaycry"), "slasher", plats = setOf("ps2")),
        P("Shadow of the Colossus", listOf("shadowofthecolossus", "wandertokyozou"), "slasher", plats = setOf("ps2")),
        P("Okami", listOf("okami", "ookami"), "slasher", plats = setOf("ps2")),
        P("Max Payne", listOf("maxpayne"), "shooter", plats = setOf("ps2")),
        P("Hitman", listOf("hitman"), "shooter", plats = setOf("ps2")),
        P("Dino Crisis", listOf("dinocrisis"), "shooter", plats = setOf("ps1", "ps2")),
        P("Tony Hawk's Pro Skater", listOf("tonyhawk"), "sport", plats = setOf("ps1", "ps2")),

        // ---------- Mega Drive: A=□(fl), B=×(fb), C=○(fr), X=L, Y=△, Z=R ----------
        r("Sonic the Hedgehog", "md", listOf("sonic"), "platform", "fb" on ("jump" to "Прыжок"), "fl" on ("jump" to "Прыжок"), "fr" on ("jump" to "Прыжок")),
        r("Streets of Rage", "md", listOf("streetsofrage", "bareknuckle"), "slasher", "fl" on ("special" to "Спецприём"), "fb" on ("attack" to "Удар"), "fr" on ("jump" to "Прыжок")),
        r("Golden Axe", "md", listOf("goldenaxe"), "slasher", "fl" on ("magic" to "Магия"), "fb" on ("sword" to "Удар"), "fr" on ("jump" to "Прыжок")),
        r("Street Fighter II (Mega Drive)", "md", listOf("streetfighter"), "fight",
            "l1" on ("jab" to "Лёгкий удар"), "ft" on ("attack" to "Средний удар"), "r1" on ("heavy" to "Сильный удар"),
            "fl" on ("kick" to "Лёгкий пинок"), "fb" on ("kick" to "Средний пинок"), "fr" on ("kick" to "Сильный пинок")),
        r("Contra: Hard Corps", "md", listOf("contrahardcorps", "contrathehardcorps", "probotector"), "shooter", "fl" on ("weapon" to "Оружие"), "fb" on ("shoot" to "Огонь"), "fr" on ("jump" to "Прыжок")),
        r("Aladdin", "md", listOf("aladdin"), "platform", "fl" on ("throw" to "Яблоко"), "fb" on ("sword" to "Сабля"), "fr" on ("jump" to "Прыжок")),
        r("Earthworm Jim", "md", listOf("earthwormjim"), "platform", "fl" on ("shoot" to "Огонь"), "fb" on ("attack" to "Хлыст"), "fr" on ("jump" to "Прыжок")),
        r("Shinobi", "md", listOf("shinobi", "revengeofshinobi", "superShinobi"), "platform", "fl" on ("magic" to "Ниндзюцу"), "fb" on ("attack" to "Атака"), "fr" on ("jump" to "Прыжок")),

        // ---------- Master System и Game Gear: 1=×(fb), 2=○(fr) ----------
        r("Alex Kidd in Miracle World", "sms", listOf("alexkidd"), "platform", "fb" on ("attack" to "Удар"), "fr" on ("jump" to "Прыжок")),
        r("Sonic the Hedgehog (Master System)", "sms", listOf("sonic"), "platform", "fb" on ("jump" to "Прыжок"), "fr" on ("jump" to "Прыжок")),
        r("Wonder Boy", "sms", listOf("wonderboy"), "platform", "fb" on ("attack" to "Атака"), "fr" on ("jump" to "Прыжок")),
        r("Sonic the Hedgehog (Game Gear)", "gg", listOf("sonic"), "platform", "fb" on ("jump" to "Прыжок"), "fr" on ("jump" to "Прыжок")),

        // ---------- Денди: B=×(fb), A=○(fr), турбо B=□, турбо A=△. Только игры сторонних студий ----------
        r("Contra", "nes", listOf("contra", "probotector"), "shooter", "fb" on ("shoot" to "Огонь"), "fr" on ("jump" to "Прыжок"), "fl" on ("turbo" to "Автоогонь")),
        r("Battletoads", "nes", listOf("battletoads"), "slasher", "fb" on ("attack" to "Удар"), "fr" on ("jump" to "Прыжок")),
        r("Chip 'n Dale", "nes", listOf("chipndale", "chipdale", "rescuerangers"), "platform", "fb" on ("throw" to "Бросить"), "fr" on ("jump" to "Прыжок")),
        r("DuckTales", "nes", listOf("ducktales"), "platform", "fb" on ("attack" to "Трость"), "fr" on ("jump" to "Прыжок")),
        r("Mega Man (Денди)", "nes", listOf("megaman", "rockman"), "platform", "fb" on ("shoot" to "Выстрел"), "fr" on ("jump" to "Прыжок"), "fl" on ("turbo" to "Автоогонь")),
        r("Ninja Gaiden", "nes", listOf("ninjagaiden", "ninjaryukenden"), "platform", "fb" on ("sword" to "Меч"), "fr" on ("jump" to "Прыжок")),
        r("Castlevania (Денди)", "nes", listOf("castlevania", "akumajou"), "platform", "fb" on ("attack" to "Хлыст"), "fr" on ("jump" to "Прыжок")),
        r("Double Dragon", "nes", listOf("doubledragon"), "slasher", "fb" on ("attack" to "Рука"), "fr" on ("kick" to "Нога")),
        r("Teenage Mutant Ninja Turtles", "nes", listOf("teenagemutant", "tmnt", "ninjaturtles"), "slasher", "fb" on ("attack" to "Удар"), "fr" on ("jump" to "Прыжок")),
        r("Battle City", "nes", listOf("battlecity", "tank1990"), "shooter", "fb" on ("shoot" to "Огонь"), "fr" on ("shoot" to "Огонь")),

        // ---------- PC Engine: I=○(fr), II=×(fb) ----------
        r("Bonk's Adventure", "pce", listOf("bonk", "pcgenjin"), "platform", "fb" on ("attack" to "Удар головой"), "fr" on ("jump" to "Прыжок")),

        // ---------- Saturn: A=×, B=○, C=L, X=□, Y=△, Z=R (файтинги Capcom: X Y Z — руки, A B C — ноги) ----------
        r("Street Fighter Alpha / Zero", "saturn", listOf("streetfighter", "streetfighterzero"), "fight",
            "fl" on ("jab" to "Лёгкий удар"), "ft" on ("attack" to "Средний удар"), "r1" on ("heavy" to "Сильный удар"),
            "fb" on ("kick" to "Лёгкий пинок"), "fr" on ("kick" to "Средний пинок"), "l1" on ("kick" to "Сильный пинок")),
        r("Darkstalkers / Vampire Savior", "saturn", listOf("darkstalkers", "vampiresavior", "vampirehunter"), "fight",
            "fl" on ("jab" to "Лёгкий удар"), "ft" on ("attack" to "Средний удар"), "r1" on ("heavy" to "Сильный удар"),
            "fb" on ("kick" to "Лёгкий пинок"), "fr" on ("kick" to "Средний пинок"), "l1" on ("kick" to "Сильный пинок")),

        // ---------- Dreamcast: A=×, B=○, X=□, Y=△ ----------
        r("Sonic Adventure", "dc", listOf("sonicadventure"), "platform", "fb" on ("jump" to "Прыжок"), "fr" on ("dash" to "Рывок"), "fl" on ("tap" to "Действие")),

        // ---------- Neo Geo: A=×, B=○, C=□, D=△ ----------
        r("Metal Slug", "neogeo", listOf("metalslug", "mslug"), "shooter", "fb" on ("shoot" to "Огонь"), "fr" on ("jump" to "Прыжок"), "fl" on ("grenade" to "Граната")),
        r("The King of Fighters", "neogeo", listOf("kingoffighters", "kof"), "fight",
            "fb" on ("jab" to "Лёгкий удар"), "fr" on ("kick" to "Лёгкий пинок"), "fl" on ("heavy" to "Сильный удар"), "ft" on ("kick" to "Сильный пинок")),
        r("Garou: Mark of the Wolves", "neogeo", listOf("garou"), "fight",
            "fb" on ("jab" to "Лёгкий удар"), "fr" on ("kick" to "Лёгкий пинок"), "fl" on ("heavy" to "Сильный удар"), "ft" on ("kick" to "Сильный пинок")),
        r("Samurai Shodown", "neogeo", listOf("samuraishodown", "samuraispirits", "samsho"), "fight",
            "fb" on ("sword" to "Слабый удар"), "fr" on ("sword" to "Средний удар"), "fl" on ("kick" to "Пинок")),
        r("The Last Blade", "neogeo", listOf("lastblade", "bakumatsuroman"), "fight",
            "fb" on ("sword" to "Слабый удар"), "fr" on ("heavy" to "Сильный удар"), "fl" on ("kick" to "Пинок"), "ft" on ("block" to "Отбив")),

        // ---------- Neo Geo Pocket: A=×, B=○ ----------
        r("Metal Slug (Neo Geo Pocket)", "ngp", listOf("metalslug"), "shooter", "fb" on ("shoot" to "Огонь"), "fr" on ("jump" to "Прыжок")),
        r("SNK vs. Capcom / King of Fighters (Pocket)", "ngp", listOf("snkvscapcom", "kingoffighters", "kof"), "fight", "fb" on ("jab" to "Удар"), "fr" on ("kick" to "Пинок")),
    )

    private fun norm(s: String?) = s.orEmpty().lowercase().replace(Regex("[^a-z0-9]"), "")

    /** Пресет по названию игры (имя файла и название из базы дисков): имя пресета и настройки кнопок (ctl). */
    fun find(plat: String?, vararg names: String?): Pair<String, JSONObject>? {
        val p = match(plat, names) ?: return null
        val o = if (p.lays != null) JSONObject().put("layouts", org.json.JSONArray().also { a -> p.lays.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("base", it.base)) } })
            else JSONObject().put("layouts", Templates.layoutsJson(Templates.genre(p.genre) ?: return null))
        p.extra(o)
        return p.name to o
    }

    /** Запись базы по названию: только для этой консоли (null — любая). */
    private fun match(plat: String?, names: Array<out String?>): P? {
        val ns = names.map { norm(it) }.filter { it.isNotEmpty() }
        if (ns.isEmpty()) return null
        return LIST.firstOrNull { e -> (plat == null || plat in e.plats) && e.keys.any { k -> ns.any { it.contains(norm(k)) } } }
    }

    /** Игра из базы для поиска во вкладке «Управление»: название, жанр, есть ли своя раскладка, консоль. */
    class Entry(val name: String, val genre: String, val own: Boolean, val plat: String)
    val entries: List<Entry> get() = LIST.map { Entry(it.name, it.genre, it.lays != null, platOf(it)) }
    private fun platOf(p: P) = if (p.plats == SONY) "psp" else p.plats.first()

    /** Консоль записи базы (для гайда): PSP — общая для PSP, PS1, PS2. */
    fun platOf(name: String): String = LIST.firstOrNull { it.name == name }?.let { platOf(it) } ?: "psp"

    /** Раскладки игры по её названию в базе. */
    fun byName(name: String): JSONObject? = LIST.firstOrNull { it.name == name }?.let { p -> find(platOf(p), p.keys.first()) ?: find(platOf(p), p.name) }?.second

    /** Жанр игры по названию файла/диска (для ярлыка на обложке). */
    fun genreOf(plat: String?, vararg names: String?): String? = match(plat, names)?.genre
    fun ownOf(plat: String?, vararg names: String?): Boolean = match(plat, names)?.lays != null

    /** Сколько игр знает база (для экрана «О приложении» / тестов). */
    val size get() = LIST.size
}
