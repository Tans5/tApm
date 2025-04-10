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

typedef struct Frame {
    uint64_t pc = 0;
    uint64_t sp = 0;
    uint64_t offsetInElf = 0;
    uint64_t offsetInSymbol = 0;
    char elfPath[256]{};
    char symbol[256]{};
} Frame;

bool unwindFramesByPtrace(ThreadStatus *targetThread, LinkedList* memoryMaps, LinkedList* outputFrames, int maxFrameSize);

bool unwindFramesLocal(ThreadStatus *targetThread, LinkedList* memoryMaps, LinkedList* outputFrames, int maxFrameSize);

void recycleFrames(LinkedList *toRecycle);

#endif //TAPM_T_UNWIND_H
