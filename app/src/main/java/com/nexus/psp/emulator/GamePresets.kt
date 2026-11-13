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
    private class P(val name: String, val keys: List<String>, val genre: String, val extra: (JSONObject) -> Unit = {})

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
        P("Tekken", listOf("tekken"), "fight"),
        P("Soulcalibur", listOf("soulcalibur", "soulcalibur"), "fight", icons("fight:psp:aft" to "sword", "fight:psp:afl" to "sword", "fight:psp:afb" to "block", "fight:psp:afr" to "kick")),
        P("Mortal Kombat", listOf("mortalkombat"), "fight"),
        P("Street Fighter", listOf("streetfighter"), "fight"),
        P("Darkstalkers", listOf("darkstalkers", "vampirechronicle"), "fight"),
        P("Dragon Ball Z", listOf("dragonball"), "fight"),
        P("Naruto", listOf("naruto"), "fight"),
        P("Bleach", listOf("bleach"), "fight"),
        P("Dissidia Final Fantasy", listOf("dissidia"), "fight"),
        P("WWE SmackDown vs. Raw", listOf("smackdown", "wwe"), "fight"),
        P("Fight Night Round 3", listOf("fightnight"), "fight"),
        P("BlazBlue", listOf("blazblue"), "fight"),
        P("The King of Fighters", listOf("kingoffighters"), "fight"),
        P("Guilty Gear", listOf("guiltygear"), "fight"),
        P("UFC Undisputed", listOf("ufc"), "fight"),
        P("Marvel vs. Capcom / Capcom Classics", listOf("marvelsuperheroes"), "fight"),
        // слэшеры и приключения
        P("God of War", listOf("godofwar"), "slasher"),
        P("Assassin's Creed: Bloodlines", listOf("assassinscreed"), "slasher"),
        P("Prince of Persia", listOf("princeofpersia"), "slasher"),
        P("Tomb Raider", listOf("tombraider"), "slasher"),
        P("Kingdom Hearts: Birth by Sleep", listOf("kingdomhearts"), "slasher"),
        P("Crisis Core: Final Fantasy VII", listOf("crisiscore"), "slasher"),
        P("Dante's Inferno", listOf("dantesinferno"), "slasher"),
        P("Star Wars: The Force Unleashed", listOf("forceunleashed"), "slasher"),
        P("X-Men Origins: Wolverine", listOf("wolverine"), "slasher"),
        P("Spider-Man", listOf("spiderman"), "slasher"),
        P("Thor: God of Thunder", listOf("thorgodofthunder"), "slasher"),
        P("Marvel Ultimate Alliance", listOf("ultimatealliance"), "slasher"),
        P("Dynasty Warriors", listOf("dynastywarriors", "shinsangokumusou"), "slasher"),
        P("Warriors Orochi", listOf("warriorsorochi", "musououochi"), "slasher"),
        P("Monster Hunter", listOf("monsterhunter"), "slasher"),
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
        P("WipEout", listOf("wipeout"), "race"),
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
        P("LocoRoco", listOf("locoroco"), "platform"),
        P("Daxter", listOf("daxter"), "platform"),
        P("LittleBigPlanet", listOf("littlebigplanet"), "platform"),
        P("Crash Bandicoot", listOf("crash"), "platform"),
        P("Mega Man", listOf("megaman", "rockman"), "platform"),
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
        P("Patapon", listOf("patapon"), "pad"),
        P("Lumines", listOf("lumines"), "pad"),
        P("Lemmings", listOf("lemmings"), "pad"),
        P("Puzzle Quest", listOf("puzzlequest"), "pad"),
        P("Hatsune Miku: Project DIVA", listOf("projectdiva", "hatsunemiku"), "pad"),
        P("DJ Max", listOf("djmax"), "pad"),
    )

    private fun norm(s: String?) = s.orEmpty().lowercase().replace(Regex("[^a-z0-9]"), "")

    /** Пресет по названию игры (имя файла и название из базы дисков): имя пресета и настройки кнопок (ctl). */
    fun find(vararg names: String?): Pair<String, JSONObject>? {
        val ns = names.map { norm(it) }.filter { it.isNotEmpty() }
        if (ns.isEmpty()) return null
        val p = LIST.firstOrNull { e -> e.keys.any { k -> ns.any { it.contains(k) } } } ?: return null
        val g = Templates.genre(p.genre) ?: return null
        val o = JSONObject().put("layouts", Templates.layoutsJson(g))
        p.extra(o)
        return p.name to o
    }

    /** Сколько игр знает база (для экрана «О приложении» / тестов). */
    val size get() = LIST.size
}
