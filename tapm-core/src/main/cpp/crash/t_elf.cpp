//
// Created by pengcheng.tan on 2025/3/28.
//
#include "t_elf.h"
#include <elf.h>
#include <cstring>
#include <link.h>
#include <malloc.h>
#include "file_mmap.h"
#include "memory_maps.h"
#include "../tapm_log.h"

void readString(char* dst, const char * src, int startIndex, int maxSize) {
    for (int i = 0; i < maxSize; i ++) {
        char c = src[startIndex + i];
        dst[i] = c;
        if (c == '\0') {
            break;
        }
    }
}

bool isElfFile(const uint8_t *buffer, size_t bufferSize) {
    if (buffer == nullptr || bufferSize < SELFMAG + 1) {
        return false;
    }

    // 检查 ELF 魔数
    if (memcmp(buffer, ELFMAG, SELFMAG) != 0) {
        return false;
    }

    // 检查 ELF 类别
    uint8_t classType = buffer[EI_CLASS];
#if defined(__LP64__)
    return (classType == ELFCLASS64);
#else
    return (classType == ELFCLASS32);
#endif
}

bool parseElf(const uint8_t *buffer, T_Elf *output) {
    if (isElfFile(buffer, 5)) {
        ElfW(Ehdr) elfHeader;
        size_t position = 0;
        // Read ElfHeader
        memcpy(&elfHeader, buffer + position, sizeof(elfHeader));
        position += sizeof(elfHeader);
        output->elfHeader.programHeaderOffset = elfHeader.e_phoff;
        output->elfHeader.programHeaderEntrySize = elfHeader.e_phentsize;
        output->elfHeader.programHeaderNum = elfHeader.e_phnum;
        output->elfHeader.sectionHeaderOffset = elfHeader.e_shoff;
        output->elfHeader.sectionHeaderEntrySize = elfHeader.e_shentsize;
        output->elfHeader.sectionHeaderNum = elfHeader.e_shnum;
        output->elfHeader.sectionNameStrIndex = elfHeader.e_shstrndx;

        // Read program headers.
        position = output->elfHeader.programHeaderOffset;
        ElfW(Phdr) programHeader;
        for (int i = 0; i < output->elfHeader.programHeaderNum; i ++) {
            memcpy(&programHeader, buffer + position, sizeof(programHeader));
            position += sizeof(programHeader);
            auto h = new T_ProgramHeader;
            h->type = programHeader.p_type;
            h->offset = programHeader.p_offset;
            h->flags = programHeader.p_flags;
            h->virtualAddress = programHeader.p_vaddr;
            h->physAddress = programHeader.p_paddr;
            h->sizeInFile = programHeader.p_filesz;
            h->sizeInMemory = programHeader.p_memsz;
            h->align = programHeader.p_align;
            output->programHeaders.addToLast(h);
        }

        // Read section headers
        // First find .shstrtab section
        position = output->elfHeader.sectionHeaderOffset + output->elfHeader.sectionHeaderEntrySize * output->elfHeader.sectionNameStrIndex;
        ElfW(Shdr) sectionHeader;
        memcpy(&sectionHeader, buffer + position, sizeof(sectionHeader));
        char shStrTab[sectionHeader.sh_size];
        memcpy(shStrTab, buffer + sectionHeader.sh_offset, sectionHeader.sh_size);
        position = output->elfHeader.sectionHeaderOffset;
        for (int i = 0; i < output->elfHeader.sectionHeaderNum; i ++) {
            memcpy(&sectionHeader, buffer + position, sizeof(sectionHeader));
            position += sizeof(sectionHeader);
            auto h = new T_SectionHeader;
            readString(h->name, shStrTab, (int) sectionHeader.sh_name, 256);
            h->type = sectionHeader.sh_type;
            h->offset = sectionHeader.sh_offset;
            h->flags = sectionHeader.sh_flags;
            h->virtualAddress = sectionHeader.sh_addr;
            h->sizeInFile = sectionHeader.sh_size;
            h->link = sectionHeader.sh_link;
            h->info = sectionHeader.sh_info;
            h->align = sectionHeader.sh_addralign;
            h->entrySize = sectionHeader.sh_entsize;
            output->sectionHeaders.addToLast(h);
        }
        return true;
    } else {
        return false;
    }
}

bool parseElf(pid_t pid, MemoryMap *map, T_Elf *output) {
    if (map == nullptr) {
        return false;
    } else {
        auto fileMapped = new Mapped;
        // TODO: Need adjust code.
        if (fileMmapRead(map->pathname, map->offset, 5, fileMapped)) {
            output->fileMapped = fileMapped;
            return parseElf(fileMapped->data, output);
        } else {
            delete fileMapped;
            return false;
        }
    }
}

void recycleElf(T_Elf *toRecycle) {
    if (toRecycle->fileMapped != nullptr) {
        recycleMmap(toRecycle->fileMapped);
        toRecycle->fileMapped = nullptr;
    }
    while (toRecycle->programHeaders.size > 0) {
        auto v = toRecycle->programHeaders.popFirst();
        free(v);
    }
    while (toRecycle->sectionHeaders.size > 0) {
        auto v = toRecycle->sectionHeaders.popFirst();
        free(v);
    }
}


