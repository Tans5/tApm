//
// Created by pengcheng.tan on 2025/3/25.
//

#ifndef TAPM_CRASH_H
#define TAPM_CRASH_H

#include <cstdint>
#include <csignal>
#include <jni.h>
#include "../linkedlist/linked_list.h"

#if defined(__aarch64__)
#define CPU_ARCH "arm64"
#elif defined(__arm__)
#define CPU_ARCH "arm"
#elif defined(__x86_64__)
#define CPU_ARCH "x86_64"
#elif defined(__i386__)
#define CPU_ARCH "x86"
#else
#define CPU_ARCH "unknown"
#endif

static int CRASH_SIGNAL[8] = {
       SIGABRT,
       SIGBUS,
       SIGFPE,
       SIGILL,
       SIGSEGV,
       SIGTRAP,
       SIGSYS,
       SIGSTKFLT
};

#define SIGNAL_STACK_SIZE (128 * 1024)

typedef struct OldCrashSignalAction {
    int  signal = 0;
    struct sigaction action {};
} OldCrashSignalAction;

typedef struct Crash {
    int64_t startTime = 0;

    JavaVM  *jvm = nullptr;
    jobject jCrashMonitor = nullptr;
    char *crashOutputDir = nullptr;

    void *newSignalStackBuffer = nullptr;
    stack_t *oldSignalStack  = nullptr;
    LinkedList *oldCrashSignalActions = nullptr;

    int32_t prepare(JNIEnv *jniEnv, jobject jCrashMonitorP, jstring crashFileDir);
    void release(JNIEnv *jniEnv);
} Crash;

#endif //TAPM_CRASH_H
