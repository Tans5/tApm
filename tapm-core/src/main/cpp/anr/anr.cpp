//
// Created by pengcheng.tan on 2025/3/18.
//
#include <malloc.h>
#include <cstring>
#include <unistd.h>
#include <dirent.h>
#include <fcntl.h>
#include <syscall.h>
#include <sys/epoll.h>
#include <sys/time.h>
#include <sys/eventfd.h>
#include <pthread.h>

#include "anr.h"
#include "../tapm_log.h"
#include "xhook.h"

static bool isNumberStr(const char* str, int maxLen);

static int64_t getTimeMillis();

static int32_t getSignalCatcherTid();

static int32_t hookWriteMethod(bool isAddHook);

static size_t my_write(int fd, const void * const buf, size_t count);

typedef struct HandleAnrDataThreadArgs{
    void * data = nullptr;
    size_t dataLen = 0;
} HandleAnrDataThreadArgs;

static void* handleAnrDataThread(void * arg);

static void anrSignalHandler(int sig, siginfo_t * sig_info, void *uc);

static pthread_mutex_t lock;
static volatile bool isInited = false;
static void init() {
    if (!isInited) {
        isInited = true;
        pthread_mutex_init(&lock, nullptr);
    }
}

static volatile Anr* workingAnrMonitor = nullptr;

static volatile AnrData* writingAnrData = nullptr;

ssize_t (*origin_write)(int fd, const void *const buf, size_t count) = write;

static bool isNumberStr(const char *str, int maxLen) {
    for (int i = 0; i < maxLen; i ++) {
        char c = str[i];
        if (c == '\0') {
            break;
        }
        if (c >= '0' && c <= '9') {
            continue;
        } else {
            return false;
        }
    }
    return true;
}

static int64_t getTimeMillis() {
    struct timeval tv{};
    gettimeofday(&tv, nullptr);
    return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

static int32_t getSignalCatcherTid() {
    pid_t myPid = getpid();
    char processPath[SIGNAL_CATCHER_BUFFER_SIZE];
    int size = sprintf(processPath, "/proc/%d/task", myPid);
    if (size >= SIGNAL_CATCHER_BUFFER_SIZE) {
        LOGE("Read proc path fail, read buffer size: %d", size);
        return -1;
    }
    DIR *processDir = opendir(processPath);
    if (processDir) {
        int32_t tid = -1;
        dirent * child = readdir(processDir);
        while (child != nullptr) {
            if (isNumberStr(child->d_name, 256)) {
                char filePath[SIGNAL_CATCHER_BUFFER_SIZE];
                size = sprintf(filePath, "%s/%s/comm", processPath, child->d_name);
                if (size >= SIGNAL_CATCHER_BUFFER_SIZE) {
                    continue;
                }
                char threadName[SIGNAL_CATCHER_BUFFER_SIZE];
                int fd = open(filePath, O_RDONLY);
                size = read(fd, threadName, SIGNAL_CATCHER_BUFFER_SIZE);
                close(fd);
                threadName[size - 1] = '\0';
                if (strcmp(threadName, "Signal Catcher") == 0) {
                    tid = atoi(child->d_name);
                    break;
                }
            }
            child = readdir(processDir);
        }
        closedir(processDir);
        return tid;
    } else {
        LOGE("Read process dir fail.");
    }
    return - 1;
}

static int32_t hookWriteMethod(bool isAddHook) {
    int apiLevel = android_get_device_api_level();
    const char *writeLibName;
    if (apiLevel >= 30 || apiLevel == 25 || apiLevel == 24) {
        writeLibName = ".*/libc\\.so$";
    } else if (apiLevel == 29) {
        writeLibName = ".*/libbase\\.so$";
    } else {
        writeLibName = ".*/libart\\.so$";
    }
    int ret = 0;
    xhook_clear();
    if (isAddHook) {
        ret = xhook_register(writeLibName, "write", (void *)my_write, nullptr);
    } else {
        ret = xhook_register(writeLibName, "write", (void *)origin_write, nullptr);
    }
    xhook_refresh(false);
    if (ret == 0) {
        if (isAddHook) {
            LOGD("Hook write symbol success.");
        } else {
            LOGD("Remove hook write symbol success.");
        }
        return 0;
    } else {
        if (isAddHook) {
            LOGE("Hook write symbol fail.");
        } else {
            LOGE("Remove hook write symbol fail.");
        }
        return -1;
    }
}

static size_t my_write(int fd, const void *const buf, size_t count) {
    auto anrMonitor = workingAnrMonitor;
    if (anrMonitor != nullptr && gettid() == anrMonitor->signalCatcherTid) {
        LOGD("Signal catcher is writing anr data.");
        if (writingAnrData != nullptr) {
            char *copy = static_cast<char *>(malloc(count));
            memcpy(copy, buf, count);
            auto args = new HandleAnrDataThreadArgs;
            args->data = copy;
            args->dataLen = count;
            pthread_t t;
            pthread_create(&t, nullptr, handleAnrDataThread, args);
        }
    }
    return origin_write(fd, buf, count);
}

static void* handleAnrDataThread(void * arg_v) {
    pthread_mutex_lock(&lock);
    auto arg = static_cast<HandleAnrDataThreadArgs*>(arg_v);
    if (writingAnrData != nullptr && workingAnrMonitor != nullptr) {
        LOGD("Receive anr trace data.");
        JNIEnv *env = nullptr;
        workingAnrMonitor->jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
        if (env == nullptr) {
            JavaVMAttachArgs jvmAttachArgs {
                    .version = JNI_VERSION_1_6,
                    .name = "HandleAnrDataThread",
                    .group = nullptr
            };
            auto result = workingAnrMonitor->jvm->AttachCurrentThread(&env, &jvmAttachArgs);
            if (result != JNI_OK) {
                LOGE("Attach java thread fail.");
                env = nullptr;
            }
        }
        if (env != nullptr) {
            auto jAnrMonitor = workingAnrMonitor->jAnrObject;
            auto jAnrMonitorClazz = env->GetObjectClass(jAnrMonitor);
            // auto jAnrMonitorClazz = env->FindClass("com/tans/tapm/monitors/AnrMonitor");
            auto anrMethodId = env->GetMethodID(jAnrMonitorClazz, "onAnr", "(JZLjava/lang/String;)V");
            auto anrStackTrackJString = env->NewStringUTF(reinterpret_cast<const char *>(static_cast<const jchar *>(arg->data)));
            env->CallVoidMethod(jAnrMonitor, anrMethodId, writingAnrData->anrTime, writingAnrData->isFromMe, anrStackTrackJString);
        }
    } else {
        LOGE("Wrong state, can't handle anr trace data.");
    }
    free(arg->data);
    free(arg);
    writingAnrData = nullptr;
    hookWriteMethod(false);
    pthread_mutex_unlock(&lock);
    return nullptr;
}

static void anrSignalHandler(int sig, siginfo_t *sig_info, void *uc) {
    auto anrMonitor = workingAnrMonitor;
    if (anrMonitor != nullptr) {
        if (sig == SIGQUIT) {
            pthread_mutex_lock(&lock);

            anrMonitor = workingAnrMonitor;

            int fromPid1 = sig_info->_si_pad[3];
            int fromPid2 = sig_info->_si_pad[4];
            int myPid = getpid();
            int ret;
            bool isSigFromMe = myPid == fromPid1 || myPid == fromPid2;
            auto anrData = new AnrData;
            LOGD("Receive SIGQUIT isFromMe=%d.", isSigFromMe);

            if (anrMonitor == nullptr) {
                LOGE("Wrong state, anr monitor is null.");
                goto End;
            }

            if (writingAnrData != nullptr) {
                LOGE("Wrong state, writing anr data is not null.");
                goto End;
            }

            ret = hookWriteMethod(true);
            if (ret != 0) {
                syscall(SYS_tgkill, myPid, anrMonitor->signalCatcherTid, SIGQUIT);
                LOGE("Hook write method fail.");
                goto End;
            }

            anrData->anrTime = getTimeMillis();
            anrData->isFromMe = isSigFromMe;
            writingAnrData = anrData;

            // Send SIGQUIT to signal catcher.
            syscall(SYS_tgkill, myPid, anrMonitor->signalCatcherTid, SIGQUIT);

            End:
            pthread_mutex_unlock(&lock);
        } else {
            LOGE("Receive sig: %d, but not SIGQUIT.", sig);
        }
    } else {
        LOGE("Receive SIGQUIT, but monitor is null.");
    }
}

int32_t Anr::prepare(JNIEnv *jniEnv, jobject j_AnrObject) {
    if (!isInited) { init(); }
    pthread_mutex_lock(&lock);

    jniEnv->GetJavaVM(&this->jvm);
    this->jAnrObject = jniEnv->NewGlobalRef(j_AnrObject);

    this->signalCatcherTid = getSignalCatcherTid();
    int32_t ret = 0;
    struct sigaction newSigaction {};
    if (this->signalCatcherTid <= 0) {
        LOGE("Get signal catcher tid fail.");
        ret = -1;
        goto End;
    }
    LOGD("Get signal catcher tid: %d", this->signalCatcherTid);

    sigset_t sigSets;
    sigemptyset(&sigSets);
    sigaddset(&sigSets, SIGQUIT);
    this->oldBlockSigSets = new sigset_t;
    sigemptyset(this->oldBlockSigSets);
    ret = pthread_sigmask(SIG_UNBLOCK, &sigSets, this->oldBlockSigSets);
    if (ret != 0) {
        delete this->oldBlockSigSets;
        this->oldBlockSigSets = nullptr;
        LOGE("Unblock SIGQUIT fail: %d", ret);
        ret = -1;
        goto End;
    }
    LOGD("Unblock SIGQUIT success.");

    sigfillset(&newSigaction.sa_mask);
    newSigaction.sa_flags = SA_RESTART | SA_ONSTACK | SA_SIGINFO;
    newSigaction.sa_sigaction = anrSignalHandler;
    this->oldQuitSigAction = new sigaction_p;
    ret = sigaction(SIGQUIT, &newSigaction, this->oldQuitSigAction);
    if (ret != 0) {
        delete this->oldQuitSigAction;
        this->oldQuitSigAction = nullptr;
        LOGE("Register new SIGQUIT action fail: %d", ret);
        goto End;
    }
    LOGD("Register new SIGQUIT action success.");

    End:
    if (ret == 0) {
        workingAnrMonitor = this;
    } else {
        workingAnrMonitor = nullptr;
    }
    pthread_mutex_unlock(&lock);
    return ret;
}

void Anr::release(JNIEnv *jniEnv) {
    pthread_mutex_lock(&lock);
    workingAnrMonitor = nullptr;
    this->jvm = nullptr;
    if (this->jAnrObject != nullptr) {
        jniEnv->DeleteGlobalRef(this->jAnrObject);
        this->jAnrObject = nullptr;
    }

    this->signalCatcherTid = -1;

    if (this->oldBlockSigSets != nullptr) {
        pthread_sigmask(SIG_BLOCK, this->oldBlockSigSets, nullptr);
        delete this->oldBlockSigSets;
        this->oldBlockSigSets = nullptr;
    }

    if (this->oldQuitSigAction != nullptr) {
        sigaction(SIGQUIT, this->oldQuitSigAction, nullptr);
        delete this ->oldQuitSigAction;
        this->oldQuitSigAction = nullptr;
    }
    pthread_mutex_unlock(&lock);
    delete this;
}