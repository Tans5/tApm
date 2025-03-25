//
// Created by pengcheng.tan on 2025/3/25.
//
#include <cstring>
#include <pthread.h>
#include <malloc.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/prctl.h>
#include <cerrno>
#include <cstdlib>
#include <sys/wait.h>
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
            auto crashedPid = getpid();
            auto crashedTid = gettid();
            pid_t childPid;
            siginfo_t sigInfoCopy;
            ucontext_t uContextCopy;
            memcpy(&sigInfoCopy, sig_info, sizeof(sigInfoCopy));
            memcpy(&uContextCopy, uc, sizeof(uContextCopy));


            ret = prctl(PR_SET_DUMPABLE, 1);
            if (ret != 0) {
                LOGE("Set progress dumpable fail: %d", ret);
                goto End;
            }
            errno = 0;
            ret = prctl(PR_SET_PTRACER, PR_SET_PTRACER_ANY);
            if (ret != 0 && errno != EINVAL) {
                LOGE("Set process tracer fail: %d", ret);
                ret = -1;
            } else {
                ret = 0;
            }
            char crashFileName[32];
            formatTime(crashTime, crashFileName, 64);
            char crashFilePath[256];
            sprintf(crashFilePath, "%s/%s", monitor->crashOutputDir, crashFileName);
            childPid = fork();
            if (childPid == 0) {
                // ChildProcess
                LOGD("Child process started.");
                alarm(30);
                int childProcessRet = 0;
                auto crashFileFd = open(crashFilePath, O_CREAT | O_RDWR, 0666);
                if (crashFileFd == -1) {
                    LOGE("Create crash file fail");
                    childProcessRet = -1;
                    goto  ChildProcessEnd;
                }

                // TODO:

                ChildProcessEnd:
                if (crashFileFd != -1) {
                    close(crashFileFd);
                }
                _Exit(childProcessRet);
            } else if (childPid > 0) {
                LOGD("Waiting child process finish work.");
                // ParentProcess.
                int childProcessStatus;
                // Waiting child process finish work.
                waitpid(childPid, &childProcessStatus, __WALL);
                LOGD("Child process finished: %d", childProcessStatus);
                if (childProcessStatus == 0) {
                    ret = 0;
                } else {
                    ret = -1;
                }
            } else {
                // Error;
                LOGE("Create child process fail: %d", childPid);
                ret = -1;
            }

            End:
            if (ret == 0) {
                // TODO: success.
            } else {
                // TODO: fail.
            }
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

    LOGD("Crash monitor prepared.");

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
