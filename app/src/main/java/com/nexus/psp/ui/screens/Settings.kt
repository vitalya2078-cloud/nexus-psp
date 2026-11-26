package com.nexus.psp.ui.screens

import kotlinx.coroutines.launch
import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.PLATFORMS
import com.nexus.psp.ui.Full
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.OvHead
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.Opt
import com.nexus.psp.ui.kit.Pill
import com.nexus.psp.ui.kit.Seg
import com.nexus.psp.ui.kit.SolarDuo
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.StateCard
import com.nexus.psp.ui.kit.Sub
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.Toggle
import com.nexus.psp.ui.kit.press
import com.nexus.psp.ui.kit.tap
import org.json.JSONObject
import com.nexus.psp.data.CoreOpt
import com.nexus.psp.data.CoreOptions
import com.nexus.psp.data.CoreStore

/** Настройки (п. 21, 22, 27, 43, 47, 51, 53) + настройки ядра PSP по утверждённому макету. */
@Composable
fun SettingsScreen() {
    val nav = listOf(
        Triple("general", "Общие", R.drawable.sl_sliders), Triple("dl", "Игры и память", R.drawable.sl_folder),
        Triple("core", "Эмулятор PSP", R.drawable.sl_monitor), Triple("ps1", "Эмулятор PS1", R.drawable.ph_disc), Triple("ctl", "Управление", R.drawable.sl_pad),
        Triple("ach", "Достижения", R.drawable.sl_trophy),
        Triple("bios", "BIOS", R.drawable.sl_chip), Triple("a11y", "Доступность", R.drawable.ph_hand), Triple("about", "О приложении", R.drawable.sl_info),
    )
    val tab = Nav.settingsTab
    Full {
        OvHead("Настройки")
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // меню прокручивается: на невысоких экранах последний пункт не уходит за край
            Column(Modifier.width(200.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                nav.forEach { (id, t, ic) ->
                    val on = tab == id
                    Row(
                        Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (on) K.s2 else androidx.compose.ui.graphics.Color.Transparent).tap { Nav.settingsTab = id }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SolarIcon(ic, 20.dp, if (on) K.acc else K.ink2)
                        Text(t, style = T.label.copy(fontSize = 14.sp, color = if (on) K.ink else K.ink2))
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                when (tab) {
                    "general" -> General()
                    "dl" -> Downloads()
                    "core" -> CoreSettings()
                    "ps1" -> {
                        Text("Для всех игр PS1. Игре можно задать свои — в её карточке или в паузе.", style = T.small, modifier = Modifier.padding(bottom = 6.dp))
                        Sub("Картинка"); CoreRows(CoreOptions.PS1_QUALITY)
                        Sub("Система"); CoreRows(CoreOptions.PS1_SYSTEM)
                        Text("Заработает со следующего запуска игры.", style = T.small, modifier = Modifier.padding(top = 10.dp))
                    }
                    "ctl" -> Controls()
                    "ach" -> Achievements()
                    "bios" -> Bios()
                    "a11y" -> A11y()
                    else -> About()
                }
            }
        }
    }
}

@Composable
private fun General() {
    val th = listOf("dark", "oled", "light")
    // тема, цвет, фон главного экрана и профиль — в «Оформлении» (быстрая панель); здесь — дорога туда
    Opt("Оформление", "Цвет акцента, фон главного экрана, аватар и рамки") { KButton("Открыть", small = true) { com.nexus.psp.ui.Nav.replace(com.nexus.psp.ui.Ov.Look()) } }
    CloudAccount()
    Opt("Язык", null) { Seg(listOf("RU", "EN"), if (S.lang == "EN") 1 else 0) { S.lang = if (it == 1) "EN" else "RU"; if (it == 1) AppState.say("Английский перевод появится позже") } }
    Opt("Маскот", "Живой маскот на главном экране и в сообщениях: здоровается, радуется, грустит вместе с тобой. Выключишь — останутся простые картинки") { Toggle(S.mascot) { S.mascot = it } }
    Opt("Вибрация", "Отклик при выборе и важных действиях") { Toggle(S.vib) { S.vib = it } }
    Opt("Звуки интерфейса", "Тихий щелчок в карусели") { Toggle(S.sound) { S.sound = it } }
    Opt("Часы и заряд в шапке", "Время и батарея на главном экране — системная строка там скрыта") { Toggle(S.headClock) { S.headClock = it } }
    Opt("Автосохранение", "Каждые N минут игра сохраняется сама — в слот «Место выхода»") {
        val m = listOf("0", "5", "10", "15")
        Seg(listOf("Выкл", "5 мин", "10 мин", "15 мин"), m.indexOf(S.autoSave).coerceAtLeast(0), small = true) { S.autoSave = m[it] }
    }
    Opt("Экономия батареи", "При заряде 15% и ниже — пресет «Батарея» до зарядки") { Toggle(S.batterySaver) { S.batterySaver = it } }
    Opt("Продолжать с места выхода", "При выходе из игры место сохраняется само, «Играть» возвращает туда же") { Toggle(S.autoResume) { S.autoResume = it } }
    CloudSavesOpt()
    Opt("Анонимная статистика", "Сколько запусков, на каких консолях, были ли сбои — без названий игр и без твоего ID. Помогает чинить KAPSULA") {
        Toggle(S.stats) { S.stats = it; if (!it) com.nexus.psp.data.Telemetry.clear() }
    }
}

@Composable
private fun Downloads() {
    val ctx = LocalContext.current
    // перечитать доступ, когда игрок вернулся из системных настроек
    val access = remember(AppState.storageTick) { com.nexus.psp.data.Storage.allFiles(ctx) }
    val folder = com.nexus.psp.emulator.GameLibrary.FOLDER
    if (access) Opt("Папка $folder", "В памяти телефона. Клади сюда игры — они появятся в библиотеке сами и запустятся без копирования") {
        KButton("Открыть", R.drawable.sl_folder, small = true) { com.nexus.psp.emulator.GameLibrary.ensureFolder(ctx); AppState.storageTick++; com.nexus.psp.ui.Act.addFile() }
    } else Opt("Папка $folder", "Своя папка для игр: кладёшь файлы — они появляются сами. Нужен «Доступ ко всем файлам»") {
        KButton("Включить", R.drawable.sl_device, BtnKind.Main, small = true) { com.nexus.psp.ui.Act.requestAllFiles?.invoke() }
    }
    Opt("Копии игр", sub = com.nexus.psp.emulator.CoreFiles.gamesDir(ctx).absolutePath.replace("/storage/emulated/0/", "") + " — сюда копируются игры из системного окна и скачанные") { }
    Opt("Добавить игру", if (access) "Игры из памяти телефона запускаются на месте, без копирования" else "Файл ISO, CSO, CHD или PBP — скопируется в KAPSULA") { KButton("Выбрать файл", R.drawable.ph_plus, small = true) { com.nexus.psp.ui.Act.addFile() } }
    Opt("Найти игры на телефоне", "Выбери папку — KAPSULA найдёт все образы в ней и вложенных папках") { KButton("Выбрать папку", R.drawable.sl_search, small = true) { com.nexus.psp.ui.Act.findGames() } }
    Opt("Обложки из интернета", "По серийному номеру диска, из открытой базы обложек. Сохраняются на телефоне") { Toggle(S.covers) { S.covers = it; if (it) AppState.byId.keys.forEach { id -> AppState.fetchMeta(id) } } }
    Opt("Обложки только по Wi-Fi", "Одна обложка — до 1 МБ") { Toggle(S.coversWifi) { S.coversWifi = it } }
    Opt("Обновить обложки", "Поискать заново для всех игр") { KButton("Обновить", small = true) { AppState.byId.keys.forEach { id -> AppState.fetchMeta(id, force = true) } } }
    Opt("Память", sub = "Игры KAPSULA ${com.nexus.psp.data.size(AppState.usedMb())} · свободно ${com.nexus.psp.data.size(AppState.freeMb())}") { }
}

@Composable
private fun Controls() {
    // геймпад: если подключён — подсказка не нужна
    val pad = remember { android.view.InputDevice.getDeviceIds().any { id -> android.view.InputDevice.getDevice(id)?.let { d -> !d.isVirtual && (d.sources and android.view.InputDevice.SOURCE_GAMEPAD) == android.view.InputDevice.SOURCE_GAMEPAD } == true } }
    if (!pad) Box(Modifier.padding(bottom = 8.dp)) {
        StateCard("mascot-gamepad", "Есть геймпад?", "Подключи его по Bluetooth или проводу — KAPSULA подхватит сама, а экранные кнопки спрячутся (если включено «Скрывать кнопки»).")
    }
    Opt("Экранный геймпад", sub = mapOf("touch" to "KAPSULA Touch", "classic" to "Классика", "pad" to "Геймпад")[S.ctlMode] + " · раскладка, размер, прозрачность") {
        KButton("Настроить", kind = com.nexus.psp.ui.kit.BtnKind.Main, small = true) { Nav.open(Ov.Pad) }
    }
    Opt("Скрывать кнопки", "Когда подключён геймпад") { Toggle(S.hide) { S.hide = it } }
    Opt("Подписи под кнопками", "«Газ», «Прыжок», «Удар» — под значками экранных кнопок. Что делает каждая кнопка — во вкладке «Управление»") { Toggle(S.caps) { S.caps = it } }
    Opt("Показывать смену раскладки", "Маленькая плашка с названием раскладки, когда переключаешь её в игре") { Toggle(S.layHint) { S.layHint = it } }
    CoreRows(CoreOptions.INPUT)
    Opt("Ускорение", "Во сколько раз быстрее при включённом ускорении") {
        Seg(listOf("×2", "×3", "×4"), ((S.fastLevel.toIntOrNull() ?: 2) - 2).coerceIn(0, 2), small = true) { S.fastLevel = (it + 2).toString() }
    }
    Opt("Чувствительность руля наклоном", "Руль наклоном включается в игре: пауза → Настройки → Управление") {
        com.nexus.psp.ui.kit.Range(S.tiltSens.toIntOrNull() ?: 100, 50, 200, fill = false) { S.tiltSens = it.toString() }
    }
    Opt("Физический геймпад", "Подключи по Bluetooth или проводу — заработает сразу. Кнопка «Mode» открывает паузу") { Text("автоматически", style = T.small) }
}

@Composable
private fun Bios() {
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var tick by remember { mutableStateOf(0) }
    var target by remember { mutableStateOf("ps1") }
    val pick = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val r = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.nexus.psp.emulator.BiosFiles.add(ctx, target, uri) }
            if (r.startsWith("ok:")) AppState.say("BIOS на месте: " + r.removePrefix("ok:"), R.drawable.ph_check) else AppState.say(r.removePrefix("bad:"), R.drawable.sl_alert)
            tick++
        }
    }
    Box(Modifier.padding(bottom = 10.dp)) {
        StateCard("mascot-chip", "BIOS — прошивка приставки", "Бери только со своей приставки: KAPSULA проверит файл и положит куда надо. Приставкам, которых нет в списке ниже, BIOS не нужен вообще.")
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.s1)) {
        PLATFORMS.filter { it.bios }.forEachIndexed { i, p ->
            val found = remember(tick, p.id, AppState.storageTick) { com.nexus.psp.emulator.BiosFiles.found(ctx, p.id) }
            if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(K.line))
            Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SolarIcon(R.drawable.sl_chip, 20.dp, if (found.isNotEmpty()) K.ok else if (p.biosLevel == 2) K.warn else K.ink3)
                Column(Modifier.weight(1f)) {
                    Text(p.short, style = T.label.copy(fontSize = 14.sp))
                    Text(if (found.isNotEmpty()) "Есть: " + found.joinToString(" · ") else p.biosHint, style = T.small, maxLines = 1)
                }
                if (found.isNotEmpty()) KButton("Убрать", small = true) { com.nexus.psp.emulator.BiosFiles.clear(ctx, p.id); tick++ }
                KButton(if (found.isEmpty()) "Добавить" else "Ещё", icon = R.drawable.ph_plus, small = true) {
                    // свой проводник; без доступа к файлам — системное окно
                    if (com.nexus.psp.data.Storage.hasAny(ctx)) com.nexus.psp.ui.Nav.open(com.nexus.psp.ui.Ov.Files("bios:" + p.id)) else { target = p.id; pick.launch(arrayOf("*/*")) }
                }
            }
        }
    }
}

@Composable
private fun A11y() {
    Opt("Крупный текст", null) { Toggle(S.big) { S.big = it; AppState.applyLook() } }
    Opt("Высокий контраст", null) { Toggle(S.contrast) { S.contrast = it; AppState.applyLook() } }
    Opt("Уменьшить движение", "Без анимаций") { Toggle(S.calm) { S.calm = it } }
    Opt("Раскладка для левшей", "Зеркальные экранные кнопки") { Toggle(S.mirror) { S.mirror = it } }
}

@Composable
private fun About() {
    val ctx = LocalContext.current
    val ver = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName } catch (e: Exception) { "" }
    Opt("KAPSULA", sub = "Версия $ver") { KButton("Знакомство заново", small = true) { S.onboarded = false; AppState.say("Знакомство покажется при следующем запуске") } }
    val scopeU = androidx.compose.runtime.rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    Opt("Обновления", "KAPSULA сама проверяет новую версию раз в 6 часов — на GitVerse (без VPN) и на GitHub") {
        KButton(if (checking) "Проверяю…" else "Проверить", small = true) {
            if (!checking) { checking = true; scopeU.launch {
                val latest = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.nexus.psp.data.Update.latest(ctx) }
                checking = false
                when {
                    latest == null -> AppState.say("Не получилось проверить. Нужен интернет")
                    com.nexus.psp.data.Update.newer(latest, com.nexus.psp.BuildConfig.VERSION_NAME) -> {
                        com.nexus.psp.data.Update.announce(ctx, latest)
                        AppState.say("Вышла KAPSULA $latest", action = "Скачать") { com.nexus.psp.data.Update.download(ctx) }
                    }
                    else -> AppState.say("У тебя последняя версия")
                }
            } }
        }
    }
    Opt("Эмуляторы", sub = "PSP, PS1, PS2, Sega, NEC, SNK, Bandai, Atari") { }
    Opt("Конфиденциальность", sub = "Игры и файлы остаются на телефоне. Что уходит на сервер и зачем — на сайте") {
        KButton("Открыть", small = true) { openUrl(ctx, "https://kapsula.gitverse.site/privacy.html") }
    }
    // связь: «Написать нам» (ВК) и «Сообщить об ошибке» — готовый текст с версией, телефоном и последним сбоем
    Row(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(18.dp)).background(K.s2).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        com.nexus.psp.ui.kit.LiveMascot("letter", 72.dp, glow = false)
        Column(Modifier.weight(1f)) {
            Text("Есть идея или что-то сломалось?", style = T.label.copy(fontSize = 15.sp))
            Text("Пиши — читаем всё. Отчёт об ошибке сам добавит версию, модель телефона и последний сбой.", style = T.small)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
            KButton("Написать нам", small = true, kind = BtnKind.Main) { openUrl(ctx, "https://vk.ru/glasty") }
            KButton("Сообщить об ошибке", small = true) { shareReport(ctx) }
        }
    }
    // авторы и тексты лицензий открытых компонентов — по условиям этих лицензий; на виду только нейтральная строка
    var legal by remember { mutableStateOf(false) }
    // незаметная серая ссылка внизу раздела
    Text(
        "Правовая информация", style = T.small.copy(fontSize = 11.sp, color = K.ink3),
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp).clip(RoundedCornerShape(6.dp)).press { legal = true }.padding(horizontal = 2.dp, vertical = 4.dp),
    )
    if (legal) LegalDialog { legal = false }
    // после вылета — неприметная ссылка «Отправить отчёт об ошибке»: окно «Поделиться» с готовым текстом
    // в отчёт идут и сбои загрузки картинок (значки достижений) — чтобы видеть причину, а не гадать
    var crash by remember { mutableStateOf(com.nexus.psp.data.CrashLog.last(ctx)?.let { f -> try { f.readText() } catch (e: Exception) { "" } }
        .let { c -> listOfNotNull(c?.takeIf { it.isNotBlank() }, com.nexus.psp.data.Net.failLog(ctx)?.let { "Загрузка картинок:\n$it" }, com.nexus.psp.emulator.LaunchCheck.saved(ctx)?.let { "Игра не запустилась:\n$it" }, com.nexus.psp.emulator.Jank.log(ctx)).joinToString("\n\n").ifBlank { null }
            ?.let { "KAPSULA ${com.nexus.psp.BuildConfig.VERSION_NAME} · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.RELEASE}\n\n$it" } }) }
    crash?.let { txt ->
        Text(
            "Отправить отчёт об ошибке", style = T.small.copy(fontSize = 11.sp, color = K.ink3),
            modifier = Modifier.padding(bottom = 8.dp).clip(RoundedCornerShape(6.dp)).press {
                try {
                    ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(android.content.Intent.EXTRA_SUBJECT, "KAPSULA — отчёт об ошибке").putExtra(android.content.Intent.EXTRA_TEXT, txt), "Отправить отчёт")
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    com.nexus.psp.data.CrashLog.clear(ctx); com.nexus.psp.data.Net.clearFailLog(ctx); com.nexus.psp.emulator.Jank.clear(ctx); crash = null
                } catch (e: Exception) { AppState.say("Не нашлось, чем отправить") }
            }.padding(horizontal = 2.dp, vertical = 4.dp),
        )
    }
}

// ---------- ядро PSP ----------

/** Общие настройки ядра для всех игр PSP. */
val GlobalCore = object : CoreStore {
    override fun vars(): JSONObject = try { JSONObject(S.coreVars) } catch (e: Exception) { JSONObject() }
    override fun set(k: String, v: String) { S.coreVars = vars().put(k, v).toString(); S.corePreset = "custom" }
    override var preset: String
        get() = S.corePreset
        set(value) { S.corePreset = value }
}

/** Пресет по мощности телефона: ОЗУ 8 ГБ+ — «Качество», 4 ГБ+ — «Баланс», иначе — «Батарея». */
fun recommended(ctx: Context): String {
    val mi = ActivityManager.MemoryInfo(); (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
    val gb = mi.totalMem / (1024.0 * 1024 * 1024)
    return when { gb >= 7.5 -> "quality"; gb >= 3.5 -> "balance"; else -> "battery" }
}

@Composable
fun CoreRows(list: List<CoreOpt>, store: CoreStore = GlobalCore, onChange: (CoreOpt, String) -> Unit = { _, _ -> }) {
    var tick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val o = androidx.compose.runtime.remember(tick, store) { store.vars() }
    list.forEach { c ->
        val cur = o.optString(c.key, c.def)
        Opt(c.title, c.desc, badge = if (c.restart) ({ Pill("после перезапуска игры", color = K.warn, bg = K.warn.copy(alpha = .14f)) }) else null) {
            Seg(c.labels, c.values.indexOf(cur).coerceAtLeast(0), small = true) { store.set(c.key, c.values[it]); tick++; onChange(c, c.values[it]) }
        }
    }
}

/** Почему Vulkan недоступен на этом телефоне (и «Повторить» после сбоя). Ничего не показывает, если всё в порядке. */
@Composable
fun VulkanNote() {
    val ctx = LocalContext.current
    var tick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val why = androidx.compose.runtime.remember(tick) { com.nexus.psp.emulator.Vk.reason(ctx) } ?: return
    Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(why, style = T.small.copy(color = K.warn), modifier = Modifier.weight(1f))
        if (com.nexus.psp.emulator.Vk.badFile(ctx).exists()) KButton("Повторить", small = true) { com.nexus.psp.emulator.Vk.retry(ctx); tick++ }
    }
}

/** Блок «Качество» с пресетами — общий для экрана ядра и настроек игры. */
@Composable
fun CoreQuality(store: CoreStore = GlobalCore, onChange: (CoreOpt?, String) -> Unit = { _, _ -> }) {
    val ctx = LocalContext.current
    val rec = remember { recommended(ctx) }
    var tick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val preset = androidx.compose.runtime.remember(tick, store) { store.preset }
    androidx.compose.runtime.LaunchedEffect(Unit) { if (store === GlobalCore && S.corePreset.isEmpty()) { store.applyPreset(rec); tick++ } }
    val names = mapOf("quality" to "Качество", "balance" to "Баланс", "battery" to "Батарея")
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(K.acc.copy(alpha = .1f)).border(1.dp, K.acc.copy(alpha = .3f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SolarDuo(R.drawable.sd_chip, 20.dp)
        Text("Подобрано под твой телефон: ${names[rec]}", style = T.label.copy(fontSize = 13.sp, color = K.ink2))
    }
    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Max).padding(top = 18.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // строка 1 — разрешение (короткая, без переносов), строка 2 — смысл пресета
        listOf(
            Triple("quality", "1440×816", "Текстуры ×2, для мощных телефонов" to 4),
            Triple("balance", "960×544", "Чётко и плавно" to 3),
            Triple("battery", "480×272", "Как на PSP, дольше играть" to 1),
            Triple("custom", "Вручную", "Включится само, если поменяешь настройки" to 0),
        ).forEach { (id, l1, rest) ->
            val on = preset == id
            // отметка «Рекомендуем» — язычком на верхней границе карточки, вне потока текста: строки всех карточек на одном уровне
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Column(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(if (on) K.acc.copy(alpha = .1f) else K.s1)
                        .border(1.5.dp, if (on) K.acc else K.line, RoundedCornerShape(14.dp))
                        .then(if (id != "custom") Modifier.press { store.applyPreset(id); tick++; onChange(null, id) } else Modifier)
                        .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(names[id] ?: "Своё", style = T.label.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (id == "custom" && !on) K.ink3 else K.ink))
                    Text(l1, style = T.label.copy(fontSize = 12.5.sp, color = if (on) K.ink else K.ink2), maxLines = 1)
                    Text(rest.first, style = T.small.copy(fontSize = 11.sp, lineHeight = 14.sp), modifier = Modifier.weight(1f))
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(4) { i -> Box(Modifier.size(14.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(if (i < rest.second) (if (on) K.acc else K.ink2) else K.s3)) }
                    }
                }
                if (id == rec) Text(
                    "РЕКОМЕНДУЕМ", style = T.caps.copy(fontSize = 9.sp, color = K.bg), maxLines = 1,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = (-10).dp, y = (-8).dp).clip(RoundedCornerShape(8.dp)).background(K.acc).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
    Sub("Изображение")
    CoreRows(CoreOptions.QUALITY, store) { c, v -> tick++; onChange(c, v) }
}

@Composable
private fun CoreSettings() {
    var sec by rememberSaveable { mutableStateOf(0) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 6.dp)) {
        Seg(listOf("Качество", "Скорость", "Система", "Для опытных"), sec) { sec = it }
        Text("для всех игр PSP", style = T.small)
    }
    when (sec) {
        0 -> {
            val f = listOf("default" to "Обычный", "smooth" to "Сглаживание", "pixel" to "Пиксели", "lcd" to "ЖК", "crt" to "ЭЛТ")
            CoreRows(CoreOptions.RENDER)
            Opt("Авто-качество", "Держать 60 кадров: разрешение снижается само при нехватке скорости") { Toggle(S.autoRes) { S.autoRes = it } }
            Opt("Экран 60 Гц в игре", "Игры идут в 60 кадров. На экране 120 или 144 Гц картинка может периодически дёргаться — на время игры KAPSULA переключает экран на 60 Гц") { Toggle(S.hz60) { S.hz60 = it } }
            Opt("Фильтр картинки", "Для всех игр; в игре можно поменять на лету: пауза → Настройки") {
                Seg(f.map { it.second }, f.indexOfFirst { it.first == S.shader }.coerceAtLeast(0), small = true) { S.shader = f[it].first }
            }
            CoreQuality()
        }
        1 -> CoreRows(CoreOptions.SPEED)
        2 -> CoreRows(CoreOptions.SYSTEM)
        else -> {
            StateCard(null, null, "Эти параметры могут сломать игру. Не помогло — верни как было.", tint = K.warn) {
                KButton("Сбросить раздел", small = true) { val o = GlobalCore.vars(); CoreOptions.EXPERT.forEach { o.remove(it.key) }; S.coreVars = o.toString(); AppState.say("Раздел «Для опытных» сброшен") }
            }
            CoreRows(CoreOptions.EXPERT)
        }
    }
    Text("Заработает со следующего запуска игры.", style = T.small, modifier = Modifier.padding(top = 10.dp))
}


/** Правовая информация: компоненты с открытым исходным кодом, авторы, лицензии (полные тексты — из assets/licenses). */
@Composable
private fun LegalDialog(onClose: () -> Unit) {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf<String?>(null) }
    val parts = listOf(
        Triple("Ядро эмуляции PSP", "Основано на PPSSPP — Copyright © 2012– Henrik Rydgård and PPSSPP contributors. GNU GPL версии 2 или более поздней. Изменено командой KAPSULA.", "GPL-2.0.txt"),
        Triple("Ядро эмуляции PS1", "Основано на PCSX-ReARMed — Copyright © PCSX Team, notaz и участники проекта. GNU GPL версии 2.", "GPL-2.0.txt"),
        Triple("Ядро PS2", "Основано на ARMSX2 (форк PCSX2) — © команда ARMSX2 и PCSX2. GNU GPL версии 3.", "GPL-3.0.txt"),
        Triple("Ядро Dreamcast", "Основано на Flycast — © Flycast team. GNU GPL версии 2.", "GPL-2.0.txt"),
        Triple("Ядро Saturn", "Основано на YabaSanshiro — © devMiyax и участники Yabause. GNU GPL версии 2.", "GPL-2.0.txt"),
        Triple("Ядро Денди", "Основано на FCEUmm (FCE Ultra mappers modified) — © участники FCE Ultra и FCEUmm. GNU GPL версии 2.", "GPL-2.0.txt"),
        Triple("Ядро Mega Drive", "Основано на ClownMDEmu — © Clownacy. GNU AGPL версии 3.", "GPL-3.0.txt"),
        Triple("Ядро Master System / Game Gear", "Основано на Gearsystem — © Ignacio Sanchez. GNU GPL версии 3.", "GPL-3.0.txt"),
        Triple("Ядро PC Engine", "Основано на Geargrafx — © Ignacio Sanchez. GNU GPL версии 3.", "GPL-3.0.txt"),
        Triple("Ядро Lynx", "Основано на Gearlynx — © Ignacio Sanchez. GNU GPL версии 3.", "GPL-3.0.txt"),
        Triple("Ядро Neo Geo", "Основано на Geolith — © Rupert Carmichael. BSD-3-Clause и MIT.", null),
        Triple("Ядра Neo Geo Pocket и WonderSwan", "Основаны на Mednafen (Beetle NeoPop, Beetle Cygne) — © Mednafen team. GNU GPL версии 2.", "GPL-2.0.txt"),
        Triple("Ядра Atari 2600 и 7800", "Основаны на Stella (© Stella team) и ProSystem (© Greg Stanton). GNU GPL версии 2.", "GPL-2.0.txt"),
        Triple("Фронтенд libretro", "LibretroDroid — Copyright © Filippo Scognamiglio. GNU GPL версии 3.", "GPL-3.0.txt"),
        Triple("Значки Solar", "© 480 Design. Creative Commons Attribution 4.0 (CC BY 4.0).", "LICENSE-solar.txt"),
        Triple("Значки Phosphor", "© Phosphor Icons. MIT License.", "LICENSE-phosphor.txt"),
        Triple("Шрифт Manrope", "© The Manrope Project Authors. SIL Open Font License 1.1.", "OFL-Manrope.txt"),
        Triple("Шрифт Rubik", "© The Rubik Project Authors. SIL Open Font License 1.1.", "OFL-Rubik.txt"),
        Triple("Шрифт Russo One", "© Jovanny Lemonad. SIL Open Font License 1.1.", "OFL-RussoOne.txt"),
        Triple("Шрифт Balsamiq Sans", "© The Balsamiq Sans Project Authors. SIL Open Font License 1.1.", "OFL-BalsamiqSans.txt"),
        Triple("Шрифт M PLUS Rounded 1c", "© The M+ FONTS Project Authors, The Rounded M+ Project Authors. SIL Open Font License 1.1.", "OFL-MPlusRounded.txt"),
    )
    androidx.compose.ui.window.Dialog(onClose, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(.86f).fillMaxHeight(.9f).clip(RoundedCornerShape(22.dp)).background(K.s1).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (open == null) "Правовая информация" else open!!, style = T.h3, modifier = Modifier.weight(1f))
                if (open != null) { KButton("Назад", small = true) { open = null }; androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp)) }
                KButton(icon = R.drawable.ph_x, small = true) { onClose() }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val f = open
                if (f != null) {
                    val txt = remember(f) { try { ctx.assets.open("licenses/$f").bufferedReader().readText() } catch (e: Exception) { "Текст лицензии не найден" } }
                    Text(txt, style = T.small.copy(fontSize = 11.sp, lineHeight = 15.sp, color = K.ink2))
                } else {
                    Text("KAPSULA использует компоненты с открытым исходным кодом. Исходный код KAPSULA, включая изменения ядра: github.com/vitalya2078-cloud/nexus-psp", style = T.small.copy(color = K.ink2))
                    parts.forEach { (t, d, file) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).padding(end = 12.dp)) { Text(t, style = T.label); Text(d, style = T.small) }
                            if (file != null) KButton("Текст", small = true) { open = file }
                        }
                    }
                }
            }
        }
    }
}


/** RetroAchievements: вход (токен хранится на телефоне, пароль — нет), включение достижений в играх. */
@Composable
private fun Achievements() {
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var user by remember { mutableStateOf(S.raUser) }
    var pass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf(0) } // 0 — вход, 1 — регистрация
    val logged = S.raToken.isNotEmpty()

    // шапка раздела: кубок и три выгоды
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color(0xFF3A2E14), K.s1)))
            .border(1.dp, androidx.compose.ui.graphics.Color(0xFFD9B36C).copy(alpha = .35f), RoundedCornerShape(20.dp)).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        AssetImage("ach-trophy.png", Modifier.size(72.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Достижения в играх", style = T.h3)
            listOf(
                R.drawable.sl_star to "Награды за игровые события — как на современных консолях",
                R.drawable.sl_trophy to "Очки и общий рейтинг игроков",
                R.drawable.sl_cloud to "Прогресс хранится в профиле, а не на телефоне",
            ).forEach { (ic, t) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SolarIcon(ic, 16.dp, androidx.compose.ui.graphics.Color(0xFFD9B36C)); Text(t, style = T.small.copy(color = K.ink2))
                }
            }
        }
    }
    androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))

    if (logged) {
        // карточка профиля
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.s1).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            com.nexus.psp.ui.kit.NetImage(com.nexus.psp.ui.kit.Look.raPic(S.raUser), Modifier.size(56.dp).clip(androidx.compose.foundation.shape.CircleShape).background(K.s2), androidx.compose.ui.layout.ContentScale.Crop)
            Column(Modifier.weight(1f)) {
                Text(S.raUser, style = T.h3)
                Text("${S.raScore} очков · мягкий режим: ускорение, сохранения и читы разрешены", style = T.small)
            }
            KButton("Выйти", small = true) { S.raToken = ""; S.raScore = "0"; if (S.avMode == "ra") S.avMode = "letter"; AppState.say("Вы вышли из аккаунта достижений") }
        }
        Opt("Достижения в играх", "Всплывают во время игры; список — в паузе, кнопка «Достижения»") { Toggle(S.raOn) { S.raOn = it } }
        Opt("Аватар профиля", "Взять картинку из аккаунта достижений для профиля KAPSULA") {
            Toggle(S.avMode == "ra") { S.avMode = if (it) "ra" else "letter" }
        }
        return
    }

    Seg(listOf("Вход", "Регистрация"), mode, small = true) { mode = it; err = null }
    androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
    if (mode == 1) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.s1).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Аккаунт создаётся на сайте сообщества RetroAchievements — бесплатно, за минуту.", style = T.label.copy(fontSize = 14.sp))
            listOf("Откроется страница регистрации — прямо поверх приложения", "Придумай имя и пароль, укажи почту и подтверди её письмом", "Вернись сюда и войди — имя уже будет подставлено").forEachIndexed { i, t ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(22.dp).clip(androidx.compose.foundation.shape.CircleShape).background(K.s3), contentAlignment = Alignment.Center) { Text("${i + 1}", style = T.small.copy(fontSize = 11.sp, color = K.ink)) }
                    Text(t, style = T.small.copy(color = K.ink2))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KButton("Создать аккаунт", icon = R.drawable.sl_user, kind = com.nexus.psp.ui.kit.BtnKind.Main, small = true) { openWeb(ctx, "https://retroachievements.org/createaccount.php"); mode = 0 }
                KButton("Уже есть аккаунт", small = true) { mode = 0 }
            }
        }
        return
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.s1).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchField(user, "Имя пользователя", Modifier.weight(1f), icon = R.drawable.sl_user) { user = it; err = null }
            PasswordField(pass, "Пароль", Modifier.weight(1f)) { pass = it; err = null }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Пароль не сохраняется: KAPSULA хранит только ключ входа.", style = T.small.copy(fontSize = 11.sp), modifier = Modifier.weight(1f))
            KButton(if (busy) "Вход…" else "Войти", kind = com.nexus.psp.ui.kit.BtnKind.Main, small = true, enabled = !busy && user.isNotBlank() && pass.isNotEmpty()) {
                busy = true; err = null
                scope.launch {
                    val r = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.nexus.psp.emulator.RA.login(user.trim(), pass) }
                    busy = false
                    r.onSuccess { (token, score) -> S.raUser = user.trim(); S.raToken = token; S.raScore = score.toString(); pass = ""; AppState.say("Вход выполнен: ${user.trim()}", R.drawable.ph_check) }
                    r.onFailure { e -> err = e.message.orEmpty().ifEmpty { "Не получилось войти" } }
                }
            }
        }
        err?.let { Text(it, style = T.small.copy(color = K.bad)) }
        Text("Нет аккаунта? Регистрация", style = T.small.copy(color = K.acc), modifier = Modifier.clip(RoundedCornerShape(6.dp)).press { mode = 1 }.padding(vertical = 4.dp))
    }
}

/** Страница сайта поверх приложения (встроенная вкладка браузера); нет браузера с вкладками — обычный браузер. */
fun openWeb(ctx: Context, url: String) {
    try {
        androidx.browser.customtabs.CustomTabsIntent.Builder().setShowTitle(true)
            .setDefaultColorSchemeParams(androidx.browser.customtabs.CustomTabColorSchemeParams.Builder().setToolbarColor(0xFF0E0F13.toInt()).build())
            .build().launchUrl(ctx, android.net.Uri.parse(url))
    } catch (e: Exception) {
        try { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (x: Exception) { AppState.say("Не нашлось браузера, чтобы открыть страницу") }
    }
}

/** Поле пароля: символы скрыты, «глаз» показывает их. */
@Composable
private fun PasswordField(value: String, hint: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    var show by remember { mutableStateOf(false) }
    Row(modifier.height(42.dp).clip(RoundedCornerShape(21.dp)).background(K.s2).padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SolarIcon(R.drawable.sl_keyhole, 18.dp, K.ink3)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, style = T.label.copy(color = K.ink3, fontSize = 14.sp))
            androidx.compose.foundation.text.BasicTextField(
                value, onChange, singleLine = true, textStyle = T.label.copy(fontSize = 14.sp),
                visualTransformation = if (show) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(K.acc), modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(Modifier.size(34.dp).clip(androidx.compose.foundation.shape.CircleShape).press { show = !show }, contentAlignment = Alignment.Center) {
            SolarIcon(if (show) R.drawable.sl_eye_off else R.drawable.sl_eye, 18.dp, K.ink3)
        }
    }
}

/** Облачные сохранения: выключатель и «Сверить сейчас». */
@Composable
private fun CloudSavesOpt() {
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    Opt("Облачные сохранения", "Сейвы игр уходят в облако после игры и возвращаются по коду восстановления на другом телефоне. Снимки «Место выхода» — только на телефоне") {
        Toggle(S.cloud) { S.cloud = it }
    }
    if (S.cloud) Opt("Сверить с облаком", "Загрузить новые сейвы и забрать те, что есть только в облаке") {
        KButton(if (busy) "Сверяю…" else "Сверить", small = true, enabled = !busy) {
            busy = true
            scope.launch { val r = com.nexus.psp.data.CloudSaves.sync(ctx.applicationContext); busy = false; AppState.say(r.text) }
        }
    }
}

private fun openUrl(ctx: android.content.Context, url: String) {
    try { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (e: Exception) { AppState.say("Не нашлось браузера, чтобы открыть ссылку") }
}

/** Отчёт об ошибке: версия, телефон, Android и последние сбои (если были) — в окно «Поделиться». */
private fun shareReport(ctx: android.content.Context) {
    val log = listOfNotNull(
        com.nexus.psp.data.CrashLog.last(ctx)?.let { f -> try { f.readText() } catch (e: Exception) { null } }?.takeIf { it.isNotBlank() },
        com.nexus.psp.emulator.LaunchCheck.saved(ctx)?.let { "Игра не запустилась:\n$it" },
    ).joinToString("\n\n")
    val txt = "KAPSULA ${com.nexus.psp.BuildConfig.VERSION_NAME} · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.RELEASE}\n\n" +
        "Что случилось:\n\n" + (if (log.isNotEmpty()) "Журнал:\n$log" else "")
    try {
        ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
            .putExtra(android.content.Intent.EXTRA_SUBJECT, "KAPSULA — ошибка").putExtra(android.content.Intent.EXTRA_TEXT, txt), "Сообщить об ошибке")
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) { AppState.say("Не получилось открыть «Поделиться»") }
}
