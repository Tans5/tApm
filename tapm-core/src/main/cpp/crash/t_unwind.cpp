//
// Created by pengcheng.tan on 2025/4/8.
//
#include "t_unwind.h"
#include "libunwind-ptrace.h"
#include "../tapm_log.h"
#include "t_regs.h"

#if defined(__aarch64__)
#define UNW_PC UNW_AARCH64_PC
#define UNW_SP UNW_AARCH64_SP
#define UNW_FP UNW_AARCH64_X29
#elif defined(__arm__)
#define UNW_PC UNW_ARM_R15
#define UNW_SP UNW_ARM_R13
#define UNW_FP UNW_ARM_R11
#elif defined(__x86_64__)
#define UNW_PC UNW_X86_64_RIP
#define UNW_SP UNW_X86_64_RSP
#define UNW_FP UNW_X86_64_RBP
#elif defined(__i386__)
#define UNW_PC UNW_X86_EIP
#define UNW_SP UNW_X86_ESP
#define UNW_FP UNW_X86_EBP
#endif

bool unwindFrames(pid_t tid, regs_t *regs,  bool forceUpdateRegs, LinkedList* outputFrames, int maxFrameSize) {
    if (maxFrameSize <= 0) {
        return false;
    }

    unw_addr_space_t remote_as = unw_create_addr_space(&_UPT_accessors, 0);
    void *context = _UPT_create(tid);

    unw_cursor_t cursor;

    int ret = unw_init_remote(&cursor, remote_as, context);
    if (ret != 0) {
        LOGE("Unwind init remote fail: %d", ret);
        goto End;
    }

    if (forceUpdateRegs) {
        // TODO:
    }

    uint64_t currentPc;
    uint64_t symbolOffset;
    do {
        unw_get_reg(&cursor, UNW_PC, reinterpret_cast<unw_word_t *>(&(currentPc)));
        if (currentPc == 0) {
            break;
        }
        auto f = new Frame;
        f->pc = currentPc;
        unw_get_proc_name(&cursor, f->symbol, sizeof(f->symbol), reinterpret_cast<unw_word_t *>(&(symbolOffset)));
        f->offsetInSymbol = symbolOffset;
        outputFrames->addToLast(f);
    } while(outputFrames->size <= maxFrameSize && unw_step(&cursor) > 0);

    End:
    _UPT_destroy(context);
    unw_destroy_addr_space(remote_as);
    return ret == 0;
}

void recycleFrames(LinkedList *toRecycle) {
    while (toRecycle->size > 0) {
        auto v = reinterpret_cast<Frame *>(toRecycle->popFirst());
        delete v;
    }
}
