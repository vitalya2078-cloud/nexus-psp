package com.nexus.psp.ui.screens

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

/** Настройки (п. 21, 22, 27, 43, 47, 51, 53) + настройки ядра PSP по утверждённому макету. */
@Composable
fun SettingsScreen() {
    val nav = listOf(
        Triple("general", "Общие", R.drawable.sl_sliders), Triple("dl", "Игры и память", R.drawable.sl_folder),
        Triple("core", "Эмулятор PSP", R.drawable.sl_monitor), Triple("ctl", "Управление", R.drawable.sl_pad),
        Triple("bios", "BIOS", R.drawable.sl_chip), Triple("a11y", "Доступность", R.drawable.ph_hand), Triple("about", "О приложении", R.drawable.sl_info),
    )
    val tab = Nav.settingsTab
    Full {
        OvHead("Настройки")
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.width(200.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                nav.forEach { (id, t, ic) ->
                    val on = tab == id
                    Row(
                        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (on) K.s2 else androidx.compose.ui.graphics.Color.Transparent).tap { Nav.settingsTab = id }.padding(horizontal = 14.dp),
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
                    "ctl" -> Controls()
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
    Opt("Тема", "OLED — чистый чёрный для AMOLED-экранов") { Seg(listOf("Тёмная", "OLED", "Светлая"), th.indexOf(S.theme).coerceAtLeast(0)) { S.theme = th[it]; AppState.applyLook() } }
    Opt("Язык", null) { Seg(listOf("RU", "EN"), if (S.lang == "EN") 1 else 0) { S.lang = if (it == 1) "EN" else "RU"; if (it == 1) AppState.say("Английский перевод появится позже") } }
    Opt("Вибрация", "Отклик при выборе и важных действиях") { Toggle(S.vib) { S.vib = it } }
    Opt("Звуки интерфейса", "Тихий щелчок в карусели") { Toggle(S.sound) { S.sound = it } }
    Opt("Облачные сохранения", "Продолжай на другом телефоне (в планах)") { Toggle(S.cloud) { S.cloud = it } }
}

@Composable
private fun Downloads() {
    val ctx = LocalContext.current
    Opt("Папка с играми", com.nexus.psp.emulator.CoreFiles.gamesDir(ctx).absolutePath.replace("/storage/emulated/0/", "")) { Text("своя папка — позже", style = T.small) }
    Opt("Добавить игру", "Файл ISO, CSO, CHD или PBP — скопируется в папку KAPSULA") { KButton("Выбрать файл", R.drawable.ph_plus, small = true) { com.nexus.psp.ui.Act.addFile() } }
    Opt("Найти игры на телефоне", "Выбери папку — KAPSULA найдёт все образы в ней и вложенных папках") { KButton("Выбрать папку", R.drawable.sl_search, small = true) { com.nexus.psp.ui.Act.findGames() } }
    Opt("Обложки из интернета", "По серийному номеру диска, из открытой базы libretro. Сохраняются на телефоне") { Toggle(S.covers) { S.covers = it; if (it) AppState.byId.keys.forEach { id -> AppState.fetchMeta(id) } } }
    Opt("Обложки только по Wi-Fi", "Одна обложка — до 1 МБ") { Toggle(S.coversWifi) { S.coversWifi = it } }
    Opt("Обновить обложки", "Поискать заново для всех игр") { KButton("Обновить", small = true) { AppState.byId.keys.forEach { id -> AppState.fetchMeta(id, force = true) } } }
    Opt("Память", "Игры KAPSULA ${com.nexus.psp.data.size(AppState.usedMb())} · свободно ${com.nexus.psp.data.size(AppState.freeMb())}") { }
}

@Composable
private fun Controls() {
    Opt("Экранный геймпад", mapOf("touch" to "KAPSULA Touch", "classic" to "Классика", "pad" to "Геймпад")[S.ctlMode] + " · раскладка, размер, прозрачность") {
        KButton("Настроить", kind = com.nexus.psp.ui.kit.BtnKind.Main, small = true) { Nav.open(Ov.Pad) }
    }
    Opt("Скрывать кнопки", "Когда подключён геймпад") { Toggle(S.hide) { S.hide = it } }
    Opt("Физический геймпад", "Кнопки передаются в ядро напрямую; «Mode» — меню паузы") { Text("автоматически", style = T.small) }
}

@Composable
private fun Bios() {
    StateCard("ic-bios.png", "BIOS — прошивка консоли.", "Для PS1 и PS2 понадобится файл BIOS с твоей приставки — KAPSULA проверит его по контрольной сумме. PSP работает без BIOS.")
    PLATFORMS.forEach { p ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssetImage(p.conImg, Modifier.width(76.dp).height(44.dp))
            Box(Modifier.weight(1f)) {
                Opt("BIOS ${p.full}", if (!p.bios) "Не требуется" else "Понадобится, когда подключим ${p.short}") {
                    if (!p.bios) Pill("не нужен", icon = R.drawable.ph_check, color = K.ok) else Pill("скоро")
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
    val core = try { ctx.assets.open("ppsspp/CORE.txt").bufferedReader().readText().trim() } catch (e: Exception) { "ядро не найдено в этой сборке" }
    Opt("KAPSULA", "Версия $ver · нативный интерфейс") { KButton("Знакомство заново", small = true) { S.onboarded = false; AppState.say("Знакомство покажется при следующем запуске") } }
    Opt("Эмулятор PSP", "KAPSULA на ядре PPSSPP · $core") { }
    Opt("PS1 и PS2", "Beetle PSX / PCSX-ReARMed и PCSX2 — будут подключены") { }
    Opt("Лицензии", "PPSSPP (GPL-2.0+), LibretroDroid (GPL-3.0), значки Solar (CC BY 4.0) и Phosphor (MIT), шрифт Manrope (OFL)") { }
    Opt("Конфиденциальность", "Данные хранятся только на телефоне") { }
}

// ---------- ядро PSP ----------

private data class CoreOpt(val key: String, val title: String, val desc: String, val values: List<String>, val labels: List<String>, val def: String, val restart: Boolean = false)

private val PRESETS = mapOf(
    "quality" to mapOf("ppsspp_internal_resolution" to "1440x816", "ppsspp_texture_scaling_level" to "2x", "ppsspp_texture_scaling_type" to "xbrz", "ppsspp_texture_filtering" to "Auto max quality", "ppsspp_texture_anisotropic_filtering" to "16x", "ppsspp_auto_frameskip" to "disabled"),
    "balance" to mapOf("ppsspp_internal_resolution" to "960x544", "ppsspp_texture_scaling_level" to "disabled", "ppsspp_texture_filtering" to "Auto", "ppsspp_texture_anisotropic_filtering" to "8x", "ppsspp_auto_frameskip" to "disabled"),
    "battery" to mapOf("ppsspp_internal_resolution" to "480x272", "ppsspp_texture_scaling_level" to "disabled", "ppsspp_texture_filtering" to "Auto", "ppsspp_texture_anisotropic_filtering" to "disabled", "ppsspp_auto_frameskip" to "enabled"),
)

private val QUALITY = listOf(
    CoreOpt("ppsspp_internal_resolution", "Разрешение", "Во сколько раз чётче, чем на PSP (480×272)", listOf("480x272", "960x544", "1440x816", "1920x1088"), listOf("×1", "×2", "×3", "×4"), "960x544"),
    CoreOpt("ppsspp_texture_scaling_level", "Улучшение текстур", "Сглаживает пиксели на текстурах. Заметно греет телефон", listOf("disabled", "2x", "3x", "4x"), listOf("Выкл", "×2", "×3", "×4"), "disabled"),
    CoreOpt("ppsspp_texture_filtering", "Фильтрация", "Как сглаживаются текстуры при масштабировании", listOf("Auto", "Nearest", "Linear", "Auto max quality"), listOf("Авто", "Чётко", "Гладко", "Макс"), "Auto"),
    CoreOpt("ppsspp_cropto16x9", "Обрезать до 16:9", "Убрать по строке сверху и снизу ради точного 16:9", listOf("enabled", "disabled"), listOf("Да", "Нет"), "enabled"),
)
private val SPEED = listOf(
    CoreOpt("ppsspp_frameskip", "Пропуск кадров", "Помогает слабым телефонам, но картинка менее плавная", listOf("disabled", "1", "2", "3"), listOf("Выкл", "1", "2", "3"), "disabled"),
    CoreOpt("ppsspp_auto_frameskip", "Авто-пропуск кадров", "Пропускать кадры только когда игра не успевает", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled"),
    CoreOpt("ppsspp_frame_duplication", "Плавные 60 кадров", "Дублировать кадры игр с 30 кадрами до 60 Гц", listOf("enabled", "disabled"), listOf("Вкл", "Выкл"), "enabled"),
    CoreOpt("ppsspp_inflight_frames", "Буфер команд", "Больше — выше скорость, но чуть больше задержка", listOf("No buffer", "Up to 1", "Up to 2"), listOf("Нет", "1", "2"), "Up to 1"),
)
private val SYSTEM = listOf(
    CoreOpt("ppsspp_language", "Язык игры", "Если игра поддерживает несколько языков", listOf("Automatic", "Russian", "English", "Japanese"), listOf("Авто", "Русский", "English", "日本語"), "Automatic"),
    CoreOpt("ppsspp_button_preference", "Кнопка «OK» в меню игры", "В японских играх подтверждение — правой кнопкой", listOf("Cross", "Circle"), listOf("Нижняя", "Правая"), "Cross"),
    CoreOpt("ppsspp_psp_model", "Модель PSP", "Новая модель — больше памяти, быстрее загрузки", listOf("psp_1000", "psp_2000_3000"), listOf("PSP-1000", "PSP-2000/3000"), "psp_2000_3000"),
    CoreOpt("ppsspp_memstick_inserted", "Карта памяти", "Без неё игра не сможет сохраняться", listOf("enabled", "disabled"), listOf("Вставлена", "Нет"), "enabled"),
    CoreOpt("ppsspp_cheats", "Читы", "Коды для игр (файл cheat.db)", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled"),
)
private val EXPERT = listOf(
    CoreOpt("ppsspp_backend", "Графика", "Vulkan обычно быстрее, OpenGL — совместимее", listOf("auto", "vulkan", "opengl"), listOf("Авто", "Vulkan", "OpenGL"), "auto", restart = true),
    CoreOpt("ppsspp_cpu_core", "Процессор эмулятора", "Интерпретатор — только для поиска ошибок, очень медленно", listOf("JIT", "IR JIT", "Interpreter"), listOf("JIT", "IR JIT", "Интерпретатор"), "JIT"),
    CoreOpt("ppsspp_locked_cpu_speed", "Частота процессора PSP", "Разгон убирает тормоза в некоторых играх", listOf("disabled", "333MHz", "444MHz"), listOf("Как в игре", "333 МГц", "444 МГц"), "disabled"),
    CoreOpt("ppsspp_skip_buffer_effects", "Пропускать эффекты буфера", "Быстрее, но могут пропасть тени и размытие", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled"),
    CoreOpt("ppsspp_software_rendering", "Программная отрисовка", "Максимальная точность, очень медленно", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled", restart = true),
)

private fun vars(): JSONObject = try { JSONObject(S.coreVars) } catch (e: Exception) { JSONObject() }

private fun setVar(k: String, v: String) {
    val o = vars(); o.put(k, v); S.coreVars = o.toString(); S.corePreset = "custom"
}

private fun applyPreset(p: String) {
    val o = vars(); PRESETS[p]?.forEach { (k, v) -> o.put(k, v) }
    S.coreVars = o.toString(); S.corePreset = p
}

/** Пресет по мощности телефона: ОЗУ 8 ГБ+ — «Качество», 4 ГБ+ — «Баланс», иначе — «Батарея». */
private fun recommended(ctx: Context): String {
    val mi = ActivityManager.MemoryInfo(); (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
    val gb = mi.totalMem / (1024.0 * 1024 * 1024)
    return when { gb >= 7.5 -> "quality"; gb >= 3.5 -> "balance"; else -> "battery" }
}

@Composable
private fun CoreRows(list: List<CoreOpt>) {
    val o = vars()
    list.forEach { c ->
        val cur = o.optString(c.key, c.def)
        Opt(c.title, c.desc, badge = if (c.restart) ({ Pill("после перезапуска игры", color = K.warn, bg = K.warn.copy(alpha = .14f)) }) else null) {
            Seg(c.labels, c.values.indexOf(cur).coerceAtLeast(0), small = true) { setVar(c.key, c.values[it]) }
        }
    }
}

/** Блок «Качество» с пресетами — общий для экрана ядра и настроек игры. */
@Composable
fun CoreQuality() {
    val ctx = LocalContext.current
    val rec = remember { recommended(ctx) }
    androidx.compose.runtime.LaunchedEffect(Unit) { if (S.corePreset.isEmpty()) applyPreset(rec) }
    val names = mapOf("quality" to "Качество", "balance" to "Баланс", "battery" to "Батарея")
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(K.acc.copy(alpha = .1f)).border(1.dp, K.acc.copy(alpha = .3f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SolarDuo(R.drawable.sd_chip, 20.dp)
        Text("Подобрано под твой телефон: ${names[rec]}", style = T.label.copy(fontSize = 13.sp, color = K.ink2))
    }
    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Max).padding(top = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple("quality", "1440×816 · текстуры ×2", "для мощных телефонов" to 4),
            Triple("balance", "960×544 · чётко", "и плавно" to 3),
            Triple("battery", "480×272 · как на PSP", "дольше играть" to 1),
            Triple("custom", "включится само, если", "поменяешь вручную" to 0),
        ).forEach { (id, l1, rest) ->
            val on = S.corePreset == id
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(if (on) K.acc.copy(alpha = .1f) else K.s1)
                    .border(1.5.dp, if (on) K.acc else K.line, RoundedCornerShape(14.dp))
                    .then(if (id != "custom") Modifier.press { applyPreset(id) } else Modifier).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(names[id] ?: "Своё", style = T.label.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (id == "custom" && !on) K.ink3 else K.ink))
                if (id == rec) Text("РЕКОМЕНДУЕМ", style = T.caps.copy(fontSize = 9.5.sp, color = K.bg), maxLines = 1, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(K.acc).padding(horizontal = 6.dp, vertical = 1.dp))
                Text(l1 + "\n" + rest.first, style = T.small.copy(fontSize = 11.sp))
                Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(4) { i -> Box(Modifier.size(14.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(if (i < rest.second) (if (on) K.acc else K.ink2) else K.s3)) }
                }
            }
        }
    }
    Sub("Изображение")
    CoreRows(QUALITY)
}

@Composable
private fun CoreSettings() {
    var sec by rememberSaveable { mutableStateOf(0) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 6.dp)) {
        Seg(listOf("Качество", "Скорость", "Система", "Для опытных"), sec) { sec = it }
        Text("для всех игр PSP", style = T.small)
    }
    when (sec) {
        0 -> CoreQuality()
        1 -> CoreRows(SPEED)
        2 -> CoreRows(SYSTEM)
        else -> {
            StateCard(null, null, "Эти параметры могут сломать игру. Не помогло — верни как было.", tint = K.warn) {
                KButton("Сбросить раздел", small = true) { val o = vars(); EXPERT.forEach { o.remove(it.key) }; S.coreVars = o.toString(); AppState.say("Раздел «Для опытных» сброшен") }
            }
            CoreRows(EXPERT)
        }
    }
    Text("Изменения применяются при следующем запуске игры.", style = T.small, modifier = Modifier.padding(top = 10.dp))
}
