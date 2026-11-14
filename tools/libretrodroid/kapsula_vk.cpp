// KAPSULA: Vulkan для ядра. GPL-3.0 (часть сборки LibretroDroid KAPSULA).
//
// Ядро рисует кадр через Vulkan в свою картинку (set_image). Мы копируем её на видеокарте
// в общий буфер AHardwareBuffer, который виден и Vulkan, и OpenGL, и переносим кадр в обычный
// GL-кадр LibretroDroid. Дальше всё как раньше: фильтры, раскладка, скриншоты работают без изменений.
// Если общий буфер недоступен — запасной путь через память процессора (медленнее, но работает).
#define VK_USE_PLATFORM_ANDROID_KHR
#include <vulkan/vulkan.h>
#include <android/hardware_buffer.h>
#include <android/log.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES3/gl3.h>
#include <GLES2/gl2ext.h>
#include <jni.h>
#include <algorithm>
#include <atomic>
#include <cstring>
#include <mutex>
#include <vector>
#include "libretro.h"
#include "libretro_vulkan.h"

#define VKLOG(...) __android_log_print(ANDROID_LOG_INFO, "KapsulaVK", __VA_ARGS__)

namespace {

constexpr uint32_t SLOTS = 2;

bool wanted = false;          // игрок выбрал Vulkan (ставится из Kotlin до запуска ядра)
bool active = false;          // устройство создано, ядро получило интерфейс
bool failed = false;          // попытка была и не удалась — дальше не пробуем
const retro_hw_render_context_negotiation_interface_vulkan* neg = nullptr;
retro_hw_context_reset_t coreReset = nullptr;

VkInstance inst = VK_NULL_HANDLE;
VkPhysicalDevice gpu = VK_NULL_HANDLE;
VkDevice dev = VK_NULL_HANDLE;
VkQueue queue = VK_NULL_HANDLE;
uint32_t qfam = 0;
retro_hw_render_interface_vulkan iface{};

std::mutex qmx;               // очередь видеокарты общая с ядром
std::mutex imx;               // данные set_image
std::atomic<uint32_t> slot{0};
VkFence fences[SLOTS]{};
bool fenceBusy[SLOTS]{};
VkCommandPool pool = VK_NULL_HANDLE;
VkCommandBuffer cmds[SLOTS]{};

retro_vulkan_image coreImg{};
bool haveImg = false;
std::vector<VkSemaphore> waitSems;
std::vector<VkCommandBuffer> extraCmds;
VkSemaphore signalSem = VK_NULL_HANDLE;
uint32_t srcFamily = VK_QUEUE_FAMILY_IGNORED;

// куда копируем кадр
bool ahbOk = false;
PFN_vkGetAndroidHardwareBufferPropertiesANDROID pGetAhbProps = nullptr;
AHardwareBuffer* ahb = nullptr;
VkImage dst = VK_NULL_HANDLE;
VkDeviceMemory dstMem = VK_NULL_HANDLE;
VkBuffer rb = VK_NULL_HANDLE;          // запасной путь: буфер для чтения кадра
VkDeviceMemory rbMem = VK_NULL_HANDLE;
void* rbMap = nullptr;
bool rbCoherent = true;
uint32_t tw = 0, th = 0;

EGLContext glCtx = EGL_NO_CONTEXT;
EGLImageKHR eglImg = EGL_NO_IMAGE_KHR;
EGLSyncKHR glFence = EGL_NO_SYNC_KHR;
GLuint tex = 0, fbo = 0;

PFNEGLGETNATIVECLIENTBUFFERANDROIDPROC pGetClientBuffer = nullptr;
PFNEGLCREATEIMAGEKHRPROC pCreateImage = nullptr;
PFNEGLDESTROYIMAGEKHRPROC pDestroyImage = nullptr;
PFNGLEGLIMAGETARGETTEXTURE2DOESPROC pImageTarget = nullptr;
PFNEGLCREATESYNCKHRPROC pCreateSync = nullptr;
PFNEGLCLIENTWAITSYNCKHRPROC pWaitSync = nullptr;
PFNEGLDESTROYSYNCKHRPROC pDestroySync = nullptr;

const char* DEVICE_EXTS[] = {
    "VK_ANDROID_external_memory_android_hardware_buffer",
    "VK_KHR_external_memory",
    "VK_KHR_dedicated_allocation",
    "VK_KHR_get_memory_requirements2",
    "VK_KHR_sampler_ycbcr_conversion",
    "VK_KHR_bind_memory2",
    "VK_KHR_maintenance1",
    "VK_EXT_queue_family_foreign",
};
constexpr unsigned NUM_DEVICE_EXTS = sizeof(DEVICE_EXTS) / sizeof(DEVICE_EXTS[0]);

// ---------- интерфейс для ядра ----------
void cbSetImage(void*, const retro_vulkan_image* img, uint32_t n, const VkSemaphore* sems, uint32_t family) {
    std::lock_guard<std::mutex> l(imx);
    coreImg = *img; haveImg = true;
    waitSems.assign(sems, sems + n);
    srcFamily = family;
}
uint32_t cbSyncIndex(void*) { return slot.load(); }
uint32_t cbSyncMask(void*) { return (1u << SLOTS) - 1; }
void cbSetCommandBuffers(void*, uint32_t n, const VkCommandBuffer* c) { std::lock_guard<std::mutex> l(imx); extraCmds.assign(c, c + n); }
void cbWaitSync(void*) {
    uint32_t i = slot.load();
    if (fenceBusy[i]) vkWaitForFences(dev, 1, &fences[i], VK_TRUE, UINT64_MAX);
}
void cbLock(void*) { qmx.lock(); }
void cbUnlock(void*) { qmx.unlock(); }
void cbSignal(void*, VkSemaphore s) { std::lock_guard<std::mutex> l(imx); signalSem = s; }

// ---------- создание ----------
uint32_t loaderVersion() {
    auto f = (PFN_vkEnumerateInstanceVersion) vkGetInstanceProcAddr(VK_NULL_HANDLE, "vkEnumerateInstanceVersion");
    uint32_t v = VK_API_VERSION_1_0;
    if (f) f(&v);
    return v;
}

std::vector<const char*> instanceExts() {
    const char* want[] = { "VK_KHR_surface", "VK_KHR_android_surface", "VK_KHR_get_physical_device_properties2",
                           "VK_KHR_external_memory_capabilities", "VK_KHR_external_semaphore_capabilities", "VK_KHR_external_fence_capabilities" };
    uint32_t n = 0; vkEnumerateInstanceExtensionProperties(nullptr, &n, nullptr);
    std::vector<VkExtensionProperties> av(n); vkEnumerateInstanceExtensionProperties(nullptr, &n, av.data());
    std::vector<const char*> out;
    for (auto w : want) for (auto& e : av) if (!strcmp(e.extensionName, w)) { out.push_back(w); break; }
    return out;
}

VkInstance createInstanceWrapper(void*, const VkInstanceCreateInfo* ci) {
    // ядро просит свой экземпляр — добавляем наши расширения к его списку
    std::vector<const char*> exts(ci->ppEnabledExtensionNames, ci->ppEnabledExtensionNames + ci->enabledExtensionCount);
    for (auto e : instanceExts()) { bool has = false; for (auto x : exts) if (!strcmp(x, e)) has = true; if (!has) exts.push_back(e); }
    VkInstanceCreateInfo c = *ci; c.enabledExtensionCount = (uint32_t) exts.size(); c.ppEnabledExtensionNames = exts.data();
    VkInstance r = VK_NULL_HANDLE;
    return vkCreateInstance(&c, nullptr, &r) == VK_SUCCESS ? r : VK_NULL_HANDLE;
}

VkDevice createDeviceWrapper(VkPhysicalDevice g, void*, const VkDeviceCreateInfo* ci) {
    uint32_t n = 0; vkEnumerateDeviceExtensionProperties(g, nullptr, &n, nullptr);
    std::vector<VkExtensionProperties> av(n); vkEnumerateDeviceExtensionProperties(g, nullptr, &n, av.data());
    std::vector<const char*> exts(ci->ppEnabledExtensionNames, ci->ppEnabledExtensionNames + ci->enabledExtensionCount);
    for (auto w : DEVICE_EXTS) {
        bool has = false; for (auto x : exts) if (!strcmp(x, w)) has = true;
        bool avail = false; for (auto& e : av) if (!strcmp(e.extensionName, w)) avail = true;
        if (!has && avail) exts.push_back(w);
    }
    VkDeviceCreateInfo c = *ci; c.enabledExtensionCount = (uint32_t) exts.size(); c.ppEnabledExtensionNames = exts.data();
    VkDevice r = VK_NULL_HANDLE;
    return vkCreateDevice(g, &c, nullptr, &r) == VK_SUCCESS ? r : VK_NULL_HANDLE;
}

bool createOwnDevice() {
    uint32_t n = 0; vkGetPhysicalDeviceQueueFamilyProperties(gpu, &n, nullptr);
    std::vector<VkQueueFamilyProperties> q(n); vkGetPhysicalDeviceQueueFamilyProperties(gpu, &n, q.data());
    int fam = -1; for (uint32_t i = 0; i < n; i++) if (q[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) { fam = (int) i; break; }
    if (fam < 0) return false;
    float prio = 1.f;
    VkDeviceQueueCreateInfo qi{VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO}; qi.queueFamilyIndex = (uint32_t) fam; qi.queueCount = 1; qi.pQueuePriorities = &prio;
    VkDeviceCreateInfo ci{VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO}; ci.queueCreateInfoCount = 1; ci.pQueueCreateInfos = &qi;
    dev = createDeviceWrapper(gpu, nullptr, &ci);
    if (!dev) return false;
    qfam = (uint32_t) fam; vkGetDeviceQueue(dev, qfam, 0, &queue);
    return true;
}

bool createAll() {
    uint32_t ver = loaderVersion();
    if (VK_API_VERSION_MINOR(ver) < 1 && VK_API_VERSION_MAJOR(ver) < 2) { VKLOG("Vulkan 1.0 — нужен 1.1+"); return false; }
    VkApplicationInfo app{VK_STRUCTURE_TYPE_APPLICATION_INFO};
    if (neg && neg->get_application_info && neg->get_application_info()) app = *neg->get_application_info();
    else { app.pApplicationName = "KAPSULA"; app.pEngineName = "KAPSULA"; }
    uint32_t cap = VK_MAKE_API_VERSION(0, 1, 3, 0);
    app.apiVersion = std::min(ver & 0xFFFFF000u, cap);

    bool v2 = neg && neg->interface_version >= 2;
    if (v2 && neg->create_instance) inst = neg->create_instance(vkGetInstanceProcAddr, &app, createInstanceWrapper, nullptr);
    else {
        auto exts = instanceExts();
        VkInstanceCreateInfo ci{VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO}; ci.pApplicationInfo = &app;
        ci.enabledExtensionCount = (uint32_t) exts.size(); ci.ppEnabledExtensionNames = exts.data();
        if (vkCreateInstance(&ci, nullptr, &inst) != VK_SUCCESS) inst = VK_NULL_HANDLE;
    }
    if (!inst) { VKLOG("не создан экземпляр Vulkan"); return false; }

    uint32_t n = 0; vkEnumeratePhysicalDevices(inst, &n, nullptr);
    if (n == 0) { VKLOG("нет видеокарты Vulkan"); return false; }
    std::vector<VkPhysicalDevice> g(n); vkEnumeratePhysicalDevices(inst, &n, g.data());
    gpu = g[0];

    retro_vulkan_context ctx{};
    bool ok = false;
    if (v2 && neg->create_device2) {
        ok = neg->create_device2(&ctx, inst, gpu, VK_NULL_HANDLE, vkGetInstanceProcAddr, createDeviceWrapper, nullptr);
    } else if (neg && neg->create_device) {
        VkPhysicalDeviceFeatures feats{};
        // только поддерживаемые расширения: ядро включает то, что есть, остальное пропускает
        ok = neg->create_device(&ctx, inst, gpu, VK_NULL_HANDLE, vkGetInstanceProcAddr, DEVICE_EXTS, NUM_DEVICE_EXTS, nullptr, 0, &feats);
    } else {
        ok = createOwnDevice();
        if (ok) { ctx.gpu = gpu; ctx.device = dev; ctx.queue = queue; ctx.queue_family_index = qfam; }
    }
    if (!ok || !ctx.device) { VKLOG("не создано устройство Vulkan"); return false; }
    if (ctx.gpu) gpu = ctx.gpu;
    dev = ctx.device; queue = ctx.queue; qfam = ctx.queue_family_index;

    pGetAhbProps = (PFN_vkGetAndroidHardwareBufferPropertiesANDROID) vkGetDeviceProcAddr(dev, "vkGetAndroidHardwareBufferPropertiesANDROID");
    ahbOk = pGetAhbProps != nullptr;
    VKLOG("устройство готово, общий буфер: %s", ahbOk ? "да" : "нет (запасной путь)");

    VkCommandPoolCreateInfo pi{VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO}; pi.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT; pi.queueFamilyIndex = qfam;
    if (vkCreateCommandPool(dev, &pi, nullptr, &pool) != VK_SUCCESS) return false;
    VkCommandBufferAllocateInfo ai{VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO}; ai.commandPool = pool; ai.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY; ai.commandBufferCount = SLOTS;
    if (vkAllocateCommandBuffers(dev, &ai, cmds) != VK_SUCCESS) return false;
    for (auto& f : fences) { VkFenceCreateInfo fi{VK_STRUCTURE_TYPE_FENCE_CREATE_INFO}; if (vkCreateFence(dev, &fi, nullptr, &f) != VK_SUCCESS) return false; }

    iface = {};
    iface.interface_type = RETRO_HW_RENDER_INTERFACE_VULKAN;
    iface.interface_version = RETRO_HW_RENDER_INTERFACE_VULKAN_VERSION;
    iface.handle = &iface;
    iface.instance = inst; iface.gpu = gpu; iface.device = dev;
    iface.get_device_proc_addr = vkGetDeviceProcAddr;
    iface.get_instance_proc_addr = vkGetInstanceProcAddr;
    iface.queue = queue; iface.queue_index = qfam;
    iface.set_image = cbSetImage;
    iface.get_sync_index = cbSyncIndex;
    iface.get_sync_index_mask = cbSyncMask;
    iface.set_command_buffers = cbSetCommandBuffers;
    iface.wait_sync_index = cbWaitSync;
    iface.lock_queue = cbLock;
    iface.unlock_queue = cbUnlock;
    iface.set_signal_semaphore = cbSignal;
    return true;
}

bool loadEgl() {
    if (pCreateImage) return true;
    pGetClientBuffer = (PFNEGLGETNATIVECLIENTBUFFERANDROIDPROC) eglGetProcAddress("eglGetNativeClientBufferANDROID");
    pCreateImage = (PFNEGLCREATEIMAGEKHRPROC) eglGetProcAddress("eglCreateImageKHR");
    pDestroyImage = (PFNEGLDESTROYIMAGEKHRPROC) eglGetProcAddress("eglDestroyImageKHR");
    pImageTarget = (PFNGLEGLIMAGETARGETTEXTURE2DOESPROC) eglGetProcAddress("glEGLImageTargetTexture2DOES");
    pCreateSync = (PFNEGLCREATESYNCKHRPROC) eglGetProcAddress("eglCreateSyncKHR");
    pWaitSync = (PFNEGLCLIENTWAITSYNCKHRPROC) eglGetProcAddress("eglClientWaitSyncKHR");
    pDestroySync = (PFNEGLDESTROYSYNCKHRPROC) eglGetProcAddress("eglDestroySyncKHR");
    if (!pGetClientBuffer || !pCreateImage || !pDestroyImage || !pImageTarget) { ahbOk = false; VKLOG("EGL без общих буферов — запасной путь"); }
    return true;
}

int memType(uint32_t bits, VkMemoryPropertyFlags want) {
    VkPhysicalDeviceMemoryProperties mp; vkGetPhysicalDeviceMemoryProperties(gpu, &mp);
    for (uint32_t i = 0; i < mp.memoryTypeCount; i++) if ((bits & (1u << i)) && (mp.memoryTypes[i].propertyFlags & want) == want) return (int) i;
    return -1;
}

void destroyTarget(bool glAlive) {
    if (dev) vkDeviceWaitIdle(dev);
    EGLDisplay dpy = eglGetCurrentDisplay();
    if (glFence != EGL_NO_SYNC_KHR && pDestroySync && dpy != EGL_NO_DISPLAY) pDestroySync(dpy, glFence);
    glFence = EGL_NO_SYNC_KHR;
    if (glAlive) { if (fbo) glDeleteFramebuffers(1, &fbo); if (tex) glDeleteTextures(1, &tex); }
    fbo = tex = 0;
    if (eglImg != EGL_NO_IMAGE_KHR && pDestroyImage && dpy != EGL_NO_DISPLAY) pDestroyImage(dpy, eglImg);
    eglImg = EGL_NO_IMAGE_KHR;
    if (dst) vkDestroyImage(dev, dst, nullptr);
    if (dstMem) vkFreeMemory(dev, dstMem, nullptr);
    if (rbMap) vkUnmapMemory(dev, rbMem);
    if (rb) vkDestroyBuffer(dev, rb, nullptr);
    if (rbMem) vkFreeMemory(dev, rbMem, nullptr);
    dst = VK_NULL_HANDLE; dstMem = rbMem = VK_NULL_HANDLE; rb = VK_NULL_HANDLE; rbMap = nullptr;
    if (ahb) AHardwareBuffer_release(ahb);
    ahb = nullptr; tw = th = 0;
}

bool makeGlTarget(unsigned w, unsigned h) {
    GLint prevTex = 0, prevFbo = 0;
    glGetIntegerv(GL_TEXTURE_BINDING_2D, &prevTex); glGetIntegerv(GL_FRAMEBUFFER_BINDING, &prevFbo);
    glGenTextures(1, &tex); glBindTexture(GL_TEXTURE_2D, tex);
    if (ahbOk) pImageTarget(GL_TEXTURE_2D, (GLeglImageOES) eglImg);
    else glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, (GLsizei) w, (GLsizei) h);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glGenFramebuffers(1, &fbo); glBindFramebuffer(GL_FRAMEBUFFER, fbo);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex, 0);
    bool ok = glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE;
    glBindTexture(GL_TEXTURE_2D, (GLuint) prevTex); glBindFramebuffer(GL_FRAMEBUFFER, (GLuint) prevFbo);
    return ok;
}

bool makeTarget(unsigned w, unsigned h) {
    destroyTarget(eglGetCurrentContext() == glCtx);
    glCtx = eglGetCurrentContext();
    VkImageCreateInfo ci{VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO};
    ci.imageType = VK_IMAGE_TYPE_2D; ci.format = VK_FORMAT_R8G8B8A8_UNORM; ci.extent = {w, h, 1};
    ci.mipLevels = 1; ci.arrayLayers = 1; ci.samples = VK_SAMPLE_COUNT_1_BIT; ci.tiling = VK_IMAGE_TILING_OPTIMAL;
    ci.sharingMode = VK_SHARING_MODE_EXCLUSIVE; ci.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;

    if (ahbOk) {
        AHardwareBuffer_Desc d{}; d.width = w; d.height = h; d.layers = 1; d.format = AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM;
        d.usage = AHARDWAREBUFFER_USAGE_GPU_SAMPLED_IMAGE | AHARDWAREBUFFER_USAGE_GPU_COLOR_OUTPUT;
        VkAndroidHardwareBufferFormatPropertiesANDROID fp{VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_FORMAT_PROPERTIES_ANDROID};
        VkAndroidHardwareBufferPropertiesANDROID props{VK_STRUCTURE_TYPE_ANDROID_HARDWARE_BUFFER_PROPERTIES_ANDROID}; props.pNext = &fp;
        VkExternalMemoryImageCreateInfo ext{VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO};
        ext.handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_ANDROID_HARDWARE_BUFFER_BIT_ANDROID;
        ci.pNext = &ext;
        ci.usage = VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
        bool ok = AHardwareBuffer_allocate(&d, &ahb) == 0
            && pGetAhbProps(dev, ahb, &props) == VK_SUCCESS
            && vkCreateImage(dev, &ci, nullptr, &dst) == VK_SUCCESS;
        if (ok) {
            VkImportAndroidHardwareBufferInfoANDROID imp{VK_STRUCTURE_TYPE_IMPORT_ANDROID_HARDWARE_BUFFER_INFO_ANDROID}; imp.buffer = ahb;
            VkMemoryDedicatedAllocateInfo ded{VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO}; ded.pNext = &imp; ded.image = dst;
            VkMemoryAllocateInfo ai{VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO}; ai.pNext = &ded; ai.allocationSize = props.allocationSize;
            int mt = memType(props.memoryTypeBits, 0);
            ok = mt >= 0; ai.memoryTypeIndex = (uint32_t) std::max(mt, 0);
            ok = ok && vkAllocateMemory(dev, &ai, nullptr, &dstMem) == VK_SUCCESS && vkBindImageMemory(dev, dst, dstMem, 0) == VK_SUCCESS;
        }
        if (ok) {
            EGLDisplay dpy = eglGetCurrentDisplay();
            EGLint attrs[] = { EGL_IMAGE_PRESERVED_KHR, EGL_TRUE, EGL_NONE };
            eglImg = pCreateImage(dpy, EGL_NO_CONTEXT, EGL_NATIVE_BUFFER_ANDROID, pGetClientBuffer(ahb), attrs);
            ok = eglImg != EGL_NO_IMAGE_KHR && makeGlTarget(w, h);
        }
        if (ok) { tw = w; th = h; return true; }
        VKLOG("общий буфер не создался — запасной путь");
        destroyTarget(true);
        glCtx = eglGetCurrentContext();
        ahbOk = false;
        ci.pNext = nullptr;
    }

    // запасной путь: своя картинка + буфер в памяти процессора
    ci.usage = VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
    if (vkCreateImage(dev, &ci, nullptr, &dst) != VK_SUCCESS) return false;
    VkMemoryRequirements mr; vkGetImageMemoryRequirements(dev, dst, &mr);
    VkMemoryAllocateInfo ai{VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO}; ai.allocationSize = mr.size;
    int mt = memType(mr.memoryTypeBits, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT); if (mt < 0) mt = memType(mr.memoryTypeBits, 0);
    if (mt < 0) return false;
    ai.memoryTypeIndex = (uint32_t) mt;
    if (vkAllocateMemory(dev, &ai, nullptr, &dstMem) != VK_SUCCESS || vkBindImageMemory(dev, dst, dstMem, 0) != VK_SUCCESS) return false;

    VkBufferCreateInfo bi{VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO}; bi.size = (VkDeviceSize) w * h * 4; bi.usage = VK_BUFFER_USAGE_TRANSFER_DST_BIT;
    if (vkCreateBuffer(dev, &bi, nullptr, &rb) != VK_SUCCESS) return false;
    vkGetBufferMemoryRequirements(dev, rb, &mr);
    mt = memType(mr.memoryTypeBits, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT);
    rbCoherent = true;
    if (mt < 0) { mt = memType(mr.memoryTypeBits, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT); rbCoherent = false; }
    if (mt < 0) { mt = memType(mr.memoryTypeBits, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT); rbCoherent = true; }
    if (mt < 0) return false;
    ai = {VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO}; ai.allocationSize = mr.size; ai.memoryTypeIndex = (uint32_t) mt;
    if (vkAllocateMemory(dev, &ai, nullptr, &rbMem) != VK_SUCCESS || vkBindBufferMemory(dev, rb, rbMem, 0) != VK_SUCCESS) return false;
    if (vkMapMemory(dev, rbMem, 0, VK_WHOLE_SIZE, 0, &rbMap) != VK_SUCCESS) return false;
    if (!makeGlTarget(w, h)) return false;
    tw = w; th = h;
    return true;
}

VkImageMemoryBarrier barrier(VkImage img, VkImageLayout from, VkImageLayout to, VkAccessFlags srcA, VkAccessFlags dstA,
                             uint32_t srcQ = VK_QUEUE_FAMILY_IGNORED, uint32_t dstQ = VK_QUEUE_FAMILY_IGNORED) {
    VkImageMemoryBarrier b{VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER};
    b.image = img; b.oldLayout = from; b.newLayout = to; b.srcAccessMask = srcA; b.dstAccessMask = dstA;
    b.srcQueueFamilyIndex = srcQ; b.dstQueueFamilyIndex = dstQ;
    b.subresourceRange = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1};
    return b;
}

} // namespace

// ---------- вызовы из LibretroDroid ----------
extern "C" {

bool kapsula_vk_wanted() { return wanted && !failed; }
bool kapsula_vk_active() { return active; }

/** Окружение ядра: ответ на запросы, связанные с Vulkan. true — обработано. */
bool kapsula_vk_environment(unsigned cmd, void* data, bool* result) {
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_HW_RENDER_INTERFACE:
            if (!active) { *result = false; return true; }
            *(const retro_hw_render_interface**) data = (const retro_hw_render_interface*) &iface;
            *result = true; return true;
        case RETRO_ENVIRONMENT_SET_HW_RENDER_CONTEXT_NEGOTIATION_INTERFACE: {
            auto* n = (const retro_hw_render_context_negotiation_interface*) data;
            if (n && n->interface_type == RETRO_HW_RENDER_CONTEXT_NEGOTIATION_INTERFACE_VULKAN) {
                neg = (const retro_hw_render_context_negotiation_interface_vulkan*) data;
                *result = true;
            } else *result = false;
            return true;
        }
        case RETRO_ENVIRONMENT_GET_HW_RENDER_CONTEXT_NEGOTIATION_INTERFACE_SUPPORT: {
            auto* s = (retro_hw_render_context_negotiation_interface*) data;
            if (s->interface_type == RETRO_HW_RENDER_CONTEXT_NEGOTIATION_INTERFACE_VULKAN) {
                s->interface_version = RETRO_HW_RENDER_CONTEXT_NEGOTIATION_INTERFACE_VULKAN_VERSION;
                *result = true;
            } else { s->interface_version = 0; *result = false; }
            return true;
        }
        default: return false;
    }
}

/** Ядро попросило контекст: true — это Vulkan и мы его берём (GL-кадр LibretroDroid остаётся для показа). */
bool kapsula_vk_set_hw_render(retro_hw_render_callback* cb) {
    if (cb->context_type != RETRO_HW_CONTEXT_VULKAN) return false;
    coreReset = cb->context_reset;
    return true;
}

/** Поверхность готова (GL-поток). 0 — ядро не на Vulkan (обычный GL), 1 — Vulkan работает, 2 — Vulkan не запустился. */
int kapsula_vk_surface_created() {
    if (coreReset == nullptr) return 0;
    if (!wanted || failed) return 2;
    loadEgl();
    if (!active) {
        if (!createAll()) { failed = true; VKLOG("Vulkan не запустился"); return 2; }
        active = true;
        coreReset();
    }
    return 1;
}

/** Кадр готов: копируем картинку ядра в GL-кадр dstFbo. flip — LibretroDroid ждёт начало снизу. */
bool kapsula_vk_present(unsigned w, unsigned h, unsigned dstFbo, bool flip) {
    if (!active || w == 0 || h == 0) return false;
    retro_vulkan_image img; std::vector<VkSemaphore> sems; std::vector<VkCommandBuffer> extra; VkSemaphore sig;
    {
        std::lock_guard<std::mutex> l(imx);
        if (!haveImg) return false;
        img = coreImg; sems = waitSems; waitSems.clear(); extra = extraCmds; extraCmds.clear(); sig = signalSem; signalSem = VK_NULL_HANDLE;
    }
    if (tw != w || th != h || eglGetCurrentContext() != glCtx) {
        if (!makeTarget(w, h)) { VKLOG("не удалось подготовить кадр %ux%u", w, h); destroyTarget(true); return false; }
    }
    EGLDisplay dpy = eglGetCurrentDisplay();
    // OpenGL должен дочитать прошлый кадр из общего буфера, прежде чем Vulkan запишет новый
    if (glFence != EGL_NO_SYNC_KHR) { pWaitSync(dpy, glFence, EGL_SYNC_FLUSH_COMMANDS_BIT_KHR, EGL_FOREVER_KHR); pDestroySync(dpy, glFence); glFence = EGL_NO_SYNC_KHR; }

    uint32_t i = slot.load();
    if (fenceBusy[i]) { vkWaitForFences(dev, 1, &fences[i], VK_TRUE, UINT64_MAX); vkResetFences(dev, 1, &fences[i]); fenceBusy[i] = false; }
    VkCommandBuffer cb = cmds[i];
    vkResetCommandBuffer(cb, 0);
    VkCommandBufferBeginInfo bi{VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO}; bi.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    vkBeginCommandBuffer(cb, &bi);

    VkImage src = img.create_info.image;
    VkImageLayout srcLayout = img.image_layout;
    VkImageMemoryBarrier pre[2] = {
        barrier(src, srcLayout, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, VK_ACCESS_MEMORY_WRITE_BIT, VK_ACCESS_TRANSFER_READ_BIT),
        // общий буфер забираем у OpenGL (старое содержимое не нужно — кадр перезаписывается целиком)
        ahbOk ? barrier(dst, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 0, VK_ACCESS_TRANSFER_WRITE_BIT, VK_QUEUE_FAMILY_EXTERNAL, qfam)
              : barrier(dst, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 0, VK_ACCESS_TRANSFER_WRITE_BIT),
    };
    vkCmdPipelineBarrier(cb, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, 0, nullptr, 0, nullptr, 2, pre);

    VkImageBlit blit{};
    blit.srcSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1};
    blit.dstSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1};
    blit.srcOffsets[1] = {(int32_t) w, (int32_t) h, 1};
    blit.dstOffsets[1] = {(int32_t) w, (int32_t) h, 1};
    vkCmdBlitImage(cb, src, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, dst, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, &blit, VK_FILTER_NEAREST);

    VkImageMemoryBarrier back = barrier(src, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, srcLayout, VK_ACCESS_TRANSFER_READ_BIT, VK_ACCESS_MEMORY_READ_BIT);
    if (ahbOk) {
        // отдаём буфер OpenGL
        VkImageMemoryBarrier rel = barrier(dst, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_GENERAL,
                                           VK_ACCESS_TRANSFER_WRITE_BIT, 0, qfam, VK_QUEUE_FAMILY_EXTERNAL);
        VkImageMemoryBarrier post[2] = {back, rel};
        vkCmdPipelineBarrier(cb, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, 0, 0, nullptr, 0, nullptr, 2, post);
    } else {
        VkImageMemoryBarrier toSrc = barrier(dst, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                                             VK_ACCESS_TRANSFER_WRITE_BIT, VK_ACCESS_TRANSFER_READ_BIT);
        VkImageMemoryBarrier post[2] = {back, toSrc};
        vkCmdPipelineBarrier(cb, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT | VK_PIPELINE_STAGE_TRANSFER_BIT, 0, 0, nullptr, 0, nullptr, 2, post);
        VkBufferImageCopy c{};
        c.imageSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1};
        c.imageExtent = {w, h, 1};
        vkCmdCopyImageToBuffer(cb, dst, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, rb, 1, &c);
        VkBufferMemoryBarrier hb{VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER};
        hb.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT; hb.dstAccessMask = VK_ACCESS_HOST_READ_BIT;
        hb.srcQueueFamilyIndex = hb.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED; hb.buffer = rb; hb.size = VK_WHOLE_SIZE;
        vkCmdPipelineBarrier(cb, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_HOST_BIT, 0, 0, nullptr, 1, &hb, 0, nullptr);
    }
    vkEndCommandBuffer(cb);

    extra.push_back(cb);
    std::vector<VkPipelineStageFlags> stages(sems.size(), VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
    VkSubmitInfo si{VK_STRUCTURE_TYPE_SUBMIT_INFO};
    si.waitSemaphoreCount = (uint32_t) sems.size(); si.pWaitSemaphores = sems.data(); si.pWaitDstStageMask = stages.data();
    si.commandBufferCount = (uint32_t) extra.size(); si.pCommandBuffers = extra.data();
    if (sig) { si.signalSemaphoreCount = 1; si.pSignalSemaphores = &sig; }
    VkResult r;
    { std::lock_guard<std::mutex> l(qmx); r = vkQueueSubmit(queue, 1, &si, fences[i]); }
    if (r != VK_SUCCESS) { VKLOG("vkQueueSubmit: %d", r); return false; }
    fenceBusy[i] = true;
    vkWaitForFences(dev, 1, &fences[i], VK_TRUE, UINT64_MAX);
    slot.store((i + 1) % SLOTS);

    GLint prevFbo = 0, prevTex = 0; glGetIntegerv(GL_FRAMEBUFFER_BINDING, &prevFbo); glGetIntegerv(GL_TEXTURE_BINDING_2D, &prevTex);
    GLboolean scissor = glIsEnabled(GL_SCISSOR_TEST);
    if (!ahbOk) {
        if (!rbCoherent) { VkMappedMemoryRange mr{VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE}; mr.memory = rbMem; mr.size = VK_WHOLE_SIZE; vkInvalidateMappedMemoryRanges(dev, 1, &mr); }
        glBindTexture(GL_TEXTURE_2D, tex);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, (GLsizei) w, (GLsizei) h, GL_RGBA, GL_UNSIGNED_BYTE, rbMap);
    }
    if (scissor) glDisable(GL_SCISSOR_TEST);
    glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo);
    glBindFramebuffer(GL_DRAW_FRAMEBUFFER, dstFbo);
    // в общем буфере верх картинки — первая строка (у OpenGL это низ текстуры)
    glBlitFramebuffer(0, 0, (GLint) w, (GLint) h, 0, flip ? (GLint) h : 0, (GLint) w, flip ? 0 : (GLint) h, GL_COLOR_BUFFER_BIT, GL_NEAREST);
    if (ahbOk && pCreateSync) glFence = pCreateSync(dpy, EGL_SYNC_FENCE_KHR, nullptr);
    if (scissor) glEnable(GL_SCISSOR_TEST);
    glBindFramebuffer(GL_FRAMEBUFFER, (GLuint) prevFbo);
    glBindTexture(GL_TEXTURE_2D, (GLuint) prevTex);
    return true;
}

/** После выгрузки ядра: всё закрыть. */
void kapsula_vk_shutdown() {
    if (!active) { coreReset = nullptr; neg = nullptr; return; }
    destroyTarget(eglGetCurrentContext() != EGL_NO_CONTEXT && eglGetCurrentContext() == glCtx);
    if (dev) {
        vkDeviceWaitIdle(dev);
        for (auto& f : fences) if (f) { vkDestroyFence(dev, f, nullptr); f = VK_NULL_HANDLE; }
        if (pool) vkDestroyCommandPool(dev, pool, nullptr);
        pool = VK_NULL_HANDLE;
        if (neg && neg->destroy_device) neg->destroy_device();
        vkDestroyDevice(dev, nullptr);
    }
    if (inst) vkDestroyInstance(inst, nullptr);
    dev = VK_NULL_HANDLE; inst = VK_NULL_HANDLE; gpu = VK_NULL_HANDLE; queue = VK_NULL_HANDLE;
    for (auto& b : fenceBusy) b = false;
    slot = 0; haveImg = false; active = false; coreReset = nullptr; neg = nullptr; glCtx = EGL_NO_CONTEXT;
}

// ---------- JNI для приложения ----------
JNIEXPORT void JNICALL Java_com_nexus_psp_emulator_Vk_nSetWanted(JNIEnv*, jclass, jboolean on) { wanted = on; failed = false; }
JNIEXPORT jboolean JNICALL Java_com_nexus_psp_emulator_Vk_nActive(JNIEnv*, jclass) { return active; }

/** Проверка телефона: Vulkan 1.1+ и общий буфер с OpenGL. 0 — подходит, иначе код причины. */
JNIEXPORT jint JNICALL Java_com_nexus_psp_emulator_Vk_nProbe(JNIEnv*, jclass) {
    uint32_t ver = loaderVersion();
    if (VK_API_VERSION_MAJOR(ver) < 1 || (VK_API_VERSION_MAJOR(ver) == 1 && VK_API_VERSION_MINOR(ver) < 1)) return 1;
    VkApplicationInfo app{VK_STRUCTURE_TYPE_APPLICATION_INFO}; app.apiVersion = VK_API_VERSION_1_1;
    VkInstanceCreateInfo ci{VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO}; ci.pApplicationInfo = &app;
    VkInstance in = VK_NULL_HANDLE;
    if (vkCreateInstance(&ci, nullptr, &in) != VK_SUCCESS) return 2;
    uint32_t n = 0; vkEnumeratePhysicalDevices(in, &n, nullptr);
    jint res = 3;
    if (n > 0) {
        std::vector<VkPhysicalDevice> g(n); vkEnumeratePhysicalDevices(in, &n, g.data());
        VkPhysicalDeviceProperties p; vkGetPhysicalDeviceProperties(g[0], &p);
        uint32_t e = 0; vkEnumerateDeviceExtensionProperties(g[0], nullptr, &e, nullptr);
        std::vector<VkExtensionProperties> av(e); vkEnumerateDeviceExtensionProperties(g[0], nullptr, &e, av.data());
        bool ahbExt = false; for (auto& x : av) if (!strcmp(x.extensionName, "VK_ANDROID_external_memory_android_hardware_buffer")) ahbExt = true;
        VKLOG("проверка: %s, Vulkan %u.%u, общий буфер %d", p.deviceName, VK_API_VERSION_MAJOR(p.apiVersion), VK_API_VERSION_MINOR(p.apiVersion), ahbExt);
        res = (VK_API_VERSION_MINOR(p.apiVersion) >= 1 || VK_API_VERSION_MAJOR(p.apiVersion) > 1) ? (ahbExt ? 0 : 4) : 5;
    }
    vkDestroyInstance(in, nullptr);
    return res;
}

}
