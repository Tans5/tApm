//
// Created by pengcheng.tan on 2025/3/26.
//
#include <sys/ptrace.h>
#include <cerrno>
#include <linux/wait.h>
#include <sys/wait.h>
#include <linux/uio.h>
#include <linux/elf.h>
#include <cstring>
#include "thread_control.h"
#include "../tapm_log.h"


void initThreadStatus(LinkedList *inputThreads, LinkedList *outputThreadsStatus) {
    Iterator threadsIterator;
    inputThreads->iterator(&threadsIterator);
    while (threadsIterator.containValue()) {
        auto t = static_cast<tApmThread *>(threadsIterator.value());
        auto threadStatus = new ThreadStatus;
        threadStatus->thread = t;
        outputThreadsStatus->addToLast(threadStatus);
        threadsIterator.next();
    }
}

void suspendThreads(LinkedList *inputThreadsStatus) {
    Iterator iterator;
    inputThreadsStatus->iterator(&iterator);
    while (iterator.containValue()) {
        auto threadAndStatus = static_cast<ThreadStatus *>(iterator.value());
        if (!threadAndStatus->isSuspend) {
            auto thread = threadAndStatus->thread;
            int r = ptrace(PTRACE_ATTACH, thread->tid, nullptr, nullptr);
            if (r != 0) {
                LOGE("Attach thread: %s, fail: %d", thread->threadName, r);
            } else {
                errno = 0;
                int ret = 0;
                while ((ret = waitpid(thread->tid, nullptr, __WALL)) < 0) {
                    if (EINTR != errno) {
                        ptrace(PTRACE_DETACH, thread->tid, nullptr, nullptr);
                        LOGE("Wait thread %s attach fail.", thread->threadName);
                        ret = -1;
                        break;
                    }
                    errno = 0;
                }
                if (ret >= 0) {
                    threadAndStatus->isSuspend = true;
                }
            }
        }
    }
}

void resumeThreads(LinkedList *inputThreadsStatus) {
    Iterator iterator;
    inputThreadsStatus->iterator(&iterator);
    while (iterator.containValue()) {
        auto status = static_cast<ThreadStatus *>(iterator.value());
        if (status->isSuspend) {
            ptrace(PTRACE_DETACH, status->thread->tid, nullptr, nullptr);
            status->isSuspend = false;
        }
    }
}

int readRegsFromPtrace(ThreadStatus *status) {
    auto thread = status->thread;
    int regLength;
    uintptr_t regs[64];
#ifdef  PTRACE_GETREGS
    // x86 / x86_64
    int ret = ptrace(PTRACE_GETREGS, thread->tid, nullptr, &regs);
    if (ret != 0) {
        LOGE("Get %s thread regs fail: %d", thread->threadName, ret);
        return ret;
    }
    regLength = T_REGS_USER_NUM;
#else
    // arm / arm64
            struct iovec iovec{};
            iovec.iov_base = &regs;
            iovec.iov_len = sizeof(regs);
            int ret = ptrace(PTRACE_GETREGSET, thread->tid, (void *)NT_PRSTATUS, &iovec);
            if (ret != 0) {
                LOGE("Get %s thread regs fail: %d", thread->threadName, ret);
                return ret;
            }
            regLength = iovec.iov_len / sizeof(uintptr_t);
#endif


#if defined(__aarch64__)
    if(regLength > T_REGS_USER_NUM) regLength = T_REGS_USER_NUM;
    memcpy(status->regs, regs, sizeof(uintptr_t) * regLength);
#elif defined(__arm__)
    if(regLength > T_REGS_USER_NUM) regLength = T_REGS_USER_NUM;
    memcpy(status->regs, regs, sizeof(uintptr_t) * regLength);
#elif defined(__x86_64__)
    auto *ptregs = (struct pt_regs *)regs;
            status->regs[T_REGS_RAX] = ptregs->rax;
            status->regs[T_REGS_RBX] = ptregs->rbx;
            status->regs[T_REGS_RCX] = ptregs->rcx;
            status->regs[T_REGS_RDX] = ptregs->rdx;
            status->regs[T_REGS_R8]  = ptregs->r8;
            status->regs[T_REGS_R9]  = ptregs->r9;
            status->regs[T_REGS_R10] = ptregs->r10;
            status->regs[T_REGS_R11] = ptregs->r11;
            status->regs[T_REGS_R12] = ptregs->r12;
            status->regs[T_REGS_R13] = ptregs->r13;
            status->regs[T_REGS_R14] = ptregs->r14;
            status->regs[T_REGS_R15] = ptregs->r15;
            status->regs[T_REGS_RDI] = ptregs->rdi;
            status->regs[T_REGS_RSI] = ptregs->rsi;
            status->regs[T_REGS_RBP] = ptregs->rbp;
            status->regs[T_REGS_RSP] = ptregs->rsp;
            status->regs[T_REGS_RIP] = ptregs->rip;
#elif defined(__i386__)
    auto *ptregs = (struct pt_regs *)regs;
    status->regs[T_REGS_EAX] = (uintptr_t)ptregs->eax;
    status->regs[T_REGS_EBX] = (uintptr_t)ptregs->ebx;
    status->regs[T_REGS_ECX] = (uintptr_t)ptregs->ecx;
    status->regs[T_REGS_EDX] = (uintptr_t)ptregs->edx;
    status->regs[T_REGS_EDI] = (uintptr_t)ptregs->edi;
    status->regs[T_REGS_ESI] = (uintptr_t)ptregs->esi;
    status->regs[T_REGS_EBP] = (uintptr_t)ptregs->ebp;
    status->regs[T_REGS_ESP] = (uintptr_t)ptregs->esp;
    status->regs[T_REGS_EIP] = (uintptr_t)ptregs->eip;

#endif
    status->isGetRegs = true;
    return 0;
}

void readRegsFromUContext(ThreadStatus *status, ucontext_t *context) {
#if defined(__aarch64__)
    status->regs[T_REGS_X0]  = context->uc_mcontext.regs[0];
    status->regs[T_REGS_X1]  = context->uc_mcontext.regs[1];
    status->regs[T_REGS_X2]  = context->uc_mcontext.regs[2];
    status->regs[T_REGS_X3]  = context->uc_mcontext.regs[3];
    status->regs[T_REGS_X4]  = context->uc_mcontext.regs[4];
    status->regs[T_REGS_X5]  = context->uc_mcontext.regs[5];
    status->regs[T_REGS_X6]  = context->uc_mcontext.regs[6];
    status->regs[T_REGS_X7]  = context->uc_mcontext.regs[7];
    status->regs[T_REGS_X8]  = context->uc_mcontext.regs[8];
    status->regs[T_REGS_X9]  = context->uc_mcontext.regs[9];
    status->regs[T_REGS_X10] = context->uc_mcontext.regs[10];
    status->regs[T_REGS_X11] = context->uc_mcontext.regs[11];
    status->regs[T_REGS_X12] = context->uc_mcontext.regs[12];
    status->regs[T_REGS_X13] = context->uc_mcontext.regs[13];
    status->regs[T_REGS_X14] = context->uc_mcontext.regs[14];
    status->regs[T_REGS_X15] = context->uc_mcontext.regs[15];
    status->regs[T_REGS_X16] = context->uc_mcontext.regs[16];
    status->regs[T_REGS_X17] = context->uc_mcontext.regs[17];
    status->regs[T_REGS_X18] = context->uc_mcontext.regs[18];
    status->regs[T_REGS_X19] = context->uc_mcontext.regs[19];
    status->regs[T_REGS_X20] = context->uc_mcontext.regs[20];
    status->regs[T_REGS_X21] = context->uc_mcontext.regs[21];
    status->regs[T_REGS_X22] = context->uc_mcontext.regs[22];
    status->regs[T_REGS_X23] = context->uc_mcontext.regs[23];
    status->regs[T_REGS_X24] = context->uc_mcontext.regs[24];
    status->regs[T_REGS_X25] = context->uc_mcontext.regs[25];
    status->regs[T_REGS_X26] = context->uc_mcontext.regs[26];
    status->regs[T_REGS_X27] = context->uc_mcontext.regs[27];
    status->regs[T_REGS_X28] = context->uc_mcontext.regs[28];
    status->regs[T_REGS_X29] = context->uc_mcontext.regs[29];
    status->regs[T_REGS_LR]  = context->uc_mcontext.regs[30];
    status->regs[T_REGS_SP]  = context->uc_mcontext.sp;
    status->regs[T_REGS_PC]  = context->uc_mcontext.pc;
#elif defined(__arm__)
    status->regs[T_REGS_R0]  = context->uc_mcontext.arm_r0;
    status->regs[T_REGS_R1]  = context->uc_mcontext.arm_r1;
    status->regs[T_REGS_R2]  = context->uc_mcontext.arm_r2;
    status->regs[T_REGS_R3]  = context->uc_mcontext.arm_r3;
    status->regs[T_REGS_R4]  = context->uc_mcontext.arm_r4;
    status->regs[T_REGS_R5]  = context->uc_mcontext.arm_r5;
    status->regs[T_REGS_R6]  = context->uc_mcontext.arm_r6;
    status->regs[T_REGS_R7]  = context->uc_mcontext.arm_r7;
    status->regs[T_REGS_R8]  = context->uc_mcontext.arm_r8;
    status->regs[T_REGS_R9]  = context->uc_mcontext.arm_r9;
    status->regs[T_REGS_R10] = context->uc_mcontext.arm_r10;
    status->regs[T_REGS_R11] = context->uc_mcontext.arm_fp;
    status->regs[T_REGS_IP]  = context->uc_mcontext.arm_ip;
    status->regs[T_REGS_SP]  = context->uc_mcontext.arm_sp;
    status->regs[T_REGS_LR]  = context->uc_mcontext.arm_lr;
    status->regs[T_REGS_PC]  = context->uc_mcontext.arm_pc;
#elif defined(__x86_64__)
    status->regs[T_REGS_RAX] = (uintptr_t)context->uc_mcontext.gregs[REG_RAX];
    status->regs[T_REGS_RBX] = (uintptr_t)context->uc_mcontext.gregs[REG_RBX];
    status->regs[T_REGS_RCX] = (uintptr_t)context->uc_mcontext.gregs[REG_RCX];
    status->regs[T_REGS_RDX] = (uintptr_t)context->uc_mcontext.gregs[REG_RDX];
    status->regs[T_REGS_R8]  = (uintptr_t)context->uc_mcontext.gregs[REG_R8];
    status->regs[T_REGS_R9]  = (uintptr_t)context->uc_mcontext.gregs[REG_R9];
    status->regs[T_REGS_R10] = (uintptr_t)context->uc_mcontext.gregs[REG_R10];
    status->regs[T_REGS_R11] = (uintptr_t)context->uc_mcontext.gregs[REG_R11];
    status->regs[T_REGS_R12] = (uintptr_t)context->uc_mcontext.gregs[REG_R12];
    status->regs[T_REGS_R13] = (uintptr_t)context->uc_mcontext.gregs[REG_R13];
    status->regs[T_REGS_R14] = (uintptr_t)context->uc_mcontext.gregs[REG_R14];
    status->regs[T_REGS_R15] = (uintptr_t)context->uc_mcontext.gregs[REG_R15];
    status->regs[T_REGS_RDI] = (uintptr_t)context->uc_mcontext.gregs[REG_RDI];
    status->regs[T_REGS_RSI] = (uintptr_t)context->uc_mcontext.gregs[REG_RSI];
    status->regs[T_REGS_RBP] = (uintptr_t)context->uc_mcontext.gregs[REG_RBP];
    status->regs[T_REGS_RSP] = (uintptr_t)context->uc_mcontext.gregs[REG_RSP];
    status->regs[T_REGS_RIP] = (uintptr_t)context->uc_mcontext.gregs[REG_RIP];
#elif defined(__i386__)
    status->regs[T_REGS_EAX] = (uintptr_t)context->uc_mcontext.gregs[REG_EAX];
    status->regs[T_REGS_EBX] = (uintptr_t)context->uc_mcontext.gregs[REG_EBX];
    status->regs[T_REGS_ECX] = (uintptr_t)context->uc_mcontext.gregs[REG_ECX];
    status->regs[T_REGS_EDX] = (uintptr_t)context->uc_mcontext.gregs[REG_EDX];
    status->regs[T_REGS_EDI] = (uintptr_t)context->uc_mcontext.gregs[REG_EDI];
    status->regs[T_REGS_ESI] = (uintptr_t)context->uc_mcontext.gregs[REG_ESI];
    status->regs[T_REGS_EBP] = (uintptr_t)context->uc_mcontext.gregs[REG_EBP];
    status->regs[T_REGS_ESP] = (uintptr_t)context->uc_mcontext.gregs[REG_ESP];
    status->regs[T_REGS_EIP] = (uintptr_t)context->uc_mcontext.gregs[REG_EIP];
#endif
    status->isGetRegs = true;
}

void readThreadsRegs(LinkedList *inputThreadsStatus, tApmThread *crashedThread, ucontext_t *crashThreadUContext) {
    Iterator iterator;
    inputThreadsStatus->iterator(&iterator);

    while (iterator.containValue()) {
        auto status = static_cast<ThreadStatus *>(iterator.value());
        if (status->thread->tid == crashedThread->tid) {
            readRegsFromUContext(status, crashThreadUContext);
        } else {
            if (status->isSuspend) {
                readRegsFromPtrace(status);
            }
        }
    }
}
