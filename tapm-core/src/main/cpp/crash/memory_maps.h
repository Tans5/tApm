//
// Created by pengcheng.tan on 2025/3/27.
//

#ifndef TAPM_MEMORY_MAPS_H
#define TAPM_MEMORY_MAPS_H

#include <cstdint>
#include <sys/types.h>
#include <../linkedlist/linked_list.h>

typedef struct MemoryMap {
    uint64_t startAddr;
    uint64_t endAddr;
    struct {
        bool read;
        bool write;
        bool exec;
        bool shared; // p=private, s=shared
    } permissions;
    uint64_t offset;
    struct {
        unsigned major;
        unsigned minor;
    } device;
    uint64_t inode;
    char pathname[256];
} MemoryMap;

void parseMemoryMaps(pid_t pid, LinkedList *output);

#endif //TAPM_MEMORY_MAPS_H
