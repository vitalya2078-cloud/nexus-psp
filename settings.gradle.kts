pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // библиотеки движка Java-игр (J2ME Loader), которых нет в Maven Central — только группы com.github.*
        maven("https://jitpack.io") { content { includeGroupByRegex("com\\.github\\..*") } }
    }
}
rootProject.name = "NEXUS-PSP"
include(":app")
// KAPSULA Админ — отдельное приложение для создателя (не входит в обычный APK)
include(":admin")
// фронтенд эмулятора из исходников с патчем KAPSULA (tools/libretrodroid/setup.sh)
include(":libretrodroid")
project(":libretrodroid").projectDir = file("libretrodroid-src/libretrodroid")
// движок Java-игр J2ME Loader (Apache-2.0) с патчем KAPSULA (tools/j2me/setup.sh)
include(":j2me")
project(":j2me").projectDir = file("j2me-src/app")
include(":j2me-dexlib")
project(":j2me-dexlib").projectDir = file("j2me-src/dexlib")
