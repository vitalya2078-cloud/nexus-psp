plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.nexus.psp"
    compileSdk = 35

    signingConfigs {
        create("nexusStable") {
            storeFile = rootProject.file("nexus-debug.keystore")
            storePassword = "nexusdebug2026"
            keyAlias = "nexus"
            keyPassword = "nexusdebug2026"
        }
    }

    defaultConfig {
        applicationId = "com.nexus.psp"
        minSdk = 26
        targetSdk = 35
        versionCode = 17
        versionName = "0.13.0"

        ndk {
            abiFilters += listOf("arm64-v8a") // ядро PPSSPP собрано только под 64-бит ARM
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("nexusStable")
        }
    }

    // иллюстрации и значки кнопок из ui/img (общие с веб-прототипом) упаковываются в assets/img
    sourceSets {
        getByName("main") {
            assets.srcDir(layout.buildDirectory.dir("generated/uiAssets"))
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // рендер экранов на JVM (Robolectric + Roborazzi) для проверки без телефона: ./gradlew :app:testDebugUnitTest
    testOptions { unitTests { isIncludeAndroidResources = true } }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.core:core-splashscreen:1.0.1")
    // libretro-фронтенд (GPL-3.0): вывод картинки, звук, ввод, сохранения; ядро PPSSPP подключается как .so
    implementation("com.github.Swordfish90:libretrodroid:0.14.0")
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
    from(rootProject.file("ui/img")) { include("**/*.png", "**/*.jpg") }
    from(rootProject.file("ui")) { include("LICENSE-phosphor.txt", "LICENSE-solar.txt"); into("../licenses") }
    into(layout.buildDirectory.dir("generated/uiAssets/img"))
}
tasks.named("preBuild") { dependsOn(copyUiImages) }
