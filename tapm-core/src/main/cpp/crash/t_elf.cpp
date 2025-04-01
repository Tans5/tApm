//
// Created by pengcheng.tan on 2025/3/28.
//
#include "t_elf.h"
#include <elf.h>
#include <cstring>
#include "file_mmap.h"
#include "memory_maps.h"

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

bool parseElf(const uint8_t *buffer, T_Elf *output) {
    if (isElfFile(buffer, 5)) {

        return true;
    } else {
        return false;
    }
}

bool parseElf(pid_t pid, MemoryMap *map, T_Elf *output) {
    if (map == nullptr) {
        return false;
    } else {
        auto fileMapped = new Mapped;
        // TODO: Need adjust code.
        if (fileMmapRead(map->pathname, map->offset, 5, fileMapped)) {
            output->fileMapped = fileMapped;
            return parseElf(fileMapped->data, output);
        } else {
            delete fileMapped;
            return false;
        }
    }
}

void recycleElf(T_Elf *toRecycle) {
    if (toRecycle->fileMapped != nullptr) {
        recycleMmap(toRecycle->fileMapped);
        toRecycle->fileMapped = nullptr;
    }
}


