//
// Created by pengcheng.tan on 2025/4/14.
//
#include <asm-generic/fcntl.h>
#include <fcntl.h>
#include <unistd.h>
#include <malloc.h>
#include <sys/system_properties.h>
#include <cstring>
#include <linux/ptrace.h>
#include "crash_writer.h"
#include "../tapm_log.h"
#include "../time/tapm_time.h"
#include "../thread/tapm_thread.h"
#include "process_read.h"

#define CRASH_START_LINE "*** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***"
#define THREAD_START_LINE "--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---"
#define WRITER_BUFFER_SIZE (1024 * 4)

const char *getSigName(const siginfo_t *si) {
    switch (si->si_signo)
    {
        case SIGABRT:   return "SIGABRT";
        case SIGBUS:    return "SIGBUS";
        case SIGFPE:    return "SIGFPE";
        case SIGILL:    return "SIGILL";
        case SIGSEGV:   return "SIGSEGV";
        case SIGTRAP:   return "SIGTRAP";
        case SIGSYS:    return "SIGSYS";
        case SIGSTKFLT: return "SIGSTKFLT";
        default:        return "?";
    }
}

const char *getSigCodeName(const siginfo_t *si) {
    switch (si->si_signo) {
        case SIGBUS:
            switch(si->si_code)
            {
                case BUS_ADRALN:    return "BUS_ADRALN";
                case BUS_ADRERR:    return "BUS_ADRERR";
                case BUS_OBJERR:    return "BUS_OBJERR";
                case BUS_MCEERR_AR: return "BUS_MCEERR_AR";
                case BUS_MCEERR_AO: return "BUS_MCEERR_AO";
                default:            break;
            }
            break;
        case SIGFPE:
            switch(si->si_code)
            {
                case FPE_INTDIV:   return "FPE_INTDIV";
                case FPE_INTOVF:   return "FPE_INTOVF";
                case FPE_FLTDIV:   return "FPE_FLTDIV";
                case FPE_FLTOVF:   return "FPE_FLTOVF";
                case FPE_FLTUND:   return "FPE_FLTUND";
                case FPE_FLTRES:   return "FPE_FLTRES";
                case FPE_FLTINV:   return "FPE_FLTINV";
                case FPE_FLTSUB:   return "FPE_FLTSUB";
                default:           break;
            }
            break;
        case SIGILL:
            switch(si->si_code)
            {
                case ILL_ILLOPC:   return "ILL_ILLOPC";
                case ILL_ILLOPN:   return "ILL_ILLOPN";
                case ILL_ILLADR:   return "ILL_ILLADR";
                case ILL_ILLTRP:   return "ILL_ILLTRP";
                case ILL_PRVOPC:   return "ILL_PRVOPC";
                case ILL_PRVREG:   return "ILL_PRVREG";
                case ILL_COPROC:   return "ILL_COPROC";
                case ILL_BADSTK:   return "ILL_BADSTK";
                default:           break;
            }
            break;
        case SIGSEGV:
            switch(si->si_code)
            {
                case SEGV_MAPERR:  return "SEGV_MAPERR";
                case SEGV_ACCERR:  return "SEGV_ACCERR";
                case SEGV_BNDERR:  return "SEGV_BNDERR";
                case SEGV_PKUERR:  return "SEGV_PKUERR";
                default:           break;
            }
            break;
        case SIGTRAP:
            switch(si->si_code)
            {
                case TRAP_BRKPT:   return "TRAP_BRKPT";
                case TRAP_TRACE:   return "TRAP_TRACE";
                case TRAP_BRANCH:  return "TRAP_BRANCH";
                case TRAP_HWBKPT:  return "TRAP_HWBKPT";
                default:           break;
            }
            if((si->si_code & 0xff) == SIGTRAP)
            {
                switch((si->si_code >> 8) & 0xff)
                {
                    case PTRACE_EVENT_FORK:       return "PTRACE_EVENT_FORK";
                    case PTRACE_EVENT_VFORK:      return "PTRACE_EVENT_VFORK";
                    case PTRACE_EVENT_CLONE:      return "PTRACE_EVENT_CLONE";
                    case PTRACE_EVENT_EXEC:       return "PTRACE_EVENT_EXEC";
                    case PTRACE_EVENT_VFORK_DONE: return "PTRACE_EVENT_VFORK_DONE";
                    case PTRACE_EVENT_EXIT:       return "PTRACE_EVENT_EXIT";
                    case PTRACE_EVENT_SECCOMP:    return "PTRACE_EVENT_SECCOMP";
                    case PTRACE_EVENT_STOP:       return "PTRACE_EVENT_STOP";
                    default:                      break;
                }
            }
            break;
        case SIGSYS:
            switch(si->si_code)
            {
                case SYS_SECCOMP: return "SYS_SECCOMP";
                default:          break;
            }
            break;
        default:
            break;
    }

    switch (si->si_code) {
        case SI_USER:     return "SI_USER";
        case SI_KERNEL:   return "SI_KERNEL";
        case SI_QUEUE:    return "SI_QUEUE";
        case SI_TIMER:    return "SI_TIMER";
        case SI_MESGQ:    return "SI_MESGQ";
        case SI_ASYNCIO:  return "SI_ASYNCIO";
        case SI_SIGIO:    return "SI_SIGIO";
        case SI_TKILL:    return "SI_TKILL";
        case SI_DETHREAD: return "SI_DETHREAD";
    }
    return "?";
}

bool hasFaultAddress(const siginfo_t *si) {
    if(si->si_code == SI_USER || si->si_code == SI_QUEUE || si->si_code == SI_TKILL) return 0;
    switch (si->si_signo)
    {
        case SIGBUS:
        case SIGFPE:
        case SIGILL:
        case SIGSEGV:
        case SIGTRAP:
            return true;
        default:
            return false;
    }
}
void writeRegs(regs_t *regs, char *buffer, int *bufferPosition) {
#if defined(__aarch64__)
    auto size = sprintf(buffer + *bufferPosition, "    x0  %016lx  x1  %016lx  x2  %016lx  x3  %016lx\n    x4  %016lx  x5  %016lx  x6  %016lx  x7  %016lx\n    x8  %016lx  x9  %016lx  x10 %016lx  x11 %016lx\n    x12 %016lx  x13 %016lx  x14 %016lx  x15 %016lx\n    x16 %016lx  x17 %016lx  x18 %016lx  x19 %016lx\n    x20 %016lx  x21 %016lx  x22 %016lx  x23 %016lx\n    x24 %016lx  x25 %016lx  x26 %016lx  x27 %016lx\n    x28 %016lx  x29 %016lx\n    lr  %016lx  sp  %016lx  pc  %016lx  pst  %016lx\n\n",
                        regs->regs[0], regs->regs[1], regs->regs[2], regs->regs[3],
                        regs->regs[4], regs->regs[5], regs->regs[6], regs->regs[7],
                        regs->regs[8], regs->regs[9], regs->regs[10], regs->regs[11],
                        regs->regs[12], regs->regs[13], regs->regs[14], regs->regs[15],
                        regs->regs[16], regs->regs[17], regs->regs[18], regs->regs[19],
                        regs->regs[20], regs->regs[21], regs->regs[22], regs->regs[23],
                        regs->regs[24], regs->regs[25], regs->regs[26], regs->regs[27],
                        regs->regs[28], regs->regs[29],
                        regs->regs[30], regs->sp, regs->pc, regs->pstate);
    *bufferPosition = *bufferPosition + size;
#elif defined(__arm__)
    auto size = sprintf(buffer + *bufferPosition, "    r0  %016lx  r1  %016lx  r2  %016lx  r3  %016lx\n    r4  %016lx  r5  %016lx  r6  %016lx  r7  %016lx\n    r8  %016lx  r9  %016lx  r10 %016lx  r11 %016lx\n    ip  %016lx  sp  %016lx  lr  %016lx  pc  %016lx\n\n",
                        regs->uregs[T_REGS_R0], regs->uregs[T_REGS_R1], regs->uregs[T_REGS_R2], regs->uregs[T_REGS_R3],
                        regs->uregs[T_REGS_R4], regs->uregs[T_REGS_R5], regs->uregs[T_REGS_R6], regs->uregs[T_REGS_R7],
                        regs->uregs[T_REGS_R8], regs->uregs[T_REGS_R9], regs->uregs[T_REGS_R10], regs->uregs[T_REGS_R11],
                        regs->uregs[T_REGS_IP], regs->uregs[T_REGS_SP], regs->uregs[T_REGS_LR], regs->uregs[T_REGS_PC]);
    *bufferPosition = *bufferPosition + size;
#elif defined(__x86_64__)
    auto size = sprintf(buffer + *bufferPosition, "    rax %016lx  rbx %016lx  rcx %016lx  rdx %016lx\n    r8  %016lx  r9  %016lx  r10 %016lx  r11 %016lx\n    r12 %016lx  r13 %016lx  r14 %016lx  r15 %016lx\n    rdi %016lx  rsi %016lx\n    rbp %016lx  rsp %016lx  rip %016lx\n\n",
                        regs->rax, regs->rbx, regs->rcx, regs->rdx,
                        regs->r8, regs->r9, regs->r10, regs->r11,
                        regs->r12, regs->r13, regs->r14, regs->r15,
                        regs->rdi, regs->rsi,
                        regs->rbp, regs->rsp, regs->rip);
    *bufferPosition = *bufferPosition + size;
#elif defined(__i386__)
    auto size = sprintf(buffer + *bufferPosition, "    eax %016x  ebx %016x  ecx %016x  edx %016x\n    edi %016x  esi %016x\n    ebp %016x  esp %016x  eip %016x\n\n",
                        regs->eax, regs->ebx, regs->ecx, regs->edx,
                        regs->edi, regs->esi,
                        regs->ebp, regs->esp, regs->eip);
    *bufferPosition = *bufferPosition + size;
#endif
}

int writeCrash(
        int sig,
        siginfo_t *sigInfo,
        ucontext_t *userContext,
        int64_t startTime,
        int64_t crashTime,
        pid_t crashPid,
        pid_t crashTid,
        uid_t crashUid,
        char *crashFilePath,
        LinkedList *memoryMaps,
        LinkedList *threadsStatus,
        ThreadStatus *crashedThreadStatus
) {
    auto crashFileFd = open(crashFilePath, O_CREAT | O_RDWR, 0666);
    if (crashFileFd == -1) {
        LOGE("Create crash file fail");
        return -1;
    }
    char *writerBuffer = static_cast<char *>(malloc(WRITER_BUFFER_SIZE));
    char strBuffer[MAX_STR_SIZE];
    int bufferPosition = 0;

    /**
     * Write header.
     */
    bufferPosition += sprintf(writerBuffer + bufferPosition, "%s\n", CRASH_START_LINE);
    // Build fingerprint
    if (__system_property_get("ro.build.fingerprint", strBuffer) == 0) {
        strncpy(strBuffer, "unknown", MAX_STR_SIZE);
    }
    bufferPosition += sprintf(writerBuffer + bufferPosition, "Build fingerprint: '%s'\n", strBuffer);
    // Revision
    if (__system_property_get("ro.revision", strBuffer) == 0) {
        if (__system_property_get("ro.boot.hardware.revision", strBuffer) == 0) {
            strncpy(strBuffer, "unknown", MAX_STR_SIZE);
        }
    }
    bufferPosition += sprintf(writerBuffer + bufferPosition, "Revision: '%s'\n", strBuffer);
    // Abi
    bufferPosition += sprintf(writerBuffer + bufferPosition, "Abi: '%s'\n", CPU_ARCH);
    // Time
    formatTime(crashTime, strBuffer, MAX_STR_SIZE);
    bufferPosition += sprintf(writerBuffer + bufferPosition, "Timestamp: %s\n", strBuffer);
    // Process uptime
    auto uptimeInSeconds = (double) (crashTime - startTime) / 1000.0;
    if (uptimeInSeconds > 60) {
        auto uptimeInMins = uptimeInSeconds / 60.0;
        bufferPosition += sprintf(writerBuffer + bufferPosition, "Process uptime: %.1lf%c\n", uptimeInMins, 'm');
    } else {
        bufferPosition += sprintf(writerBuffer + bufferPosition, "Process uptime: %.1lf%c\n", uptimeInSeconds, 's');
    }
    // Cmdline
    if (getCmdline(crashPid, strBuffer) != 0) {
        strncpy(strBuffer, "unknown", MAX_STR_SIZE);
    }
    bufferPosition += sprintf(writerBuffer + bufferPosition, "Cmdline: %s\n", strBuffer);
    // pid/tid/crashThread
    bufferPosition += sprintf(writerBuffer + bufferPosition, "pid: %d, tid: %d, name: %s  >>> %s <<<\n", crashPid, crashTid, crashedThreadStatus->thread->threadName, strBuffer);
    // uid
    bufferPosition += sprintf(writerBuffer + bufferPosition, "uid: %d\n", crashUid);
    write(crashFileFd, writerBuffer, bufferPosition);
    bufferPosition = 0;

    /**
     * Write crash regs
     */
    // signal/code
    bufferPosition += sprintf(writerBuffer + bufferPosition, "signal %d (%s), code %d (%s", sig, getSigName(sigInfo), sigInfo->si_code, getSigCodeName(sigInfo));
    // sigfrom
    if ((SI_FROMUSER(sigInfo) && (sigInfo->si_pid != 0) && (sigInfo->si_pid != crashPid))) {
        bufferPosition += sprintf(writerBuffer + bufferPosition, " from pid %d, uid %d), ", sigInfo->si_pid, sigInfo->si_uid);
    } else {
        bufferPosition += sprintf(writerBuffer + bufferPosition, "), ");
    }
    // fault address
    if (hasFaultAddress(sigInfo)) {
        auto faultAddress = sigInfo->si_addr;
        if (sig == SIGILL) {
            uint32_t instruction = 0;
            processRead(crashPid, (addr_t) faultAddress, &instruction, sizeof(uint32_t));
            bufferPosition += sprintf(writerBuffer + bufferPosition, "fault addr 0x%016x (*pc=%#08x)\n", faultAddress, instruction);
        } else {
            bufferPosition += sprintf(writerBuffer + bufferPosition, "fault addr 0x%016x\n", faultAddress);
        }
    } else {
        bufferPosition += sprintf(writerBuffer + bufferPosition, "fault addr --------\n");
    }
    // Abort msg
    if (tryFindAbortMsg(crashPid, memoryMaps, strBuffer)) {
        bufferPosition += sprintf(writerBuffer + bufferPosition, "Abort message: '%s'\n", strBuffer);
    }
    // regs
    writeRegs(&crashedThreadStatus->regs, writerBuffer, &bufferPosition);
    write(crashFileFd, writerBuffer, bufferPosition);
    bufferPosition = 0;

    /**
     * Write crash thread. backtrace
     */
    // TODO:

    /**
     * Write other threads.
     */
    // TODO:
    free(writerBuffer);
    close(crashFileFd);
    return 0;
}
