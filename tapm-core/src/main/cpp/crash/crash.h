//
// Created by pengcheng.tan on 2025/3/25.
//

#ifndef TAPM_CRASH_H
#define TAPM_CRASH_H

#include <cstdint>
#include <csignal>

#if defined(__aarch64__)
#define CPU_ARCH "arm64"
#elif defined(__arm__)
#define CPU_ARCH "arm"
#elif defined(__x86_64__)
#define CPU_ARCH "x86_64"
#elif defined(__i386__)
#define CPU_ARCH "x86"
#else
#define CPU_ARCH "unknown"
#endif

static int CRASH_SIGNAL[8] = {
       SIGABRT,
       SIGBUS,
       SIGFPE,
       SIGILL,
       SIGSEGV,
       SIGTRAP,
       SIGSYS,
       SIGSTKFLT
};

typedef struct Crash {
    int64_t startTime = 0;
} Crash;

#endif //TAPM_CRASH_H
