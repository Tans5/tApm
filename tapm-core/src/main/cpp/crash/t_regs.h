//
// Created by pengcheng.tan on 2025/3/28.
//

#ifndef TAPM_T_REGS_H
#define TAPM_T_REGS_H

#include <sys/ucontext.h>

/***
 * arm64
 *
 寄存器	    别名	    用途
  x0	    -	    参数1 / 返回值
 x1-x7	    -	    参数2-参数8
  x8	    -	    间接结果（如返回大结构体的地址）
 x9-x15	    -	    临时寄存器（调用者保存）
x19-x28	    -	    被调用者保存寄存器
 x29	    FP	    帧指针（Frame Pointer）
 x30	    LR	    链接寄存器（保存返回地址）
  SP	    -	    栈指针（Stack Pointer）
  PC	    -	    程序计数器（当前指令地址）


int add(int a, int b) {
    return a + b;
}

int main() {
    int result = add(3, 4);
    return result;
}

// 函数 add()
add:
    sub sp, sp, #16         // 分配栈空间（16字节对齐）
    str x29, [sp, #0]       // 保存旧的 FP
    str x30, [sp, #8]       // 保存旧的 LR
    add x29, sp, #0         // 设置新的 FP（当前栈顶）
    add w0, w0, w1          // 计算 a + b（结果存入 w0）
    ldr x29, [sp, #0]       // 恢复旧的 FP
    ldr x30, [sp, #8]       // 恢复旧的 LR
    add sp, sp, #16         // 释放栈空间
    ret                     // 返回（跳转到 LR 地址）

// 函数 main()
main:
    sub sp, sp, #16         // 分配栈空间
    str x29, [sp, #0]       // 保存旧的 FP
    str x30, [sp, #8]       // 保存旧的 LR
    add x29, sp, #0         // 设置新的 FP
    mov w0, #3              // 参数1：a = 3（存入 w0）
    mov w1, #4              // 参数2：b = 4（存入 w1）
    bl add                  // 调用 add()（LR 更新为下一条指令地址）
    ldr x29, [sp, #0]       // 恢复旧的 FP
    ldr x30, [sp, #8]       // 恢复旧的 LR
    add sp, sp, #16         // 释放栈空间
    ret                     // 返回


一、假设条件
 **main() 函数起始地址**：0x1000
 **add() 函数起始地址**：0x2000
 初始栈指针（SP）：0x3000

 二、main() 函数执行流程
 1.分配栈空间
 sub sp, sp, #16    ; SP = 0x3000 - 0x10 = 0x2FF0

 分配 16 字节; SP 从 0x3000 减到 0x2FF0

 2.保存旧 FP 和 LR
 str x29, [sp, #0]   ; 旧 FP（例如 0x4000）存入地址 0x2FF0
 str x30, [sp, #8]   ; 旧 LR（例如 0x5000）存入地址 0x2FF8

 旧 FP 和 LR 保存到栈的 0x2FF0 和 0x2FF8 处。

 3. 设置新 FP
 add x29, sp, #0     ; 新 FP = SP（0x2FF0）

 x29 指向当前栈帧基址 0x2FF0

 4. 参数传递
 mov w0, #3          ; 参数1：w0 = 3
 mov w1, #4          ; 参数2：w1 = 4

 5. 调用 add() 函数
 bl add              ; LR = 0x1008（假设 `bl add` 的地址是 0x1004）

 bl add 指令的地址假设为 0x1004，则下一条指令地址是 0x1008，存入 LR。
 PC 跳转到 add() 的入口地址 0x2000。

 三、add() 函数执行流程

 1. 分配栈空间
 sub sp, sp, #16     ; SP = 0x2FF0 - 0x10 = 0x2FE0

 2. 保存旧 FP 和 LR
 str x29, [sp, #0]   ; 旧 FP（0x2FF0）存入地址 0x2FE0
 str x30, [sp, #8]   ; 旧 LR（0x1008）存入地址 0x2FE8

 3. 设置新 FP
 add x29, sp, #0     ; 新 FP = SP（0x2FE0）

 4. 执行计算
 add w0, w0, w1      ; w0 = 3 + 4 = 7

 5. 恢复旧 FP 和 LR
 ldr x29, [sp, #0]   ; 恢复旧 FP = 0x2FF0
 ldr x30, [sp, #8]   ; 恢复旧 LR = 0x1008

 6. 释放栈空间
 add sp, sp, #16     ; SP = 0x2FE0 + 0x10 = 0x2FF0

 7. 返回 main()
 ret                 ; 跳转到 LR（0x1008）

 四、main() 函数恢复执行

 1. 恢复旧 FP 和 LR
 ldr x29, [sp, #0]   ; 恢复旧 FP（例如 0x4000）
 ldr x30, [sp, #8]   ; 恢复旧 LR（例如 0x5000）

 2. 释放栈空间
 add sp, sp, #16     ; SP = 0x2FF0 + 0x10 = 0x3000

 3. 返回
 ret                 ; 返回到调用者（例如系统或父函数）

 */
#if defined(__aarch64__)

/***
struct user_regs_struct {
  uint64_t regs[31];
  uint64_t sp;
  uint64_t pc;
  uint64_t pstate;
};
 */

typedef struct user_regs_struct regs_t;

#elif defined(__arm__)
/**
struct user_regs {
  unsigned long uregs[18];
};
 */
typedef struct user_regs regs_t;
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
#define T_REGS_CPSR 16
#define T_REGS_FAULT_ADDRESS 17

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
/**
struct user_regs_struct {
  unsigned long r15;
  unsigned long r14;
  unsigned long r13;
  unsigned long r12;
  unsigned long rbp;
  unsigned long rbx;
  unsigned long r11;
  unsigned long r10;
  unsigned long r9;
  unsigned long r8;
  unsigned long rax;
  unsigned long rcx;
  unsigned long rdx;
  unsigned long rsi;
  unsigned long rdi;
  unsigned long orig_rax;
  unsigned long rip;
  unsigned long cs;
  unsigned long eflags;
  unsigned long rsp;
  unsigned long ss;
  unsigned long fs_base;
  unsigned long gs_base;
  unsigned long ds;
  unsigned long es;
  unsigned long fs;
  unsigned long gs;
};
 */
typedef struct user_regs_struct regs_t;

#elif defined(__i386__)
/**
struct user_regs_struct {
  long ebx;
  long ecx;
  long edx;
  long esi;
  long edi;
  long ebp;
  long eax;
  long xds;
  long xes;
  long xfs;
  long xgs;
  long orig_eax;
  long eip;
  long xcs;
  long eflags;
  long esp;
  long xss;
};
 */
typedef struct user_regs_struct regs_t;

#endif

int readRegsFromPtrace(pid_t tid, regs_t *outputRegs);

int setRegsByPtrace(pid_t tid, regs_t *regs);

void readRegsFromUContext(ucontext_t *context, regs_t *outputRegs);

uint64_t getPc(regs_t *regs);

uint64_t getSp(regs_t *regs);

uint64_t getFp(regs_t *regs);

#endif //TAPM_T_REGS_H
