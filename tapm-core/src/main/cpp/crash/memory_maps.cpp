//
// Created by pengcheng.tan on 2025/3/27.
//
#include <cstdio>
#include <iostream>
#include <fstream>
#include <sstream>
#include <cstdint>
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

        stream >> std::hex >> memoryMap->offset;

        stream >> device;
        sscanf(device, "%x:%x", &memoryMap->device.major, &memoryMap->device.minor);

        stream >> std::dec >> memoryMap->inode;

        stream >> std::ws; // Skip empty.
        stream >> memoryMap->pathname;
    }
    file.close();
}
