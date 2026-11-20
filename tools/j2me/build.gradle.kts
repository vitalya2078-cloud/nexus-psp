// Движок Java-игр J2ME Loader (Apache-2.0; часть кода MicroEmulator — LGPL-2.1 или Apache-2.0 на выбор),
// github.com/nikita36078/J2ME-Loader. Собирается как библиотека KAPSULA; скачивается tools/j2me/setup.sh,
// правки — tools/j2me/patch.py. Версии зависимостей — под compileSdk 35 / AGP 8.7 (у оригинала новее).
plugins { id("com.android.library") }

android {
    namespace = "ru.playsoftware.j2meloader"
    compileSdk = 35
    ndkVersion = "27.2.12479018"
    defaultConfig {
        minSdk = 26
        ndk { abiFilters += "arm64-v8a" }
        buildConfigField("boolean", "FULL_EMULATOR", "true")
        buildConfigField("String", "FLAVOR", "\"kapsula\"")
        buildConfigField("int", "VERSION_CODE", "101")
        buildConfigField("String", "VERSION_NAME", "\"1.8.2-kapsula\"")
        resValue("string", "app_name", "KAPSULA")
        consumerProguardFiles("proguard-rules.pro", "proguard-common.pro")
        javaCompileOptions { annotationProcessorOptions { arguments += mapOf("room.schemaLocation" to "$projectDir/schemas") } }
    }
    externalNativeBuild { ndkBuild { path = file("src/main/cpp/Android.mk") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { viewBinding = true; prefab = true; buildConfig = true; resValues = true }
    lint { abortOnError = false; disable += setOf("MissingTranslation", "ExtraTranslation") }
}

dependencies {
    implementation(project(":j2me-dexlib"))
    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-rxjava2:$room")
    annotationProcessor("androidx.room:room-compiler:$room")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.activity:activity:1.10.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.google.guava:guava:33.3.1-android")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("net.lingala.zip4j:zip4j:2.11.6")
    implementation("com.github.nikita36078:pngj:2.2.3")
    implementation("com.google.oboe:oboe:1.9.0")
    val camerax = "1.4.1"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
    implementation("com.github.nikita36078:filepicker:4.3.0")
    implementation("com.github.yukuku:ambilwarna:2.0.1")
    // отчёты об ошибках оригинала: в KAPSULA не включаются (ACRA не инициализируется), нужны только для сборки
    implementation("ch.acra:acra-dialog:5.11.4")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
}
