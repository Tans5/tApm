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
    int crashFileFd = -1;
    LinkedList crashedProcessThreads;
    LinkedList crashedProcessThreadsStatus;
    ThreadStatus *crashedThreadStatus = nullptr;
    LinkedList memoryMaps;
    MemoryMap *crashedMemoryMap = nullptr;

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
//    memoryMaps.forEach(nullptr, [](void *m, void *c) -> bool {
//        auto map = static_cast<MemoryMap *> (m);
//        Mapped fileMapped;
//        bool isElf = false;
//        if (map->permissions.read && fileMmapRead(map->pathname, map->offset, 5, &fileMapped)) {
//            isElf = isElfFile(fileMapped.data, fileMapped.dataSize);
//        }
//        LOGD("Start=%llx, End=%llx, Offset=%llx, Path=%s, IsElf=%d, R=%d, E=%d", map->startAddr,
//             map->endAddr, map->offset, map->pathname, isElf, map->permissions.read,
//             map->permissions.exec);
//        return true;
//    });

    // Read all threads register value.
    readThreadsRegs(&crashedProcessThreadsStatus, crashSignal->crashTid, &crashSignal->userContext);
    crashedProcessThreadsStatus.forEach(&memoryMaps, [](void *ts, void *memoryMaps) -> bool {
        auto *threadStatus = static_cast<ThreadStatus *>(ts);
        if (threadStatus->isGetRegs) {
            MemoryMap * map = nullptr;
            findMemoryMapByAddress(threadStatus->pc, static_cast<LinkedList *>(memoryMaps), &map);
            char *mapPath = "";
            if (map != nullptr) {
                mapPath = map->pathname;
            }
            LOGD("Thread=%s, Tid=%d, PC=0x%lx, SP=0x%lx, FP=0x%lx MapPath=%s", threadStatus->thread->threadName,
                 threadStatus->thread->tid, threadStatus->pc, threadStatus->sp, threadStatus->fp, mapPath);
        } else {
            LOGD("Thread %s don't get regs", threadStatus->thread->threadName);
        }
        return true;
    });


    findMemoryMapByAddress(crashedThreadStatus->pc, &memoryMaps, &crashedMemoryMap);
    if (crashedMemoryMap != nullptr) {
        if (tryLoadElf(crashedMemoryMap)) {
            auto crashedElf = crashedMemoryMap->elf;
            LOGD("Parse crash elf success: %s (%s)", crashedElf->soName, crashedElf->buildId);
            auto elfHeader = crashedElf->elfHeader;
            LOGD("ELF Header: ");
            LOGD("ProgramHeaderOffset=0x%x, ProgramHeaderEntrySize=%d, ProgramHeaderNum=%d",
                 elfHeader.programHeaderOffset, elfHeader.programHeaderEntrySize,
                 elfHeader.programHeaderNum);
            LOGD("SectionHeaderOffset=0x%x, SectionHeaderEntrySize=%d, SectionHeaderNum=%d, SectionNameStrIndex=%d",
                 elfHeader.sectionHeaderOffset, elfHeader.sectionHeaderEntrySize,
                 elfHeader.sectionHeaderNum, elfHeader.sectionNameStrIndex);

            LOGD("Program Headers: ");
            crashedElf->programHeaders.forEach(nullptr, [](void *p, void *) {
                auto ph = static_cast<T_ProgramHeader *>(p);
                LOGD("Type=%d, Start=0x%x, SizeInFile=%d, SizeInMemory=%d", ph->type, ph->offset,
                     ph->sizeInFile, ph->sizeInMemory);
                return true;
            });
            LOGD("Section Headers: ");
            crashedElf->sectionHeaders.forEach(nullptr, [](void *s, void *) {
                auto sh = static_cast<T_SectionHeader *>(s);
                LOGD("Name=%s, Offset=0x%x, SizeInFile=%d, EntrySize=%d, Index=%d, Link=%d, Info=%d",
                     sh->name, sh->offset, sh->sizeInFile, sh->entrySize, sh->index, sh->link,
                     sh->info);
                return true;
            });
            auto elfOffset = convertAddressToElfOffset(crashedMemoryMap, crashedThreadStatus->pc);
            char symbolName[256];
            addr_t symbolOffset;
            readAddressSymbol(crashedMemoryMap->elfFileMap->data, crashedElf, elfOffset, symbolName, &symbolOffset);
            LOGD("CrashedSymbolName=%s, Offset=0x%llx", symbolName, symbolOffset);
        } else {
            LOGE("Parse crash elf fail.");
        }
    }
//    crashedProcessThreadsStatus.forEach(&memoryMaps, [](void *s, void *m) {
//        auto threadStatus = static_cast<ThreadStatus *>(s);
//        if (threadStatus->isGetRegs) {
//            auto memoryMaps = static_cast<LinkedList *>(m);
//            MemoryMap *t = nullptr;
//            if (findMemoryMapByAddress(threadStatus->pc, memoryMaps, &t)) {
//                if (tryLoadElf(t)) {
//                    LOGD("Thread=%s, load elf file %s success.", threadStatus->thread->threadName,
//                         t->pathname);
//                } else {
//                    LOGE("Thread=%s, load elf fail.", threadStatus->thread->threadName);
//                }
//            } else {
//                LOGE("Thread=%s, no memory map", threadStatus->thread->threadName);
//            }
//        } else {
//            LOGE("Thread=%s, no pc", threadStatus->thread->threadName);
//        }
//        return true;
//    });

    crashedProcessThreadsStatus.forEach(&memoryMaps, [](void *s, void *m) {
        auto threadStatus = static_cast<ThreadStatus *>(s);
        auto *memoryMaps = static_cast<LinkedList *>(m);
        LinkedList frames;
        LOGD("Thread=%s, InitPC=0x%llx", threadStatus->thread->threadName, threadStatus->pc);
        unwindFramesLocal(threadStatus, memoryMaps, &frames, 64);
        frames.forEach(nullptr, [](void *f, void*) {
            auto frame = static_cast<Frame *>(f);
            LOGD("PC=0x%llx, SP=0x%llx, ElfPath=%s, PC_ELF=0x%llx, Symbol=%s, OffsetInSymbol=0x%llx", frame->pc, frame->sp, frame->elfPath, frame->offsetInElf, frame->symbol, frame->offsetInSymbol);
            return true;
        });
        recycleFrames(&frames);
        return true;
    });


    char abortMsg[MAX_STR_SIZE];
    if (tryFindAbortMsg(crashSignal->crashPid, &memoryMaps, abortMsg)) {
        std::string s(abortMsg);
        LOGD("Found abort msg: %s", s.c_str());
    }

    crashFileFd = open(crashSignal->crashFilePath, O_CREAT | O_RDWR, 0666);
    if (crashFileFd == -1) {
        LOGE("Create crash file fail");
        ret = -1;
        goto  End;
    }

    // TODO:


    End:
    if (crashFileFd != -1) {
        close(crashFileFd);
    }
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

static int handleCrashOnNewThread(CrashSignal *crashSignal) {
    LOGD("Receive crash sig: %d", crashSignal->sig);
    pthread_attr_t attr;
    pthread_t t;
    int ret = prctl(PR_SET_DUMPABLE, 1);
    void * threadRet = nullptr;
    if (ret != 0) {
        LOGE("Set progress dumpable fail: %d", ret);
        goto End;
    }
    errno = 0;
    ret = prctl(PR_SET_PTRACER, PR_SET_PTRACER_ANY);
    if (ret != 0 && errno != EINVAL) {
        LOGE("Set process tracer fail: %d", ret);
        ret = -1;
        goto End;
    } else {
        ret = 0;
    }

    ret = pthread_attr_init(&attr);
    if (ret != 0) {
        LOGE("Init thread attr fail: %d", ret);
        ret = -1;
        goto End;
    }

    ret = pthread_attr_setstacksize(&attr, MAX_THREAD_STACK_SIZE);
    if (ret != 0) {
        LOGE("Set thread stack size fail: %d", ret);
        ret = -1;
        goto End;
    }

    pthread_create(&t, &attr, handleCrashThread, crashSignal);
    pthread_join(t, &threadRet);
    if (threadRet != nullptr) {
        ret = *static_cast<int *>(threadRet);
        free(threadRet);
        threadRet = nullptr;
    } else {
        ret = -1;
    }
    LOGD("Thread result: %d", ret);
    End:
    pthread_attr_destroy(&attr);
    return ret;
}

static int handleCrashOnNewProcess(CrashSignal *crashSignal) {
    int ret = prctl(PR_SET_DUMPABLE, 1);
    int childProcessPid = 0;
    if (ret != 0) {
        LOGE("Set progress dumpable fail: %d", ret);
        goto End;
    }
    errno = 0;
    ret = prctl(PR_SET_PTRACER, PR_SET_PTRACER_ANY);
    if (ret != 0 && errno != EINVAL) {
        LOGE("Set process tracer fail: %d", ret);
        ret = -1;
        goto End;
    } else {
        ret = 0;
    }
    childProcessPid = fork();
    if (childProcessPid == 0) {
        // ChildProcess
        LOGD("Child process started.");
        alarm(30);
        int r = handleCrashOnNewThread(crashSignal);
        _Exit(r);
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

    End:
    if (ret == 0) {
        // TODO: success.
    } else {
        // TODO: fail.
    }
    return ret;
}

static void crashSignalHandler(int sig, siginfo_t *sig_info, void *uc) {
    auto *monitor = workingMonitor;
    if (monitor != nullptr && !isCrashed) {
        isCrashed = true;
        CrashSignal crashSignal {
            .sig = sig,
            .crashTime = nowInMillis(),
            .crashPid = getpid(),
            .crashTid = gettid(),
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
