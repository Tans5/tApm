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

void initThreadStatus(LinkedList *inputThreads, LinkedList *outputThreadsStatus);

void suspendThreads(LinkedList *inputThreadsStatus);

void resumeThreads(LinkedList *inputThreadsStatus);

void readThreadsRegs(LinkedList *inputThreadsStatus, tApmThread *crashedThread, ucontext_t *crashThreadUContext);

#endif //TAPM_THREAD_CONTROL_H
