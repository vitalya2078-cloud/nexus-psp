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
// фронтенд эмулятора из исходников с патчем KAPSULA (tools/libretrodroid/setup.sh)
include(":libretrodroid")
project(":libretrodroid").projectDir = file("libretrodroid-src/libretrodroid")
