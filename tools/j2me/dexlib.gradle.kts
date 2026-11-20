// dexlib из J2ME Loader (Apache-2.0): перевод .jar игры в dex при добавлении. Скачивается tools/j2me/setup.sh.
plugins { id("com.android.library") }

android {
    namespace = "ru.playsoftware.j2meloader.dexlib"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    lint { abortOnError = false }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("net.lingala.zip4j:zip4j:2.11.6")
    implementation("org.ow2.asm:asm:9.11")
}
