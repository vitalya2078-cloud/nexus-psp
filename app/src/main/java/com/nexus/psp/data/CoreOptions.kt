package com.nexus.psp.data

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Настройки ядра PPSSPP: ключ libretro, подписи, значения. restart = true — ядро применяет только при запуске игры
 * (сверено с libretro/libretro.cpp PPSSPP, check_variables: остальные меняются на лету через GET_VARIABLE_UPDATE).
 */
data class CoreOpt(val key: String, val title: String, val desc: String, val values: List<String>, val labels: List<String>, val def: String, val restart: Boolean = false)

object CoreOptions {
    val PRESETS = mapOf(
        "quality" to mapOf("ppsspp_internal_resolution" to "1440x816", "ppsspp_texture_scaling_level" to "2x", "ppsspp_texture_scaling_type" to "xbrz", "ppsspp_texture_filtering" to "Auto max quality", "ppsspp_texture_anisotropic_filtering" to "16x", "ppsspp_auto_frameskip" to "disabled", "ppsspp_skip_gpu_readbacks" to "disabled"),
        "balance" to mapOf("ppsspp_internal_resolution" to "960x544", "ppsspp_texture_scaling_level" to "disabled", "ppsspp_texture_filtering" to "Auto", "ppsspp_texture_anisotropic_filtering" to "8x", "ppsspp_auto_frameskip" to "disabled", "ppsspp_skip_gpu_readbacks" to "disabled"),
        "battery" to mapOf("ppsspp_internal_resolution" to "480x272", "ppsspp_texture_scaling_level" to "disabled", "ppsspp_texture_filtering" to "Auto", "ppsspp_texture_anisotropic_filtering" to "disabled", "ppsspp_auto_frameskip" to "enabled", "ppsspp_skip_gpu_readbacks" to "enabled"),
    )
    val PRESET_NAMES = mapOf("quality" to "Качество", "balance" to "Баланс", "battery" to "Батарея", "custom" to "Своё")

    val QUALITY = listOf(
        CoreOpt("ppsspp_internal_resolution", "Разрешение", "Во сколько раз чётче, чем на PSP (480×272)", listOf("480x272", "960x544", "1440x816", "1920x1088"), listOf("×1", "×2", "×3", "×4"), "960x544"),
        CoreOpt("ppsspp_texture_scaling_level", "Улучшение текстур", "Сглаживает пиксели на текстурах. Заметно греет телефон", listOf("disabled", "2x", "3x", "4x"), listOf("Выкл", "×2", "×3", "×4"), "disabled"),
        CoreOpt("ppsspp_texture_filtering", "Фильтрация", "Как сглаживаются текстуры при масштабировании", listOf("Auto", "Nearest", "Linear", "Auto max quality"), listOf("Авто", "Чётко", "Гладко", "Макс"), "Auto"),
        CoreOpt("ppsspp_cropto16x9", "Обрезать до 16:9", "Убрать по строке сверху и снизу ради точного 16:9", listOf("enabled", "disabled"), listOf("Да", "Нет"), "enabled"),
        CoreOpt("ppsspp_lower_resolution_for_effects", "Мягкие эффекты", "Свечение и размытие считаются в меньшем разрешении: меньше артефактов на высоком разрешении", listOf("disabled", "Safe", "Balanced", "Aggressive"), listOf("Выкл", "Мягко", "Средне", "Сильно"), "disabled"),
        CoreOpt("ppsspp_depth_raster", "Блики за объектами", "Солнечные блики не просвечивают сквозь стены", listOf("default", "low", "off", "always_on"), listOf("Авто", "Быстро", "Выкл", "Всегда"), "default"),
        CoreOpt("ppsspp_smart_2d_texture_filtering", "Чёткая 2D-графика", "Убирает размытие в 2D-играх (платформеры, файтинги)", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled"),
        CoreOpt("ppsspp_texture_deposterize", "Без полос на текстурах", "Сглаживает ступеньки цвета при улучшении текстур", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled"),
    )
    /** Графика: Vulkan — пробный режим (при сбое игра сама вернётся на OpenGL). */
    val RENDER = listOf(
        CoreOpt("ppsspp_backend", "Графика", "Vulkan обычно быстрее и плавнее. Пробный режим: при сбое игра сама вернётся на OpenGL", listOf("opengl", "vulkan"), listOf("OpenGL", "Vulkan"), "opengl", restart = true),
    )
    val SPEED = listOf(
        CoreOpt("ppsspp_frameskip", "Пропуск кадров", "Помогает слабым телефонам, но картинка менее плавная", listOf("disabled", "1", "2", "3"), listOf("Выкл", "1", "2", "3"), "disabled"),
        CoreOpt("ppsspp_auto_frameskip", "Авто-пропуск кадров", "Пропускать кадры только когда игра не успевает", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled"),
        CoreOpt("ppsspp_frame_duplication", "Плавные 60 кадров", "Дублировать кадры игр с 30 кадрами до 60 Гц", listOf("enabled", "disabled"), listOf("Вкл", "Выкл"), "enabled"),
        CoreOpt("ppsspp_inflight_frames", "Буфер команд", "Больше — выше скорость, но чуть больше задержка", listOf("No buffer", "Up to 1", "Up to 2"), listOf("Нет", "1", "2"), "Up to 1"),
        CoreOpt("ppsspp_skip_gpu_readbacks", "Ускорение для слабых", "Заметно быстрее на слабых телефонах; в редких играх ломает эффекты", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled"),
        CoreOpt("ppsspp_cache_iso", "Игра в памяти", "Образ целиком в оперативной памяти: без подгрузок. Нужно 2+ ГБ свободной памяти", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled", restart = true),
        CoreOpt("ppsspp_io_timing_method", "Загрузки", "«Как на UMD» чинит редкие игры, которым мешают слишком быстрые загрузки", listOf("Fast", "Simulate UMD delays"), listOf("Быстро", "Как на UMD"), "Fast", restart = true),
    )
    val SYSTEM = listOf(
        CoreOpt("ppsspp_language", "Язык игры", "Если игра поддерживает несколько языков", listOf("Automatic", "Russian", "English", "Japanese"), listOf("Авто", "Русский", "English", "日本語"), "Automatic", restart = true),
        CoreOpt("ppsspp_button_preference", "Кнопка «OK» в меню игры", "В японских играх подтверждение — правой кнопкой", listOf("Cross", "Circle"), listOf("Нижняя", "Правая"), "Cross"),
        CoreOpt("ppsspp_psp_model", "Модель PSP", "Новая модель — больше памяти, быстрее загрузки", listOf("psp_1000", "psp_2000_3000"), listOf("PSP-1000", "PSP-2000/3000"), "psp_2000_3000", restart = true),
        CoreOpt("ppsspp_memstick_inserted", "Карта памяти", "Без неё игра не сможет сохраняться", listOf("enabled", "disabled"), listOf("Вставлена", "Нет"), "enabled"),
        CoreOpt("ppsspp_cheats", "Читы", "Коды для игр (файл cheat.db)", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled", restart = true),
    )
    val EXPERT = listOf(
        CoreOpt("ppsspp_cpu_core", "Процессор эмулятора", "Интерпретатор — только для поиска ошибок, очень медленно", listOf("JIT", "IR JIT", "Interpreter"), listOf("JIT", "IR JIT", "Интерпретатор"), "JIT", restart = true),
        CoreOpt("ppsspp_locked_cpu_speed", "Частота процессора PSP", "Разгон убирает тормоза в некоторых играх", listOf("disabled", "333MHz", "444MHz"), listOf("Как в игре", "333 МГц", "444 МГц"), "disabled", restart = true),
        CoreOpt("ppsspp_skip_buffer_effects", "Пропускать эффекты буфера", "Быстрее, но могут пропасть тени и размытие", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled"),
        CoreOpt("ppsspp_software_rendering", "Программная отрисовка", "Максимальная точность, очень медленно", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled", restart = true),
    )
    /** Стик: мёртвая зона и чувствительность (применяются на лету). */
    val INPUT = listOf(
        CoreOpt("ppsspp_analog_deadzone", "Мёртвая зона стика", "Сколько можно сдвинуть стик, прежде чем игра заметит движение", listOf("0.0", "0.05", "0.10", "0.15", "0.25"), listOf("0", "5%", "10%", "15%", "25%"), "0.0"),
        CoreOpt("ppsspp_analog_is_circular", "Круглый ход стика", "Диагонали стика — как у настоящего стика PSP", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled"),
        CoreOpt("ppsspp_analog_sensitivity", "Чувствительность стика", "Насколько резко персонаж или машина отзываются на стик", listOf("1.00", "1.10", "1.25", "1.50"), listOf("×1", "×1,1", "×1,25", "×1,5"), "1.00"),
    )
    // ---------- PS1 (ядро PCSX-ReARMed, GPL-2.0) ----------
    /** Картинка PS1: чёткость ×2 (своя отрисовка NEON), её ускорение, сглаживание цвета (дизеринг). */
    val PS1_QUALITY = listOf(
        CoreOpt("pcsx_rearmed_neon_enhancement_enable", "Чёткость ×2", "3D-графика в двойном разрешении. На слабых телефонах может замедлить игру", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled"),
        CoreOpt("pcsx_rearmed_neon_enhancement_no_main", "Быстрая чёткость", "Ускоряет «Чёткость ×2», но в редких играх появляются ошибки картинки", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled"),
        CoreOpt("pcsx_rearmed_dithering", "Зернистость цвета", "Как на настоящей PS1: мелкая сетка вместо полос в градиентах", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "enabled"),
        CoreOpt("pcsx_rearmed_frameskip_type", "Пропуск кадров", "Помогает слабым телефонам: кадры пропускаются, только когда игра не успевает", listOf("disabled", "auto"), listOf("Выкл", "Авто"), "disabled"),
    )
    /** Система PS1: регион, BIOS, звук, контроллер. */
    val PS1_SYSTEM = listOf(
        CoreOpt("kapsula_ps1_pad", "Джойстик", "«Обычный» — стик работает как крестовина, подходит почти всем играм. DualShock — настоящие стики (Ape Escape, Spyro, гонки); аналог включается L1 + R1 + Select", listOf("standard", "dualshock"), listOf("Обычный", "DualShock"), "standard"),
        CoreOpt("pcsx_rearmed_region", "Регион", "Обычно определяется сам. PAL — 50 кадров, NTSC — 60", listOf("auto", "NTSC", "PAL"), listOf("Авто", "NTSC", "PAL"), "auto", restart = true),
        CoreOpt("pcsx_rearmed_bios", "BIOS", "«Свой файл» — лучше совместимость (добавь в «Настройки → BIOS»); «Встроенный» работает и без него", listOf("auto", "HLE"), listOf("Свой файл", "Встроенный"), "auto", restart = true),
        CoreOpt("pcsx_rearmed_spu_interpolation", "Звук", "«Как на PS1» — ближе всего к оригиналу", listOf("simple", "gaussian", "cubic", "off"), listOf("Простой", "Как на PS1", "Чистый", "Выкл"), "simple"),
        CoreOpt("pcsx_rearmed_cd_turbo", "Быстрые загрузки", "Ускоряет чтение диска. Некоторые игры из-за этого зависают — включай осторожно", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled", restart = true),
    )
    val PS1 = PS1_QUALITY + PS1_SYSTEM

    val ALL = QUALITY + RENDER + SPEED + SYSTEM + EXPERT + INPUT
    fun byKey(k: String) = ALL.firstOrNull { it.key == k }
}

/** Где лежат значения настроек ядра: общие для всех игр PSP или свои для игры. */
interface CoreStore {
    fun vars(): JSONObject
    fun set(k: String, v: String)
    var preset: String
    fun applyPreset(p: String) { CoreOptions.PRESETS[p]?.forEach { (k, v) -> set(k, v) }; preset = p }
}

/**
 * Свои настройки игры: файл meta/<id>/settings.json — его читают и главный экран, и процесс игры.
 * Поля: own (свои настройки включены), vars, preset, ctlMode, op, sc, mirror, fps, keyPos.
 */
class GameConfig(private val c: Context, val id: String) {
    private val f get() = File(c.filesDir, "meta/$id/settings.json")
    fun read(): JSONObject = try { JSONObject(f.readText()) } catch (e: Exception) { JSONObject() }
    fun write(o: JSONObject) { f.parentFile?.mkdirs(); f.writeText(o.toString()) }
    fun edit(block: (JSONObject) -> Unit) { val o = read(); block(o); write(o) }
    val own get() = read().optBoolean("own", false)
    /** Вернуть игру к общим настройкам; кнопки игры (ctl) и ответ про жанр остаются — они всегда свои у игры. */
    fun clear() {
        val o = read(); val keep = JSONObject()
        listOf("ctl", "genreAsked").forEach { k -> if (o.has(k)) keep.put(k, o.get(k)) }
        if (keep.length() == 0) f.delete() else write(keep)
    }

    /** Хранилище настроек ядра этой игры. */
    fun store(onChange: () -> Unit = {}) = object : CoreStore {
        override fun vars(): JSONObject = read().optJSONObject("vars") ?: JSONObject()
        override fun set(k: String, v: String) { edit { o -> o.put("own", true); o.put("vars", (o.optJSONObject("vars") ?: JSONObject()).put(k, v)); o.put("preset", "custom") }; onChange() }
        override var preset: String
            get() = read().optString("preset", "")
            set(value) { edit { it.put("own", true).put("preset", value) }; onChange() }
    }

    companion object {
        /** Изменения «для всех игр», сделанные в процессе игры: главный экран применит их к общим настройкам. */
        fun pendingGlobal(c: Context) = File(c.filesDir, "pending_global.json")
    }
}
