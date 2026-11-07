package com.nexus.psp.emulator

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewAssetLoader
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.LibretroDroid
import com.swordfish.libretrodroid.ShaderConfig
import com.swordfish.libretrodroid.Variable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Экран игры: ядро PPSSPP (libretro) через LibretroDroid + прозрачный WebView поверх
 * с кнопками KAPSULA Touch и меню паузы из того же веб-интерфейса.
 * Работает в отдельном процессе ":game" — ядро гарантированно выгружается при выходе.
 */
class GameActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KapsulaGame"
        const val EXTRA_BOOT = "boot"
        const val CORE_PSP = "libppsspp_libretro_android.so"

        fun start(context: Context, bootJson: String) {
            context.startActivity(Intent(context, GameActivity::class.java).putExtra(EXTRA_BOOT, bootJson))
        }
    }

    private lateinit var root: FrameLayout
    private lateinit var web: WebView
    private var retro: GLRetroView? = null
    private var boot = JSONObject()
    private var paused = false
    private var readySent = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        // у WebView в отдельном процессе должна быть своя папка данных (Android 9+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try { WebView.setDataDirectorySuffix("game") } catch (e: IllegalStateException) { Log.w(TAG, e) }
        }
        super.onCreate(savedInstanceState)
        boot = try { JSONObject(intent.getStringExtra(EXTRA_BOOT) ?: "{}") } catch (e: Exception) { JSONObject() }

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

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        web = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            isFocusable = false
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                    assetLoader.shouldInterceptRequest(request.url)
            }
            addJavascriptInterface(Bridge(), "KapsulaNative")
        }
        root.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        web.loadUrl("https://appassets.androidplatform.net/assets/web/index.html?app=1&game=1")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { js("window.kapsulaBack&&kapsulaBack()") }
        })

        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { CoreFiles.ensureSystem(this@GameActivity) }
            if (!ok) { sendEvent("error", "Не удалось подготовить системные файлы PPSSPP"); return@launch }
            startCore()
        }
    }

    private fun startCore() {
        val path = boot.optString("path")
        if (path.isEmpty() || !File(path).exists()) { sendEvent("error", "Файл игры не найден"); return }
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
        lifecycleScope.launch {
            view.getGLRetroEvents().collect {
                if (it is GLRetroView.GLRetroEvents.FrameRendered && !readySent) { readySent = true; sendEvent("ready", "") }
            }
        }
        lifecycleScope.launch {
            view.getGLRetroErrors().collect { code ->
                val msg = when (code) {
                    GLRetroView.ERROR_LOAD_LIBRARY -> "Не удалось загрузить ядро PPSSPP"
                    GLRetroView.ERROR_LOAD_GAME -> "Ядро не смогло открыть этот файл"
                    GLRetroView.ERROR_GL_NOT_COMPATIBLE -> "Графика телефона не поддерживается ядром"
                    else -> "Ошибка ядра ($code)"
                }
                sendEvent("error", msg)
            }
        }
    }

    /** Пресет «Баланс» по умолчанию; экран настроек ядра появится следующим этапом. */
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

    private fun sendEvent(type: String, msg: String) {
        val o = JSONObject().put("type", type).put("msg", msg)
        js("window.kapsulaGame&&kapsulaGame($o)")
    }

    private fun js(code: String) = runOnUiThread { if (::web.isInitialized) web.evaluateJavascript(code, null) }

    private fun setPaused(p: Boolean) = runOnUiThread { setPausedNow(p) }

    private fun stateFile(slot: Int): File {
        val name = File(boot.optString("path")).nameWithoutExtension
        return File(CoreFiles.statesDir(this), "$name.state$slot")
    }

    /** Вызовы из веб-интерфейса (приходят не в главном потоке). */
    inner class Bridge {
        @JavascriptInterface fun boot(): String = boot.toString()
        @JavascriptInterface fun key(code: Int, down: Boolean) {
            retro?.sendKeyEvent(if (down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, code)
        }
        @JavascriptInterface fun stick(right: Boolean, x: Float, y: Float) {
            retro?.sendMotionEvent(if (right) GLRetroView.MOTION_SOURCE_ANALOG_RIGHT else GLRetroView.MOTION_SOURCE_ANALOG_LEFT, x, y)
        }
        @JavascriptInterface fun pause() = setPaused(true)
        @JavascriptInterface fun resume() = setPaused(false)
        @JavascriptInterface fun setSpeed(n: Int) = runOnUiThread { retro?.frameSpeed = n.coerceIn(1, 4) }
        @JavascriptInterface fun saveState(slot: Int): Boolean = withRunning {
            val bytes = retro?.serializeState() ?: return@withRunning false
            stateFile(slot).writeBytes(bytes); bytes.isNotEmpty()
        }
        @JavascriptInterface fun loadState(slot: Int): Boolean = withRunning {
            val f = stateFile(slot)
            f.exists() && (retro?.unserializeState(f.readBytes()) ?: false)
        }
        @JavascriptInterface fun hasState(slot: Int): Boolean = stateFile(slot).exists()
        @JavascriptInterface fun vibrate(ms: Int) { /* вибрация — через navigator.vibrate в WebView */ }
        @JavascriptInterface fun exitGame() = runOnUiThread { finish() }
    }

    /** Сохранение/загрузка выполняются в потоке эмуляции, поэтому на время операции снимаем паузу. */
    private fun withRunning(block: () -> Boolean): Boolean {
        val wasPaused = paused
        if (wasPaused) runOnUiThreadBlocking { setPausedNow(false) }
        val r = try { block() } catch (e: Exception) { Log.e(TAG, "state", e); false }
        if (wasPaused) runOnUiThreadBlocking { setPausedNow(true) }
        return r
    }

    private fun setPausedNow(p: Boolean) {
        val v = retro ?: return
        if (p == paused) return
        paused = p
        if (p) { v.onPause(); LibretroDroid.pause() } else { LibretroDroid.resume(); v.onResume() }
    }

    private fun runOnUiThreadBlocking(block: () -> Unit) {
        val latch = java.util.concurrent.CountDownLatch(1)
        runOnUiThread { try { block() } finally { latch.countDown() } }
        latch.await()
    }

    // физический геймпад: кнопки и стики идут прямо в ядро
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val v = retro
        val fromPad = event.source and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK) != 0
        if (v != null && fromPad && event.keyCode != KeyEvent.KEYCODE_BACK && event.keyCode != KeyEvent.KEYCODE_BUTTON_MODE) {
            if (event.repeatCount == 0) v.sendKeyEvent(event.action, event.keyCode)
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_MODE && event.action == KeyEvent.ACTION_UP) { js("window.kapsulaBack&&kapsulaBack()"); return true }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val v = retro
        if (v != null && event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK && event.action == MotionEvent.ACTION_MOVE) {
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
