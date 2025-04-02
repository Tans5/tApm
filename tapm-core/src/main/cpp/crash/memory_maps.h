//
// Created by pengcheng.tan on 2025/3/27.
//

#ifndef TAPM_MEMORY_MAPS_H
#define TAPM_MEMORY_MAPS_H

#include <cstdint>
#include <sys/types.h>
#include <../linkedlist/linked_list.h>
#include "t_elf.h"
#include "file_mmap.h"

#define ANDROID_10_ABORT_MSG_MAP_PATH "[anon:abort message]"

#define ANDROID_10_ABORT_MSG_MAGIC_1 0xb18e40886ac388f0ULL
#define ANDROID_10_ABORT_MSG_MAGIC_2 0xc6dfba755a1de0b5ULL

typedef struct MemoryMap {
    uint64_t startAddr = 0;
    uint64_t endAddr = 0;
    struct {
        bool read = false;
        bool write = false;
        bool exec = false;
        bool shared = false; // p=private, s=shared
    } permissions;
    uint64_t offset = 0;
    struct {
        unsigned major = 0;
        unsigned minor = 0;
    } device;
    uint64_t inode = 0;
    char pathname[256] {};
    bool isMapPortDevice = false;
    T_Elf *elf = nullptr;
    bool isLoadedElf = false;
    Mapped *elfFileMap = nullptr;
} MemoryMap;

void parseMemoryMaps(pid_t pid, LinkedList *output);

bool tryFindAbortMsg(pid_t pid, LinkedList *maps, char *output);

bool findMemoryMapByAddress(uintptr_t address, LinkedList *maps, MemoryMap **target, MemoryMap ** previous);

bool tryLoadElf(MemoryMap *memoryMap, MemoryMap *previousMemoryMap);

void recycleMemoryMaps(LinkedList *toRecycle);

#endif //TAPM_MEMORY_MAPS_H
