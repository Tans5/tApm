//
// Created by pengcheng.tan on 2025/3/28.
//

#ifndef TAPM_FILE_MMAP_H
#define TAPM_FILE_MMAP_H

#include <cstdint>

typedef struct Mapped {
    int fileFd = -1;
    void * mmap = nullptr;
    uint8_t * data = nullptr;
    uint64_t dataSize = 0;
    uint64_t offset = 0;
    uint64_t mappedSize = 0;
} Mapped;

bool fileMmapRead(const char* filePath, uint64_t offset, uint64_t requireMinSize, Mapped *output);

bool fileMmapRead(int fileFd, uint64_t fileSize, uint64_t offset, uint64_t requireMinSize, Mapped *output);

void recycleFileMmap(Mapped *toRecycle);

#endif //TAPM_FILE_MMAP_H
