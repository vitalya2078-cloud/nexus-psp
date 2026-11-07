package com.nexus.psp

import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.nexus.psp.data.AppState
import com.nexus.psp.emulator.GameLibrary
import com.nexus.psp.ui.Act
import com.nexus.psp.ui.KapsulaApp
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.kit.KapsulaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** KAPSULA — нативный интерфейс на Jetpack Compose (п. 49). Игра запускается в отдельном процессе (GameActivity). */
class MainActivity : ComponentActivity() {

    /** Выбор своего образа игры (ISO/CSO/CHD/PBP) через системный файловый менеджер, копия с прогрессом. */
    private val pickGame = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val name = GameLibrary.displayName(this@MainActivity, uri).first
            AppState.importing = name to 0f
            try {
                val f = withContext(Dispatchers.IO) {
                    GameLibrary.importFile(this@MainActivity, uri) { p -> runOnUiThread { AppState.importing = name to p.toFloat() } }
                }
                AppState.importing = null
                val g = AppState.addImported(GameLibrary.info(f))
                AppState.say("«${g.t}» в библиотеке", R.drawable.ph_check, "Играть") { Act.play(this@MainActivity, g.id) }
            } catch (e: Exception) {
                AppState.importing = null
                AppState.say(e.message ?: "Не удалось добавить файл", R.drawable.sl_alert)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // системная заставка Android 12+ невидима; показывается фирменная заставка интерфейса
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AppState.init(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
        }
        hideSystemBars()
        Act.view = window.decorView
        Act.pickFile = { pickGame.launch(arrayOf("*/*")) }
        AppState.onReady = { g ->
            Act.buzzLong()
            AppState.say("«${g.t}» готова", R.drawable.ph_check, "Играть") { Act.play(this, g.id) }
        }
        watchNetwork()
        setContent { KapsulaTheme { KapsulaApp() } }
    }

    override fun onResume() {
        super.onResume()
        // после выхода из игры — время в игре, сохранения; файлы могли удалить вручную
        AppState.loadReal()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun watchNetwork() {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        try {
            AppState.online = cm.activeNetwork != null
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { runOnUiThread { if (!AppState.online) AppState.say("Интернет снова есть"); AppState.online = true } }
                override fun onLost(network: Network) { runOnUiThread { AppState.online = false; AppState.say("Нет интернета — играть можно в установленные") } }
            })
        } catch (e: Exception) { }
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    @Suppress("unused")
    private fun openDownloads() = Nav.open(Ov.Downloads)
}
