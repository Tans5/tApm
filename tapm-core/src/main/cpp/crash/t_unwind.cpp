//
// Created by pengcheng.tan on 2025/4/8.
//
#include <cstring>
#include "t_unwind.h"
#include "../tapm_log.h"
#include "t_regs.h"
#include "unwindstack/AndroidUnwinder.h"
#include "unwindstack/Regs.h"
#include "unwindstack/RegsX86.h"
#include "unwindstack/RegsArm64.h"
#include "unwindstack/RegsX86_64.h"
#include "unwindstack/RegsArm.h"
#include "unwindstack/UserArm.h"
#include "unwindstack/UserArm64.h"
#include "unwindstack/UserX86.h"
#include "unwindstack/UserX86_64.h"
#include <cxxabi.h>

void copyString(char *dst, const char *src) {
    auto srcLen = strlen(src);
    if (srcLen > MAX_STR_SIZE - 1) {
        srcLen = MAX_STR_SIZE - 1;
    }
    memcpy(dst, src, srcLen);
    dst[srcLen] = '\0';
}

bool unwindFramesByUnwindStack(ThreadStatus *targetThread, unwindstack::AndroidUnwinder* unwinder, LinkedList* outputFrames, int maxFrameSize) {
    if (maxFrameSize <= 0 || !targetThread->isGetRegs) {
        return false;
    }
    unwindstack::Regs *regs = nullptr;
    unwindstack::ArchEnum arch;
    void * regsBuffer = nullptr;
#if defined(__aarch64__)
    arch = unwindstack::ARCH_ARM64;
#elif defined(__arm__)
    arch = unwindstack::ARCH_ARM;
#elif defined(__x86_64__)
    arch = unwindstack::ARCH_X86_64;
#elif defined(__i386__)
    arch = unwindstack::ARCH_X86;
#endif
    if (targetThread->crashSignalCtx != nullptr) {
        regs = unwindstack::Regs::CreateFromUcontext(arch, targetThread->crashSignalCtx);
    } else {
        auto maxRegsSize = std::max(sizeof(unwindstack::arm_user_regs),
                std::max(sizeof(unwindstack::arm64_user_regs), std::max(sizeof(unwindstack::x86_user_regs), sizeof(unwindstack::x86_64_user_regs))));
        regsBuffer = malloc(maxRegsSize);
        memcpy(regsBuffer, &targetThread->regs, sizeof(targetThread->regs));
        switch (arch) {
            case unwindstack::ARCH_ARM64: {
                regs = unwindstack::RegsArm64::Read(regsBuffer);
                break;
            }
            case unwindstack::ARCH_ARM: {
                regs = unwindstack::RegsArm::Read(regsBuffer);
                break;
            }
            case unwindstack::ARCH_X86_64: {
                regs = unwindstack::RegsX86_64::Read(regsBuffer);
                break;
            }
            case unwindstack::ARCH_X86: {
                regs = unwindstack::RegsX86::Read(regsBuffer);
                break;
            }
            default: {}
        }
    }
    if (regs == nullptr) {
        regs = unwindstack::Regs::RemoteGet(targetThread->thread->tid);
    }
    if (regs != nullptr) {
        unwindstack::AndroidUnwinderData unwinderData((size_t) maxFrameSize);
        auto ret = unwinder->Unwind(regs, unwinderData);
        if (ret) {
            for (const auto& f : unwinderData.frames) {
                auto mf = new Frame;
                mf->pc = f.pc;
                mf->sp = f.sp;
                mf->index = f.num;
                auto map = f.map_info.get();
                if (map != nullptr) {
                    mf->isLoadMap = true;
                    mf->mapStartAddr = map->start();
                    mf->mapEndAddr = map->end();
                    copyString(mf->mapPath, map->name().c_str());
                    mf->elfFileStart =  map->elf_start_offset();
                    mf->elfLoadStart = map->elf_offset();
                    auto elf = map->elf().get();
                    if (elf != nullptr) {
                        mf->isLoadElf = true;
                        copyString(mf->soName, elf->GetSoname().c_str());
                        size_t buildIdSize = elf->GetBuildID().size();
                        const uint8_t * buildIdCode = reinterpret_cast<const uint8_t *>(elf->GetBuildID().c_str());
                        int buildIdWriteIndex = 0;
                        for (int i = 0; i < buildIdSize; i ++) {
                            if (buildIdWriteIndex >= (MAX_STR_SIZE - 2)) {
                                break;
                            }
                            int s = sprintf(reinterpret_cast<char *>(mf->elfBuildId + buildIdWriteIndex), "%02x", buildIdCode[i]);
                            buildIdWriteIndex += s;
                        }
                    }
                }
                if (f.function_name.c_str()[0] != '\0') {
                    mf->isLoadSymbol = true;
                    mf->offsetInElf = f.rel_pc;
                    mf->offsetInSymbol = f.function_offset;
                    int demgangleRet = 0;
                    auto demaglinged = abi::__cxa_demangle(f.function_name.c_str(), nullptr, nullptr, &demgangleRet);
                    if (demaglinged != nullptr) {
                        copyString(mf->symbol, demaglinged);
                    } else {
                        copyString(mf->symbol, f.function_name.c_str());
                    }
                    if (demaglinged != nullptr) {
                        free(demaglinged);
                    }
                }
                outputFrames->addToLast(mf);
            }
        } else {
            LOGE("Unwind fail.");
        }
        if (regsBuffer != nullptr) {
            free(regsBuffer);
        }
        return ret;
    } else {
        LOGE("Create regs fail.");
        if (regsBuffer != nullptr) {
            free(regsBuffer);
        }
        return false;
    }

}

void recycleFrames(LinkedList *toRecycle) {
    while (toRecycle->size > 0) {
        auto v = reinterpret_cast<Frame *>(toRecycle->popFirst());
        delete v;
    }
}
