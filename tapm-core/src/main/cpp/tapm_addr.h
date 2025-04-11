//
// Created by pengcheng.tan on 2025/4/11.
//

#ifndef TAPM_TAPM_ADDR_H
#define TAPM_TAPM_ADDR_H

#include <cstdint>

#if defined(__aarch64__)
#define CPU_ARCH "arm64"
typedef uint64_t addr_t;
#elif defined(__arm__)
#define CPU_ARCH "arm"
typedef uint32_t addr_t;
#elif defined(__x86_64__)
#define CPU_ARCH "x86_64"
typedef uint64_t addr_t;
#elif defined(__i386__)
#define CPU_ARCH "x86"
typedef uint32_t addr_t;
#else
#define CPU_ARCH "unknown"
#endif

#endif //TAPM_TAPM_ADDR_H
