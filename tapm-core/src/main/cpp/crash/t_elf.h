//
// Created by pengcheng.tan on 2025/3/28.
//

#ifndef TAPM_T_ELF_H
#define TAPM_T_ELF_H

#include <cstdint>
#include <sys/types.h>
#include "file_mmap.h"
#include "memory_maps.h"

typedef struct T_Elf {
    Mapped * fileMapped = nullptr;
} T_Elf;

bool isElfFile(const uint8_t *buffer, size_t bufferSize);

bool parseElf(const uint8_t *buffer, T_Elf *output);

bool parseElf(pid_t pid, MemoryMap *map, T_Elf *output);

void recycleElf(T_Elf *toRecycle);

#endif //TAPM_T_ELF_H
