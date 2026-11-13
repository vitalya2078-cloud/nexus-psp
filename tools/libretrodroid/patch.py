"""Патч KAPSULA для LibretroDroid 0.14.0 (GPL-3.0): чтение памяти игры (retro_get_memory_data).
Нужен для автопереключения раскладок и достижений. Запуск: python3 patch.py <папка LibretroDroid>."""
import sys, os
root = os.path.join(sys.argv[1], 'libretrodroid', 'src', 'main')

def edit(rel, old, new):
    p = os.path.join(root, rel); s = open(p).read()
    if new in s: return
    assert old in s, f'{rel}: нет якоря'
    open(p, 'w').write(s.replace(old, new, 1))

edit('cpp/libretrodroid.h', '    std::pair<int8_t*, size_t> serializeState();',
     '    std::pair<int8_t*, size_t> serializeState();\n'
     '    // KAPSULA: чтение памяти игры (без блокировки: чтение флагов во время кадра безопасно)\n'
     '    std::vector<uint8_t> readMemory(unsigned id, size_t offset, size_t length);\n'
     '    size_t memorySize(unsigned id);')

edit('cpp/libretrodroid.cpp', '#include "core.h"\n',
     '#include "core.h"\n\n'
     'std::vector<uint8_t> libretrodroid::LibretroDroid::readMemory(unsigned id, size_t offset, size_t length) {\n'
     '    std::vector<uint8_t> out;\n'
     '    if (!core) return out;\n'
     '    auto* base = static_cast<uint8_t*>(core->retro_get_memory_data(id));\n'
     '    size_t size = core->retro_get_memory_size(id);\n'
     '    if (base == nullptr || offset >= size) return out;\n'
     '    size_t n = std::min(length, size - offset);\n'
     '    out.assign(base + offset, base + offset + n);\n'
     '    return out;\n'
     '}\n\n'
     'size_t libretrodroid::LibretroDroid::memorySize(unsigned id) {\n'
     '    return core ? core->retro_get_memory_size(id) : 0;\n'
     '}\n')

edit('cpp/libretrodroidjni.cpp', 'JNIEXPORT jbyteArray JNICALL Java_com_swordfish_libretrodroid_LibretroDroid_serializeState(',
     'JNIEXPORT jbyteArray JNICALL Java_com_swordfish_libretrodroid_LibretroDroid_readMemory(\n'
     '    JNIEnv* env, jclass obj, jint id, jint offset, jint length\n'
     ') {\n'
     '    auto data = LibretroDroid::getInstance().readMemory((unsigned) id, (size_t) offset, (size_t) length);\n'
     '    if (data.empty()) return nullptr;\n'
     '    jbyteArray result = env->NewByteArray((jsize) data.size());\n'
     '    env->SetByteArrayRegion(result, 0, (jsize) data.size(), reinterpret_cast<const jbyte*>(data.data()));\n'
     '    return result;\n'
     '}\n\n'
     'JNIEXPORT jint JNICALL Java_com_swordfish_libretrodroid_LibretroDroid_memorySize(JNIEnv* env, jclass obj, jint id) {\n'
     '    return (jint) LibretroDroid::getInstance().memorySize((unsigned) id);\n'
     '}\n\n'
     'JNIEXPORT jbyteArray JNICALL Java_com_swordfish_libretrodroid_LibretroDroid_serializeState(')

edit('java/com/swordfish/libretrodroid/LibretroDroid.java', '    public static native byte[] serializeState();',
     '    public static native byte[] serializeState();\n'
     '    /** KAPSULA: память игры (id — RETRO_MEMORY_*, 2 = системная RAM). null — память ещё не готова. */\n'
     '    public static native byte[] readMemory(int id, int offset, int length);\n'
     '    public static native int memorySize(int id);')
# NDK 27 (libc++): std::function требует явного <functional>
for rel in ['cpp/rumble.h', 'cpp/utils/javautils.h']:
    p = os.path.join(root, rel); t = open(p).read()
    if '#include <functional>' not in t:
        i = t.index('#include'); open(p, 'w').write(t[:i] + '#include <functional>\n' + t[i:])

# ---- RetroAchievements (rcheevos): мост, крючок кадра, чтение памяти, сборка ----
import shutil
here = os.path.dirname(os.path.abspath(__file__))
shutil.copy(os.path.join(here, 'kapsula_ra.cpp'), os.path.join(root, 'cpp', 'kapsula_ra.cpp'))

edit('cpp/libretrodroid.h', '    size_t memorySize(unsigned id);',
     '    size_t memorySize(unsigned id);\n'
     '    size_t readInto(unsigned id, size_t offset, uint8_t* dst, size_t length);')

READ_INTO = (
    'size_t libretrodroid::LibretroDroid::readInto(unsigned id, size_t offset, uint8_t* dst, size_t length) {\n'
    '    if (!core) return 0;\n'
    '    auto* base = static_cast<uint8_t*>(core->retro_get_memory_data(id));\n'
    '    size_t size = core->retro_get_memory_size(id);\n'
    '    if (base == nullptr || offset >= size) return 0;\n'
    '    size_t n = std::min(length, size - offset);\n'
    '    memcpy(dst, base + offset, n);\n'
    '    return n;\n'
    '}\n\n'
    '// KAPSULA: крючок после каждого кадра (достижения) и чтение памяти для rcheevos\n'
    'void (*kapsula_frame_hook)() = nullptr;\n'
    'extern "C" uint32_t kapsula_read_memory(uint32_t address, uint8_t* buffer, uint32_t num_bytes) {\n'
    '    return (uint32_t) libretrodroid::LibretroDroid::getInstance().readInto(RETRO_MEMORY_SYSTEM_RAM, address, buffer, num_bytes);\n'
    '}\n\n'
)
edit('cpp/libretrodroid.cpp', 'size_t libretrodroid::LibretroDroid::memorySize(unsigned id) {',
     READ_INTO + 'size_t libretrodroid::LibretroDroid::memorySize(unsigned id) {')

RUN_OLD = '    for (size_t i = 0; i < frames * frameSpeed; i++)\n        core->retro_run();'
RUN_NEW = ('    for (size_t i = 0; i < frames * frameSpeed; i++) {\n'
           '        core->retro_run();\n'
           '        if (kapsula_frame_hook) kapsula_frame_hook();\n'
           '    }')
edit('cpp/libretrodroid.cpp', RUN_OLD, RUN_NEW)

edit('cpp/libretrodroid.cpp', '#include "core.h"\n\nstd::vector',
     '#include "core.h"\n#include <cstring>\n\nextern void (*kapsula_frame_hook)();\n\nstd::vector')

edit('cpp/CMakeLists.txt', 'add_library(libretrodroid SHARED\n',
     '# KAPSULA: rcheevos (MIT) — достижения RetroAchievements\n'
     'file(GLOB RCHEEVOS_SOURCES rcheevos/src/rc_client*.c rcheevos/src/rc_compat.c rcheevos/src/rc_util.c rcheevos/src/rc_version.c rcheevos/src/rapi/*.c rcheevos/src/rcheevos/*.c rcheevos/src/rhash/md5.c)\n'
     'include_directories(rcheevos/include rcheevos/src)\n\n'
     'add_library(libretrodroid SHARED\n        kapsula_ra.cpp\n        ${RCHEEVOS_SOURCES}\n')
print('patched')
