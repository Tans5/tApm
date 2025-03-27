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
    }
    file.close();
}
