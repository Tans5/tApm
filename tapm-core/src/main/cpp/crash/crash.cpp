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
#include <sys/ptrace.h>
#include <iostream>
#include "crash.h"
#include "../time/tapm_time.h"
#include "../tapm_log.h"
#include "../thread/tapm_thread.h"
#include "thread_control.h"
#include "../crash/memory_maps.h"
#include "file_mmap.h"
#include "t_elf.h"
#include "memory_maps.h"
#include "t_unwind.h"
#include "crash_writer.h"

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

static int handleCrash(CrashSignal *crashSignal) {
    int ret = 0;
    LinkedList crashedProcessThreads;
    LinkedList crashedProcessThreadsStatus;
    ThreadStatus *crashedThreadStatus = nullptr;
    LinkedList memoryMaps;

    // Get all threads
    getProcessThreads(crashSignal->crashPid, &crashedProcessThreads);

    // Suspend all threads
    initThreadStatus(&crashedProcessThreads, crashSignal->crashTid, &crashedProcessThreadsStatus, &crashedThreadStatus);
    if (crashedThreadStatus == nullptr) {
        ret = -1;
        LOGE("Don't find crash thread.");
        goto End;
    }
    suspendThreads(&crashedProcessThreadsStatus);

    // Parse memory maps.
    parseMemoryMaps(crashSignal->crashPid, &memoryMaps);

    // Read all threads register value.
    readThreadsRegs(&crashedProcessThreadsStatus, crashSignal->crashTid, &crashSignal->userContext);

    ret = writeCrash(
            crashSignal->sig,
            &crashSignal->sigInfo,
            &crashSignal->userContext,
            crashSignal->startTime,
            crashSignal->crashTime,
            crashSignal->crashPid,
            crashSignal->crashTid,
            crashSignal->crashUid,
            crashSignal->crashFilePath,
            &memoryMaps,
            &crashedProcessThreadsStatus,
            crashedThreadStatus);

    End:
    resumeThreads(&crashedProcessThreadsStatus);
    recycleProcessThreads(&crashedProcessThreads);
    recycleThreadsStatus(&crashedProcessThreadsStatus);
    recycleMemoryMaps(&memoryMaps);
    return ret;
}

void* handleCrashThread(void * args) {
    int ret = handleCrash(static_cast<CrashSignal *>(args));
    auto b = malloc(sizeof(int));
    memcpy(b, &ret, sizeof(int));
    return b;
}

static void* reportJavaThread(void * args) {
    auto crashSignal = static_cast<CrashSignal *>(args);
    auto *monitor = workingMonitor;
    if (monitor != nullptr) {
        JNIEnv *env = nullptr;
        monitor->jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
        if (env == nullptr) {
            JavaVMAttachArgs jvmAttachArgs {
                    .version = JNI_VERSION_1_6,
                    .name = "NativeCrashReporter",
                    .group = nullptr
            };
            auto result = monitor->jvm->AttachCurrentThread(&env, &jvmAttachArgs);
            if (result != JNI_OK) {
                LOGE("Attach java thread fail.");
                env = nullptr;
            }
        }
        if (env != nullptr) {
            LOGD("Start report java.");
            auto jMonitor = monitor->jCrashMonitor;
            auto jMonitorClazz = env->GetObjectClass(jMonitor);
            // auto jMonitorClazz = env->FindClass("com/tans/tapm/monitors/NativeCrashMonitor");
            auto crashMethodId = env->GetMethodID(jMonitorClazz, "onNativeCrash", "(IIIIIJJLjava/lang/String;I)V");
            auto crashStackFilePath = env->NewStringUTF(crashSignal->crashFilePath);
            env->CallVoidMethod(jMonitor, crashMethodId,
                                crashSignal->sig, crashSignal->sigInfo.si_code, crashSignal->crashPid, crashSignal->crashTid, crashSignal->crashTid, crashSignal->startTime, crashSignal->crashTime, crashStackFilePath, crashSignal->handleRet);
            LOGD("Report java success.");
        } else {
            LOGE("Report java fail.");
        }
    }
    return nullptr;
}

static int handleCrashOnNewProcess(CrashSignal *crashSignal) {
    int ret = prctl(PR_SET_DUMPABLE, 1);
    int childProcessPid = 0;
    if (ret != 0) {
        LOGE("Set progress dumpable fail: %d", ret);
        ret = -1;
        goto End;
    }
    errno = 0;
    ret = prctl(PR_SET_PTRACER, PR_SET_PTRACER_ANY);
    if (ret != 0 && errno != EINVAL) {
        LOGE("Set process tracer fail: %d", ret);
        ret = -1;
        goto End;
    }
    childProcessPid = fork();
    if (childProcessPid == 0) {
        // ChildProcess
        LOGD("Child process started.");
        alarm(30);
        pthread_attr_t attr;
        pthread_t t;
        void * threadRet = nullptr;
        ret = pthread_attr_init(&attr);
        if (ret != 0) {
            LOGE("Init thread attr fail: %d", ret);
            ret = -1;
            goto ChildProcessEnd;
        }

        ret = pthread_attr_setstacksize(&attr, MAX_THREAD_STACK_SIZE);
        if (ret != 0) {
            LOGE("Set thread stack size fail: %d", ret);
            ret = -1;
            goto ChildProcessEnd;
        }

        pthread_create(&t, &attr, handleCrashThread, crashSignal);
        pthread_join(t, &threadRet);
        pthread_attr_destroy(&attr);
        if (threadRet != nullptr) {
            ret = *static_cast<int *>(threadRet);
            free(threadRet);
            threadRet = nullptr;
        } else {
            ret = -1;
        }
        ChildProcessEnd:
        _Exit(ret);
    } else if (childProcessPid > 0) {
        LOGD("Waiting child process finish work.");
        // ParentProcess.
        int childProcessStatus;
        // Waiting child process finish work.
        waitpid(childProcessPid, &childProcessStatus, __WALL);
        LOGD("Child process finished: %d", childProcessStatus);
        if (childProcessStatus == 0) {
            ret = 0;
        } else {
            ret = -1;
        }
    } else {
        // Error;
        LOGE("Create child process fail: %d", childProcessPid);
        ret = -1;
    }
    crashSignal->handleRet = ret;
    End:
    pthread_attr_t attr;
    pthread_t t;
    if (pthread_attr_init(&attr) == 0 && pthread_attr_setstacksize(&attr, MAX_THREAD_STACK_SIZE) == 0) {
        pthread_create(&t, &attr, reportJavaThread, crashSignal);
        pthread_join( t, nullptr);
    } else {
        LOGE("Start report java thread fail.");
    }
    pthread_attr_destroy(&attr);
    return ret;
}

static void crashSignalHandler(int sig, siginfo_t *sig_info, void *uc) {
    auto *monitor = workingMonitor;
    if (monitor != nullptr && !isCrashed) {
        isCrashed = true;
        CrashSignal crashSignal {
            .sig = sig,
            .startTime = monitor->startTime,
            .crashTime = nowInMillis(),
            .crashPid = getpid(),
            .crashTid = gettid(),
            .crashUid = getuid()
        };
        memcpy(&crashSignal.sigInfo, sig_info, sizeof(siginfo_t));
        memcpy(&crashSignal.userContext, uc, sizeof(crashSignal.userContext));
        char crashFileName[MAX_STR_SIZE];
        formatTime(crashSignal.crashTime, crashFileName, MAX_STR_SIZE);
        sprintf(crashSignal.crashFilePath, "%s/%s", monitor->crashOutputDir, crashFileName);
        int ret = pthread_mutex_trylock(&lock);
        if (ret == 0) {
            handleCrashOnNewProcess(&crashSignal);
            pthread_mutex_unlock(&lock);
        }
    }

    if (monitor != nullptr) {
        OldCrashSignalAction * oldAction = nullptr;
        Iterator i;
        monitor->oldCrashSignalActions->iterator(&i);
        while (i.containValue()) {
            auto a = static_cast<OldCrashSignalAction *>(i.value());
            if (a->signal == sig) {
                oldAction = a;
                break;
            }
            i.next();
        }
        if (oldAction != nullptr && oldAction->action.sa_flags & SA_SIGINFO) {
            oldAction->action.sa_sigaction(sig, sig_info, uc);
        } else if (oldAction != nullptr && oldAction->action.sa_handler != SIG_DFL && oldAction->action.sa_handler != SIG_IGN) {
            oldAction->action.sa_handler(sig);
        } else {
            signal(sig, SIG_DFL);
            kill(getpid(), sig);
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
    workingMonitor = nullptr;
    pthread_mutex_unlock(&lock);
}
