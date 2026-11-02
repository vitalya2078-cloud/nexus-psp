package com.nexus.psp.emulator

/**
 * Bridge placeholder. PPSSPP integration will replace/extend this API.
 */
object NativeBridge {
    init {
        System.loadLibrary("nexus_native")
    }

    external fun nativeGetEngineStatus(): String
}
