//
// Created by pengcheng.tan on 2025/3/28.
//
#include <bits/sysconf.h>
#include <sys/mman.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include "file_mmap.h"

bool fileMmapRead(const char* filePath, addr_t offset, uint64_t requireMinSize, Mapped *output) {

    if (access(filePath, F_OK) != 0) {
        return false;
    }
    if (access(filePath, R_OK) != 0) {
        return false;
    }

    auto fileFd = open(filePath, O_RDONLY | O_CLOEXEC);
    if (fileFd == -1) {
        return false;
    }
    struct stat s {};
    fstat(fileFd, &s);
    if (fileMmapRead(fileFd, s.st_size, offset, requireMinSize, output)) {
        return true;
    } else {
        recycleFileMmap(output);
        return false;
    }
}

bool fileMmapRead(int fileFd, uint64_t fileSize, addr_t offset, uint64_t requireMinSize, Mapped *output) {
    if (offset + requireMinSize > fileSize || fileFd < 0 || !output) {
        return false;
    }
    // 获取系统页大小，通常是 4096
    uint64_t pageSize = (size_t)sysconf(_SC_PAGE_SIZE);

    // 等于：(offset / page_size) * page_size
    uint64_t alignOffset = offset & (~(pageSize - 1));
    // 计算当前偏移量在内存页内的偏移，等于：offset % page_size
    uint64_t pageOffset = offset & (pageSize - 1);

    uint64_t mapSize = fileSize - alignOffset;

    if (mapSize - pageOffset < 0) {
        return false;
    }

    void * mapped = mmap(nullptr, mapSize, PROT_READ, MAP_PRIVATE, fileFd, (off_t) alignOffset);
    if (mapped == MAP_FAILED) {
        return false;
    }
    output->fileFd = fileFd;
    output->offset = offset;
    output->mmap = mapped;
    output->data = (uint8_t *) mapped + pageOffset;
    output->mappedSize = mapSize;
    output->dataSize = mapSize - pageOffset;
    return true;
}

void recycleFileMmap(Mapped *toRecycle) {
    if (toRecycle == nullptr) {
        return;
    }
    if (toRecycle->mmap != nullptr) {
        munmap(toRecycle->mmap, toRecycle->mappedSize);
        toRecycle->mmap = nullptr;
    }
    if (toRecycle->fileFd != -1) {
        close(toRecycle->fileFd);
        toRecycle->fileFd = -1;
    }
    toRecycle->dataSize = 0;
    toRecycle->offset = 0;
    toRecycle->mappedSize = 0;
}
