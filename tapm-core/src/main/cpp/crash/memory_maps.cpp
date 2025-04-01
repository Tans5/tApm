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

MemoryMap * findMapByAddress(uintptr_t address, LinkedList *maps) {
    Iterator iterator;
    maps->iterator(&iterator);
    while (iterator.containValue()) {
        auto map = static_cast<MemoryMap *>(iterator.value());
        if (map->startAddr <= address && map->endAddr > address) {
            return map;
        }
        iterator.next();
    }
    return nullptr;
}
