//
// Created by pengcheng.tan on 2025/3/26.
//

#ifndef TAPM_THREAD_CONTROL_H
#define TAPM_THREAD_CONTROL_H
#include "../thread/tapm_thread.h"
#include "../linkedlist/linked_list.h"

#if defined(__aarch64__)
#define T_REGS_USER_NUM    34
#define T_REGS_MACHINE_NUM 33

#define T_REGS_X0  0
#define T_REGS_X1  1
#define T_REGS_X2  2
#define T_REGS_X3  3
#define T_REGS_X4  4
#define T_REGS_X5  5
#define T_REGS_X6  6
#define T_REGS_X7  7
#define T_REGS_X8  8
#define T_REGS_X9  9
#define T_REGS_X10 10
#define T_REGS_X11 11
#define T_REGS_X12 12
#define T_REGS_X13 13
#define T_REGS_X14 14
#define T_REGS_X15 15
#define T_REGS_X16 16
#define T_REGS_X17 17
#define T_REGS_X18 18
#define T_REGS_X19 19
#define T_REGS_X20 20
#define T_REGS_X21 21
#define T_REGS_X22 22
#define T_REGS_X23 23
#define T_REGS_X24 24
#define T_REGS_X25 25
#define T_REGS_X26 26
#define T_REGS_X27 27
#define T_REGS_X28 28
#define T_REGS_X29 29
#define T_REGS_LR  30
#define T_REGS_SP  31
#define T_REGS_PC  32

static const char *regsLabels[] = {
   "x0",
   "x1",
   "x2",
   "x3",
   "x4",
   "x5",
   "x6",
   "x7",
   "x8",
   "x9",
   "x10",
   "x11",
   "x12",
   "x13",
   "x14",
   "x15",
   "x16",
   "x17",
   "x18",
   "x19",
   "x20",
   "x21",
   "x22",
   "x23",
   "x24",
   "x25",
   "x26",
   "x27",
   "x28",
   "x29",
   "sp",
   "lr",
   "pc"
};

#elif defined(__arm__)
#define T_REGS_USER_NUM    18
#define T_REGS_MACHINE_NUM 16

#define T_REGS_R0   0
#define T_REGS_R1   1
#define T_REGS_R2   2
#define T_REGS_R3   3
#define T_REGS_R4   4
#define T_REGS_R5   5
#define T_REGS_R6   6
#define T_REGS_R7   7
#define T_REGS_R8   8
#define T_REGS_R9   9
#define T_REGS_R10  10
#define T_REGS_R11  11
#define T_REGS_IP   12
#define T_REGS_SP   13
#define T_REGS_LR   14
#define T_REGS_PC   15

static const char *regsLabels[] = {
     "r0",
     "r1",
     "r2",
     "r3",
     "r4",
     "r5",
     "r6",
     "r7",
     "r8",
     "r9",
     "r10",
     "r11",
     "ip",
     "sp",
     "lr",
     "pc"
};

#elif defined(__x86_64__)
#define T_REGS_USER_NUM    27
#define T_REGS_MACHINE_NUM 17

#define T_REGS_RAX 0
#define T_REGS_RDX 1
#define T_REGS_RCX 2
#define T_REGS_RBX 3
#define T_REGS_RSI 4
#define T_REGS_RDI 5
#define T_REGS_RBP 6
#define T_REGS_RSP 7
#define T_REGS_R8  8
#define T_REGS_R9  9
#define T_REGS_R10 10
#define T_REGS_R11 11
#define T_REGS_R12 12
#define T_REGS_R13 13
#define T_REGS_R14 14
#define T_REGS_R15 15
#define T_REGS_RIP 16

#define T_REGS_SP  T_REGS_RSP
#define T_REGS_PC  T_REGS_RIP

static const char *regsLabels[] = {
        "rax",
        "rdx",
        "rcx",
        "rbx",
        "rsi",
        "rdi",
        "rbp",
        "rsp",
        "r8",
        "r9",
        "r10",
        "r11",
        "r12",
        "r13",
        "r14",
        "r15",
        "r16",
};

#elif defined(__i386__)
#define T_REGS_USER_NUM    17
#define T_REGS_MACHINE_NUM 16

#define T_REGS_EAX 0
#define T_REGS_ECX 1
#define T_REGS_EDX 2
#define T_REGS_EBX 3
#define T_REGS_ESP 4
#define T_REGS_EBP 5
#define T_REGS_ESI 6
#define T_REGS_EDI 7
#define T_REGS_EIP 8
#define T_REGS_SP  T_REGS_ESP
#define T_REGS_PC  T_REGS_EIP

static const char *regsLabels[] = {
        "eax",
        "ecx",
        "edx",
        "ebx",
        "esp",
        "ebp",
        "esi",
        "edi",
        "eip"
};

#endif

typedef struct ThreadStatus {
    tApmThread * thread = nullptr;
    bool isSuspend = false;
    bool isGetRegs = false;
    uintptr_t regs[T_REGS_USER_NUM]{};
} ThreadStatus;

void initThreadStatus(LinkedList *inputThreads, LinkedList *outputThreadsStatus);

void suspendThreads(LinkedList *inputThreadsStatus);

void resumeThreads(LinkedList *inputThreadsStatus);

void readThreadsRegs(LinkedList *inputThreadsStatus, tApmThread *crashedThread, ucontext_t *crashThreadUContext);

#endif //TAPM_THREAD_CONTROL_H
