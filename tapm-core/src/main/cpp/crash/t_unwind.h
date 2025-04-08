//
// Created by pengcheng.tan on 2025/4/8.
//

#ifndef TAPM_T_UNWIND_H
#define TAPM_T_UNWIND_H

#include <sys/types.h>
#include "t_regs.h"

int unwindFrames(pid_t tid, regs_t *regs, uint64_t *outputFramePcs, int maxFrameSize);

#endif //TAPM_T_UNWIND_H
