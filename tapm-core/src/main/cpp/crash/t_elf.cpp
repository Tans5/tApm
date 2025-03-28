//
// Created by pengcheng.tan on 2025/3/28.
//
#include "t_elf.h"
#include <elf.h>
#include <cstring>

bool isElfFile(const uint8_t *buffer, size_t bufferSize) {
    if (buffer == nullptr || bufferSize < SELFMAG + 1) {
        return false;
    }

    // 检查 ELF 魔数
    if (memcmp(buffer, ELFMAG, SELFMAG) != 0) {
        return false;
    }

    // 检查 ELF 类别
    uint8_t classType = buffer[EI_CLASS];
#if defined(__LP64__)
    return (classType == ELFCLASS64);
#else
    return (classType == ELFCLASS32);
#endif
}
