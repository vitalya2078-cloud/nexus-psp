// KAPSULA: достижения RetroAchievements через rcheevos (rc_client). GPL-3.0 (часть сборки LibretroDroid KAPSULA).
// Память читается прямо из ядра после каждого кадра; HTTP-запросы выполняет Kotlin (com.nexus.psp.emulator.RA).
#include <jni.h>
#include <map>
#include <mutex>
#include <string>
#include <android/log.h>
#include "rc_client.h"

#define RALOG(...) __android_log_print(ANDROID_LOG_INFO, "KapsulaRA", __VA_ARGS__)

extern "C" uint32_t kapsula_read_memory(uint32_t address, uint8_t* buffer, uint32_t num_bytes);
extern void (*kapsula_frame_hook)();

static rc_client_t* g_client = nullptr;
static JavaVM* g_vm = nullptr;
static jobject g_bridge = nullptr;
static jmethodID m_http, m_event, m_login, m_game;
static std::mutex g_mx;
static std::map<int, std::pair<rc_client_server_callback_t, void*>> g_pending;
static int g_next = 1;

static JNIEnv* env() {
    JNIEnv* e = nullptr;
    if (g_vm->GetEnv(reinterpret_cast<void**>(&e), JNI_VERSION_1_6) != JNI_OK) g_vm->AttachCurrentThread(&e, nullptr);
    return e;
}

static jstring str(JNIEnv* e, const char* s) { return s ? e->NewStringUTF(s) : nullptr; }

static uint32_t read_mem(uint32_t address, uint8_t* buffer, uint32_t num_bytes, rc_client_t*) {
    // адреса RetroAchievements для PSP: 0x0 = 0x08000000 — так же отдаёт память ядро
    return kapsula_read_memory(address, buffer, num_bytes);
}

static void server_call(const rc_api_request_t* req, rc_client_server_callback_t cb, void* data, rc_client_t*) {
    int id;
    { std::lock_guard<std::mutex> l(g_mx); id = g_next++; g_pending[id] = {cb, data}; }
    JNIEnv* e = env();
    jstring url = str(e, req->url), post = str(e, req->post_data), ct = str(e, req->content_type);
    e->CallVoidMethod(g_bridge, m_http, id, url, post, ct);
    if (url) e->DeleteLocalRef(url); if (post) e->DeleteLocalRef(post); if (ct) e->DeleteLocalRef(ct);
}

static void send_event(int type, uint32_t id, const char* title, const char* desc, const char* badge, uint32_t points) {
    JNIEnv* e = env();
    jstring t = str(e, title), d = str(e, desc), b = str(e, badge);
    e->CallVoidMethod(g_bridge, m_event, type, (jint) id, t, d, b, (jint) points);
    if (t) e->DeleteLocalRef(t); if (d) e->DeleteLocalRef(d); if (b) e->DeleteLocalRef(b);
}

static void event_handler(const rc_client_event_t* ev, rc_client_t*) {
    switch (ev->type) {
        case RC_CLIENT_EVENT_ACHIEVEMENT_TRIGGERED:
            send_event(1, ev->achievement->id, ev->achievement->title, ev->achievement->description, ev->achievement->badge_name, ev->achievement->points);
            break;
        case RC_CLIENT_EVENT_GAME_COMPLETED: send_event(2, 0, "Все достижения получены", nullptr, nullptr, 0); break;
        case RC_CLIENT_EVENT_SERVER_ERROR:
            send_event(9, 0, ev->server_error ? ev->server_error->api : "", ev->server_error ? ev->server_error->error_message : "", nullptr, 0);
            break;
        default: break;
    }
}

static void frame() { if (g_client) rc_client_do_frame(g_client); }

static void login_cb(int result, const char* error, rc_client_t* client, void*) {
    const rc_client_user_t* u = result == RC_OK ? rc_client_get_user_info(client) : nullptr;
    JNIEnv* e = env();
    jstring er = str(e, error), tok = str(e, u ? u->token : nullptr), name = str(e, u ? u->display_name : nullptr);
    e->CallVoidMethod(g_bridge, m_login, result == RC_OK, er, tok, name, (jint) (u ? u->score : 0));
    if (er) e->DeleteLocalRef(er); if (tok) e->DeleteLocalRef(tok); if (name) e->DeleteLocalRef(name);
}

static void game_cb(int result, const char* error, rc_client_t* client, void*) {
    const rc_client_game_t* g = result == RC_OK ? rc_client_get_game_info(client) : nullptr;
    rc_client_user_game_summary_t s{};
    if (g) rc_client_get_user_game_summary(client, &s);
    JNIEnv* e = env();
    jstring er = str(e, error), t = str(e, g ? g->title : nullptr);
    e->CallVoidMethod(g_bridge, m_game, result == RC_OK, er, t, (jint) s.num_core_achievements, (jint) s.num_unlocked_achievements, (jint) s.points_core, (jint) s.points_unlocked);
    if (er) e->DeleteLocalRef(er); if (t) e->DeleteLocalRef(t);
    if (result == RC_OK) kapsula_frame_hook = frame;
}

extern "C" {

JNIEXPORT void JNICALL Java_com_nexus_psp_emulator_RA_nInit(JNIEnv* e, jclass, jobject bridge) {
    e->GetJavaVM(&g_vm);
    if (g_bridge) e->DeleteGlobalRef(g_bridge);
    g_bridge = e->NewGlobalRef(bridge);
    jclass c = e->GetObjectClass(bridge);
    m_http = e->GetMethodID(c, "http", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    m_event = e->GetMethodID(c, "event", "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
    m_login = e->GetMethodID(c, "loginDone", "(ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
    m_game = e->GetMethodID(c, "gameDone", "(ZLjava/lang/String;Ljava/lang/String;IIII)V");
    if (!g_client) {
        g_client = rc_client_create(read_mem, server_call);
        rc_client_set_event_handler(g_client, event_handler);
        rc_client_set_hardcore_enabled(g_client, 0); // мягкий режим: ускорение, сохранения и читы не блокируются
    }
}

JNIEXPORT void JNICALL Java_com_nexus_psp_emulator_RA_nResponse(JNIEnv* e, jclass, jint id, jstring body, jint status) {
    std::pair<rc_client_server_callback_t, void*> p{nullptr, nullptr};
    { std::lock_guard<std::mutex> l(g_mx); auto it = g_pending.find(id); if (it == g_pending.end()) return; p = it->second; g_pending.erase(it); }
    const char* b = body ? e->GetStringUTFChars(body, nullptr) : "";
    rc_api_server_response_t r{};
    r.body = b; r.body_length = body ? strlen(b) : 0; r.http_status_code = status;
    p.first(&r, p.second);
    if (body) e->ReleaseStringUTFChars(body, b);
}

JNIEXPORT void JNICALL Java_com_nexus_psp_emulator_RA_nLoginToken(JNIEnv* e, jclass, jstring user, jstring token) {
    if (!g_client) return;
    const char* u = e->GetStringUTFChars(user, nullptr); const char* t = e->GetStringUTFChars(token, nullptr);
    rc_client_begin_login_with_token(g_client, u, t, login_cb, nullptr);
    e->ReleaseStringUTFChars(user, u); e->ReleaseStringUTFChars(token, t);
}

JNIEXPORT void JNICALL Java_com_nexus_psp_emulator_RA_nLoadGame(JNIEnv* e, jclass, jstring hash) {
    if (!g_client) return;
    const char* h = e->GetStringUTFChars(hash, nullptr);
    rc_client_begin_load_game(g_client, h, game_cb, nullptr);
    e->ReleaseStringUTFChars(hash, h);
}

/** Список достижений игры одной строкой: id\ttitle\tdesc\tpoints\tunlocked\tbadge\tprogress, по строке на достижение. */
JNIEXPORT jstring JNICALL Java_com_nexus_psp_emulator_RA_nList(JNIEnv* e, jclass) {
    if (!g_client || !rc_client_is_game_loaded(g_client)) return nullptr;
    rc_client_achievement_list_t* l = rc_client_create_achievement_list(g_client, RC_CLIENT_ACHIEVEMENT_CATEGORY_CORE, RC_CLIENT_ACHIEVEMENT_LIST_GROUPING_LOCK_STATE);
    std::string out;
    for (uint32_t i = 0; i < l->num_buckets; i++) {
        const auto& b = l->buckets[i];
        for (uint32_t j = 0; j < b.num_achievements; j++) {
            const rc_client_achievement_t* a = b.achievements[j];
            auto clean = [](const char* s) { std::string r = s ? s : ""; for (auto& ch : r) if (ch == '\t' || ch == '\n') ch = ' '; return r; };
            out += std::to_string(a->id) + "\t" + clean(a->title) + "\t" + clean(a->description) + "\t" + std::to_string(a->points) + "\t"
                + (a->unlocked ? "1" : "0") + "\t" + clean(a->badge_name) + "\t" + clean(a->measured_progress) + "\n";
        }
    }
    rc_client_destroy_achievement_list(l);
    return e->NewStringUTF(out.c_str());
}

JNIEXPORT void JNICALL Java_com_nexus_psp_emulator_RA_nReset(JNIEnv*, jclass) { if (g_client) rc_client_reset(g_client); }

JNIEXPORT void JNICALL Java_com_nexus_psp_emulator_RA_nUnload(JNIEnv*, jclass) {
    kapsula_frame_hook = nullptr;
    if (g_client) rc_client_unload_game(g_client);
}

}
