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
    }

    private val ui = UiState()
    private lateinit var root: FrameLayout
    private lateinit var controls: ControlsView
    private var retro: GLRetroView? = null
    private var boot = JSONObject()
    private var corePaused = false
    private var hideWithPad = true
    private var toastJob: kotlinx.coroutines.Job? = null

    val title: String get() = boot.optString("name", "Игра")
    val platform: String get() = boot.optString("plat", "psp")

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
            setLayout(ControlLayout.layout(if (s.optString("ctlMode") == "classic") "classic" else "touch", platform))
            visibility = View.GONE
        }
        hideWithPad = s.optBoolean("hide", true)
        val mode = s.optString("ctlMode", "touch")
        if (mode != "pad") root.addView(controls, full)

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

        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { CoreFiles.ensureSystem(this@GameActivity) }
            if (!ok) { fail("Не удалось подготовить системные файлы PPSSPP"); return@launch }
            ui.hasState = withContext(Dispatchers.IO) { stateFile(0).exists() }
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
                if (it is GLRetroView.GLRetroEvents.FrameRendered && ui.phase == "loading") {
                    ui.phase = "ready"; updateControlsVisibility()
                }
            }
        }
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
        ui.paused = p
        controls.releaseAll()
        setCorePaused(p)
        updateControlsVisibility()
    }

    private fun backPressed() {
        when (ui.phase) {
            "error" -> finish()
            "ready" -> setPaused(!ui.paused)
        }
    }

    private fun padConnected(): Boolean = InputDevice.getDeviceIds().any { id ->
        val d = InputDevice.getDevice(id) ?: return@any false
        !d.isVirtual && (d.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD || d.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
    }

    private fun updateControlsVisibility() {
        val show = ui.phase == "ready" && !ui.paused && !(hideWithPad && padConnected())
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

    fun saveQuick() = lifecycleScope.launch {
        val ok = try { running { retro?.serializeState()?.also { stateFile(0).writeBytes(it) }?.isNotEmpty() ?: false } } catch (e: Exception) { Log.e(TAG, "save", e); false }
        if (ok) ui.hasState = true
        toast(if (ok) "Сохранено в быстрый слот" else "Не удалось сохранить")
    }

    fun loadQuick() = lifecycleScope.launch {
        val f = stateFile(0)
        if (!f.exists()) { toast("Быстрых сохранений пока нет"); return@launch }
        val ok = try { running { retro?.unserializeState(f.readBytes()) ?: false } } catch (e: Exception) { Log.e(TAG, "load", e); false }
        toast(if (ok) "Сохранение загружено" else "Не удалось загрузить")
        if (ok && ui.paused) setPaused(false)
    }

    fun toggleFast() {
        ui.fast = !ui.fast
        retro?.frameSpeed = if (ui.fast) 2 else 1
        controls.setToggle("ff", ui.fast)
        toast(if (ui.fast) "Ускорение ×2" else "Обычная скорость")
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
        when (act) {
            "pause" -> setPaused(true)
            "save" -> saveQuick()
            "load" -> loadQuick()
            "ff" -> toggleFast()
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
