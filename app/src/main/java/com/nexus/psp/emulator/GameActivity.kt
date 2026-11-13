package com.nexus.psp.emulator

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.nexus.psp.ui.kit.KapsulaTheme
import com.nexus.psp.data.CoreOptions
import com.nexus.psp.data.CoreStore
import com.nexus.psp.data.GameConfig
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.LibretroDroid
import com.swordfish.libretrodroid.ShaderConfig
import com.swordfish.libretrodroid.Variable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Экран игры — полностью нативный: ядро PPSSPP (LibretroDroid), слой кнопок ControlsView (Canvas),
 * заставка/меню паузы/ошибки на Compose. Отдельный процесс ":game": ядро выгружается при выходе.
 */
class GameActivity : ComponentActivity(), ControlsView.Listener {

    companion object {
        private const val TAG = "KapsulaGame"
        const val EXTRA_BOOT = "boot"
        const val CORE_PSP = "libppsspp_libretro_android.so"

        fun start(context: Context, bootJson: String) {
            context.startActivity(Intent(context, GameActivity::class.java).putExtra(EXTRA_BOOT, bootJson))
        }
    }

    /** Состояние экрана для Compose. */
    class UiState {
        var phase by mutableStateOf("loading") // loading | ready | error
        var paused by mutableStateOf(false)
        var error by mutableStateOf("")
        var toast by mutableStateOf("")
        var hasState by mutableStateOf(false)
        var fast by mutableStateOf(false)
        /** Слоты: 0 — быстрый, 1–3 — свои. */
        var slots by mutableStateOf<List<Slot>>(emptyList())
        /** Открыта панель настроек поверх игры. */
        var settings by mutableStateOf(false)
        /** Редактор кнопок поверх игры. */
        var editing by mutableStateOf(false)
        /** Изменены настройки, которые ядро применяет только при запуске. */
        var restartNeeded by mutableStateOf(false)
        /** Куда сохранять изменения из паузы: true — только эта игра, false — все игры. */
        var scopeGame by mutableStateOf(true)
        var showFps by mutableStateOf(false)
        var fps by mutableStateOf(0)
        /** Можно отменить последнюю загрузку сохранения. */
        var canUndo by mutableStateOf(false)
        var ctlMode by mutableStateOf("touch")
        var op by mutableStateOf(55)
        var sc by mutableStateOf(100)
        var mirror by mutableStateOf(false)
        var volume by mutableStateOf(50)
        /** Увеличивается при каждом изменении настроек ядра — для перерисовки панели. */
        var tick by mutableStateOf(0)
        /** Редактор: кнопка, для которой выбирают значок ("раскладка:кнопка"), её значок по умолчанию и турбо. */
        var picking by mutableStateOf<String?>(null)
        var pickingDefault by mutableStateOf("")
        var pickingTurbo by mutableStateOf(false)
        /** Своя кнопка в выборе значка: что она нажимает ("" — обычная кнопка). */
        var pickingExtra by mutableStateOf("")
        /** Панель «Новая кнопка». */
        var adding by mutableStateOf(false)
        var pickingIsExtra by mutableStateOf(false)
        /** Раскладки игры и текущая; встроенный пресет (имя), панели «Пресеты» и выбора жанра. */
        var layouts by mutableStateOf<List<LaySpec>>(emptyList())
        var layId by mutableStateOf("touch")
        var builtin by mutableStateOf<String?>(null)
        var presets by mutableStateOf(false)
        var genreAsk by mutableStateOf(false)
        // фильтр картинки, уровень ускорения, руль наклоном
        var shader by mutableStateOf("default")
        var fastLevel by mutableStateOf(2)
        var tilt by mutableStateOf(false)
        var tiltSens by mutableStateOf(100)
        // экраны «Читы», «HD-текстуры», «Игра по сети»
        var cheatsOpen by mutableStateOf(false)
        var texOpen by mutableStateOf(false)
        var netOpen by mutableStateOf(false)
        var cheats by mutableStateOf<List<Cheat>>(emptyList())
        var cheatsOn by mutableStateOf<Set<String>>(emptySet())
        /** idle | noserial | nodb | load | dl:<0..100> | ready | fail */
        var cheatState by mutableStateOf("idle")
        var texState by mutableStateOf("")
        var netMode by mutableStateOf("off")
        var netIp by mutableStateOf("")
    }

    data class Slot(val i: Int, val exists: Boolean, val time: Long, val thumb: File?)

    internal val ui = UiState()
    private lateinit var root: FrameLayout
    private lateinit var controls: ControlsView
    private var retro: GLRetroView? = null
    private var boot = JSONObject()
    private var corePaused = false
    private var hideWithPad = true
    private var toastJob: kotlinx.coroutines.Job? = null

    val title: String get() = boot.optString("name", "Игра")
    val platform: String get() = boot.optString("plat", "psp")
    val gameId: String get() = boot.optString("id")
    /** Фон игры для экрана загрузки (картинка из образа или титульный экран). */
    val bgFile: File? get() = boot.optString("bg").takeIf { it.isNotEmpty() }?.let { File(it) }?.takeIf { it.exists() }
    private var layName = ""
    private var tilt = false
    private var startedAt = 0L
    private var savesMade = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.nexus.psp.data.CrashLog.install(applicationContext, "game")
        boot = try { JSONObject(intent.getStringExtra(EXTRA_BOOT) ?: "{}") } catch (e: Exception) { JSONObject() }
        val s = boot.optJSONObject("s") ?: JSONObject()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()
        // системный скриншот не опрашивает окно игры для «длинного скриншота»
        if (Build.VERSION.SDK_INT >= 31) window.decorView.scrollCaptureHint = View.SCROLL_CAPTURE_HINT_EXCLUDE_DESCENDANTS

        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(root)
        val full = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // меню паузы, заставка, ошибки, подсказки
        root.addView(ComposeView(this).apply {
            setContent { KapsulaTheme { GameOverlay(this@GameActivity, ui) } }
        }, full)

        // слой кнопок — сверху; на паузе и при загрузке скрыт
        controls = ControlsView(this, this).apply {
            mirror = s.optBoolean("mirror", false)
            scaleK = (s.optDouble("sc", 100.0) / 100.0).toFloat()
            alpha = (s.optDouble("op", 55.0) / 100.0).toFloat().coerceIn(.15f, 1f)
            vibrate = s.optBoolean("vib", true)
            visibility = View.GONE
        }
        ui.ctlMode = s.optString("ctlMode", "touch"); ui.op = s.optInt("op", 55); ui.sc = s.optInt("sc", 100); ui.mirror = s.optBoolean("mirror", false)
        ui.showFps = s.optBoolean("fps", false)
        presetName = boot.optString("preset", "")
        ui.shader = s.optString("shader", "default"); ui.fastLevel = s.optInt("fastLevel", 2).coerceIn(2, 4); ui.tiltSens = s.optInt("tiltSens", 100)
        gx = s.optJSONObject("gx") ?: JSONObject()
        ui.cheatsOn = gx.optJSONObject("cheatsOn")?.let { o -> o.keys().asSequence().filter { o.optBoolean(it) }.toSet() } ?: emptySet()
        gx.optJSONObject("net")?.let { ui.netMode = it.optString("mode", "off"); ui.netIp = it.optString("ip") }
        ui.scopeGame = boot.optBoolean("own", false)
        ui.volume = audioVolume()
        boot.optJSONObject("vars")?.let { o -> o.keys().forEach { k -> vars.put(k, o.get(k)) } }
        controls.onTap = { key, b ->
            ui.picking = key; ui.pickingDefault = b.icon ?: ""; ui.pickingTurbo = key in controls.turbo
            ui.pickingExtra = b.keys?.joinToString(if (b.combo) " → " else " + ") { partName(it) } ?: ""
            ui.pickingIsExtra = extrasFor(controls.layName).any { it.id == b.id }
            ui.tick++
            updateControlsVisibility()
        }
        controls.onMoved = { key, x, y -> saveCtl { o -> o.put("keyPos", (o.optJSONObject("keyPos") ?: JSONObject()).put(key, org.json.JSONArray(listOf(x.toDouble(), y.toDouble())))) } }
        loadCtl(s)
        applyLayout(if (s.optString("ctlMode") == "classic") "classic" else lays.first().id)
        hideWithPad = s.optBoolean("hide", true)
        val mode = s.optString("ctlMode", "touch")
        root.addView(controls, full)

        // вырез камеры: кнопки отступают от него
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, ins ->
            val c = ins.getInsets(WindowInsetsCompat.Type.displayCutout())
            controls.setInsets(c.left.toFloat(), c.right.toFloat())
            ins
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = backPressed()
        })

        getSystemService(InputManager::class.java)?.registerInputDeviceListener(object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(id: Int) = updateControlsVisibility()
            override fun onInputDeviceRemoved(id: Int) = updateControlsVisibility()
            override fun onInputDeviceChanged(id: Int) = updateControlsVisibility()
        }, null)

        startedAt = System.currentTimeMillis()
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { CoreFiles.ensureSystem(this@GameActivity) }
            if (!ok) { fail("Не удалось подготовить системные файлы эмулятора"); return@launch }
            ui.hasState = withContext(Dispatchers.IO) { stateFile(0).exists() }
            refreshSlots()
            startCore()
        }
    }

    private fun startCore() {
        val path = boot.optString("path")
        if (path.isEmpty() || !File(path).exists()) { fail("Файл игры не найден: возможно, его удалили"); return }
        val data = GLRetroViewData(this).apply {
            coreFilePath = CORE_PSP
            gameFilePath = path
            systemDirectory = CoreFiles.systemDir(this@GameActivity).absolutePath
            savesDirectory = CoreFiles.savesDir(this@GameActivity).absolutePath
            variables = coreVariables()
            shader = shaderOf(boot.optJSONObject("s")?.optString("shader", "default") ?: "default")
            preferLowLatencyAudio = true
            rumbleEventsEnabled = false
        }
        val view = GLRetroView(this, data)
        retro = view
        root.addView(view, 0, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        lifecycle.addObserver(view)
        // при возврате в приложение LibretroDroid сам снимает ядро с паузы — если открыто меню паузы, возвращаем паузу
        lifecycle.addObserver(LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) root.post { if (corePaused) { view.onPause(); LibretroDroid.pause() } }
        })
        lifecycleScope.launch {
            view.getGLRetroEvents().collect {
                if (it is GLRetroView.GLRetroEvents.FrameRendered) {
                    frames++
                    if (ui.phase == "loading") {
                        ui.phase = "ready"; updateControlsVisibility()
                        ui.builtin?.let { toast("Кнопки настроены под «$it» · изменить: пауза → «Кнопки»") }
                        maybeAskGenre()
                        // включённые читы этой игры — сразу после запуска
                        if (ui.cheatsOn.isNotEmpty()) lifecycleScope.launch {
                            val l = withContext(Dispatchers.IO) { (serial?.takeIf { CheatDb.ready(this@GameActivity) }?.let { CheatDb.forGame(this@GameActivity, it) } ?: emptyList()) + ownCheats() }
                            ui.cheats = l; delay(1500); pushCheats()
                        }
                        // «Продолжить с этого места» или возврат после незаметного перезапуска
                        val slot = boot.optInt("loadSlot", -1)
                        if (slot >= 0) { boot.remove("loadSlot"); lifecycleScope.launch { delay(300); loadSlot(slot, backup = slot == 4, quiet = slot == 8 || slot == 4); if (slot == 4) toast("Продолжаем с места выхода · начать сначала — «Отменить загрузку» в паузе") } }
                    }
                }
            }
        }
        // счётчик кадров
        lifecycleScope.launch { while (true) { delay(1000); ui.fps = frames; frames = 0 } }
        lifecycleScope.launch {
            view.getGLRetroErrors().collect { code ->
                fail(when (code) {
                    GLRetroView.ERROR_LOAD_LIBRARY -> "Не удалось загрузить ядро эмулятора"
                    GLRetroView.ERROR_LOAD_GAME -> "Ядро не смогло открыть этот файл"
                    GLRetroView.ERROR_GL_NOT_COMPATIBLE -> "Графика телефона не поддерживается ядром"
                    else -> "Ошибка ядра ($code)"
                })
            }
        }
    }

    /** Пресет «Баланс» по умолчанию; экран настроек ядра — следующий этап. */
    private fun coreVariables(): Array<Variable> {
        val v = mutableMapOf(
            "ppsspp_internal_resolution" to "960x544",
            "ppsspp_button_preference" to "Cross",
            "ppsspp_language" to "Automatic",
            "ppsspp_frame_duplication" to "enabled",
        )
        boot.optJSONObject("vars")?.let { o -> o.keys().forEach { k -> if (k.startsWith("ppsspp_")) v[k] = o.optString(k) } }
        // читы включаются на лету (retro_cheat_set) — движок читов должен работать с запуска
        if (!v.containsKey("ppsspp_cheats")) v["ppsspp_cheats"] = "enabled"
        // HD-текстуры этой игры
        if (gx.optBoolean("tex") && serial != null) v["ppsspp_texture_replacement"] = "enabled"
        // игра по сети
        gx.optJSONObject("net")?.let { n -> v.putAll(NetPlay.vars(n.optString("mode"), n.optString("ip"), NetPlay.mac(this))) }
        return v.map { Variable(it.key, it.value) }.toTypedArray()
    }

    // ---------- раскладки игры: несколько раскладок, роли кнопок, пресеты ----------
    /** Раскладки этой игры (до 4). Положения, значки и назначения хранятся по ключу «раскладка:консоль:кнопка». */
    private var lays = mutableListOf(LaySpec("touch", "Основная", "touch"))
    /** Настройки кнопок свои у игры (после выбора жанра, пресета или правки ролей) — хранятся в её файле, а не в общих. */
    private var ctlOwn = false
    private var holdFrom: String? = null

    private fun specOf(x: String) = lays.firstOrNull { it.id == x } ?: lays.firstOrNull { it.base == x }

    /** Раскладка по id; classic — «Классика» консоли. */
    private fun applyLayout(lay: String) {
        holdFrom = null
        if (lay == "classic") {
            layName = "classic"; controls.layName = "classic:$platform"; controls.style = "classic"; controls.platform = platform
            controls.setLayout(ControlLayout.layout("classic", platform) + extrasFor(controls.layName)); ui.layId = "classic"; return
        }
        val sp = specOf(lay) ?: lays.first()
        layName = sp.id
        controls.layName = "${sp.id}:$platform"
        controls.style = "touch"; controls.platform = platform
        controls.setLayout(buildLayout(sp))
        ui.layId = sp.id; ui.layouts = lays.toList()
    }

    private fun buildLayout(sp: LaySpec): List<Btn> {
        val base = Templates.build(sp.base, platform)
        val swap = if (lays.size > 1 && base.none { it.act == "swap" }) listOf(Templates.swapBtn()) else emptyList()
        return base + swap + extrasFor("${sp.id}:$platform")
    }

    private fun switchLayout(to: String) {
        val sp = specOf(to) ?: return
        if (sp.id == layName) return
        applyLayout(sp.id)
        toast("Раскладка: ${sp.name}")
        controls.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
    }

    /** «Пока держишь»: другая раскладка, нажатая кнопка остаётся под пальцем. */
    private fun holdLayout(to: String, btnId: String) {
        val sp = specOf(to) ?: return
        if (sp.id == layName) return
        val held = controls.current().firstOrNull { it.id == btnId }
        holdFrom = layName
        layName = sp.id; controls.layName = "${sp.id}:$platform"
        val list = buildLayout(sp).let { l -> if (held != null && l.none { it.id == btnId }) l + held else l }
        controls.setLayoutKeep(list, btnId); ui.layId = sp.id
    }

    private fun unhold() { val f = holdFrom ?: return; holdFrom = null; applyLayout(f) }

    /** Загрузить настройки кнопок: свои у игры (ctl) → встроенный пресет → общие. */
    private fun loadCtl(s: JSONObject) {
        val own = s.optJSONObject("ctl")
        val src = when {
            own != null -> { ctlOwn = true; own }
            else -> GamePresets.find(title, boot.optString("title2"))?.let { (name, o) -> ui.builtin = name; o } ?: s
        }
        readCtl(src)
    }

    private fun readCtl(o: JSONObject) {
        controls.offsets.clear(); controls.customIcons.clear(); controls.turbo.clear()
        controls.keyMap.clear(); controls.sizes.clear(); controls.roles.clear(); controls.hidden.clear()
        o.optJSONObject("keyPos")?.let { m -> m.keys().forEach { k -> val a = m.getJSONArray(k); controls.offsets[k] = android.graphics.PointF(a.getDouble(0).toFloat(), a.getDouble(1).toFloat()) } }
        o.optJSONObject("keyIcon")?.let { m -> m.keys().forEach { k -> controls.customIcons[k] = m.getString(k) } }
        o.optJSONObject("keyTurbo")?.let { m -> m.keys().forEach { k -> if (m.optBoolean(k)) controls.turbo.add(k) } }
        o.optJSONObject("keyMap")?.let { m -> m.keys().forEach { k -> controls.keyMap[k] = m.getString(k) } }
        o.optJSONObject("keySize")?.let { m -> m.keys().forEach { k -> controls.sizes[k] = m.getDouble(k).toFloat() } }
        o.optJSONObject("keyRole")?.let { m -> m.keys().forEach { k -> controls.roles[k] = m.getString(k) } }
        o.optJSONObject("keyHide")?.let { m -> m.keys().forEach { k -> if (m.optBoolean(k)) controls.hidden.add(k) } }
        extras.keys().asSequence().toList().forEach { extras.remove(it) }
        o.optJSONObject("extraBtns")?.let { m -> m.keys().forEach { k -> extras.put(k, m.get(k)) } }
        val a = o.optJSONArray("layouts")
        lays = if (a != null && a.length() > 0) (0 until a.length()).map { i -> a.getJSONObject(i).let { LaySpec(it.getString("id"), it.optString("name", "Раскладка"), it.optString("base", "touch")) } }.toMutableList()
            else mutableListOf(LaySpec("touch", "Основная", "touch"))
        ui.layouts = lays.toList()
    }

    /** Все настройки кнопок одним JSON — для сохранения в игре и в «Моих пресетах». */
    fun ctlSnapshot(): JSONObject {
        val pos = JSONObject().also { j -> controls.offsets.forEach { (k, p) -> j.put(k, org.json.JSONArray(listOf(p.x.toDouble(), p.y.toDouble()))) } }
        return JSONObject()
            .put("layouts", org.json.JSONArray().also { a -> lays.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("base", it.base)) } })
            .put("keyPos", pos).put("keyIcon", iconsJson()).put("keyTurbo", turboJson())
            .put("keyMap", JSONObject(controls.keyMap as Map<*, *>)).put("keySize", JSONObject().also { j -> controls.sizes.forEach { (k, v) -> j.put(k, v.toDouble()) } })
            .put("keyRole", JSONObject(controls.roles as Map<*, *>)).put("keyHide", JSONObject().also { j -> controls.hidden.forEach { j.put(it, true) } })
            .put("extraBtns", JSONObject(extras.toString()))
    }

    /** Сохранить кнопки в файл игры (свои у игры, независимо от «для всех игр»). */
    private fun persistCtl() {
        ctlOwn = true
        val snap = ctlSnapshot()
        lifecycleScope.launch(Dispatchers.IO) { GameConfig(this@GameActivity, gameId).edit { it.put("ctl", snap) } }
    }

    /** Старые правки (места, значки, турбо, свои кнопки): в файл игры, если у неё свои кнопки, иначе — как раньше. */
    private fun saveCtl(legacy: (JSONObject) -> Unit) { if (ctlOwn) persistCtl() else persist(legacy) }

    /** Применить настройки кнопок целиком (жанр, свой или встроенный пресет). */
    fun applyCtl(o: JSONObject, msg: String? = null) {
        readCtl(o); applyLayout(lays.first().id); persistCtl(); ui.tick++
        msg?.let { toast(it) }
    }

    fun openPresets() { ui.presets = true; ui.tick++; updateControlsVisibility() }
    fun closePresets() { ui.presets = false; updateControlsVisibility() }

    /** Ответ на «Какой это жанр?» при первом запуске (null — «Не сейчас»). Больше не спрашиваем. */
    fun answerGenre(id: String?) {
        ui.genreAsk = false
        lifecycleScope.launch(Dispatchers.IO) { GameConfig(this@GameActivity, gameId).edit { it.put("genreAsked", true) } }
        if (id != null) applyGenre(id)
        setPaused(false)
    }

    /** Первый запуск игры без своих кнопок и без встроенного пресета — спросить жанр (один раз). */
    private fun maybeAskGenre() {
        if (ui.ctlMode != "touch" || ctlOwn || ui.builtin != null) return
        lifecycleScope.launch {
            val asked = withContext(Dispatchers.IO) { GameConfig(this@GameActivity, gameId).read().optBoolean("genreAsked") }
            if (asked) return@launch
            delay(1200)
            if (ui.phase != "ready" || ui.paused) return@launch
            setPaused(true); ui.genreAsk = true; updateControlsVisibility()
        }
    }

    fun applyGenre(id: String) {
        val g = Templates.genre(id) ?: return
        applyCtl(JSONObject().put("layouts", Templates.layoutsJson(g)), "Кнопки: ${g.name}")
    }

    // ---------- редактор: раскладки и свойства кнопки ----------
    fun selectLayout(id: String) { applyLayout(id); ui.tick++ }

    fun addLayout(base: String, name: String) {
        if (lays.size >= 4) { toast("Не больше 4 раскладок"); return }
        var id = base; var n = 2
        while (lays.any { it.id == id }) id = base + n++
        lays.add(LaySpec(id, name, base)); persistCtl(); applyLayout(id); ui.tick++
    }

    fun removeLayout(id: String) {
        if (lays.size <= 1) return
        lays.removeAll { it.id == id }
        val pre = "$id:"
        listOf(controls.customIcons.keys, controls.keyMap.keys, controls.sizes.keys, controls.roles.keys, controls.offsets.keys).forEach { ks -> ks.removeAll { it.startsWith(pre) } }
        controls.turbo.removeAll { it.startsWith(pre) }; controls.hidden.removeAll { it.startsWith(pre) }
        extras.keys().asSequence().toList().filter { it.startsWith(pre) }.forEach { extras.remove(it) }
        persistCtl(); applyLayout(lays.first().id); ui.tick++
    }

    fun renameLayout(id: String, name: String) {
        val i = lays.indexOfFirst { it.id == id }; if (i < 0 || name.isBlank()) return
        lays[i] = lays[i].copy(name = name.trim()); ui.layouts = lays.toList(); persistCtl(); ui.tick++
    }

    fun btnKey(key: String): String? = controls.current().firstOrNull { controls.keyOf(it) == key }?.let { controls.keyName(it) }
    fun btnSize(key: String): Float = controls.sizes[key] ?: 1f
    fun btnRole(key: String): String? = controls.current().firstOrNull { controls.keyOf(it) == key }?.let { controls.roleOf(it) }
    fun btnHidden(key: String) = key in controls.hidden

    fun setBtnKey(key: String, name: String) { controls.keyMap[key] = name; controls.invalidate(); persistCtl(); ui.tick++ }
    fun setBtnSize(key: String, f: Float) { if (f == 1f) controls.sizes.remove(key) else controls.sizes[key] = f; controls.invalidate(); persistCtl(); ui.tick++ }
    /** role: null — обычная, "switch:<id>" — нажал и переключил, "hold:<id>" — пока держишь. */
    fun setBtnRole(key: String, role: String?) { controls.roles[key] = role ?: "none"; persistCtl(); ui.tick++ }
    fun setBtnHidden(key: String, on: Boolean) { if (on) controls.hidden.add(key) else controls.hidden.remove(key); controls.invalidate(); persistCtl(); ui.tick++ }

    // ---------- мои пресеты (общие для всех игр, файл presets.json) ----------
    private fun presetsFile() = File(filesDir, "presets.json")
    fun myPresets(): List<Pair<String, JSONObject>> = try {
        val a = org.json.JSONArray(presetsFile().readText()); (0 until a.length()).map { a.getJSONObject(it).let { o -> o.getString("name") to o.getJSONObject("ctl") } }
    } catch (e: Exception) { emptyList() }
    private fun writePresets(l: List<Pair<String, JSONObject>>) {
        presetsFile().writeText(org.json.JSONArray().also { a -> l.forEach { (n, c) -> a.put(JSONObject().put("name", n).put("ctl", c)) } }.toString())
        ui.tick++
    }
    fun savePreset(name: String) { val n = name.trim().ifEmpty { title }; writePresets(myPresets().filter { it.first != n } + (n to ctlSnapshot())); toast("Пресет «$n» сохранён") }
    fun deletePreset(name: String) = writePresets(myPresets().filter { it.first != name })
    fun renamePreset(old: String, new: String) { if (new.isBlank()) return; writePresets(myPresets().map { if (it.first == old) new.trim() to it.second else it }) }
    fun applyMyPreset(name: String) { myPresets().firstOrNull { it.first == name }?.let { applyCtl(JSONObject(it.second.toString()), "Пресет «$name» применён") } }
    fun applyBuiltin() { GamePresets.find(title, boot.optString("title2"))?.let { (n, o) -> applyCtl(JSONObject(o.toString()), "Пресет «$n» применён") } }
    fun hasBuiltin() = GamePresets.find(title, boot.optString("title2")) != null

    private fun fail(msg: String) {
        ui.error = msg; ui.phase = "error"; ui.paused = false
        updateControlsVisibility()
    }

    // ---------- пауза ----------
    private fun setCorePaused(p: Boolean) {
        val v = retro ?: return
        if (p == corePaused) return
        corePaused = p
        if (p) { v.onPause(); LibretroDroid.pause() } else { LibretroDroid.resume(); v.onResume() }
    }

    fun setPaused(p: Boolean) {
        if (ui.phase != "ready") return
        if (p && !ui.paused) {
            // кадр в момент паузы — для слотов сохранений и скриншота; затем ставим ядро на паузу
            ui.paused = true; controls.releaseAll(); updateControlsVisibility()
            lifecycleScope.launch { pauseFrame = captureFrame(); setCorePaused(true); refreshSlots() }
            return
        }
        if (!p) { pauseFrame = null; ui.settings = false }
        ui.paused = p
        controls.releaseAll()
        setCorePaused(p)
        updateControlsVisibility()
    }

    private fun backPressed() {
        when {
            ui.phase == "error" -> finish()
            ui.picking != null -> closePicker()
            ui.genreAsk -> answerGenre(null)
            ui.cheatsOpen -> closeCheats()
            ui.texOpen -> closeTextures()
            ui.netOpen -> closeNet()
            ui.presets -> closePresets()
            ui.editing -> stopEditing()
            ui.settings -> ui.settings = false
            ui.phase == "ready" -> setPaused(!ui.paused)
        }
    }

    private fun padConnected(): Boolean = InputDevice.getDeviceIds().any { id ->
        val d = InputDevice.getDevice(id) ?: return@any false
        !d.isVirtual && (d.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD || d.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
    }

    private fun updateControlsVisibility() {
        if (ui.picking != null || ui.adding || ui.presets || ui.genreAsk || ui.cheatsOpen || ui.texOpen || ui.netOpen) { controls.visibility = View.GONE; return }
        if (ui.editing) { controls.visibility = View.VISIBLE; return }
        val show = ui.phase == "ready" && !ui.paused && ui.ctlMode != "pad" && !(hideWithPad && padConnected())
        controls.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) controls.releaseAll()
    }

    fun toast(msg: String) {
        ui.toast = msg
        toastJob?.cancel()
        toastJob = lifecycleScope.launch { delay(2200); ui.toast = "" }
    }

    // ---------- сохранения состояния (быстрый слот) ----------
    private fun stateFile(slot: Int): File {
        val name = File(boot.optString("path")).nameWithoutExtension
        return File(CoreFiles.statesDir(this), "$name.state$slot")
    }

    /** Сериализация идёт в потоке эмуляции, поэтому на время операции ядро снимается с паузы. */
    private suspend fun <T> running(block: () -> T): T {
        val was = corePaused
        if (was) setCorePaused(false)
        try { return withContext(Dispatchers.IO) { block() } } finally { if (was) setCorePaused(true) }
    }

    fun saveQuick() = saveSlot(0)
    fun loadQuick() = loadSlot(0)

    private fun thumbFile(slot: Int) = File(stateFile(slot).path + ".jpg")

    fun refreshSlots() = lifecycleScope.launch {
        ui.slots = withContext(Dispatchers.IO) { (0..3).map { i -> val f = stateFile(i); Slot(i, f.exists(), f.lastModified(), thumbFile(i).takeIf { it.exists() }) } }
        ui.hasState = ui.slots.firstOrNull()?.exists == true
    }

    /** Сохранить в слот: снимок состояния ядра + кадр игры (кадр снят в момент паузы). */
    fun saveSlot(slot: Int, quiet: Boolean = false) = lifecycleScope.launch {
        val frame = pauseFrame ?: captureFrame()
        val ok = try { running { retro?.serializeState()?.also { stateFile(slot).writeBytes(it) }?.isNotEmpty() ?: false } } catch (e: Exception) { Log.e(TAG, "save", e); false }
        if (ok && frame != null) withContext(Dispatchers.IO) { saveJpeg(frame, thumbFile(slot), 320) }
        if (ok && slot in 0..3) savesMade++
        refreshSlots()
        if (!quiet) toast(if (!ok) "Не удалось сохранить" else if (slot == 0) "Сохранено в быстрый слот" else "Сохранено в слот $slot")
    }

    /** Загрузить слот; перед этим — резервный снимок «до загрузки» (слот 9), чтобы можно было отменить. */
    fun loadSlot(slot: Int, backup: Boolean = true, quiet: Boolean = false) = lifecycleScope.launch {
        val f = stateFile(slot)
        if (!f.exists()) { toast("В этом слоте пусто"); return@launch }
        if (backup) {
            val b = try { running { retro?.serializeState() } } catch (e: Exception) { null }
            if (b != null && b.isNotEmpty()) { withContext(Dispatchers.IO) { stateFile(9).writeBytes(b) }; ui.canUndo = true }
        }
        val ok = try { running { retro?.unserializeState(f.readBytes()) ?: false } } catch (e: Exception) { Log.e(TAG, "load", e); false }
        if (!quiet) toast(if (!ok) "Не удалось загрузить" else if (backup) "Загружено · можно отменить в паузе" else "Продолжаем с сохранения")
        if (ok && ui.paused) setPaused(false)
    }

    /** Отменить последнюю загрузку: вернуть состояние «до загрузки». */
    fun undoLoad() = lifecycleScope.launch {
        val f = stateFile(9); if (!f.exists()) return@launch
        val ok = try { running { retro?.unserializeState(f.readBytes()) ?: false } } catch (e: Exception) { false }
        ui.canUndo = false
        toast(if (ok) "Вернулись к моменту до загрузки" else "Не удалось отменить")
        if (ok && ui.paused) setPaused(false)
    }

    fun deleteSlot(slot: Int) = lifecycleScope.launch {
        withContext(Dispatchers.IO) { stateFile(slot).delete(); thumbFile(slot).delete() }
        refreshSlots(); toast("Слот очищен")
    }

    // ---------- кадр игры: для слотов и скриншотов ----------
    private var pauseFrame: android.graphics.Bitmap? = null
    private var frames = 0

    /** Снимок картинки ядра (SurfaceView) через PixelCopy. */
    private suspend fun captureFrame(): android.graphics.Bitmap? {
        val v = retro ?: return null
        if (v.width <= 0 || v.height <= 0) return null
        val bmp = android.graphics.Bitmap.createBitmap(v.width, v.height, android.graphics.Bitmap.Config.ARGB_8888)
        val done = kotlinx.coroutines.CompletableDeferred<Boolean>()
        try {
            android.view.PixelCopy.request(v, bmp, { r -> done.complete(r == android.view.PixelCopy.SUCCESS) }, android.os.Handler(mainLooper))
        } catch (e: Exception) { return null }
        return if (kotlinx.coroutines.withTimeoutOrNull(1500) { done.await() } == true) bmp else null
    }

    private fun saveJpeg(b: android.graphics.Bitmap, f: File, w: Int) {
        val k = w.toFloat() / b.width
        val s = if (k < 1f) android.graphics.Bitmap.createScaledBitmap(b, w, (b.height * k).toInt(), true) else b
        f.outputStream().use { s.compress(android.graphics.Bitmap.CompressFormat.JPEG, 86, it) }
    }

    /** Скриншот в галерею: Pictures/KAPSULA. */
    fun screenshot() = lifecycleScope.launch {
        // Android 8–9: в галерею можно писать только с разрешением на память
        if (Build.VERSION.SDK_INT < 29 && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 7)
            toast("Разреши доступ к фото и нажми «Скриншот» ещё раз"); return@launch
        }
        val b = pauseFrame ?: captureFrame()
        if (b == null) { toast("Не удалось снять кадр"); return@launch }
        val ok = withContext(Dispatchers.IO) {
            try {
                val name = "KAPSULA_" + java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date()) + ".png"
                val v = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= 29) put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KAPSULA")
                }
                val uri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v) ?: return@withContext false
                contentResolver.openOutputStream(uri)?.use { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } != null
            } catch (e: Exception) { Log.w(TAG, "shot", e); false }
        }
        toast(if (ok) "Скриншот сохранён в галерею (KAPSULA)" else "Не удалось сохранить скриншот")
    }

    // ---------- настройки поверх игры ----------
    /** Текущие значения настроек ядра (общие + свои для игры). */
    val vars = JSONObject()

    /** Записать изменение: в настройки этой игры или в общие (их применит главный экран). */
    private fun persist(block: (JSONObject) -> Unit) {
        if (ui.scopeGame) {
            GameConfig(this, gameId).edit { o ->
                if (!o.optBoolean("own")) { o.put("own", true); o.put("vars", JSONObject(vars.toString())) }
                block(o)
            }
        } else {
            val f = GameConfig.pendingGlobal(this)
            val o = try { JSONObject(f.readText()) } catch (e: Exception) { JSONObject() }
            block(o); f.writeText(o.toString())
        }
    }

    /** Настройка ядра: применяется сразу (ядро перечитывает настройки), отмеченные restart — после перезапуска. */
    fun setCoreVar(k: String, v: String) {
        vars.put(k, v)
        retro?.updateVariables(Variable(k, v))
        if (CoreOptions.byKey(k)?.restart == true) ui.restartNeeded = true
        persist { o -> o.put("vars", (o.optJSONObject("vars") ?: JSONObject()).put(k, v)); o.put("preset", "custom") }
        ui.tick++
    }

    fun setPreset(p: String) {
        CoreOptions.PRESETS[p]?.forEach { (k, v) -> vars.put(k, v); retro?.updateVariables(Variable(k, v)) }
        persist { o -> val vv = o.optJSONObject("vars") ?: JSONObject(); CoreOptions.PRESETS[p]?.forEach { (k, v) -> vv.put(k, v) }; o.put("vars", vv); o.put("preset", p) }
        presetName = p; ui.tick++
    }
    var presetName = ""

    /** Хранилище для экранов настроек ядра (общие компоненты с главным экраном). */
    val coreStore = object : CoreStore {
        override fun vars(): JSONObject = JSONObject(vars.toString())
        override fun set(k: String, v: String) = setCoreVar(k, v)
        override var preset: String
            get() = presetName
            set(value) { presetName = value }
        override fun applyPreset(p: String) = setPreset(p)
    }

    fun setCtlMode(m: String) {
        ui.ctlMode = m; persist { it.put("ctlMode", m) }
        applyLayout(if (m == "classic") "classic" else lays.first().id)
        updateControlsVisibility()
    }
    fun setOpacity(v: Int) { ui.op = v; controls.alpha = (v / 100f).coerceIn(.15f, 1f); persist { it.put("op", v) } }
    fun setSize(v: Int) { ui.sc = v; controls.scaleK = v / 100f; controls.invalidate(); persist { it.put("sc", v) } }
    fun setMirror(on: Boolean) { ui.mirror = on; controls.mirror = on; controls.invalidate(); persist { it.put("mirror", on) } }
    fun setFps(on: Boolean) { ui.showFps = on; persist { it.put("fps", on) } }

    private fun audioVolume(): Int {
        val am = getSystemService(android.media.AudioManager::class.java) ?: return 50
        val mx = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) * 100 / mx
    }
    fun setVolume(v: Int) {
        ui.volume = v
        val am = getSystemService(android.media.AudioManager::class.java) ?: return
        val mx = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (v * mx + 50) / 100, 0)
    }

    /**
     * Незаметный перезапуск для настроек «после перезапуска»: сохраняем состояние (служебный слот 8),
     * просим главный экран запустить игру заново с новыми настройками и сразу загружаем это место.
     */
    fun applyRestart() = lifecycleScope.launch {
        toast("Перезапускаю игру с новыми настройками…")
        val b = try { running { retro?.serializeState() } } catch (e: Exception) { null }
        if (b != null && b.isNotEmpty()) withContext(Dispatchers.IO) { stateFile(8).writeBytes(b) }
        val nb = JSONObject(boot.toString()).put("vars", JSONObject(vars.toString()))
        nb.optJSONObject("s")?.let { s ->
            s.put("ctlMode", ui.ctlMode).put("op", ui.op).put("sc", ui.sc).put("mirror", ui.mirror).put("fps", ui.showFps)
                .put("shader", ui.shader).put("fastLevel", ui.fastLevel).put("tiltSens", ui.tiltSens).put("gx", JSONObject(gx.toString()))
            if (ctlOwn) s.put("ctl", ctlSnapshot())
        }
        if (b != null) nb.put("loadSlot", 8)
        withContext(Dispatchers.IO) { File(filesDir, "relaunch.json").writeText(nb.toString()) }
        finish()
    }

    // ---------- фильтр картинки, ускорение, руль наклоном ----------
    private fun shaderOf(n: String): ShaderConfig = when (n) {
        "smooth" -> ShaderConfig.CUT(); "pixel" -> ShaderConfig.Sharp; "lcd" -> ShaderConfig.LCD; "crt" -> ShaderConfig.CRT; else -> ShaderConfig.Default
    }
    fun setShader(n: String) { ui.shader = n; retro?.shader = shaderOf(n); persist { it.put("shader", n) } }
    fun setFastLevel(n: Int) { ui.fastLevel = n; if (ui.fast) retro?.frameSpeed = n; persist { it.put("fastLevel", n) } }
    fun setTiltSens(n: Int) { ui.tiltSens = n; persist { it.put("tiltSens", n) } }

    private var tiltZero = Float.NaN
    private val tiltListener = object : android.hardware.SensorEventListener {
        override fun onSensorChanged(e: android.hardware.SensorEvent) {
            // альбомная ориентация: наклон «руля» — ось Y акселерометра (знак зависит от того, какой стороной повёрнут телефон)
            val rot = display?.rotation ?: android.view.Surface.ROTATION_90
            val raw = e.values[1] * (if (rot == android.view.Surface.ROTATION_270) -1f else 1f)
            if (tiltZero.isNaN()) tiltZero = raw
            val x = ((raw - tiltZero) / 4.5f * ui.tiltSens / 100f).coerceIn(-1f, 1f)
            if (!controls.stickBusy()) stick(false, x, 0f)
        }
        override fun onAccuracyChanged(s: android.hardware.Sensor?, a: Int) {}
    }
    /** Руль наклоном: телефон — как руль, текущее положение — «прямо». */
    fun setTilt(on: Boolean) {
        val sm = getSystemService(android.hardware.SensorManager::class.java)
        val acc = sm?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        if (on && acc == null) { toast("В телефоне нет датчика наклона"); return }
        tilt = on; ui.tilt = on; controls.setToggle("tilt", on)
        if (on) { tiltZero = Float.NaN; sm?.registerListener(tiltListener, acc, android.hardware.SensorManager.SENSOR_DELAY_GAME) }
        else { sm?.unregisterListener(tiltListener); stick(false, 0f, 0f) }
        toast(if (on) "Руль — наклоном телефона: держи ровно, это «прямо»" else "Руль — стиком")
    }

    // ---------- допы игры: читы, HD-текстуры, игра по сети (хранятся в файле игры, «gx») ----------
    private var gx = JSONObject()
    val serial: String? get() = boot.optString("serial").ifEmpty { null }
    private fun saveGx() {
        boot.optJSONObject("s")?.put("gx", JSONObject(gx.toString()))
        val snap = JSONObject(gx.toString())
        lifecycleScope.launch(Dispatchers.IO) { GameConfig(this@GameActivity, gameId).edit { it.put("gx", snap) } }
    }

    fun openCheats() { ui.cheatsOpen = true; updateControlsVisibility(); loadCheats() }
    fun closeCheats() { ui.cheatsOpen = false; updateControlsVisibility() }
    private fun ownCheats(): List<Cheat> = gx.optJSONArray("cheatsOwn")?.let { a -> (0 until a.length()).map { a.getJSONObject(it).let { o -> Cheat(o.getString("name"), o.getString("code"), true) } } } ?: emptyList()

    fun loadCheats() {
        val sr = serial
        lifecycleScope.launch {
            if (sr == null) { ui.cheats = ownCheats(); ui.cheatState = "noserial"; return@launch }
            if (!CheatDb.ready(this@GameActivity)) { ui.cheats = ownCheats(); ui.cheatState = "nodb"; return@launch }
            ui.cheatState = "load"
            val l = withContext(Dispatchers.IO) { CheatDb.forGame(this@GameActivity, sr) }
            ui.cheats = l + ownCheats(); ui.cheatState = "ready"
        }
    }

    fun downloadCheats() = lifecycleScope.launch {
        ui.cheatState = "dl:0"
        val ok = withContext(Dispatchers.IO) { CheatDb.download(this@GameActivity) { p -> runOnUiThread { ui.cheatState = "dl:" + (p * 100).toInt() } } }
        if (ok) loadCheats() else ui.cheatState = "fail"
    }

    /** Передать ядру все читы с их состоянием (ядро включает их сразу). */
    private fun pushCheats() {
        val v = retro ?: return
        ui.cheats.forEachIndexed { i, c -> try { v.setCheat(i, c.name in ui.cheatsOn, c.code.replace(' ', '+')) } catch (e: Exception) { Log.w(TAG, "cheat", e) } }
    }

    fun toggleCheat(c: Cheat, on: Boolean) {
        ui.cheatsOn = if (on) ui.cheatsOn + c.name else ui.cheatsOn - c.name
        gx.put("cheatsOn", JSONObject().also { j -> ui.cheatsOn.forEach { j.put(it, true) } }); saveGx()
        pushCheats()
        toast(if (on) "Чит включён: ${c.name}" else "Чит выключен")
    }

    fun addOwnCheat(name: String, code: String) {
        val lines = code.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { if (it.startsWith("_L")) it else "_L $it" }
        if (name.isBlank() || lines.isEmpty()) { toast("Нужны название и строки кода"); return }
        val a = gx.optJSONArray("cheatsOwn") ?: org.json.JSONArray().also { gx.put("cheatsOwn", it) }
        a.put(JSONObject().put("name", name.trim()).put("code", lines.joinToString("+")))
        saveGx(); loadCheats(); toast("Код добавлен")
    }

    fun removeOwnCheat(name: String) {
        val a = gx.optJSONArray("cheatsOwn") ?: return
        val keep = org.json.JSONArray(); for (i in 0 until a.length()) a.getJSONObject(i).takeIf { it.optString("name") != name }?.let { keep.put(it) }
        gx.put("cheatsOwn", keep); ui.cheatsOn = ui.cheatsOn - name; saveGx(); loadCheats()
    }

    // HD-текстуры
    private val pickTex = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) importTextures(uri) }
    fun openTextures() { ui.texOpen = true; updateControlsVisibility() }
    fun closeTextures() { ui.texOpen = false; updateControlsVisibility() }
    fun pickTextures() = pickTex.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
    fun texInstalled() = serial?.let { Textures.installed(this, it) } == true
    fun texSize() = serial?.let { Textures.sizeMb(this, it) } ?: 0
    private fun importTextures(uri: android.net.Uri) {
        val sr = serial ?: return
        lifecycleScope.launch {
            ui.texState = "Распаковываю…"
            val err = withContext(Dispatchers.IO) { try { Textures.import(this@GameActivity, uri, sr) { n -> runOnUiThread { ui.texState = "Распаковано файлов: $n" } } } catch (e: Exception) { "Не удалось распаковать: ${e.message}" } }
            if (err != null) { ui.texState = err; return@launch }
            gx.put("tex", true); saveGx()
            ui.texState = "Готово · применится после перезапуска игры"; ui.restartNeeded = true; ui.tick++
        }
    }
    fun setTexturesOn(on: Boolean) { gx.put("tex", on); saveGx(); ui.restartNeeded = true; ui.tick++ }
    fun texOn() = gx.optBoolean("tex")
    fun removeTextures() { serial?.let { Textures.remove(this, it) }; gx.put("tex", false); saveGx(); ui.texState = "Набор удалён"; ui.restartNeeded = true; ui.tick++ }

    // игра по сети
    fun openNet() { ui.netOpen = true; updateControlsVisibility() }
    fun closeNet() { ui.netOpen = false; updateControlsVisibility() }
    fun myIp() = NetPlay.myIp(this)
    /** mode: host / join / off. Применяется перезапуском игры (сеть поднимается при старте). */
    fun setNet(mode: String, ip: String = "") {
        if (mode == "join" && !NetPlay.validIp(ip)) { toast("Введи адрес вида 192.168.1.5"); return }
        ui.netMode = mode; ui.netIp = ip
        gx.put("net", JSONObject().put("mode", mode).put("ip", ip)); saveGx()
        applyRestart()
    }

    // ---------- редактор кнопок поверх игры ----------
    fun startEditing() { ui.settings = false; ui.editing = true; controls.releaseAll(); controls.editMode = true; updateControlsVisibility(); toast(if (ui.ctlMode == "classic") "Перетаскивай кнопки пальцем" else "Перетаскивай кнопки · нажми на кнопку — выбрать значок") }
    /** Свой значок кнопки Touch (null — как было). */
    fun setIcon(key: String, name: String?) {
        if (name == null) controls.customIcons.remove(key) else controls.customIcons[key] = name
        controls.invalidate()
        saveCtl { o -> o.put("keyIcon", iconsJson()) }
    }

    fun currentIcon(key: String): String = controls.customIcons[key] ?: ui.pickingDefault

    // полный набор, а не изменение: так общие настройки и настройки игры не расходятся
    private fun iconsJson() = JSONObject().also { j -> controls.customIcons.forEach { (k, v) -> j.put(k, v) } }
    private fun turboJson() = JSONObject().also { j -> controls.turbo.forEach { j.put(it, true) } }

    // ---------- свои кнопки: «две разом» и комбо ----------
    /** "раскладка:платформа" → [{id, keys, combo}]. */
    private val extras = JSONObject()

    private fun extrasFor(lay: String): List<Btn> {
        val a = extras.optJSONArray(lay) ?: return emptyList()
        return (0 until a.length()).mapNotNull { i ->
            val o = a.optJSONObject(i) ?: return@mapNotNull null
            val keys = o.optJSONArray("keys")?.let { k -> (0 until k.length()).map { k.getString(it) } } ?: return@mapNotNull null
            if (keys.size == 1) Btn(o.getString("id"), o.optString("icon").ifEmpty { "tap" }, 54f, "r230 b150", ControlLayout.AMBER, key = keys[0])
            else ControlLayout.extra(o.getString("id"), keys, o.optBoolean("combo"), null)
        }
    }

    fun partName(k: String) = when (k) {
        "ft" -> "Верх"; "fr" -> "Право"; "fb" -> "Низ"; "fl" -> "Лево"
        "up" -> "↑"; "down" -> "↓"; "left" -> "←"; "right" -> "→"
        "select" -> "Select"; "start" -> "Start"; else -> k.uppercase()
    }

    fun startAdding() { ui.adding = true; updateControlsVisibility() }
    fun closeAdding() { ui.adding = false; updateControlsVisibility() }

    fun addExtra(keys: List<String>, combo: Boolean, icon: String? = null) {
        val lay = controls.layName
        val a = extras.optJSONArray(lay) ?: org.json.JSONArray().also { extras.put(lay, it) }
        val id = "x" + (System.currentTimeMillis() % 1_000_000)
        a.put(JSONObject().put("id", id).put("keys", org.json.JSONArray(keys)).put("combo", combo).put("icon", icon ?: ""))
        saveCtl { o -> o.put("extraBtns", JSONObject(extras.toString())) }
        applyLayout(layName)
        closeAdding()
        toast("Кнопка добавлена — перетащи её на удобное место")
    }

    fun removeExtra(key: String) {
        val lay = controls.layName; val id = key.removePrefix("$lay:")
        extras.optJSONArray(lay)?.let { a -> val keep = org.json.JSONArray(); for (i in 0 until a.length()) a.optJSONObject(i)?.takeIf { it.optString("id") != id }?.let { keep.put(it) }; extras.put(lay, keep) }
        controls.customIcons.remove(key); controls.turbo.remove(key); controls.offsets.remove(key)
        saveCtl { o ->
            o.put("extraBtns", JSONObject(extras.toString())); o.put("keyIcon", iconsJson()); o.put("keyTurbo", turboJson())
            o.optJSONObject("keyPos")?.remove(key)
        }
        applyLayout(layName)
        closePicker(); toast("Кнопка удалена")
    }

    fun closePicker() { ui.picking = null; updateControlsVisibility() }

    /** Турбо для кнопки: пока палец на ней, она быстро нажимается сама. */
    fun setTurbo(key: String, on: Boolean) {
        if (on) controls.turbo.add(key) else controls.turbo.remove(key)
        ui.pickingTurbo = on; controls.invalidate()
        saveCtl { o -> o.put("keyTurbo", turboJson()) }
    }

    /** Вернуть кнопки текущей раскладки на места по умолчанию. */
    fun resetButtons() {
        val pre = controls.layName + ":"
        val gone = controls.offsets.keys.filter { it.startsWith(pre) }
        gone.forEach { controls.offsets.remove(it) }
        controls.invalidate()
        val icons = controls.customIcons.keys.filter { it.startsWith(pre) }; icons.forEach { controls.customIcons.remove(it) }
        val tb = controls.turbo.filter { it.startsWith(pre) }; controls.turbo.removeAll(tb.toSet())
        listOf(controls.keyMap.keys, controls.sizes.keys, controls.roles.keys).forEach { ks -> ks.removeAll { it.startsWith(pre) } }
        controls.hidden.removeAll { it.startsWith(pre) }
        if (ctlOwn) persistCtl() else persist { o ->
            val kp = o.optJSONObject("keyPos") ?: JSONObject(); gone.forEach { kp.remove(it) }; o.put("keyPos", kp)
            o.put("keyIcon", iconsJson()); o.put("keyTurbo", turboJson())
        }
        toast("Кнопки этой раскладки — как по умолчанию")
    }
    fun stopEditing() { ui.editing = false; controls.editMode = false; controls.invalidate(); updateControlsVisibility(); toast("Положение кнопок сохранено") }

    fun toggleFast() {
        ui.fast = !ui.fast
        retro?.frameSpeed = if (ui.fast) ui.fastLevel else 1
        controls.setToggle("ff", ui.fast)
        toast(if (ui.fast) "Ускорение включено: ×${ui.fastLevel}" else "Ускорение выключено")
    }

    /** Выход: место в игре сохраняется само (слот 4), «Играть» вернёт сюда же. */
    fun exitGame() = lifecycleScope.launch {
        if (resume) { toast("Сохраняю место…"); saveAuto(capture = true) }
        finish()
    }

    private val resume get() = boot.optJSONObject("s")?.optBoolean("resume", true) ?: true

    private suspend fun saveAuto(capture: Boolean) {
        if (ui.phase != "ready" || retro == null) return
        val frame = pauseFrame ?: if (capture) captureFrame() else null
        val b = try { running { retro?.serializeState() } } catch (e: Exception) { Log.w(TAG, "auto", e); null }
        if (b == null || b.isEmpty()) return
        withContext(Dispatchers.IO) {
            stateFile(4).writeBytes(b)
            if (frame != null) saveJpeg(frame, thumbFile(4), 320) else thumbFile(4).delete()
        }
    }

    /** Домой / к другим приложениям: игра ещё работает — успеваем сохранить место (кадр — только если стоит пауза). */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!resume || ui.phase != "ready") return
        try { kotlinx.coroutines.runBlocking { kotlinx.coroutines.withTimeoutOrNull(2500) { saveAuto(capture = false) } } } catch (e: Exception) { Log.w(TAG, "leave", e) }
    }

    // ---------- ControlsView.Listener ----------
    override fun key(code: Int, down: Boolean) {
        retro?.sendKeyEvent(if (down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, code)
    }

    override fun stick(right: Boolean, x: Float, y: Float) {
        retro?.sendMotionEvent(if (right) GLRetroView.MOTION_SOURCE_ANALOG_RIGHT else GLRetroView.MOTION_SOURCE_ANALOG_LEFT, x, y)
    }

    override fun action(act: String) {
        when {
            act == "pause" -> setPaused(true)
            act == "save" -> saveQuick()
            act == "load" -> loadQuick()
            act == "ff" -> toggleFast()
            act == "tilt" -> setTilt(!tilt)
            act == "swap" -> if (lays.size > 1) switchLayout(lays[(lays.indexOfFirst { it.id == layName } + 1).mod(lays.size)].id)
            act.startsWith("switch:") -> lifecycleScope.launch { delay(800); switchLayout(act.removePrefix("switch:")) }
            act.startsWith("hold:") -> act.removePrefix("hold:").split(":").let { p -> if (p.size >= 2) holdLayout(p[0], p[1]) }
            act == "unhold" -> unhold()
            // умный триггер «сесть/выйти»: кнопка уходит в игру, раскладка меняется через полсекунды
            act.startsWith("ctx:") -> lifecycleScope.launch { delay(500); switchLayout(act.removePrefix("ctx:")) }
        }
    }

    // ---------- физический геймпад: прямо в ядро ----------
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val v = retro
        val fromPad = event.source and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK) != 0
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_MODE) { if (event.action == KeyEvent.ACTION_UP) backPressed(); return true }
        if (v != null && fromPad && !ui.paused && ui.phase == "ready" && event.keyCode != KeyEvent.KEYCODE_BACK) {
            if (event.repeatCount == 0) v.sendKeyEvent(event.action, event.keyCode)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val v = retro
        if (v != null && !ui.paused && event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK && event.action == MotionEvent.ACTION_MOVE) {
            v.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT, event.getAxisValue(MotionEvent.AXIS_X), event.getAxisValue(MotionEvent.AXIS_Y))
            v.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, event.getAxisValue(MotionEvent.AXIS_Z), event.getAxisValue(MotionEvent.AXIS_RZ))
            v.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, event.getAxisValue(MotionEvent.AXIS_HAT_X), event.getAxisValue(MotionEvent.AXIS_HAT_Y))
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // потеря фокуса — это ещё и шторка уведомлений и системный скриншот: пауза ставится только при настоящем уходе (onPause)
        if (hasFocus) hideSystemBars()
    }

    override fun onPause() {
        // свернули игру — открываем меню паузы
        if (ui.phase == "ready" && !ui.paused && !isFinishing) try { setPaused(true) } catch (e: Exception) { Log.w(TAG, "pause", e) }
        super.onPause()
    }

    /** Итог сессии для главного процесса: минуты в игре и новые сохранения. */
    private fun writeSession() {
        if (gameId.isEmpty() || startedAt == 0L) return
        val min = ((System.currentTimeMillis() - startedAt) / 60000).toInt()
        try {
            File(filesDir, "last_session.json").writeText(JSONObject().put("id", gameId).put("minutes", min).put("saves", savesMade).toString())
        } catch (e: Exception) { Log.w(TAG, "session", e) }
    }

    override fun finish() { writeSession(); super.finish() }

    override fun onDestroy() {
        if (tilt) getSystemService(android.hardware.SensorManager::class.java)?.unregisterListener(tiltListener)
        super.onDestroy()
        // ядро libretro не рассчитано на повторную загрузку в том же процессе
        if (isFinishing) Process.killProcess(Process.myPid())
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
