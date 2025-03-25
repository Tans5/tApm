//
// Created by pengcheng.tan on 2025/3/25.
//
#include <cstring>
#include <pthread.h>
#include <malloc.h>
#include "crash.h"
#include "../time/tapm_time.h"
#include "../tapm_log.h"

static pthread_mutex_t lock;
static volatile bool isInited = false;
static void init() {
    if (!isInited) {
        isInited = true;
        pthread_mutex_init(&lock, nullptr);
    }
}
static volatile bool isCrashed = false;


static volatile Crash * workingMonitor = nullptr;

static void crashSignalHandler(int sig, siginfo_t *sig_info, void *uc) {
    auto *monitor = workingMonitor;
    if (monitor != nullptr && !isCrashed) {
        isCrashed = true;
        auto crashTime = nowInMillis();
        int ret = pthread_mutex_trylock(&lock);
        if (ret == 0) {
            LOGD("Receive crash sig: %d", sig);
            // TODO:


            pthread_mutex_unlock(&lock);
        }
    }
}

int32_t Crash::prepare(JNIEnv *jniEnv, jobject jCrashMonitorP, jstring crashFileDir) {
    init();
    pthread_mutex_lock(&lock);
    this->startTime = nowInMillis();
    jniEnv->GetJavaVM(&this->jvm);
    this->jCrashMonitor = jniEnv->NewGlobalRef(jCrashMonitorP);
    this->crashOutputDir = strdup(jniEnv->GetStringUTFChars(crashFileDir, JNI_FALSE));

    struct sigaction newSigAction {};
    int32_t ret = 0;

    // Create new signal stack size
    stack_t newSignalStack;
    this->newSignalStackBuffer = malloc(SIGNAL_STACK_SIZE);
    newSignalStack.ss_sp = this->newSignalStackBuffer;
    newSignalStack.ss_size = SIGNAL_STACK_SIZE;
    newSignalStack.ss_flags = 0;
    auto *oldSS = new stack_t;
    ret = sigaltstack(&newSignalStack, oldSS);
    if (0 != ret) {
        delete oldSS;
        ret = -1;
        LOGE("Set new signal stack fail.");
        goto End;
    } else {
        this->oldSignalStack = oldSS;
    }

    // Register crash signal action
    memset(&newSigAction, 0, sizeof(newSigAction));
    sigfillset(&newSigAction.sa_mask);
    newSigAction.sa_sigaction = crashSignalHandler;
    newSigAction.sa_flags =  SA_RESTART | SA_SIGINFO | SA_ONSTACK;
    this->oldCrashSignalActions = new LinkedList;
    for (int sig : CRASH_SIGNAL) {
        auto oldSigAction = new OldCrashSignalAction;
        oldSigAction->signal = sig;
        if (sigaction(sig, &newSigAction, &oldSigAction->action) == 0) {
            this->oldCrashSignalActions->addToLast(oldSigAction);
        } else {
            LOGE("Register crash action: %d, fail.", sig);
            delete oldSigAction;
        }
    }
    if (oldCrashSignalActions->size == 0) {
        LOGE("No crash signals registered.");
        ret = -1;
        goto End;
    } else {
        ret = 0;
    }

    workingMonitor = this;

    End:
    pthread_mutex_unlock(&lock);
    if (ret != 0) {
        return -1;
    } else {
        return 0;
    }
}

void Crash::release(JNIEnv *jniEnv) {
    pthread_mutex_lock(&lock);
    this->jvm = nullptr;
    if (this->jCrashMonitor != nullptr) {
        jniEnv->DeleteGlobalRef(this->jCrashMonitor);
        this->jCrashMonitor = nullptr;
    }

    if (this->crashOutputDir != nullptr) {
        delete this->crashOutputDir;
        this->crashOutputDir = nullptr;
    }
    if (oldSignalStack != nullptr) {
        sigaltstack(oldSignalStack, nullptr);
        delete oldSignalStack;
        this->oldSignalStack = nullptr;
    }
    if (newSignalStackBuffer != nullptr) {
        free(newSignalStackBuffer);
        this->newSignalStackBuffer = nullptr;
    }
    if (oldCrashSignalActions != nullptr) {
        while (oldCrashSignalActions->size > 0) {
            auto oldAction = static_cast<OldCrashSignalAction *>(oldCrashSignalActions->popFirst());
            sigaction(oldAction->signal, &oldAction->action, nullptr);
            delete oldAction;
        }
        delete oldCrashSignalActions;
        this->oldCrashSignalActions = nullptr;
    }

    delete this;
    workingMonitor = nullptr;
    pthread_mutex_unlock(&lock);
}
