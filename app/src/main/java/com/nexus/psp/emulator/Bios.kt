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
