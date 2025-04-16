//
// Created by pengcheng.tan on 2025/3/27.
//

#ifndef TAPM_MEMORY_MAPS_H
#define TAPM_MEMORY_MAPS_H

#include <cstdint>
#include <sys/types.h>
#include <../linkedlist/linked_list.h>
#include "file_mmap.h"
#include "../tapm_size.h"

#define ANDROID_10_ABORT_MSG_MAP_PATH "[anon:abort message]"

#define ANDROID_10_ABORT_MSG_MAGIC_1 0xb18e40886ac388f0ULL
#define ANDROID_10_ABORT_MSG_MAGIC_2 0xc6dfba755a1de0b5ULL

typedef struct MemoryMap {
    addr_t startAddr = 0;
    addr_t endAddr = 0;
    struct {
        bool read = false;
        bool write = false;
        bool exec = false;
        bool shared = false; // p=private, s=shared
    } permissions;
    addr_t offset = 0;
    struct {
        unsigned major = 0;
        unsigned minor = 0;
    } device;
    uint64_t inode = 0;
    char pathname[MAX_STR_SIZE] {};
    bool isMapPortDevice = false;
    MemoryMap *previous = nullptr;
} MemoryMap;

void parseMemoryMaps(pid_t pid, LinkedList *output);

bool tryFindAbortMsg(pid_t pid, LinkedList *maps, char *output);

bool findMemoryMapByAddress(addr_t address, LinkedList *maps, MemoryMap **target);

void recycleMemoryMaps(LinkedList *toRecycle);

#endif //TAPM_MEMORY_MAPS_H
