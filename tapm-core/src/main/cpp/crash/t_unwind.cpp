//
// Created by pengcheng.tan on 2025/4/8.
//
#include <cstring>
#include "t_unwind.h"
#include "libunwind-ptrace.h"
#include "../tapm_log.h"
#include "t_regs.h"

void copyRegs(unw_context_t *target, regs_t *src) {
#if defined(__aarch64__)
    memcpy(target->uc_mcontext.regs, src->regs, sizeof(src->regs));
    target->uc_mcontext.pc = src->pc;
    target->uc_mcontext.sp = src->sp;
    target->uc_mcontext.pstate = src->pstate;
#elif defined(__arm__)
    target->regs[UNW_ARM_R0] = src->uregs[T_REGS_R0];
    target->regs[UNW_ARM_R1] = src->uregs[T_REGS_R1];
    target->regs[UNW_ARM_R2] = src->uregs[T_REGS_R2];
    target->regs[UNW_ARM_R3] = src->uregs[T_REGS_R3];
    target->regs[UNW_ARM_R4] = src->uregs[T_REGS_R4];
    target->regs[UNW_ARM_R5] = src->uregs[T_REGS_R5];
    target->regs[UNW_ARM_R6] = src->uregs[T_REGS_R6];
    target->regs[UNW_ARM_R7] = src->uregs[T_REGS_R7];
    target->regs[UNW_ARM_R8] = src->uregs[T_REGS_R8];
    target->regs[UNW_ARM_R9] = src->uregs[T_REGS_R9];
    target->regs[UNW_ARM_R10] = src->uregs[T_REGS_R10];
    target->regs[UNW_ARM_R11] = src->uregs[T_REGS_R11];
    target->regs[UNW_ARM_R12] = src->uregs[T_REGS_IP];
    target->regs[UNW_ARM_R13] = src->uregs[T_REGS_SP];
    target->regs[UNW_ARM_R14] = src->uregs[T_REGS_LR];
    target->regs[UNW_ARM_R15] = src->uregs[T_REGS_PC];
#elif defined(__x86_64__)
    // 通用寄存器 (15)
    target->uc_mcontext.gregs[REG_R15] = src->r15;
    target->uc_mcontext.gregs[REG_R14] = src->r14;
    target->uc_mcontext.gregs[REG_R13] = src->r13;
    target->uc_mcontext.gregs[REG_R12] = src->r12;
    target->uc_mcontext.gregs[REG_RBP] = src->rbp;
    target->uc_mcontext.gregs[REG_RBX] = src->rbx;
    target->uc_mcontext.gregs[REG_R11] = src->r11;
    target->uc_mcontext.gregs[REG_R10] = src->r10;
    target->uc_mcontext.gregs[REG_R9]  = src->r9;
    target->uc_mcontext.gregs[REG_R8]  = src->r8;
    target->uc_mcontext.gregs[REG_RAX] = src->rax;
    target->uc_mcontext.gregs[REG_RCX] = src->rcx;
    target->uc_mcontext.gregs[REG_RDX] = src->rdx;
    target->uc_mcontext.gregs[REG_RSI] = src->rsi;
    target->uc_mcontext.gregs[REG_RDI] = src->rdi;

    // 程序计数器和标志寄存器
    target->uc_mcontext.gregs[REG_RIP] = src->rip;
    target->uc_mcontext.gregs[REG_EFL] = src->eflags;

    // 栈指针
    target->uc_mcontext.gregs[REG_RSP] = src->rsp;
#elif defined(__i386__)
    target->uc_mcontext.gregs[REG_EAX]  =   src->eax;
    target->uc_mcontext.gregs[REG_EBX]  =   src->ebx;
    target->uc_mcontext.gregs[REG_ECX]  =   src->ecx;
    target->uc_mcontext.gregs[REG_EDX]  =   src->edx;
    target->uc_mcontext.gregs[REG_ESI]  =   src->esi;
    target->uc_mcontext.gregs[REG_EDI]  =   src->edi;
    target->uc_mcontext.gregs[REG_EBP]  =   src->ebp;
    target->uc_mcontext.gregs[REG_ESP]  =   src->esp;
    target->uc_mcontext.gregs[REG_EIP]  =   src->eip;
    target->uc_mcontext.gregs[REG_EFL]  =   src->eflags;
    target->uc_mcontext.gregs[REG_DS]   =   src->xds;
    target->uc_mcontext.gregs[REG_ES]   =   src->xes;
    target->uc_mcontext.gregs[REG_FS]   =   src->xfs;
    target->uc_mcontext.gregs[REG_GS]   =   src->xgs;
    target->uc_mcontext.gregs[REG_CS]   =   src->xcs;
    target->uc_mcontext.gregs[REG_SS]   =   src->xss;
#endif
}

bool unwindFramesByPtrace(ThreadStatus *targetThread, LinkedList* memoryMaps, LinkedList* outputFrames, int maxFrameSize) {
    if (maxFrameSize <= 0 || !targetThread->isSuspend) {
        return false;
    }

    unw_cursor_t cursor;
    unw_word_t ip, sp;
    Iterator i;

    unw_addr_space_t remote_as = unw_create_addr_space(&_UPT_accessors, 0);
    void *context = _UPT_create(targetThread->thread->tid);

    int ret = unw_init_remote(&cursor, remote_as, context);
    if (ret != 0) {
        LOGE("Unwind init remote fail: %d", ret);
        goto End;
    }

    do {
        unw_get_reg(&cursor, UNW_REG_IP, &ip);
        if (ip == 0) {
            break;
        }
        unw_get_reg(&cursor, UNW_REG_SP, &sp);
        if (outputFrames->size != 0) {
#if defined(__aarch64__)
            if (ip >= 4) {
                ip -= 4;
            }
#elif defined(__arm__)
            if (ip & 0x1) {
                if (ip >= 4) {
                    ip -= 4;
                }
            } else {
                if (ip >= 8) {
                    ip -= 8;
                }
            }
#endif
        }
        auto f = new Frame;
        f->pc = ip;
        f->sp = sp;
        f->index = outputFrames->size;
        outputFrames->addToLast(f);
    } while(outputFrames->size <= maxFrameSize && unw_step(&cursor) > 0);

    outputFrames->iterator(&i);
    while(i.containValue()) {
        auto f = static_cast<Frame *>(i.value());
        f->isLoadSymbol = loadElfSymbol(f->pc, memoryMaps, &f->mapped, &f->offsetInElf, f->symbol, &f->offsetInSymbol);
        i.next();
    }
    outputFrames->iterator(&i);
    while (i.containValue()) {
        auto m = static_cast<Frame *>(i.value())->mapped;
        if (m != nullptr) {
            recycleElfFileMap(m);
        }
        i.next();
    }

    End:
    _UPT_destroy(context);
    unw_destroy_addr_space(remote_as);
    return ret == 0;
}

bool unwindFramesLocal(ThreadStatus *targetThread, LinkedList* memoryMaps, LinkedList* outputFrames, int maxFrameSize) {
    if (maxFrameSize <= 0 || !targetThread->isGetRegs) {
        return false;
    }

    unw_cursor_t cursor; unw_context_t uc;
    unw_word_t ip, sp;

    auto regs = targetThread->regs;
    unw_getcontext(&uc);
    if (targetThread->crashSignalCtx != nullptr) {
#if defined(__arm__)
        copyRegs(&uc, &regs);
#else
        memcpy(&uc, targetThread->crashSignalCtx, sizeof(uc));
#endif
    } else {
        copyRegs(&uc, &regs);
    }

    unw_init_local(&cursor, &uc);
    do {
        unw_get_reg(&cursor, UNW_REG_IP, &ip);
        if (ip == 0) {
            break;
        }
        unw_get_reg(&cursor, UNW_REG_SP, &sp);
        if (outputFrames->size != 0) {
#if defined(__aarch64__)
            if (ip >= 4) {
                ip -= 4;
            }
#elif defined(__arm__)
            if (ip & 0x1) {
                if (ip >= 4) {
                    ip -= 4;
                }
            } else {
                if (ip >= 8) {
                    ip -= 8;
                }
            }
#endif
        }
        auto f = new Frame;
        f->pc = ip;
        f->sp = sp;
        f->index = outputFrames->size;
        outputFrames->addToLast(f);
    } while (outputFrames->size <= maxFrameSize && unw_step(&cursor) > 0);

    Iterator i;
    outputFrames->iterator(&i);
    while(i.containValue()) {
        auto f = static_cast<Frame *>(i.value());
        f->isLoadSymbol = loadElfSymbol(f->pc, memoryMaps, &f->mapped, &f->offsetInElf, f->symbol, &f->offsetInSymbol);
        i.next();
    }
    outputFrames->iterator(&i);
    while (i.containValue()) {
        auto m = static_cast<Frame *>(i.value())->mapped;
        if (m != nullptr) {
            recycleElfFileMap(m);
        }
        i.next();
    }

    return true;
}

void recycleFrames(LinkedList *toRecycle) {
    while (toRecycle->size > 0) {
        auto v = reinterpret_cast<Frame *>(toRecycle->popFirst());
        delete v;
    }
}
