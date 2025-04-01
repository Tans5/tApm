//
// Created by pengcheng.tan on 2025/3/28.
//

#ifndef TAPM_T_ELF_H
#define TAPM_T_ELF_H

#include <cstdint>
#include <sys/types.h>
#include "file_mmap.h"
#include "memory_maps.h"

typedef struct T_ElfHeader {
    uint32_t programHeaderOffset = 0;
    uint32_t programHeaderEntrySize = 0;
    uint32_t programHeaderNum = 0;

    uint32_t sectionHeaderOffset = 0;
    uint32_t sectionHeaderEntrySize = 0;
    uint32_t sectionHeaderNum = 0;
    uint32_t sectionNameStrIndex = 0;
} T_ElfHeader;

typedef struct T_ProgramHeader {
    uint32_t type = 0;
    uint32_t flags = 0;
    uint32_t offset = 0;
    uint64_t virtualAddress = 0;
    uint64_t physAddress = 0;
    uint32_t sizeInFile = 0;
    uint32_t sizeInMemory = 0;
    uint32_t align = 0;
} T_ProgramHeader;

typedef struct T_Elf {
    T_ElfHeader elfHeader;
    LinkedList programHeaders;
    Mapped * fileMapped = nullptr;
} T_Elf;

bool isElfFile(const uint8_t *buffer, size_t bufferSize);

bool parseElf(const uint8_t *buffer, T_Elf *output);

bool parseElf(pid_t pid, MemoryMap *map, T_Elf *output);

void recycleElf(T_Elf *toRecycle);

#endif //TAPM_T_ELF_H
