//
// Created by pengcheng.tan on 2025/3/26.
//

#ifndef TAPM_THREAD_CONTROL_H
#define TAPM_THREAD_CONTROL_H
#include "../thread/tapm_thread.h"
#include "../linkedlist/linked_list.h"
#include "t_regs.h"


typedef struct ThreadStatus {
    tApmThread * thread = nullptr;
    bool isSuspend = false;
    bool isGetRegs = false;
    uintptr_t regs[T_REGS_USER_NUM]{};
    uintptr_t pc = 0;
    uintptr_t sp = 0;
} ThreadStatus;

void initThreadStatus(LinkedList *inputThreads, pid_t crashThreadTid, LinkedList *outputThreadsStatus, ThreadStatus **outputCrashThreadStatus);

void suspendThreads(LinkedList *inputThreadsStatus);

void resumeThreads(LinkedList *inputThreadsStatus);

void readThreadsRegs(LinkedList *inputThreadsStatus, pid_t crashTreadTid, ucontext_t *crashThreadUContext);

ThreadStatus * findThreadStatus(LinkedList *inputThreadStatus, pid_t tid);

#endif //TAPM_THREAD_CONTROL_H
