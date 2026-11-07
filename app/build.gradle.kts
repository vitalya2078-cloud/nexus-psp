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
        versionCode = 12
        versionName = "0.9.0"

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("nexusStable")
        }
    }

    // интерфейс из docs/ упаковывается в assets/web
    sourceSets {
        getByName("main") {
            assets.srcDir(layout.buildDirectory.dir("generated/webAssets"))
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

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
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

val copyWebAssets by tasks.registering(Copy::class) {
    from(rootProject.file("ui")) {
        include("index.html", "kapsula.css", "kapsula.js", "img/**", "LICENSE-phosphor.txt", "LICENSE-solar.txt")
    }
    into(layout.buildDirectory.dir("generated/webAssets/web"))
}
tasks.named("preBuild") { dependsOn(copyWebAssets) }
