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

int readRegsFromPtrace(pid_t tid, regs_t *outputRegs) {
#ifdef PTRACE_GETREGS
    // x86 / x86_64 / arm
    auto ret = ptrace(PTRACE_GETREGS, tid, nullptr, outputRegs);
    if (ret != 0) {
        return (int) ret;
    }
#else
    // arm64
    struct iovec iovec{};
    iovec.iov_base = outputRegs;
    iovec.iov_len = sizeof(regs_t);
    auto ret = ptrace(PTRACE_GETREGSET, tid, NT_PRSTATUS, &iovec);
    if (ret != 0) {
        return (int)ret;
    }
#endif
    return 0;
}

int setRegsByPtrace(pid_t tid, regs_t *regs) {
#if defined(__aarch64__)
    struct iovec io = {
            .iov_base = regs,
            .iov_len = sizeof(*regs)
    };
    auto ret = ptrace(PTRACE_SETREGSET, tid, NT_PRSTATUS, &io);
    if (ret != 0) {
        return (int) ret;
    }
#else
    auto ret = ptrace(PTRACE_SETREGS, tid, nullptr, regs);
    if (ret != 0) {
        return (int) ret;
    }
#endif
    return 0;
}

void readRegsFromUContext(ucontext_t *context, regs_t *outputRegs) {

#if defined(__aarch64__)
    memcpy(outputRegs->regs, context->uc_mcontext.regs, sizeof(outputRegs->regs));
    outputRegs->sp  = context->uc_mcontext.sp;
    outputRegs->pc  = context->uc_mcontext.pc;
    outputRegs->pstate = context->uc_mcontext.pstate;
#elif defined(__arm__)
    auto uregs = outputRegs->uregs;
    uregs[T_REGS_R0]  = context->uc_mcontext.arm_r0;
    uregs[T_REGS_R1]  = context->uc_mcontext.arm_r1;
    uregs[T_REGS_R2]  = context->uc_mcontext.arm_r2;
    uregs[T_REGS_R3]  = context->uc_mcontext.arm_r3;
    uregs[T_REGS_R4]  = context->uc_mcontext.arm_r4;
    uregs[T_REGS_R5]  = context->uc_mcontext.arm_r5;
    uregs[T_REGS_R6]  = context->uc_mcontext.arm_r6;
    uregs[T_REGS_R7]  = context->uc_mcontext.arm_r7;
    uregs[T_REGS_R8]  = context->uc_mcontext.arm_r8;
    uregs[T_REGS_R9]  = context->uc_mcontext.arm_r9;
    uregs[T_REGS_R10] = context->uc_mcontext.arm_r10;
    uregs[T_REGS_R11] = context->uc_mcontext.arm_fp;
    uregs[T_REGS_IP]  = context->uc_mcontext.arm_ip;
    uregs[T_REGS_SP]  = context->uc_mcontext.arm_sp;
    uregs[T_REGS_LR]  = context->uc_mcontext.arm_lr;
    uregs[T_REGS_PC]  = context->uc_mcontext.arm_pc;
    uregs[T_REGS_CPSR] = context->uc_mcontext.arm_cpsr;
    uregs[T_REGS_FAULT_ADDRESS] = context->uc_mcontext.fault_address;
#elif defined(__x86_64__)
    // 通用寄存器 (15)
    outputRegs->r15 = context->uc_mcontext.gregs[REG_R15];
    outputRegs->r14 = context->uc_mcontext.gregs[REG_R14];
    outputRegs->r13 = context->uc_mcontext.gregs[REG_R13];
    outputRegs->r12 = context->uc_mcontext.gregs[REG_R12];
    outputRegs->rbp = context->uc_mcontext.gregs[REG_RBP];
    outputRegs->rbx = context->uc_mcontext.gregs[REG_RBX];
    outputRegs->r11 = context->uc_mcontext.gregs[REG_R11];
    outputRegs->r10 = context->uc_mcontext.gregs[REG_R10];
    outputRegs->r9  = context->uc_mcontext.gregs[REG_R9];
    outputRegs->r8  = context->uc_mcontext.gregs[REG_R8];
    outputRegs->rax = context->uc_mcontext.gregs[REG_RAX];
    outputRegs->rcx = context->uc_mcontext.gregs[REG_RCX];
    outputRegs->rdx = context->uc_mcontext.gregs[REG_RDX];
    outputRegs->rsi = context->uc_mcontext.gregs[REG_RSI];
    outputRegs->rdi = context->uc_mcontext.gregs[REG_RDI];

    // 程序计数器和标志寄存器
    outputRegs->rip    = context->uc_mcontext.gregs[REG_RIP];
    outputRegs->eflags = context->uc_mcontext.gregs[REG_EFL];

    // 栈指针
    outputRegs->rsp = context->uc_mcontext.gregs[REG_RSP];

    // 其他字段（如 cs/ss 等）可能不直接映射，需根据场景处理
    outputRegs->cs = 0; // 默认值或从其他来源获取
    outputRegs->ss = 0;
#elif defined(__i386__)

    // 通用寄存器
    outputRegs->eax = context->uc_mcontext.gregs[REG_EAX];
    outputRegs->ebx = context->uc_mcontext.gregs[REG_EBX];
    outputRegs->ecx = context->uc_mcontext.gregs[REG_ECX];
    outputRegs->edx = context->uc_mcontext.gregs[REG_EDX];
    outputRegs->esi = context->uc_mcontext.gregs[REG_ESI];
    outputRegs->edi = context->uc_mcontext.gregs[REG_EDI];
    outputRegs->ebp = context->uc_mcontext.gregs[REG_EBP];
    outputRegs->esp = context->uc_mcontext.gregs[REG_ESP];  // 栈指针

    // 程序计数器（EIP）和标志寄存器（EFLAGS）
    outputRegs->eip    = context->uc_mcontext.gregs[REG_EIP];
    outputRegs->eflags = context->uc_mcontext.gregs[REG_EFL];

    // 段寄存器（可选，通常不需要修改）
    outputRegs->xds = context->uc_mcontext.gregs[REG_DS];
    outputRegs->xes = context->uc_mcontext.gregs[REG_ES];
    outputRegs->xfs = context->uc_mcontext.gregs[REG_FS];
    outputRegs->xgs = context->uc_mcontext.gregs[REG_GS];
    outputRegs->xcs = context->uc_mcontext.gregs[REG_CS];
    outputRegs->xss = context->uc_mcontext.gregs[REG_SS];

    // orig_eax 通常在信号上下文中不直接映射，需特殊处理
    outputRegs->orig_eax = -1; // 默认无效值
#endif
}

uint64_t getPc(regs_t *regs) {
#if defined(__aarch64__)
    return regs->pc;
#elif defined(__arm__)
    return regs->uregs[T_REGS_PC];
#elif defined(__x86_64__)
    return regs->rip;
#elif defined(__i386__)
    return regs->eip;
#endif
}

uint64_t getSp(regs_t *regs) {
#if defined(__aarch64__)
    return regs->sp;
#elif defined(__arm__)
    return regs->uregs[T_REGS_SP];
#elif defined(__x86_64__)
    return regs->rsp;
#elif defined(__i386__)
    return regs->esp;
#endif
}
