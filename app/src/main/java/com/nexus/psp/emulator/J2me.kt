package com.nexus.psp.emulator

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.nexus.psp.data.AppState
import com.nexus.psp.data.Game
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * Java-игры (J2ME) на движке J2ME Loader (модуль :j2me, процесс :j2me).
 * Установка при первом запуске: .jar → dex (dx), ресурсы, описание игры, настройки экрана — в папку движка.
 * Файл .jar остаётся там, где лежал (как и образы других консолей).
 */
object J2me {
    private const val TAG = "KapsulaJ2me"

    /** Папка установленной игры у движка: <данные>/converted/<ключ>. */
    private fun appDir(c: Context, g: Game): File {
        javax.microedition.util.ContextHolder.setAppContext(c)
        return File(ru.playsoftware.j2meloader.config.Config.getAppDir(), key(g))
    }

    /** Ключ установки: имя файла + хэш пути (две одноимённые игры из разных папок не путаются). */
    fun key(g: Game): String {
        val base = File(g.path).nameWithoutExtension.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(40).ifEmpty { "game" }
        val h = MessageDigest.getInstance("SHA-1").digest(g.path.toByteArray()).joinToString("") { "%02x".format(it) }.take(8)
        return "${base}_$h"
    }

    fun installed(c: Context, g: Game): Boolean = File(appDir(c, g), ru.playsoftware.j2meloader.config.Config.MIDLET_DEX_FILE).exists()

    /** Похоже ли на Java-игру: zip с META-INF/MANIFEST.MF, где есть MIDlet-1. */
    fun isMidlet(f: File): Boolean = try {
        ZipFile(f).use { z -> z.getEntry("META-INF/MANIFEST.MF")?.let { e -> z.getInputStream(e).bufferedReader().readText().contains("MIDlet-1", true) } == true }
    } catch (e: Exception) { false }

    /** Установить игру (несколько секунд: перевод в dex). Ответ — текст ошибки или null. */
    fun install(c: Context, g: Game): String? {
        val src = File(g.path)
        if (!src.canRead()) return "Нет доступа к файлу игры"
        val dir = appDir(c, g)
        return installTo(src, dir) ?: run { writeConfig(dir); null }
    }

    /** Перевести .jar в dex и разложить файлы игры в dir (как установщик J2ME Loader). */
    internal fun installTo(src: File, dir: File): String? {
        val tmp = File(dir.parentFile, ".tmp_" + dir.name)
        val dex = "/converted.dex"; val res = "/res.jar"; val icon = "/icon.png"; val conf = "/converted.dex.conf"
        return try {
            tmp.deleteRecursively(); tmp.mkdirs()
            val manifest = ZipFile(src).use { z ->
                val e = z.getEntry("META-INF/MANIFEST.MF") ?: return "В файле нет описания Java-игры (MANIFEST.MF)"
                z.getInputStream(e).bufferedReader(Charsets.UTF_8).readText()
            }
            val desc = ru.woesss.j2me.jar.Descriptor(manifest, false)
            com.android.dx.command.dexer.Main.main(arrayOf("--no-optimize", "--core-library", "--output=" + tmp.path + dex, src.absolutePath))
            if (File(tmp, dex).length() <= 0) return "Игру не удалось подготовить к запуску"
            src.copyTo(File(tmp, res), overwrite = true)
            try { desc.icon?.let { ic -> ZipFile(src).use { z -> z.getEntry(ic.trimStart('/'))?.let { e -> z.getInputStream(e).use { i -> File(tmp, icon).outputStream().use { i.copyTo(it) } } } } } } catch (e: Exception) { }
            desc.writeTo(File(tmp, conf))
            dir.deleteRecursively()
            if (!tmp.renameTo(dir)) return "Игру не удалось подготовить к запуску"
            null
        } catch (e: Throwable) {
            Log.w(TAG, "install", e)
            tmp.deleteRecursively()
            "Это не Java-игра или файл повреждён"
        }
    }

    /** Настройки игры у движка: экран 240×320, телефонная клавиатура, тёмные кнопки в цветах KAPSULA. */
    private fun writeConfig(appDir: File) {
        val cfg = File(File(ru.playsoftware.j2meloader.config.Config.getConfigsDir()), appDir.name).apply { mkdirs() }
        if (File(cfg, ru.playsoftware.j2meloader.config.Config.MIDLET_CONFIG_FILE).exists()) return
        val p = ru.playsoftware.j2meloader.config.ProfileModel(cfg)
        p.showKeyboard = true
        p.vkType = 1 // телефон: игра сверху, клавиатура снизу
        p.vkAlpha = 230
        p.vkBgColor = 0x23262E; p.vkFgColor = 0xF2F1EC
        p.vkBgColorSelected = 0x7FA7D9; p.vkFgColorSelected = 0x0B0C0F; p.vkOutlineColor = 0x3A3F4B
        p.screenBackgroundColor = 0x0B0C0F
        ru.playsoftware.j2meloader.config.ProfilesManager.saveConfig(p)
    }

    /** Запуск: установить, если ещё нет (в фоне), потом экран игры. */
    fun play(c: Context, g: Game) {
        AppState.scope.launch {
            if (!installed(c, g)) {
                AppState.say("Готовлю Java-игру к первому запуску — несколько секунд")
                val err = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { install(c, g) }
                if (err != null) { AppState.say(err); return@launch }
            }
            val dir = appDir(c, g)
            c.startActivity(Intent(Intent.ACTION_DEFAULT, Uri.parse(dir.path), c, javax.microedition.shell.MicroActivity::class.java)
                .putExtra(ru.playsoftware.j2meloader.util.Constants.KEY_MIDLET_NAME, g.t)
                .putExtra("kapsula_id", g.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** Убрать установку (при удалении игры из библиотеки); сохранения игры (RMS) остаются. */
    fun uninstall(c: Context, g: Game) { try { appDir(c, g).deleteRecursively() } catch (e: Exception) { } }
}
