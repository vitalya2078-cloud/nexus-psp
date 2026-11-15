package com.nexus.psp.emulator

import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest

/**
 * BIOS PS1: файл с приставки игрока. Проверяется по MD5 и кладётся в папку system под тем именем,
 * которое ищет ядро. Без файла PS1 работает на встроенном BIOS (совместимость немного ниже).
 */
object Bios {
    /** MD5 → имя файла для ядра и подпись версии (таблица известных дампов, как в документации libretro). */
    private val PS1 = mapOf(
        "8dd7d5296a650fac7319bce665a6a53c" to ("scph5500" to "SCPH-5500 · Япония"),
        "490f666e1afb15b7362b406ed1cea246" to ("scph5501" to "SCPH-5501 · США"),
        "32736f17079d0b2b7024407c39bd3050" to ("scph5502" to "SCPH-5502 · Европа"),
        "6e3735ff4c7dc899ee98981385f6f3d0" to ("scph101" to "SCPH-101 · PS one, США"),
        "1e68c231d0896b7eadcad1d7d8e76129" to ("scph7001" to "SCPH-7001 · США"),
        "924e392ed05558ffdb115408c263dccf" to ("scph1001" to "SCPH-1001 · США"),
        "c53ca5908936d412331790f4426c6c33" to ("psxonpsp660" to "PSP-эмулятор PS1 6.60"),
    )
    private const val SIZE = 512 * 1024

    private fun md5(b: ByteArray) = MessageDigest.getInstance("MD5").digest(b).joinToString("") { "%02x".format(it) }

    /** Найденные BIOS PS1 в папке system: подписи версий. */
    fun ps1(c: Context): List<String> {
        val dir = CoreFiles.systemDir(c)
        return PS1.values.filter { (n, _) -> File(dir, "$n.bin").exists() }.map { it.second } +
            (dir.listFiles()?.filter { it.name.startsWith("kapsula_ps1_") }?.map { "Своя версия (${it.name.removePrefix("kapsula_ps1_").removeSuffix(".bin")})" } ?: emptyList())
    }

    /** Добавить файл: «ok:подпись» — подходит; «bad:причина» — нет. */
    fun addPs1(c: Context, uri: Uri): String {
        val b = c.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return "bad:Файл не открылся"
        if (b.size != SIZE) return "bad:Это не BIOS PS1: у него размер ровно 512 КБ"
        val sum = md5(b)
        val known = PS1[sum]
        val name = known?.first ?: "kapsula_ps1_${sum.take(8)}"
        File(CoreFiles.systemDir(c), "$name.bin").writeBytes(b)
        return "ok:" + (known?.second ?: "неизвестная версия — может не подойти")
    }

    fun clearPs1(c: Context) {
        val dir = CoreFiles.systemDir(c)
        PS1.values.forEach { (n, _) -> File(dir, "$n.bin").delete() }
        dir.listFiles()?.filter { it.name.startsWith("kapsula_ps1_") }?.forEach { it.delete() }
    }
}

/**
 * BIOS других консолей: куда класть (относительно папки system), какие имена и размеры подходят.
 * Файлы — только с приставки игрока; KAPSULA проверяет размер (и для PS1 — контрольную сумму).
 */
object BiosFiles {
    private class Spec(val dir: String, val fixedName: String?, val names: Set<String> = emptySet(), val size: Long = 0)
    private val SPECS = mapOf(
        "ps2" to Spec("pcsx2/bios", null, size = 4L * 1024 * 1024),
        "dc" to Spec("dc", "dc_boot.bin", size = 2L * 1024 * 1024),
        "saturn" to Spec("", "saturn_bios.bin", size = 512L * 1024),
        "neogeo" to Spec("", null, names = setOf("aes.zip", "neogeo.zip")),
        "lynx" to Spec("", "lynxboot.img", size = 512),
    )

    private fun dir(c: Context, s: Spec) = if (s.dir.isEmpty()) CoreFiles.systemDir(c) else File(CoreFiles.systemDir(c), s.dir).apply { mkdirs() }

    /** Найденные файлы BIOS консоли (имена). */
    fun found(c: Context, plat: String): List<String> {
        if (plat == "ps1") return Bios.ps1(c)
        val s = SPECS[plat] ?: return emptyList()
        val d = dir(c, s)
        return when {
            s.fixedName != null -> listOfNotNull(s.fixedName.takeIf { File(d, it).exists() })
            s.names.isNotEmpty() -> s.names.filter { File(d, it).exists() }
            else -> d.listFiles()?.filter { it.isFile && it.length() == s.size }?.map { it.name } ?: emptyList()
        }
    }

    /** Нужен, но не добавлен — игру не запустить. */
    fun missing(c: Context, plat: String) = com.nexus.psp.data.PL[plat]?.biosLevel == 2 && found(c, plat).isEmpty()

    /** Добавить файл: «ok:…» или «bad:причина». */
    fun add(c: Context, plat: String, uri: Uri): String {
        if (plat == "ps1") return Bios.addPs1(c, uri)
        val s = SPECS[plat] ?: return "bad:Этой консоли BIOS не нужен"
        val name = GameLibrary.displayName(c, uri).first.substringAfterLast('/')
        val b = c.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return "bad:Файл не открылся"
        val want = when { s.size >= 1048576 -> "${s.size / 1048576} МБ"; s.size >= 1024 -> "${s.size / 1024} КБ"; else -> "${s.size} байт" }
        if (s.size > 0 && b.size.toLong() != s.size) return "bad:Это не BIOS: у него размер ровно $want"
        if (s.names.isNotEmpty() && name.lowercase() !in s.names) return "bad:Нужен файл ${s.names.joinToString(" или ")}"
        val out = File(dir(c, s), s.fixedName ?: name.lowercase().takeIf { s.names.isNotEmpty() } ?: name)
        out.writeBytes(b)
        return "ok:${out.name}"
    }

    fun clear(c: Context, plat: String) {
        if (plat == "ps1") { Bios.clearPs1(c); return }
        val s = SPECS[plat] ?: return
        found(c, plat).forEach { File(dir(c, s), it).delete() }
    }
}
