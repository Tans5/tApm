//
// Created by pengcheng.tan on 2025/3/25.
//

#ifndef TAPM_CRASH_H
#define TAPM_CRASH_H

#include <cstdint>
#include <csignal>
#include <jni.h>
#include "../linkedlist/linked_list.h"
#include "../tapm_size.h"

static int CRASH_SIGNAL[8] = {
       SIGABRT, // 程序主动调用 abort() 函数终止自身
       SIGBUS, // 对齐错误/物理地址异常
       SIGFPE, // 浮点运算错误或整数算术异常
       SIGILL, // 非法指令执行
       SIGSEGV, // 非法内存访问（空指针、越界等）
       SIGTRAP, // 调试断点触发
       SIGSYS, // 非法或无效的系统调用执行
       SIGSTKFLT
};

#define SIGNAL_STACK_SIZE (128 * 1024)

typedef struct CrashSignal {
    int sig = 0;
    siginfo_t sigInfo {};
    ucontext_t userContext{};
    int64_t startTime = 0;
    int64_t crashTime = 0;
    pid_t crashPid = 0;
    pid_t crashTid = 0;
    uid_t crashUid = 0;
    char crashFilePath[2 * MAX_STR_SIZE]{};
    char fingerprint[MAX_STR_SIZE]{};
    int handleRet = 0;
} CrashSignal;

typedef struct OldCrashSignalAction {
    int  signal = 0;
    struct sigaction action {};
} OldCrashSignalAction;

typedef struct Crash {
    int64_t startTime = 0;

    JavaVM  *jvm = nullptr;
    jobject jCrashMonitor = nullptr;
    char *crashOutputDir = nullptr;
    char *fingerprint = nullptr;

    void *newSignalStackBuffer = nullptr;
    stack_t *oldSignalStack  = nullptr;
    LinkedList *oldCrashSignalActions = nullptr;

    int32_t prepare(JNIEnv *jniEnv, jobject jCrashMonitorP, jstring crashFileDir, jstring fingerprint);
    void release(JNIEnv *jniEnv);
} Crash;

#endif //TAPM_CRASH_H
