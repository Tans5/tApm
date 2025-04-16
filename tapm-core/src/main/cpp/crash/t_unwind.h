//
// Created by pengcheng.tan on 2025/4/8.
//

#ifndef TAPM_T_UNWIND_H
#define TAPM_T_UNWIND_H

#include <sys/types.h>
#include "t_regs.h"
#include "../linkedlist/linked_list.h"
#include "thread_control.h"
#include "memory_maps.h"
#include "../tapm_size.h"

typedef struct Frame {
    uint32_t index = 0;
    addr_t pc = 0;
    addr_t sp = 0;
    bool isLoadMap = false;
    char mapPath[MAX_STR_SIZE]{};
    addr_t mapStartAddr = 0;
    addr_t mapEndAddr = 0;
    bool isLoadElf = false;
    addr_t elfFileStart = 0;
    addr_t elfLoadStart = 0;
    char elfBuildId[MAX_STR_SIZE]{};
    char soName[MAX_STR_SIZE]{};
    bool isLoadSymbol = false;
    addr_t offsetInElf = 0;
    addr_t offsetInSymbol = 0;
    char symbol[MAX_STR_SIZE]{};
} Frame;

bool unwindFramesByUnwindStack(ThreadStatus *targetThread, pid_t crashedPid, LinkedList* outputFrames, int maxFrameSize);

void recycleFrames(LinkedList *toRecycle);

#endif //TAPM_T_UNWIND_H
