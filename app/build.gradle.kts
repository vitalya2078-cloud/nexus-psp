plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.nexus.psp"
    compileSdk = 35
    // NDK — чтобы при упаковке вырезать отладочную информацию из нативных библиотек (LibretroDroid собирается из исходников)
    ndkVersion = "27.2.12479018"

    // ключ выпусков — только в секретах сборки (GitHub Actions), в репозитории его нет.
    // KAPSULA_KEYSTORE — путь к файлу ключа, KAPSULA_KEY_PASS — пароль, KAPSULA_KEY_ALIAS — имя ключа (по умолчанию kapsula)
    val ksPath = System.getenv("KAPSULA_KEYSTORE")
    signingConfigs {
        if (ksPath != null) create("kapsula") {
            storeFile = file(ksPath)
            storePassword = System.getenv("KAPSULA_KEY_PASS")
            keyAlias = System.getenv("KAPSULA_KEY_ALIAS") ?: "kapsula"
            keyPassword = System.getenv("KAPSULA_KEY_PASS")
        }
    }

    defaultConfig {
        applicationId = "com.nexus.psp"
        minSdk = 26
        targetSdk = 35
        versionCode = 81
        versionName = "0.50.4"

        ndk {
            abiFilters += listOf("arm64-v8a") // ядро PPSSPP собрано только под 64-бит ARM
        }
    }

    buildTypes {
        // отладочная сборка — обычный отладочный ключ Android (только для проверки на своём телефоне)
        // выпускная сборка: сжатый код, без средств отладки; ключ выпусков — ставится поверх прежних версий
        getByName("release") {
            signingConfigs.findByName("kapsula")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // иллюстрации и значки кнопок из ui/img (общие с веб-прототипом) упаковываются в assets/img
    sourceSets {
        getByName("main") {
            assets.srcDir(layout.buildDirectory.dir("generated/uiAssets"))
        }
    }

    // ядра в APK сжаты: скачивать вдвое меньше; Android распаковывает их один раз при установке, скорость игр та же
    packaging {
        jniLibs { useLegacyPackaging = true }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // рендер экранов на JVM (Robolectric + Roborazzi) для проверки без телефона: ./gradlew :app:testDebugUnitTest
    // каждый тестовый класс — в своей JVM: состояние приложения и кэши картинок не протекают между тестами
    testOptions { unitTests { isIncludeAndroidResources = true; all { it.forkEvery = 1 } } }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // push-уведомления
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.core:core-splashscreen:1.0.1")
    // страницы сайтов поверх приложения (регистрация в сообществе достижений)
    implementation("androidx.browser:browser:1.8.0")
    // libretro-фронтенд (GPL-3.0): вывод картинки, звук, ввод, сохранения; ядро PPSSPP подключается как .so
    implementation(project(":libretrodroid"))
    // движок Java-игр J2ME Loader (Apache-2.0), собирается из исходников с патчем KAPSULA (tools/j2me/setup.sh)
    implementation(project(":j2me"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.39.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.39.0")
}

val copyUiImages by tasks.registering(Copy::class) {
    from(rootProject.file("ui/img")) { include("**/*.png", "**/*.jpg", "**/*.webp") }
    from(rootProject.file("ui")) { include("LICENSE-phosphor.txt", "LICENSE-solar.txt"); into("../licenses") }
    into(layout.buildDirectory.dir("generated/uiAssets/img"))
}
// «Что нового» — тот же список, что на сайте (site/changes.json)
val copyChanges by tasks.registering(Copy::class) {
    from(rootProject.file("site/changes.json"))
    into(layout.buildDirectory.dir("generated/uiAssets"))
}
tasks.named("preBuild") { dependsOn(copyUiImages, copyChanges) }
