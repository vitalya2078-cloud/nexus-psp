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
    private var preset: List<String>? = null
    private var layName = ""
    private var tilt = false
    private var startedAt = 0L
    private var savesMade = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            s.optJSONObject("keyPos")?.let { o -> o.keys().forEach { k -> val a = o.getJSONArray(k); offsets[k] = android.graphics.PointF(a.getDouble(0).toFloat(), a.getDouble(1).toFloat()) } }
            s.optJSONObject("keyIcon")?.let { o -> o.keys().forEach { k -> customIcons[k] = o.getString(k) } }
            s.optJSONObject("keyTurbo")?.let { o -> o.keys().forEach { k -> if (o.optBoolean(k)) turbo.add(k) } }
        }
        ui.ctlMode = s.optString("ctlMode", "touch"); ui.op = s.optInt("op", 55); ui.sc = s.optInt("sc", 100); ui.mirror = s.optBoolean("mirror", false)
        ui.showFps = s.optBoolean("fps", false)
        presetName = boot.optString("preset", "")
        ui.scopeGame = boot.optBoolean("own", false)
        ui.volume = audioVolume()
        boot.optJSONObject("vars")?.let { o -> o.keys().forEach { k -> vars.put(k, o.get(k)) } }
        controls.onTap = { key, b -> ui.picking = key; ui.pickingDefault = b.icon ?: ""; ui.pickingTurbo = key in controls.turbo; controls.visibility = View.GONE }
        controls.onMoved = { key, x, y -> persist { o -> o.put("keyPos", (o.optJSONObject("keyPos") ?: JSONObject()).put(key, org.json.JSONArray(listOf(x.toDouble(), y.toDouble())))) } }
        val classic = s.optString("ctlMode") == "classic"
        preset = if (!classic) ControlLayout.PRESETS[gameId] else null
        applyLayout(preset?.first() ?: if (classic) "classic" else "touch")
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
            if (!ok) { fail("Не удалось подготовить системные файлы PPSSPP"); return@launch }
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
            shader = ShaderConfig.Default
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
                        // «Продолжить с этого места» или возврат после незаметного перезапуска
                        val slot = boot.optInt("loadSlot", -1)
                        if (slot >= 0) { boot.remove("loadSlot"); lifecycleScope.launch { delay(300); loadSlot(slot, backup = false, quiet = slot == 8) } }
                    }
                }
            }
        }
        // счётчик кадров
        lifecycleScope.launch { while (true) { delay(1000); ui.fps = frames; frames = 0 } }
        lifecycleScope.launch {
            view.getGLRetroErrors().collect { code ->
                fail(when (code) {
                    GLRetroView.ERROR_LOAD_LIBRARY -> "Не удалось загрузить ядро PPSSPP"
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
        return v.map { Variable(it.key, it.value) }.toTypedArray()
    }

    /** Раскладка: touch / classic / foot / drive; положения кнопок хранятся по имени «раскладка:консоль». */
    private fun applyLayout(lay: String) {
        layName = lay
        controls.layName = "$lay:$platform"
        controls.style = if (lay == "classic") "classic" else "touch"
        controls.platform = platform
        controls.setLayout(if (lay == "foot" || lay == "drive") ControlLayout.preset(lay, (preset?.size ?: 0) > 1) else ControlLayout.layout(lay, platform))
    }

    private fun switchLayout(to: String) {
        val p = preset ?: return
        if (to !in p || to == layName) return
        applyLayout(to)
        toast(if (to == "drive") "Раскладка: за рулём" else "Раскладка: пешком")
        controls.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
    }

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
        val classic = m == "classic"
        preset = if (!classic) ControlLayout.PRESETS[gameId] else null
        applyLayout(preset?.first() ?: if (classic) "classic" else "touch")
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
        nb.optJSONObject("s")?.let { s -> s.put("ctlMode", ui.ctlMode).put("op", ui.op).put("sc", ui.sc).put("mirror", ui.mirror).put("fps", ui.showFps) }
        if (b != null) nb.put("loadSlot", 8)
        withContext(Dispatchers.IO) { File(filesDir, "relaunch.json").writeText(nb.toString()) }
        finish()
    }

    // ---------- редактор кнопок поверх игры ----------
    fun startEditing() { ui.settings = false; ui.editing = true; controls.releaseAll(); controls.editMode = true; updateControlsVisibility(); toast(if (ui.ctlMode == "classic") "Перетаскивай кнопки пальцем" else "Перетаскивай кнопки · нажми на кнопку — выбрать значок") }
    /** Свой значок кнопки Touch (null — как было). */
    fun setIcon(key: String, name: String?) {
        if (name == null) controls.customIcons.remove(key) else controls.customIcons[key] = name
        controls.invalidate()
        persist { o -> o.put("keyIcon", iconsJson()) }
    }

    fun currentIcon(key: String): String = controls.customIcons[key] ?: ui.pickingDefault

    // полный набор, а не изменение: так общие настройки и настройки игры не расходятся
    private fun iconsJson() = JSONObject().also { j -> controls.customIcons.forEach { (k, v) -> j.put(k, v) } }
    private fun turboJson() = JSONObject().also { j -> controls.turbo.forEach { j.put(it, true) } }

    fun closePicker() { ui.picking = null; updateControlsVisibility() }

    /** Турбо для кнопки: пока палец на ней, она быстро нажимается сама. */
    fun setTurbo(key: String, on: Boolean) {
        if (on) controls.turbo.add(key) else controls.turbo.remove(key)
        ui.pickingTurbo = on; controls.invalidate()
        persist { o -> o.put("keyTurbo", turboJson()) }
    }

    /** Вернуть кнопки текущей раскладки на места по умолчанию. */
    fun resetButtons() {
        val pre = controls.layName + ":"
        val gone = controls.offsets.keys.filter { it.startsWith(pre) }
        gone.forEach { controls.offsets.remove(it) }
        controls.invalidate()
        val icons = controls.customIcons.keys.filter { it.startsWith(pre) }; icons.forEach { controls.customIcons.remove(it) }
        val tb = controls.turbo.filter { it.startsWith(pre) }; controls.turbo.removeAll(tb.toSet())
        persist { o ->
            val kp = o.optJSONObject("keyPos") ?: JSONObject(); gone.forEach { kp.remove(it) }; o.put("keyPos", kp)
            o.put("keyIcon", iconsJson()); o.put("keyTurbo", turboJson())
        }
        toast("Кнопки как по умолчанию: места, значки, турбо")
    }
    fun stopEditing() { ui.editing = false; controls.editMode = false; controls.invalidate(); updateControlsVisibility(); toast("Положение кнопок сохранено") }

    fun toggleFast() {
        ui.fast = !ui.fast
        retro?.frameSpeed = if (ui.fast) 2 else 1
        controls.setToggle("ff", ui.fast)
        toast(if (ui.fast) "Ускорение включено: ×2" else "Ускорение выключено")
    }

    fun exitGame() = finish()

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
            act == "tilt" -> { tilt = !tilt; controls.setToggle("tilt", tilt); toast(if (tilt) "Руль — наклоном телефона (скоро)" else "Руль — кнопками") }
            act == "swap" -> switchLayout(if (layName == "drive") "foot" else "drive")
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
        if (hasFocus) hideSystemBars()
        // свернули приложение посреди игры — открываем меню паузы
        else if (ui.phase == "ready" && !ui.paused && !isFinishing) setPaused(true)
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
