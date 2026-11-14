package com.nexus.psp.emulator

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Vulkan для ядра (пробный режим). Сам мост — в нашей сборке библиотеки эмулятора (kapsula_vk.cpp).
 *  - проверка телефона: Vulkan 1.1+ и общий буфер с OpenGL; результат запоминается;
 *  - защита: если игра на Vulkan упала или не показала картинку — помечаем сбой, дальше игры идут на OpenGL.
 */
object Vk {
    @JvmStatic external fun nSetWanted(on: Boolean)
    @JvmStatic external fun nActive(): Boolean
    @JvmStatic external fun nProbe(): Int

    private fun load() = try { System.loadLibrary("libretrodroid"); true } catch (e: Throwable) { Log.w("KapsulaVK", "load", e); false }

    private fun probeFile(c: Context) = File(c.filesDir, "vk_probe")
    fun badFile(c: Context) = File(c.filesDir, "vk_bad")
    fun tryFile(c: Context) = File(c.filesDir, "vk_try")

    /** 0 — подходит; иначе причина (см. [reason]). Проверка один раз, затем из файла. */
    fun probe(c: Context): Int {
        probeFile(c).takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull()?.let { return it }
        val r = if (!load()) 9 else try { nProbe() } catch (e: Throwable) { 9 }
        try { probeFile(c).writeText(r.toString()) } catch (_: Exception) {}
        return r
    }

    fun usable(c: Context) = probe(c) == 0 && !badFile(c).exists()

    /** Понятная причина, почему Vulkan выключен (null — доступен). */
    fun reason(c: Context): String? = when {
        badFile(c).exists() -> "Vulkan дал сбой на этом телефоне — игры идут на OpenGL"
        else -> when (probe(c)) {
            0 -> null
            1, 5 -> "Видеокарта поддерживает только старый Vulkan 1.0"
            4 -> "Видеокарта не умеет передавать кадр между Vulkan и OpenGL"
            else -> "На этом телефоне Vulkan недоступен"
        }
    }

    /** Повторить попытку после сбоя. */
    fun retry(c: Context) { badFile(c).delete(); tryFile(c).delete(); probeFile(c).delete() }

    fun markBad(c: Context) { try { badFile(c).writeText(System.currentTimeMillis().toString()) } catch (_: Exception) {}; tryFile(c).delete() }

    /** Перед запуском ядра (процесс игры): решаем, Vulkan или OpenGL. true — Vulkan. */
    fun prepare(c: Context, requested: String?): Boolean {
        // прошлый запуск на Vulkan не дошёл до подтверждения — значит, упал
        if (tryFile(c).exists()) markBad(c)
        val want = requested == "vulkan" && usable(c)
        if (!load()) return false
        try { nSetWanted(want) } catch (e: Throwable) { return false }
        if (want) try { tryFile(c).writeText("1") } catch (_: Exception) {}
        return want
    }

    /** Игра идёт на Vulkan нормально — снимаем отметку попытки. */
    fun confirm(c: Context) { tryFile(c).delete() }
}
