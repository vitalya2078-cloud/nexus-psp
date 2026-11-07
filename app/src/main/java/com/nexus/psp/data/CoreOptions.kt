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
        "quality" to mapOf("ppsspp_internal_resolution" to "1440x816", "ppsspp_texture_scaling_level" to "2x", "ppsspp_texture_scaling_type" to "xbrz", "ppsspp_texture_filtering" to "Auto max quality", "ppsspp_texture_anisotropic_filtering" to "16x", "ppsspp_auto_frameskip" to "disabled"),
        "balance" to mapOf("ppsspp_internal_resolution" to "960x544", "ppsspp_texture_scaling_level" to "disabled", "ppsspp_texture_filtering" to "Auto", "ppsspp_texture_anisotropic_filtering" to "8x", "ppsspp_auto_frameskip" to "disabled"),
        "battery" to mapOf("ppsspp_internal_resolution" to "480x272", "ppsspp_texture_scaling_level" to "disabled", "ppsspp_texture_filtering" to "Auto", "ppsspp_texture_anisotropic_filtering" to "disabled", "ppsspp_auto_frameskip" to "enabled"),
    )
    val PRESET_NAMES = mapOf("quality" to "Качество", "balance" to "Баланс", "battery" to "Батарея", "custom" to "Своё")

    val QUALITY = listOf(
        CoreOpt("ppsspp_internal_resolution", "Разрешение", "Во сколько раз чётче, чем на PSP (480×272)", listOf("480x272", "960x544", "1440x816", "1920x1088"), listOf("×1", "×2", "×3", "×4"), "960x544"),
        CoreOpt("ppsspp_texture_scaling_level", "Улучшение текстур", "Сглаживает пиксели на текстурах. Заметно греет телефон", listOf("disabled", "2x", "3x", "4x"), listOf("Выкл", "×2", "×3", "×4"), "disabled"),
        CoreOpt("ppsspp_texture_filtering", "Фильтрация", "Как сглаживаются текстуры при масштабировании", listOf("Auto", "Nearest", "Linear", "Auto max quality"), listOf("Авто", "Чётко", "Гладко", "Макс"), "Auto"),
        CoreOpt("ppsspp_cropto16x9", "Обрезать до 16:9", "Убрать по строке сверху и снизу ради точного 16:9", listOf("enabled", "disabled"), listOf("Да", "Нет"), "enabled"),
    )
    val SPEED = listOf(
        CoreOpt("ppsspp_frameskip", "Пропуск кадров", "Помогает слабым телефонам, но картинка менее плавная", listOf("disabled", "1", "2", "3"), listOf("Выкл", "1", "2", "3"), "disabled"),
        CoreOpt("ppsspp_auto_frameskip", "Авто-пропуск кадров", "Пропускать кадры только когда игра не успевает", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled"),
        CoreOpt("ppsspp_frame_duplication", "Плавные 60 кадров", "Дублировать кадры игр с 30 кадрами до 60 Гц", listOf("enabled", "disabled"), listOf("Вкл", "Выкл"), "enabled"),
        CoreOpt("ppsspp_inflight_frames", "Буфер команд", "Больше — выше скорость, но чуть больше задержка", listOf("No buffer", "Up to 1", "Up to 2"), listOf("Нет", "1", "2"), "Up to 1"),
    )
    val SYSTEM = listOf(
        CoreOpt("ppsspp_language", "Язык игры", "Если игра поддерживает несколько языков", listOf("Automatic", "Russian", "English", "Japanese"), listOf("Авто", "Русский", "English", "日本語"), "Automatic", restart = true),
        CoreOpt("ppsspp_button_preference", "Кнопка «OK» в меню игры", "В японских играх подтверждение — правой кнопкой", listOf("Cross", "Circle"), listOf("Нижняя", "Правая"), "Cross"),
        CoreOpt("ppsspp_psp_model", "Модель PSP", "Новая модель — больше памяти, быстрее загрузки", listOf("psp_1000", "psp_2000_3000"), listOf("PSP-1000", "PSP-2000/3000"), "psp_2000_3000", restart = true),
        CoreOpt("ppsspp_memstick_inserted", "Карта памяти", "Без неё игра не сможет сохраняться", listOf("enabled", "disabled"), listOf("Вставлена", "Нет"), "enabled"),
        CoreOpt("ppsspp_cheats", "Читы", "Коды для игр (файл cheat.db)", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled", restart = true),
    )
    val EXPERT = listOf(
        CoreOpt("ppsspp_backend", "Графика", "Vulkan обычно быстрее, OpenGL — совместимее", listOf("auto", "vulkan", "opengl"), listOf("Авто", "Vulkan", "OpenGL"), "auto", restart = true),
        CoreOpt("ppsspp_cpu_core", "Процессор эмулятора", "Интерпретатор — только для поиска ошибок, очень медленно", listOf("JIT", "IR JIT", "Interpreter"), listOf("JIT", "IR JIT", "Интерпретатор"), "JIT", restart = true),
        CoreOpt("ppsspp_locked_cpu_speed", "Частота процессора PSP", "Разгон убирает тормоза в некоторых играх", listOf("disabled", "333MHz", "444MHz"), listOf("Как в игре", "333 МГц", "444 МГц"), "disabled", restart = true),
        CoreOpt("ppsspp_skip_buffer_effects", "Пропускать эффекты буфера", "Быстрее, но могут пропасть тени и размытие", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled"),
        CoreOpt("ppsspp_software_rendering", "Программная отрисовка", "Максимальная точность, очень медленно", listOf("disabled", "enabled"), listOf("Выкл", "Вкл"), "disabled", restart = true),
    )
    val ALL = QUALITY + SPEED + SYSTEM + EXPERT
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
    fun clear() { f.delete() }

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
