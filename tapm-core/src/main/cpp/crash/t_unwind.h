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
    addr_t offsetInElf = 0;
    addr_t offsetInSymbol = 0;
    MemoryMap * mapped = nullptr;
    bool isLoadSymbol = false;
    char symbol[MAX_STR_SIZE]{};
} Frame;

bool unwindFramesByPtrace(ThreadStatus *targetThread, LinkedList* memoryMaps, LinkedList* outputFrames, int maxFrameSize);

bool unwindFramesLocal(ThreadStatus *targetThread, LinkedList* memoryMaps, LinkedList* outputFrames, int maxFrameSize);

bool unwindFramesByUnwindStack(ThreadStatus *targetThread, LinkedList* memoryMaps, LinkedList* outputFrames, int maxFrameSize);

void recycleFrames(LinkedList *toRecycle);

#endif //TAPM_T_UNWIND_H
