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
    T_Elf *elf = nullptr;
    bool isLoadedElf = false;
    // elf 文件在文件中的偏移，例如 apk 中的 so 库通常就有一个偏移；而单独文件的 so 就没有偏移。
    addr_t elfFileStart = 0;
    // elf 文件被加载到内存中的偏移部分，例如当前的位置只加载了 DYNAMIC 段，这个段在文件中的偏移量为 4096.
    addr_t elfLoadedStart = 0;
    Mapped * elfFileMap = nullptr;
    MemoryMap *previous = nullptr;
} MemoryMap;

void parseMemoryMaps(pid_t pid, LinkedList *output);

bool tryFindAbortMsg(pid_t pid, LinkedList *maps, char *output);

bool findMemoryMapByAddress(addr_t address, LinkedList *maps, MemoryMap **target);

bool tryLoadElfFileMmap(MemoryMap *memoryMap);

bool tryLoadElf(MemoryMap *memoryMap);

addr_t convertAddressToElfOffset(MemoryMap *memoryMap, addr_t address);

bool loadElfSymbol(addr_t address, LinkedList *maps, char *outputElfPath, addr_t *outputElfOffset, char *outputSymbol, addr_t *outputSymbolOffset) ;

void recycleElfFileMap(MemoryMap *memoryMap);

void recycleMemoryMaps(LinkedList *toRecycle);

#endif //TAPM_MEMORY_MAPS_H
