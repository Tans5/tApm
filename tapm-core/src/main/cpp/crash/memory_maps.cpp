//
// Created by pengcheng.tan on 2025/3/27.
//
#include <cstdio>
#include <iostream>
#include <fstream>
#include <sstream>
#include <cstdint>
#include <sys/uio.h>
#include "process_read.h"
#include "../tapm_log.h"
#include "memory_maps.h"
#include "file_mmap.h"

void parseMemoryMaps(pid_t pid, LinkedList *output) {
    using namespace std;
    char filePath[256];
    sprintf(filePath, "/proc/%d/maps", pid);
    std::ifstream file(filePath);
    if (!file.is_open()) {
        LOGE("Open maps file: %s fail.", filePath);
        return;
    }
    string line;
    string path;
    while(getline(file, line)) {
        istringstream stream(line);
        auto memoryMap = new MemoryMap;
        output->addToLast(memoryMap);
        char dash, permissions[5], device[6];

        stream >> hex >> memoryMap->startAddr >> dash >> memoryMap->endAddr;
        stream >> permissions;
        memoryMap->permissions.read  = (permissions[0] == 'r');
        memoryMap->permissions.write = (permissions[1] == 'w');
        memoryMap->permissions.exec  = (permissions[2] == 'x');
        memoryMap->permissions.shared= (permissions[3] == 's');

        stream >> hex >> memoryMap->offset;

        stream >> device;
        sscanf(device, "%x:%x", &memoryMap->device.major, &memoryMap->device.minor);

        stream >> dec >> memoryMap->inode;

        stream >> ws; // Skip empty.
        path.clear();
        getline(stream, path);
        auto chars = path.c_str();
        auto size = path.size();
        if (size > 256) {
            size = 256;
        }
        memcpy(memoryMap->pathname, chars, size);
        if (strncmp(memoryMap->pathname, "/dev/", 5) == 0 && 0 != strncmp(memoryMap->pathname + 5, "ashmem/", 7)) {
            memoryMap->isMapPortDevice = true;
        } else {
            memoryMap->isMapPortDevice = false;
        }
    }
    file.close();
}

bool tryFindAbortMsg(pid_t pid, LinkedList *maps, char *output) {
    int apiLevel = android_get_device_api_level();
    if (apiLevel >= 29) {
        Iterator iterator;
        maps->iterator(&iterator);
        MemoryMap * abortMsgMap = nullptr;
        while (iterator.containValue()) {
            auto m = static_cast<MemoryMap *>(iterator.value());
            if (strcmp(m->pathname, ANDROID_10_ABORT_MSG_MAP_PATH) == 0 && m->permissions.read && m->endAddr > m->startAddr) {
                abortMsgMap = m;
                break;
            }
            iterator.next();
        }
        if (abortMsgMap != nullptr) {
            size_t mapSize = abortMsgMap->endAddr - abortMsgMap->startAddr;
            if (mapSize <= (sizeof(uint64_t) * 2 + sizeof(size_t) + 1)) {
                return false;
            }
            uint64_t readAddress = abortMsgMap->startAddr;
            uint64_t code;
            processRead(pid, readAddress, &code, sizeof(code));
            if (code != ANDROID_10_ABORT_MSG_MAGIC_1) {
                return false;
            }
            readAddress += sizeof(code);
            processRead(pid, readAddress, &code, sizeof(code));
            if (code != ANDROID_10_ABORT_MSG_MAGIC_2) {
                return false;
            }
            readAddress += sizeof(code);
            size_t msgSize;
            processRead(pid, readAddress, &msgSize, sizeof(msgSize));
            if (msgSize <= 0) {
                return false;
            }
            readAddress += sizeof(msgSize);
            if (processRead(pid, readAddress, output, msgSize) > 0) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    } else {
        // TODO: Support blow Android 10
        return false;
    }
}

bool findMemoryMapByAddress(uintptr_t address, LinkedList *maps, MemoryMap **target, MemoryMap ** previous) {
    Iterator iterator;
    maps->iterator(&iterator);
    MemoryMap * p = nullptr;
    while (iterator.containValue()) {
        auto map = static_cast<MemoryMap *>(iterator.value());
        if (map->startAddr <= address && map->endAddr > address) {
            *target = map;
            *previous = p;
            return true;
        }
        p = map;
        iterator.next();
    }
    return false;
}

bool tryLoadElf(MemoryMap *memoryMap, MemoryMap *previousMemoryMap) {
    if (memoryMap == nullptr) {
        return false;
    } else {
        if (!memoryMap->isLoadedElf) {
            memoryMap->isLoadedElf = true;
            auto fileMapped = new Mapped;
            auto elf = new T_Elf;
            bool isSuccess = false;
            if (!memoryMap->permissions.read || !memoryMap->permissions.exec || strlen(memoryMap->pathname) <= 0) {
                goto LoadFromFileEnd;
            }

            //CASE 1: Offset is zero.
            //        The whole file is an ELF?
            //
            // -->    d9b9c000-d9bb6000 r-xp 00000000 fd:00 2666  /system/lib/libjavacrypto.so
            //        d9bb6000-d9bb7000 r--p 00019000 fd:00 2666  /system/lib/libjavacrypto.so
            //        d9bb7000-d9bb8000 rw-p 0001a000 fd:00 2666  /system/lib/libjavacrypto.so
            //
            // Offset 为 0 直接加载 ELF 文件，同时没有偏移量
            if (memoryMap->offset == 0) {
                if (!fileMmapRead(memoryMap->pathname, 0, 5, fileMapped)) {
                    goto LoadFromFileEnd;
                }
                if (parseElf(fileMapped->data, elf)) {
                    isSuccess = true;
                } else {
                    recycleFileMmap(fileMapped);
                }
                goto LoadFromFileEnd;
            }

            //CASE 2: Offset is not zero.
            //        The start of this map is an ELF header? (ELF embedded in another file)
            //
            //        cc2aa000-ce2aa000 rw-p 00000000 00:01 3811616  /dev/ashmem/dalvik-data-code-cache (deleted)
            //        ce2aa000-d02aa000 r-xp 00000000 00:01 3811617  /dev/ashmem/dalvik-jit-code-cache (deleted)
            // -->    d02aa000-d286d000 r-xp 048b3000 fd:00 623      /system/app/WebViewGoogle/WebViewGoogle.apk
            //        d286d000-d286e000 ---p 00000000 00:00 0
            // Offset 不为 0，尝试从 offset 位置加载 ELF。比如 apk 中的 so，通常 so 文件的位置相对 apk 有一个加载的偏移量。
            if (!fileMmapRead(memoryMap->pathname, memoryMap->offset, 5, fileMapped)) {
                goto LoadFromFileEnd;
            }
            if (isElfFile(fileMapped->data, fileMapped->dataSize)) {
                if (parseElf(fileMapped->data, elf)) {
                    isSuccess = true;
                    memoryMap->elfFileStart = memoryMap->offset;
                } else {
                    recycleFileMmap(fileMapped);
                }
                goto LoadFromFileEnd;
            } else {
                recycleFileMmap(fileMapped);
            }

            //CASE 3: Offset is not zero.
            //        No ELF header at the start of this map.
            //        The whole file is an ELF? (this map is part of an ELF file)
            //
            //        72a12000-72a1e000 r--p 00000000 fd:00 1955  /system/framework/arm/boot-apache-xml.oat
            // -->    72a1e000-72a36000 r-xp 0000c000 fd:00 1955  /system/framework/arm/boot-apache-xml.oat
            //        72a36000-72a37000 r--p 00024000 fd:00 1955  /system/framework/arm/boot-apache-xml.oat
            //        72a37000-72a38000 rw-p 00025000 fd:00 1955  /system/framework/arm/boot-apache-xml.oat
            // Offset 不为 0，但是从 Offset 位置开始加载失败，再次尝试从 0 的位置再次加载，有可能加载的 elf 文件不是从头开始加载的只加载 elf 文件中的一部份到内存中。所以再尝试从头开始加载。
            if (!fileMmapRead(memoryMap->pathname, 0, 5, fileMapped)) {
                goto LoadFromFileEnd;
            }
            if (isElfFile(fileMapped->data, fileMapped->dataSize)) {
                if (parseElf(fileMapped->data, elf)) {
                    isSuccess = true;
                    memoryMap->elfLoadedStart = memoryMap->offset;
                } else {
                    recycleFileMmap(fileMapped);
                }
                goto LoadFromFileEnd;
            } else {
                recycleFileMmap(fileMapped);
            }

            //CASE 4: Offset is not zero.
            //        No ELF header at the start of this map.
            //        The whole file is not an ELF.
            //        The start of the previous map is an ELF header? (this map is part of an ELF which embedded in another file)
            //
            //        d1ea6000-d256d000 r--p 0095b000 fc:00 1158  /system/app/Chrome/Chrome.apk
            // -->    d256d000-d5ff0000 r-xp 01022000 fc:00 1158  /system/app/Chrome/Chrome.apk
            //        d5ff0000-d6009000 rw-p 04aa5000 fc:00 1158  /system/app/Chrome/Chrome.apk
            // 从 Offset 位置和 0 的位置都加载失败了，尝试从前一个 MemoryMap 中再去加载
            if (previousMemoryMap != nullptr && previousMemoryMap->permissions.read &&
                previousMemoryMap->offset < memoryMap->offset &&
                strcmp(previousMemoryMap->pathname, memoryMap->pathname) == 0) {

                if (!fileMmapRead(previousMemoryMap->pathname, previousMemoryMap->offset, 5, fileMapped)) {
                    goto LoadFromFileEnd;
                }
                if (isElfFile(fileMapped->data, fileMapped->dataSize)) {
                    if (parseElf(fileMapped->data, elf)) {
                        isSuccess = true;
                        memoryMap->elfFileStart = previousMemoryMap->offset;
                        memoryMap->elfLoadedStart = memoryMap->offset - previousMemoryMap->offset;
                    } else {
                        recycleFileMmap(fileMapped);
                    }
                    goto LoadFromFileEnd;
                } else {
                    recycleFileMmap(fileMapped);
                }
            }

            LoadFromFileEnd:
            if (!isSuccess) {
                delete fileMapped;
                delete elf;
            } else {
                memoryMap->elf = elf;
                memoryMap->elfFileMap = fileMapped;
            }

            return isSuccess;
        } else {
            return memoryMap->elf != nullptr;
        }
    }
}

uint64_t convertAddressToElfOffset(MemoryMap *memoryMap, uint64_t address) {
    uint64_t bias = 0;
    if (memoryMap->elf != nullptr && memoryMap->elf->loadXHeader != nullptr) {
        bias = memoryMap->elf->loadXHeader->bias;
    }
    return address - (memoryMap->startAddr - memoryMap->elfLoadedStart - bias);
}

void recycleMemoryMaps(LinkedList *toRecycle) {
    while (toRecycle->size > 0) {
        auto v = static_cast<MemoryMap *>(toRecycle->popFirst());
        if (v->elf != nullptr) {
            recycleElf(v->elf);
            v->elf = nullptr;
        }
        if (v->elfFileMap != nullptr) {
            recycleFileMmap(v->elfFileMap);
        }
        delete v;
    }
}
