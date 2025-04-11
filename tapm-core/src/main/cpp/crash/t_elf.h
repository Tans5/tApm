//
// Created by pengcheng.tan on 2025/3/28.
//

#ifndef TAPM_T_ELF_H
#define TAPM_T_ELF_H

#include <cstdint>
#include <sys/types.h>
#include "../linkedlist/linked_list.h"
#include "../tapm_addr.h"

typedef struct T_ElfHeader {
    uint32_t programHeaderOffset = 0;
    uint32_t programHeaderEntrySize = 0;
    uint32_t programHeaderNum = 0;

    addr_t sectionHeaderOffset = 0;
    uint32_t sectionHeaderEntrySize = 0;
    uint32_t sectionHeaderNum = 0;
    uint32_t sectionNameStrIndex = 0;
} T_ElfHeader;

typedef struct T_ProgramHeader {
    uint32_t type = 0;
    uint32_t flags = 0;
    addr_t offset = 0;
    addr_t virtualAddress = 0;
    addr_t physAddress = 0;
    uint32_t sizeInFile = 0;
    uint32_t sizeInMemory = 0;
    uint32_t align = 0;
    addr_t bias = 0;
} T_ProgramHeader;

typedef struct T_SectionHeader {
    char name[256] {};
    uint32_t type = 0;
    uint32_t flags = 0;
    addr_t offset = 0;
    addr_t virtualAddress = 0;
    uint32_t sizeInFile = 0;
    uint32_t link = 0;
    uint32_t info = 0;
    uint32_t align = 0;
    uint32_t entrySize = 0;
    uint32_t index = 0;
} T_SectionHeader;

typedef struct T_Elf {
    T_ElfHeader elfHeader;
    LinkedList programHeaders;
    // 可执行的 PT_LOAD
    T_ProgramHeader *loadXHeader = nullptr;
    T_ProgramHeader *gnuEhFrameHeader = nullptr;
    T_ProgramHeader *armExidxHeader = nullptr;
    T_ProgramHeader *dynamicHeader = nullptr;

    LinkedList sectionHeaders;
    // 符号表头
    T_SectionHeader *symtabHeader = nullptr;
    // 符号表的字符串头
    T_SectionHeader *strtabHeader = nullptr;
    // 动态符号表头
    T_SectionHeader *dynsymHeader = nullptr;
    // 动态符号表的字符串头
    T_SectionHeader *dynstrHeader = nullptr;
    T_SectionHeader *debugFrameHeader = nullptr;
    T_SectionHeader *ehFrameHeader = nullptr;
    T_SectionHeader *ehFrameHdrHeader = nullptr;
    T_SectionHeader *gnuDebugDataHeader = nullptr;

} T_Elf;

int readString(char* dst, const char * src, uint32_t startIndex, int maxSize);

bool isElfFile(const uint8_t *buffer, size_t bufferSize);

bool parseElf(const uint8_t *buffer, T_Elf *output);

bool readAddressSymbol(const uint8_t *buffer, T_Elf *elf, addr_t elfOffset, char *outputSymbolName, addr_t * outputSymbolOffset);

void recycleElf(T_Elf *toRecycle);

#endif //TAPM_T_ELF_H
