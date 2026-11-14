// KAPSULA: журнал падений нативной части (ядро, графика, Vulkan). GPL-3.0 (часть сборки LibretroDroid KAPSULA).
// При сигнале SIGSEGV/SIGABRT/SIGBUS/SIGFPE/SIGILL пишет в файл: сигнал, адрес, место падения (библиотека + смещение)
// и стек вызовов. Затем передаёт сигнал прежнему обработчику — система закрывает процесс как обычно.
#include <jni.h>
#include <signal.h>
#include <unwind.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <unistd.h>
#include <ucontext.h>
#include <cstdio>
#include <cstring>
#include <ctime>

namespace {

char path[512];
char head[512];
struct sigaction prev[32];
const int SIGS[] = {SIGSEGV, SIGABRT, SIGBUS, SIGFPE, SIGILL};

struct Trace { uintptr_t* cur; uintptr_t* end; };

_Unwind_Reason_Code onFrame(_Unwind_Context* ctx, void* arg) {
    auto* t = static_cast<Trace*>(arg);
    uintptr_t pc = _Unwind_GetIP(ctx);
    if (pc) { if (t->cur == t->end) return _URC_END_OF_STACK; *t->cur++ = pc; }
    return _URC_NO_REASON;
}

const char* sigName(int s) {
    switch (s) { case SIGSEGV: return "SIGSEGV — обращение к чужой памяти"; case SIGABRT: return "SIGABRT — аварийная остановка";
        case SIGBUS: return "SIGBUS — ошибка шины памяти"; case SIGFPE: return "SIGFPE — ошибка вычисления"; case SIGILL: return "SIGILL — недопустимая команда"; }
    return "сигнал";
}

void line(int fd, const char* s) { write(fd, s, strlen(s)); }

void frame(int fd, int n, uintptr_t pc) {
    char b[512];
    Dl_info info{};
    if (dladdr(reinterpret_cast<void*>(pc), &info) && info.dli_fname) {
        const char* lib = strrchr(info.dli_fname, '/'); lib = lib ? lib + 1 : info.dli_fname;
        uintptr_t off = pc - reinterpret_cast<uintptr_t>(info.dli_fbase);
        if (info.dli_sname) snprintf(b, sizeof b, "#%02d %s +0x%zx (%s+0x%zx)\n", n, lib, (size_t) off, info.dli_sname, (size_t) (pc - reinterpret_cast<uintptr_t>(info.dli_saddr)));
        else snprintf(b, sizeof b, "#%02d %s +0x%zx\n", n, lib, (size_t) off);
    } else snprintf(b, sizeof b, "#%02d 0x%zx\n", n, (size_t) pc);
    line(fd, b);
}

void handler(int sig, siginfo_t* info, void* uctx) {
    int fd = open(path, O_WRONLY | O_CREAT | O_TRUNC, 0600);
    if (fd >= 0) {
        char b[256];
        time_t now = time(nullptr); struct tm tm{}; localtime_r(&now, &tm);
        line(fd, head); line(fd, "\n");
        snprintf(b, sizeof b, "%04d-%02d-%02d %02d:%02d:%02d · сбой эмулятора\n\n", tm.tm_year + 1900, tm.tm_mon + 1, tm.tm_mday, tm.tm_hour, tm.tm_min, tm.tm_sec);
        line(fd, b);
        snprintf(b, sizeof b, "Сигнал %d (%s), код %d, адрес %p\n", sig, sigName(sig), info ? info->si_code : 0, info ? info->si_addr : nullptr);
        line(fd, b);
#if defined(__aarch64__)
        auto* uc = static_cast<ucontext_t*>(uctx);
        line(fd, "\nМесто падения:\n");
        frame(fd, 0, uc->uc_mcontext.pc);
        frame(fd, 1, uc->uc_mcontext.regs[30]); // адрес возврата (LR)
#endif
        uintptr_t pcs[48]; Trace t{pcs, pcs + 48};
        _Unwind_Backtrace(onFrame, &t);
        line(fd, "\nСтек:\n");
        for (int i = 0; i < t.cur - pcs; i++) frame(fd, i, pcs[i]);
        close(fd);
    }
    // прежний обработчик (системный отчёт) и завершение процесса
    for (unsigned i = 0; i < sizeof SIGS / sizeof SIGS[0]; i++) if (SIGS[i] == sig) { sigaction(sig, &prev[i], nullptr); break; }
    raise(sig);
}

} // namespace

extern "C" JNIEXPORT void JNICALL Java_com_nexus_psp_emulator_NativeCrash_nInit(JNIEnv* e, jclass, jstring p, jstring h) {
    const char* ps = e->GetStringUTFChars(p, nullptr); strncpy(path, ps, sizeof path - 1); e->ReleaseStringUTFChars(p, ps);
    const char* hs = e->GetStringUTFChars(h, nullptr); strncpy(head, hs, sizeof head - 1); e->ReleaseStringUTFChars(h, hs);
    // отдельный стек: при переполнении стека обработчик тоже сработает
    static char altstack[64 * 1024];
    stack_t ss{}; ss.ss_sp = altstack; ss.ss_size = sizeof altstack; sigaltstack(&ss, nullptr);
    for (unsigned i = 0; i < sizeof SIGS / sizeof SIGS[0]; i++) {
        struct sigaction sa{}; sa.sa_sigaction = handler; sa.sa_flags = SA_SIGINFO | SA_ONSTACK; sigemptyset(&sa.sa_mask);
        sigaction(SIGS[i], &sa, &prev[i]);
    }
}
