//
// Created by pengcheng.tan on 2025/4/14.
//

#ifndef TAPM_CRASH_WRITER_H
#define TAPM_CRASH_WRITER_H

#include <asm-generic/siginfo.h>
#include <sys/ucontext.h>
#include "../linkedlist/linked_list.h"
#include "t_unwind.h"

int writeCrash(
        int sig,
        siginfo_t *sigInfo,
        ucontext_t *userContext,
        int64_t startTime,
        int64_t crashTime,
        pid_t crashPid,
        pid_t crashTid,
        uid_t crashUid,
        const char *crashFilePath,
        const char *fingerprint,
        LinkedList *memoryMaps,
        LinkedList *threadsStatus,
        ThreadStatus *crashedThreadStatus
);

#endif //TAPM_CRASH_WRITER_H
