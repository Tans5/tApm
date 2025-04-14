//
// Created by pengcheng.tan on 2025/3/28.
//
#include "t_elf.h"
#include <elf.h>
#include <cstring>
#include <link.h>
#include <cxxabi.h>
#include <malloc.h>
#include "file_mmap.h"
#include "memory_maps.h"
#include "../tapm_log.h"

int readString(char* dst, const char * src, uint32_t startIndex) {
    for (int i = 0; i < MAX_STR_SIZE; i ++) {
        char c = src[startIndex + i];
        dst[i] = c;
        if (c == '\0') {
            return i;
        }
    }
    return MAX_STR_SIZE;
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

const static char SYMTAB[]          = ".symtab";
const static char STRTAB[]          = ".strtab";
const static char DYNSYM[]          = ".dynsym";
const static char DYNSTR[]          = ".dynstr";
const static char DEBUG_FRAME[]     = ".debug_frame";
const static char EH_FRAME[]        = ".eh_frame";
const static char EH_FRAME_HDR[]    = ".eh_frame_hdr";
const static char GNU_DEBUGDATA[]   = ".gnu_debugdata";


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
            h->bias = h->virtualAddress - h->offset;
            switch (h->type) {
                case PT_LOAD:
                    if (0 == (h->flags & PF_X)) {
                        continue;
                    }
                    output->loadXHeader = h;
                    break;
                case PT_GNU_EH_FRAME:
                    output->gnuEhFrameHeader = h;
                    break;
                case PT_ARM_EXIDX:
                    output->armExidxHeader = h;
                    break;
                case PT_DYNAMIC:
                    output->dynamicHeader = h;
                    break;
                case PT_NOTE:
                    output->noteHeader = h;
                    break;
                default: {
                    break;
                }
            }
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
            readString(h->name, shStrTab, (int) sectionHeader.sh_name);
            h->type = sectionHeader.sh_type;
            h->offset = sectionHeader.sh_offset;
            h->flags = sectionHeader.sh_flags;
            h->virtualAddress = sectionHeader.sh_addr;
            h->sizeInFile = sectionHeader.sh_size;
            h->link = sectionHeader.sh_link;
            h->info = sectionHeader.sh_info;
            h->align = sectionHeader.sh_addralign;
            h->entrySize = sectionHeader.sh_entsize;
            h->index = i;
            output->sectionHeaders.addToLast(h);

            if (strncmp(h->name, SYMTAB, sizeof(SYMTAB)) == 0) {
                output->symtabHeader = h;
            } else if (strncmp(h->name, STRTAB, sizeof(STRTAB)) == 0) {
                output->strtabHeader = h;
            } else if (strncmp(h->name, DYNSYM, sizeof(DYNSYM)) == 0) {
                output->dynsymHeader = h;
            } else if (strncmp(h->name, DYNSTR, sizeof(DYNSTR)) == 0) {
                output->dynstrHeader = h;
            } else if (strncmp(h->name, DEBUG_FRAME, sizeof(DEBUG_FRAME)) == 0) {
                output->debugFrameHeader = h;
            } else if (strncmp(h->name, EH_FRAME, sizeof(EH_FRAME)) == 0) {
                output->ehFrameHeader = h;
            } else if (strncmp(h->name, EH_FRAME_HDR, sizeof(EH_FRAME_HDR)) == 0) {
                output->ehFrameHdrHeader = h;
            } else if (strncmp(h->name, GNU_DEBUGDATA, sizeof(GNU_DEBUGDATA)) == 0) {
                output->gnuDebugDataHeader = h;
            }
        }

        // Check symtab link.
        if ((output->symtabHeader != nullptr &&
             output->strtabHeader != nullptr &&
             output->symtabHeader->link != output->strtabHeader->index) || (output->symtabHeader != nullptr && output->strtabHeader == nullptr)) {
            Iterator iterator;
            output->sectionHeaders.iterator(&iterator);
            output->strtabHeader = nullptr;
            while (iterator.containValue()) {
                auto sh = static_cast<T_SectionHeader *>(iterator.value());
                if (sh->index == output->symtabHeader->link && sh->index != 0) {
                    output->strtabHeader = sh;
                    break;
                }
                iterator.next();
            }
        }

        // Check dynsym link.
        if ((output->dynsymHeader != nullptr &&
             output->dynstrHeader != nullptr &&
             output->dynsymHeader->link != output->dynstrHeader->index) || (output->dynsymHeader != nullptr && output->dynstrHeader == nullptr)) {
            Iterator iterator;
            output->sectionHeaders.iterator(&iterator);
            output->strtabHeader = nullptr;
            while (iterator.containValue()) {
                auto sh = static_cast<T_SectionHeader *>(iterator.value());
                if (sh->index == output->dynsymHeader->link && sh->index != 0) {
                    output->strtabHeader = sh;
                    break;
                }
                iterator.next();
            }
        }

        // So name
        if (output->dynamicHeader != nullptr) {
            ElfW(Dyn) dyn;
            auto offset = output->dynamicHeader->offset;
            auto size = output->dynamicHeader->sizeInFile;
            auto entrySize = sizeof(dyn);
            auto count = size / entrySize;
            position = offset;
            addr_t strOffset = -1;
            addr_t soNameOffset = -1;
            for (int i = 0; i < count; i ++) {
                if (strOffset != -1 && soNameOffset != -1) {
                    break;
                }
                memcpy(&dyn, buffer + position, sizeof(dyn));
                position += sizeof(dyn);
                switch (dyn.d_tag) {
                    case DT_SONAME: {
                        soNameOffset = dyn.d_un.d_val;
                        break;
                    }
                    case DT_STRTAB: {
                        strOffset = dyn.d_un.d_val;
                        break;
                    }
                    default: {
                    }
                }
            }
            if (strOffset != -1 && soNameOffset != -1) {
                readString(output->soName, reinterpret_cast<const char *>(buffer + strOffset), (uint32_t) soNameOffset);
            }
        }

        // Build id
        if (output->noteHeader != nullptr) {
            ElfW(Nhdr) nhdr;
            auto offset = output->noteHeader->offset;
            auto size = output->noteHeader->sizeInFile;
            auto headerSize = sizeof(nhdr);
            position = offset;
            unsigned int hasRead = 0;
            while (hasRead < size) {
                memcpy(&nhdr, buffer + position, headerSize);
                hasRead += headerSize;
                position = offset + hasRead;
                const char *name = reinterpret_cast<const char *>(buffer + position);
                auto nameLen = nhdr.n_namesz;
                hasRead += (nameLen + 3) & ~3;
                position = offset + hasRead;
                if (nhdr.n_type == NT_GNU_BUILD_ID && strcmp(name, "GNU") == 0) {
                    auto descLen = nhdr.n_descsz;
                    int buildIdWriteIndex = 0;
                    for (int i = 0; i < descLen; i ++) {
                        if (buildIdWriteIndex >= (MAX_STR_SIZE - 2)) {
                            break;
                        }
                        int s = sprintf(reinterpret_cast<char *>(output->buildId + buildIdWriteIndex), "%02x", (uint8_t) buffer[position + i]);
                        buildIdWriteIndex += s;
                    }
                    break;
                }
                hasRead += (nhdr.n_descsz + 3) & ~3;
                position = offset + hasRead;
            }
        }

        return true;
    } else {
        return false;
    }
}

static bool readAddressSymbol(const uint8_t * elfData, T_SectionHeader *symbolSectionHeader, T_SectionHeader *strSectionHeader, addr_t elfOffset, char * outputSymbolName, addr_t *outputSymbolOffset) {
    bool result = false;
    uint32_t position = symbolSectionHeader->offset;
    ElfW(Sym) symbol;
    uint32_t entrySize = symbolSectionHeader->entrySize;
    uint32_t entryCount = symbolSectionHeader->sizeInFile / entrySize;
    for (int i = 0; i < entryCount; i ++) {
        memcpy(&symbol, elfData + position, sizeof(symbol));
        auto symbolStart = symbol.st_value;
        auto symbolEnd = symbol.st_size + symbolStart;
        if (elfOffset >= symbolStart && elfOffset < symbolEnd) {
            *outputSymbolOffset = (elfOffset - symbolStart);
            readString(outputSymbolName, reinterpret_cast<const char *>(elfData + strSectionHeader->offset), symbol.st_name);
            result = true;
            // do c++ demangling
            int ret = 0;
            auto demaglinged = abi::__cxa_demangle(outputSymbolName, nullptr, nullptr, &ret);
            if (ret == 0 && demaglinged != nullptr) {
                auto l = strlen(demaglinged);
                if (l > MAX_STR_SIZE - 1) {
                    l = MAX_STR_SIZE - 1;
                }
                memcpy(outputSymbolName, demaglinged, l);
                outputSymbolName[l] = '\0';
            }
            if (demaglinged != nullptr) {
                free(demaglinged);
            }
            break;
        }

        position += sizeof(symbol);
    }

    return result;
}

bool readAddressSymbol(const uint8_t *buffer, T_Elf *elf, addr_t elfOffset, char *outputSymbolName, addr_t * outputSymbolOffset) {
    bool result = false;
    if (elf->dynsymHeader != nullptr && elf->dynstrHeader != nullptr) {
        // Find symbol from .dynsym
        result = readAddressSymbol(buffer, elf->dynsymHeader, elf->dynstrHeader, elfOffset,  outputSymbolName, outputSymbolOffset);
    }
    if (!result && elf->symtabHeader != nullptr && elf->strtabHeader != nullptr) {
        // Find symbol from .symtab
        result = readAddressSymbol(buffer, elf->symtabHeader, elf->strtabHeader, elfOffset,  outputSymbolName, outputSymbolOffset);
    }

    return result;
}

void recycleElf(T_Elf *toRecycle) {
    toRecycle->loadXHeader = nullptr;
    toRecycle->gnuEhFrameHeader = nullptr;
    toRecycle->armExidxHeader = nullptr;
    toRecycle->dynamicHeader = nullptr;
    toRecycle->noteHeader = nullptr;
    while (toRecycle->programHeaders.size > 0) {
        auto v = toRecycle->programHeaders.popFirst();
        free(v);
    }

    toRecycle->symtabHeader = nullptr;
    toRecycle->strtabHeader = nullptr;
    toRecycle->dynsymHeader = nullptr;
    toRecycle->dynstrHeader = nullptr;
    toRecycle->debugFrameHeader = nullptr;
    toRecycle->ehFrameHeader = nullptr;
    toRecycle->ehFrameHdrHeader = nullptr;
    toRecycle->gnuEhFrameHeader = nullptr;
    while (toRecycle->sectionHeaders.size > 0) {
        auto v = toRecycle->sectionHeaders.popFirst();
        free(v);
    }
}


