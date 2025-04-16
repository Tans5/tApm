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
#include "../time/tapm_time.h"
#include "../thread/tapm_thread.h"


static int32_t hookWriteMethod(bool isAddHook);

static size_t my_write(int fd, const void * const buf, size_t count);

typedef struct HandleAnrDataThreadArgs{
    char anrTraceFile[MAX_STR_SIZE * 2] {};
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
            char crashFileName[MAX_STR_SIZE];
            formatTime(writingAnrData->anrTime, crashFileName, MAX_STR_SIZE);
            sprintf(args->anrTraceFile, "%s/%s", anrMonitor->anrOutputDir, crashFileName);
            auto traceFileFd = open(args->anrTraceFile, O_CREAT | O_RDWR, 0666);
            if (traceFileFd <= 0) {
                LOGE("Create anr trace file fail.");
                delete args;
            } else {
                int writeCount = origin_write(traceFileFd, buf, count);
                close(traceFileFd);
                if (writeCount <= 0) {
                    LOGE("Write anr trace file fail.");
                    delete args;
                } else {
                    pthread_t t;
                    pthread_attr_t attr;
                    if (pthread_attr_init(&attr) == 0 && pthread_attr_setstacksize(&attr, MAX_THREAD_STACK_SIZE) == 0) {
                        pthread_create(&t, &attr, handleAnrDataThread, args);
                    } else {
                        LOGE("Init thread attr fail.");
                    }
                    pthread_attr_destroy(&attr);
                }
            }
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
            auto jAnrMonitor = workingAnrMonitor->jAnrMonitor;
            auto jAnrMonitorClazz = env->GetObjectClass(jAnrMonitor);
            // auto jAnrMonitorClazz = env->FindClass("com/tans/tapm/monitors/AnrMonitor");
            auto anrMethodId = env->GetMethodID(jAnrMonitorClazz, "onAnr", "(JZLjava/lang/String;)V");
            auto anrStackTrackFilePath = env->NewStringUTF(arg->anrTraceFile);
            env->CallVoidMethod(jAnrMonitor, anrMethodId, writingAnrData->anrTime, writingAnrData->isFromMe, anrStackTrackFilePath);
        }
    } else {
        LOGE("Wrong state, can't handle anr trace data.");
    }
    delete arg;
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

            anrData->anrTime = nowInMillis();
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

int32_t Anr::prepare(JNIEnv *jniEnv, jobject j_AnrObject, jstring j_AnrFileOutputDir) {
    if (!isInited) { init(); }
    pthread_mutex_lock(&lock);

    jniEnv->GetJavaVM(&this->jvm);
    this->jAnrMonitor = jniEnv->NewGlobalRef(j_AnrObject);
    this->anrOutputDir = strdup(jniEnv->GetStringUTFChars(j_AnrFileOutputDir, JNI_FALSE));
    tApmThread signalCatcher;
    int ret = findThreadByName(getpid(), "Signal Catcher", &signalCatcher);

    this->signalCatcherTid = signalCatcher.tid;
    struct sigaction newSigaction {};
    if (ret != 0) {
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
    if (this->jAnrMonitor != nullptr) {
        jniEnv->DeleteGlobalRef(this->jAnrMonitor);
        this->jAnrMonitor = nullptr;
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

    if (this->anrOutputDir != nullptr) {
        free(this->anrOutputDir);
        this->anrOutputDir = nullptr;
    }

    pthread_mutex_unlock(&lock);
}