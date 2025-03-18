//
// Created by pengcheng.tan on 2025/3/18.
//
#include <malloc.h>
#include <cstring>
#include <unistd.h>
#include <dirent.h>
#include <asm-generic/fcntl.h>
#include <fcntl.h>
#include "anr.h"
#include "../tapm_log.h"


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

#ifndef SIGNAL_CATCHER_BUFFER_SIZE
#define SIGNAL_CATCHER_BUFFER_SIZE 1024
#endif

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

static volatile Anr* workingAnrMonitor = nullptr;

static void anrSignalHandler(int sig, siginfo_t *sig_info, void *uc) {
    auto anrMonitor = workingAnrMonitor;
    if (anrMonitor != nullptr) {
        if (sig == SIGQUIT) {
            LOGD("Receive SIGQUIT.");
            auto oldAction = anrMonitor->oldQuitSigAction;
            if (oldAction->sa_sigaction != nullptr) {
                oldAction->sa_sigaction(sig, sig_info, uc);
            }
        } else {
            LOGE("Receive sig: %d, but not SIGQUIT.", sig);
        }
    } else {
        LOGE("Receive SIGQUIT, but monitor is null.");
    }
}


int32_t Anr::prepare(JNIEnv *jniEnv, jobject j_AnrObject, jstring anrOutputDir) {
    jniEnv->GetJavaVM(&this->jvm);
    this->jAnrObject = jniEnv->NewGlobalRef(j_AnrObject);

    int32_t dirLen = jniEnv->GetStringUTFLength(anrOutputDir);
    const char* dir = jniEnv->GetStringUTFChars(anrOutputDir, JNI_FALSE);
    char * dirCopy = static_cast<char *>(malloc(dirLen + 1));
    memcpy(dirCopy, dir, dirLen);
    dirCopy[dirLen] = '\0';
    this->anrTraceOutputDir = dirCopy;
    this->signalCatcherTid = getSignalCatcherTid();
    if (this->signalCatcherTid <= 0) {
        return -1;
    }
    LOGD("Get signal catcher tid: %d", this->signalCatcherTid);

    int32_t ret = 0;
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
        return -1;
    }

    struct sigaction newSigaction {};
    sigfillset(&newSigaction.sa_mask);
    newSigaction.sa_flags = SA_RESTART | SA_ONSTACK | SA_SIGINFO;
    newSigaction.sa_sigaction = anrSignalHandler;
    this->oldQuitSigAction = new sigaction_p;
    ret = sigaction(SIGQUIT, &newSigaction, this->oldQuitSigAction);
    if (ret != 0) {
        delete this->oldQuitSigAction;
        this->oldQuitSigAction = nullptr;
        LOGE("Register new SIGQUIT action fail: %d", ret);
        return -1;
    }
    LOGD("Register new SIGQUIT action success.");

    workingAnrMonitor = this;
    return 0;
}

void Anr::release(JNIEnv *jniEnv) {
    workingAnrMonitor = nullptr;
    this->jvm = nullptr;
    if (this->jAnrObject != nullptr) {
        jniEnv->DeleteGlobalRef(this->jAnrObject);
        this->jAnrObject = nullptr;
    }
    if (this->anrTraceOutputDir != nullptr) {
        free((void *) this->anrTraceOutputDir);
        this->anrTraceOutputDir = nullptr;
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

    delete this;
}