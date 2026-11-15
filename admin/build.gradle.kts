// KAPSULA Админ — отдельное вертикальное приложение для создателя (com.nexus.psp.admin).
// Без эмуляторов и картинок игры: только экраны управления. Подпись — тем же ключом выпусков (из секретов сборки).
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.nexus.psp.admin"
    compileSdk = 35
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
        applicationId = "com.nexus.psp.admin"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    buildTypes {
        getByName("release") {
            signingConfigs.findByName("kapsula")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests { isIncludeAndroidResources = true } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.39.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.39.0")
}
