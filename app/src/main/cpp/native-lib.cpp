#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_nexus_psp_emulator_NativeBridge_nativeGetEngineStatus(
        JNIEnv* env, jobject /* this */) {
    std::string status = "NEXUS native layer initialized. PSP core not connected yet.";
    return env->NewStringUTF(status.c_str());
}
