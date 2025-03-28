//
// Created by pengcheng.tan on 2025/3/28.
//
#include <cstdint>
#include <sys/ucontext.h>
#include <sys/ptrace.h>
#include <linux/uio.h>
#include <linux/elf.h>
#include <cstring>
#include "t_regs.h"

int readRegsFromPtrace(pid_t tid, uintptr_t *outputRegs) {
    int regLength;
    uintptr_t regs[64];
#ifdef  PTRACE_GETREGS
    // x86 / x86_64
    int ret = ptrace(PTRACE_GETREGS, tid, nullptr, &regs);
    if (ret != 0) {
        return ret;
    }
    regLength = T_REGS_USER_NUM;
#else
    // arm / arm64
    struct iovec iovec{};
    iovec.iov_base = &regs;
    iovec.iov_len = sizeof(regs);
    int ret = ptrace(PTRACE_GETREGSET, tid, (void *)NT_PRSTATUS, &iovec);
    if (ret != 0) {
        return ret;
    }
    regLength = iovec.iov_len / sizeof(uintptr_t);
#endif


#if defined(__aarch64__)
    if(regLength > T_REGS_USER_NUM) regLength = T_REGS_USER_NUM;
    memcpy(outputRegs, regs, sizeof(uintptr_t) * regLength);
#elif defined(__arm__)
    if(regLength > T_REGS_USER_NUM) regLength = T_REGS_USER_NUM;
    memcpy(outputRegs, regs, sizeof(uintptr_t) * regLength);
#elif defined(__x86_64__)
    auto *ptregs = (struct pt_regs *)regs;
    outputRegs[T_REGS_RAX] = ptregs->rax;
    outputRegs[T_REGS_RBX] = ptregs->rbx;
    outputRegs[T_REGS_RCX] = ptregs->rcx;
    outputRegs[T_REGS_RDX] = ptregs->rdx;
    outputRegs[T_REGS_R8]  = ptregs->r8;
    outputRegs[T_REGS_R9]  = ptregs->r9;
    outputRegs[T_REGS_R10] = ptregs->r10;
    outputRegs[T_REGS_R11] = ptregs->r11;
    outputRegs[T_REGS_R12] = ptregs->r12;
    outputRegs[T_REGS_R13] = ptregs->r13;
    outputRegs[T_REGS_R14] = ptregs->r14;
    outputRegs[T_REGS_R15] = ptregs->r15;
    outputRegs[T_REGS_RDI] = ptregs->rdi;
    outputRegs[T_REGS_RSI] = ptregs->rsi;
    outputRegs[T_REGS_RBP] = ptregs->rbp;
    outputRegs[T_REGS_RSP] = ptregs->rsp;
    outputRegs[T_REGS_RIP] = ptregs->rip;
#elif defined(__i386__)
    auto *ptregs = (struct pt_regs *)regs;
    outputRegs[T_REGS_EAX] = (uintptr_t)ptregs->eax;
    outputRegs[T_REGS_EBX] = (uintptr_t)ptregs->ebx;
    outputRegs[T_REGS_ECX] = (uintptr_t)ptregs->ecx;
    outputRegs[T_REGS_EDX] = (uintptr_t)ptregs->edx;
    outputRegs[T_REGS_EDI] = (uintptr_t)ptregs->edi;
    outputRegs[T_REGS_ESI] = (uintptr_t)ptregs->esi;
    outputRegs[T_REGS_EBP] = (uintptr_t)ptregs->ebp;
    outputRegs[T_REGS_ESP] = (uintptr_t)ptregs->esp;
    outputRegs[T_REGS_EIP] = (uintptr_t)ptregs->eip;
#endif
    return 0;
}

void readRegsFromUContext(uintptr_t *outputRegs, ucontext_t *context) {
#if defined(__aarch64__)
    outputRegs[T_REGS_X0]  = context->uc_mcontext.regs[0];
    outputRegs[T_REGS_X1]  = context->uc_mcontext.regs[1];
    outputRegs[T_REGS_X2]  = context->uc_mcontext.regs[2];
    outputRegs[T_REGS_X3]  = context->uc_mcontext.regs[3];
    outputRegs[T_REGS_X4]  = context->uc_mcontext.regs[4];
    outputRegs[T_REGS_X5]  = context->uc_mcontext.regs[5];
    outputRegs[T_REGS_X6]  = context->uc_mcontext.regs[6];
    outputRegs[T_REGS_X7]  = context->uc_mcontext.regs[7];
    outputRegs[T_REGS_X8]  = context->uc_mcontext.regs[8];
    outputRegs[T_REGS_X9]  = context->uc_mcontext.regs[9];
    outputRegs[T_REGS_X10] = context->uc_mcontext.regs[10];
    outputRegs[T_REGS_X11] = context->uc_mcontext.regs[11];
    outputRegs[T_REGS_X12] = context->uc_mcontext.regs[12];
    outputRegs[T_REGS_X13] = context->uc_mcontext.regs[13];
    outputRegs[T_REGS_X14] = context->uc_mcontext.regs[14];
    outputRegs[T_REGS_X15] = context->uc_mcontext.regs[15];
    outputRegs[T_REGS_X16] = context->uc_mcontext.regs[16];
    outputRegs[T_REGS_X17] = context->uc_mcontext.regs[17];
    outputRegs[T_REGS_X18] = context->uc_mcontext.regs[18];
    outputRegs[T_REGS_X19] = context->uc_mcontext.regs[19];
    outputRegs[T_REGS_X20] = context->uc_mcontext.regs[20];
    outputRegs[T_REGS_X21] = context->uc_mcontext.regs[21];
    outputRegs[T_REGS_X22] = context->uc_mcontext.regs[22];
    outputRegs[T_REGS_X23] = context->uc_mcontext.regs[23];
    outputRegs[T_REGS_X24] = context->uc_mcontext.regs[24];
    outputRegs[T_REGS_X25] = context->uc_mcontext.regs[25];
    outputRegs[T_REGS_X26] = context->uc_mcontext.regs[26];
    outputRegs[T_REGS_X27] = context->uc_mcontext.regs[27];
    outputRegs[T_REGS_X28] = context->uc_mcontext.regs[28];
    outputRegs[T_REGS_X29] = context->uc_mcontext.regs[29];
    outputRegs[T_REGS_LR]  = context->uc_mcontext.regs[30];
    outputRegs[T_REGS_SP]  = context->uc_mcontext.sp;
    outputRegs[T_REGS_PC]  = context->uc_mcontext.pc;
#elif defined(__arm__)
    outputRegs[T_REGS_R0]  = context->uc_mcontext.arm_r0;
    outputRegs[T_REGS_R1]  = context->uc_mcontext.arm_r1;
    outputRegs[T_REGS_R2]  = context->uc_mcontext.arm_r2;
    outputRegs[T_REGS_R3]  = context->uc_mcontext.arm_r3;
    outputRegs[T_REGS_R4]  = context->uc_mcontext.arm_r4;
    outputRegs[T_REGS_R5]  = context->uc_mcontext.arm_r5;
    outputRegs[T_REGS_R6]  = context->uc_mcontext.arm_r6;
    outputRegs[T_REGS_R7]  = context->uc_mcontext.arm_r7;
    outputRegs[T_REGS_R8]  = context->uc_mcontext.arm_r8;
    outputRegs[T_REGS_R9]  = context->uc_mcontext.arm_r9;
    outputRegs[T_REGS_R10] = context->uc_mcontext.arm_r10;
    outputRegs[T_REGS_R11] = context->uc_mcontext.arm_fp;
    outputRegs[T_REGS_IP]  = context->uc_mcontext.arm_ip;
    outputRegs[T_REGS_SP]  = context->uc_mcontext.arm_sp;
    outputRegs[T_REGS_LR]  = context->uc_mcontext.arm_lr;
    outputRegs[T_REGS_PC]  = context->uc_mcontext.arm_pc;
#elif defined(__x86_64__)
    outputRegs[T_REGS_RAX] = (uintptr_t)context->uc_mcontext.gregs[REG_RAX];
    outputRegs[T_REGS_RBX] = (uintptr_t)context->uc_mcontext.gregs[REG_RBX];
    outputRegs[T_REGS_RCX] = (uintptr_t)context->uc_mcontext.gregs[REG_RCX];
    outputRegs[T_REGS_RDX] = (uintptr_t)context->uc_mcontext.gregs[REG_RDX];
    outputRegs[T_REGS_R8]  = (uintptr_t)context->uc_mcontext.gregs[REG_R8];
    outputRegs[T_REGS_R9]  = (uintptr_t)context->uc_mcontext.gregs[REG_R9];
    outputRegs[T_REGS_R10] = (uintptr_t)context->uc_mcontext.gregs[REG_R10];
    outputRegs[T_REGS_R11] = (uintptr_t)context->uc_mcontext.gregs[REG_R11];
    outputRegs[T_REGS_R12] = (uintptr_t)context->uc_mcontext.gregs[REG_R12];
    outputRegs[T_REGS_R13] = (uintptr_t)context->uc_mcontext.gregs[REG_R13];
    outputRegs[T_REGS_R14] = (uintptr_t)context->uc_mcontext.gregs[REG_R14];
    outputRegs[T_REGS_R15] = (uintptr_t)context->uc_mcontext.gregs[REG_R15];
    outputRegs[T_REGS_RDI] = (uintptr_t)context->uc_mcontext.gregs[REG_RDI];
    outputRegs[T_REGS_RSI] = (uintptr_t)context->uc_mcontext.gregs[REG_RSI];
    outputRegs[T_REGS_RBP] = (uintptr_t)context->uc_mcontext.gregs[REG_RBP];
    outputRegs[T_REGS_RSP] = (uintptr_t)context->uc_mcontext.gregs[REG_RSP];
    outputRegs[T_REGS_RIP] = (uintptr_t)context->uc_mcontext.gregs[REG_RIP];
#elif defined(__i386__)
    outputRegs[T_REGS_EAX] = (uintptr_t)context->uc_mcontext.gregs[REG_EAX];
    outputRegs[T_REGS_EBX] = (uintptr_t)context->uc_mcontext.gregs[REG_EBX];
    outputRegs[T_REGS_ECX] = (uintptr_t)context->uc_mcontext.gregs[REG_ECX];
    outputRegs[T_REGS_EDX] = (uintptr_t)context->uc_mcontext.gregs[REG_EDX];
    outputRegs[T_REGS_EDI] = (uintptr_t)context->uc_mcontext.gregs[REG_EDI];
    outputRegs[T_REGS_ESI] = (uintptr_t)context->uc_mcontext.gregs[REG_ESI];
    outputRegs[T_REGS_EBP] = (uintptr_t)context->uc_mcontext.gregs[REG_EBP];
    outputRegs[T_REGS_ESP] = (uintptr_t)context->uc_mcontext.gregs[REG_ESP];
    outputRegs[T_REGS_EIP] = (uintptr_t)context->uc_mcontext.gregs[REG_EIP];
#endif
}
