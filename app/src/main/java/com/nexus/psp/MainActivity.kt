package com.nexus.psp

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.os.Build
import android.view.WindowManager
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import android.webkit.JavascriptInterface
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.nexus.psp.emulator.GameActivity
import com.nexus.psp.emulator.GameLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Временная оболочка: показывает интерфейс KAPSULA из папки docs/ (упакована в assets).
 * Нужна, чтобы проверять дизайн на телефоне до переноса экранов в Jetpack Compose.
 */
class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var safe: Insets = Insets.NONE

    /** Выбор своего образа игры (ISO/CSO/CHD/PBP) через системный файловый менеджер. */
    private val pickGame = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) { fileEvent(JSONObject().put("type", "cancel")); return@registerForActivityResult }
        lifecycleScope.launch {
            val name = GameLibrary.displayName(this@MainActivity, uri).first
            fileEvent(JSONObject().put("type", "progress").put("name", name).put("p", 0))
            try {
                val f = withContext(Dispatchers.IO) {
                    GameLibrary.importFile(this@MainActivity, uri) { p -> fileEvent(JSONObject().put("type", "progress").put("name", name).put("p", p)) }
                }
                fileEvent(JSONObject().put("type", "done").put("game", GameLibrary.info(f)))
            } catch (e: Exception) {
                fileEvent(JSONObject().put("type", "error").put("msg", e.message ?: "Не удалось добавить файл"))
            }
        }
    }

    private fun fileEvent(o: JSONObject) = runOnUiThread {
        if (::webView.isInitialized) webView.evaluateJavascript("window.kapsulaFile&&kapsulaFile($o)", null)
    }

    /** Мост для веб-интерфейса: настоящие игры и запуск ядра. */
    inner class Bridge {
        @JavascriptInterface fun listGames(): String = GameLibrary.list(this@MainActivity)
        @JavascriptInterface fun pickGame() = runOnUiThread { pickGame.launch(arrayOf("*/*")) }
        @JavascriptInterface fun deleteGame(path: String): Boolean = GameLibrary.delete(this@MainActivity, path)
        @JavascriptInterface fun startGame(bootJson: String) = runOnUiThread { GameActivity.start(this@MainActivity, bootJson) }
        @JavascriptInterface fun coreInfo(): String = try { assets.open("ppsspp/CORE.txt").bufferedReader().readText() } catch (e: Exception) { "" }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Системная заставка Android 12+ сделана невидимой; показывается только фирменная заставка интерфейса
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Рисуем и в области выреза камеры; интерфейс сам отступает от неё (см. pushInsets)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        hideSystemBars()

        // https-адрес вместо file://, чтобы localStorage и шрифты работали как в браузере
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#0B0C0F"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            overScrollMode = View.OVER_SCROLL_NEVER
            addJavascriptInterface(Bridge(), "KapsulaNative")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                override fun onPageFinished(view: WebView, url: String) {
                    pushInsets()
                }
            }
        }
        setContentView(webView)

        // Безопасные зоны: вырез камеры (и системные панели, когда они показаны)
        ViewCompat.setOnApplyWindowInsetsListener(webView) { _, insets ->
            safe = insets.getInsets(
                WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.systemBars()
            )
            pushInsets()
            insets
        }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl("https://appassets.androidplatform.net/assets/web/index.html?app=1")
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // после выхода из игры — обновить библиотеку и время «последний запуск»
        if (::webView.isInitialized) webView.evaluateJavascript("window.kapsulaResume&&kapsulaResume()", null)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    /** Передаёт размеры выреза в интерфейс (CSS-переменные --sal/--sat/--sar/--sab в CSS-пикселях). */
    private fun pushInsets() {
        if (!::webView.isInitialized) return
        val d = resources.displayMetrics.density
        val js = "window.kapsulaInsets&&window.kapsulaInsets(" +
            "${(safe.left / d).toInt()},${(safe.top / d).toInt()},${(safe.right / d).toInt()},${(safe.bottom / d).toInt()})"
        webView.evaluateJavascript(js, null)
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
