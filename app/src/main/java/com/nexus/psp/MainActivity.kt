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
import android.net.Uri
import android.provider.DocumentsContract
import com.nexus.psp.data.AppState
import com.nexus.psp.data.Found
import com.nexus.psp.data.Game
import com.nexus.psp.data.Importing
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

    /** Выбор своего образа игры (ISO/CSO/CHD/PBP) через системный файловый менеджер. */
    private val pickGame = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importQueue(listOf(uri))
    }

    /** «Найти игры на телефоне»: выбор папки, поиск образов в ней и во вложенных папках. */
    private val pickTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree == null) return@registerForActivityResult
        AppState.scanning = true
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) { scanTree(tree) }
            AppState.scanning = false
            AppState.found = list
        }
    }

    /** Своя обложка из галереи для игры coverFor. */
    private var coverFor: String? = null
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val id = coverFor ?: return@registerForActivityResult
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val bytes = withContext(Dispatchers.IO) { try { contentResolver.openInputStream(uri)?.use { it.readBytes() } } catch (e: Exception) { null } }
            if (bytes != null) AppState.setUserCover(id, bytes) else AppState.say("Не удалось открыть картинку")
        }
    }

    /** Разрешение на уведомления (Android 13+). */
    private var notifyCb: ((Boolean) -> Unit)? = null
    private val askNotify = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> notifyCb?.invoke(ok); notifyCb = null }

    private fun ensureNotify(cb: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) { cb(true); return }
        notifyCb = cb; askNotify.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openNotifySettings() {
        try {
            startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName))
        } catch (e: Exception) {
            startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun scanTree(tree: Uri): List<Found> {
        val out = ArrayList<Found>()
        val known = AppState.byId.values.map { it.file.lowercase() }.toSet()
        val knownTitles = AppState.byId.values.map { com.nexus.psp.data.prettyName(it.file.substringBeforeLast('.')).lowercase() }.toSet()
        fun walk(docId: String, depth: Int) {
            if (depth > 4 || out.size >= 200) return
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
            val cols = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE)
            try {
                contentResolver.query(children, cols, null, null, null)?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0) ?: continue; val name = c.getString(1) ?: continue
                        if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) walk(id, depth + 1)
                        else if (name.substringAfterLast('.', "").lowercase() in GameLibrary.EXT) {
                            // тот же файл или та же игра в другом формате — показываем «уже есть», не копируем
                            val dup = name.lowercase() in known || com.nexus.psp.data.prettyName(name.substringBeforeLast('.')).lowercase() in knownTitles
                            out.add(Found(name, DocumentsContract.buildDocumentUriUsingTree(tree, id), (c.getLong(3) / 1048576).toInt(), dup))
                        }
                    }
                }
            } catch (e: Exception) { }
        }
        walk(DocumentsContract.getTreeDocumentId(tree), 0)
        return out.sortedBy { it.name.lowercase() }
    }

    /** Копирует файлы по очереди с прогрессом; места не хватает — останавливается с подсказкой. */
    private fun importQueue(uris: List<Uri>) {
        if (AppState.importing != null) { AppState.say("Подожди — уже копирую игру"); return }
        lifecycleScope.launch {
            var added: Game? = null; var count = 0
            for ((i, uri) in uris.withIndex()) {
                val (name, bytes) = GameLibrary.displayName(this@MainActivity, uri)
                // тот же файл уже в библиотеке — не копируем заново
                val same = AppState.hasFile(name)
                if (same != null) {
                    if (uris.size == 1) AppState.say("«${same.t}» уже в библиотеке", R.drawable.ph_check, "Открыть") { Nav.open(com.nexus.psp.ui.Ov.Game(same.id)) }
                    continue
                }
                // та же игра в другом формате — спросить
                var replace: Game? = null
                val twin = AppState.sameTitle(name)
                if (twin != null) {
                    val a = AppState.askUser("«${twin.t}» уже есть", "В библиотеке эта игра в формате ${twin.format}. Новый файл — ${name.substringAfterLast('.').uppercase()}.", "Заменить", "Оставить обе", "Не добавлять")
                    if (a == 0) replace = twin else if (a != 1) continue
                }
                if (bytes > 0 && bytes / 1048576 > AppState.freeMb() - 200) { AppState.say("Мало места для «$name» — копирование остановлено", R.drawable.sl_alert); break }
                AppState.importing = Importing(name, 0f, i + 1, uris.size)
                try {
                    val f = withContext(Dispatchers.IO) {
                        GameLibrary.importFile(this@MainActivity, uri) { p -> runOnUiThread { AppState.importing = Importing(name, p.toFloat(), i + 1, uris.size) } }
                    }
                    added = AppState.addImported(GameLibrary.info(f)); count++
                    val r = replace; val ad = added
                    if (r != null && ad != null) AppState.replaceWith(r.id, ad.id)
                } catch (e: Exception) {
                    AppState.say(e.message ?: "Не удалось добавить «$name»", R.drawable.sl_alert)
                }
            }
            AppState.importing = null
            val g = added
            if (g != null) {
                if (count == 1) AppState.say("«${g.t}» в библиотеке", R.drawable.ph_check, "Играть") { Act.play(this@MainActivity, g.id) }
                else AppState.say("Добавлено игр: $count", R.drawable.ph_check)
                Act.buzzLong(); Nav.go("games")
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
        Act.pickFolder = { pickTree.launch(null) }
        Act.importAll = { l -> importQueue(l.filter { !it.exists }.map { it.uri }) }
        Act.pickCover = { id -> coverFor = id; pickImage.launch("image/*") }
        Act.ensureNotify = { cb -> ensureNotify(cb) }
        Act.openNotifySettings = { openNotifySettings() }
        watchNetwork()
        setContent { KapsulaTheme { KapsulaApp() } }
    }

    override fun onResume() {
        super.onResume()
        // после выхода из игры — время в игре, сохранения; файлы могли удалить вручную
        AppState.loadReal()
        AppState.consumePendingGlobal()
        // «незаметный перезапуск»: игра сохранилась и просит запустить себя заново с новыми настройками
        val r = java.io.File(filesDir, "relaunch.json")
        if (r.exists()) {
            val boot = try { r.readText() } catch (e: Exception) { null }
            r.delete()
            if (boot != null) com.nexus.psp.emulator.GameActivity.start(this, boot)
        }
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
