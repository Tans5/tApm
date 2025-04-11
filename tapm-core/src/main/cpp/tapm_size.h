//
// Created by pengcheng.tan on 2025/4/11.
//

#ifndef TAPM_TAPM_SIZE_H
#define TAPM_TAPM_SIZE_H

#include <cstdint>

#if defined(__aarch64__)
#define CPU_ARCH "arm64"
typedef uint64_t addr_t;
#define MAX_THREAD_STACK_SIZE (512 * 1024 * 1024)
#elif defined(__arm__)
#define CPU_ARCH "arm"
typedef uint32_t addr_t;
#define MAX_THREAD_STACK_SIZE (256 * 1024 * 1024)
#elif defined(__x86_64__)
#define CPU_ARCH "x86_64"
typedef uint64_t addr_t;
#define MAX_THREAD_STACK_SIZE (512 * 1024 * 1024)
#elif defined(__i386__)
#define CPU_ARCH "x86"
typedef uint32_t addr_t;
#define MAX_THREAD_STACK_SIZE (256 * 1024 * 1024)
#else
#define CPU_ARCH "unknown"
#endif

#define MAX_STR_SIZE 256

#endif //TAPM_TAPM_SIZE_H
