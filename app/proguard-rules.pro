# KAPSULA: правила сжатия кода для release.
# Нативная часть (libretrodroid.so) ищет классы и методы по имени — их нельзя переименовывать или вырезать.
-keep class com.swordfish.libretrodroid.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }
# Мост достижений: C++ вызывает методы Bridge по имени (http, event, loginDone, gameDone)
-keep class com.nexus.psp.emulator.RA { *; }
-keep class com.nexus.psp.emulator.RA$** { *; }
# Vulkan и чтение памяти: JNI-функции привязаны к имени класса
-keep class com.nexus.psp.emulator.Vk { *; }
-keep class com.nexus.psp.emulator.AutoSwitch { *; }
# Номера строк в отчётах о падениях (раздел «Последняя ошибка»)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
