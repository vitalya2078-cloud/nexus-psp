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
    }
}
rootProject.name = "NEXUS-PSP"
include(":app")
// KAPSULA Админ — отдельное приложение для создателя (не входит в обычный APK)
include(":admin")
// фронтенд эмулятора из исходников с патчем KAPSULA (tools/libretrodroid/setup.sh)
include(":libretrodroid")
project(":libretrodroid").projectDir = file("libretrodroid-src/libretrodroid")
