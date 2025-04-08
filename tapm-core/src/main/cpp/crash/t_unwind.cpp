//
// Created by pengcheng.tan on 2025/4/8.
//
#include "t_unwind.h"
#include "libunwind-ptrace.h"

int unwindFrames(pid_t tid, regs_t *regs, uint64_t *outputFramePcs, int maxFrameSize) {
    int frameSize = 0;
    if (maxFrameSize <= 0) {
        return 0;
    }

    unw_addr_space_t remote_as = unw_create_addr_space(&_UPT_accessors, 0);
    void *context = _UPT_create(tid);

    unw_cursor_t cursor;
    unw_init_remote(&cursor, remote_as, context);

    // TODO: unwind.


    _UPT_destroy(context);
    unw_destroy_addr_space(remote_as);
    return frameSize;
}
