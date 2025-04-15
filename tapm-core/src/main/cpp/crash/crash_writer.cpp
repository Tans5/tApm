//
// Created by pengcheng.tan on 2025/4/14.
//
#include <asm-generic/fcntl.h>
#include <fcntl.h>
#include <unistd.h>
#include <malloc.h>
#include <sys/system_properties.h>
#include <cstring>
#include "crash_writer.h"
#include "../tapm_log.h"
#include "../time/tapm_time.h"
#include "../thread/tapm_thread.h"

#define CRASH_START_LINE "*** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***"
#define THREAD_START_LINE "--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---"

int writeCrash(
        int sig,
        siginfo_t *sigInfo,
        ucontext_t *userContext,
        int64_t startTime,
        int64_t crashTime,
        pid_t crashPid,
        pid_t crashTid,
        uid_t crashUid,
        char *crashFilePath,
        LinkedList *threadsStatus,
        ThreadStatus *crashedThreadStatus
) {
    auto crashFileFd = open(crashFilePath, O_CREAT | O_RDWR, 0666);
    if (crashFileFd == -1) {
        LOGE("Create crash file fail");
        return -1;
    }
    char *writerBuffer = static_cast<char *>(malloc(1024 * 2)); // 2kb
    char strBuffer[MAX_STR_SIZE];
    int bufferPosition = 0;

    /**
     * Write header.
     */
    bufferPosition += sprintf(writerBuffer + bufferPosition, "%s\n", CRASH_START_LINE);
    // Build fingerprint
    if (__system_property_get("ro.build.fingerprint", strBuffer) == 0) {
        strncpy(strBuffer, "unknown", MAX_STR_SIZE);
    }
    bufferPosition += sprintf(writerBuffer + bufferPosition, "Build fingerprint: '%s'\n", strBuffer);
    // Revision
    if (__system_property_get("ro.revision", strBuffer) == 0) {
        if (__system_property_get("ro.boot.hardware.revision", strBuffer) == 0) {
            strncpy(strBuffer, "unknown", MAX_STR_SIZE);
        }
    }
    bufferPosition += sprintf(writerBuffer + bufferPosition, "Revision: '%s'\n", strBuffer);
    // Abi
    bufferPosition += sprintf(writerBuffer + bufferPosition, "Abi: '%s'\n", CPU_ARCH);
    // Time
    formatTime(crashTime, strBuffer, MAX_STR_SIZE);
    bufferPosition += sprintf(writerBuffer + bufferPosition, "Timestamp: %s\n", strBuffer);
    // Process uptime
    auto uptimeInSeconds = (double) (crashTime - startTime) / 1000.0;
    if (uptimeInSeconds > 60) {
        auto uptimeInMins = uptimeInSeconds / 60.0;
        bufferPosition += sprintf(writerBuffer + bufferPosition, "Process uptime: %.1lf%c\n", uptimeInMins, 'm');
    } else {
        bufferPosition += sprintf(writerBuffer + bufferPosition, "Process uptime: %.1lf%c\n", uptimeInSeconds, 's');
    }
    // Cmdline
    if (getCmdline(crashPid, strBuffer) != 0) {
        strncpy(strBuffer, "unknown", MAX_STR_SIZE);
    }
    bufferPosition += sprintf(writerBuffer + bufferPosition, "Cmdline: %s\n", strBuffer);
    // pid/tid/crashThread
    bufferPosition += sprintf(writerBuffer + bufferPosition, "pid: %d, tid: %d, name: %s  >>> %s <<<\n", crashPid, crashTid, crashedThreadStatus->thread->threadName, strBuffer);
    // uid
    bufferPosition += sprintf(writerBuffer + bufferPosition, "uid: %d\n", crashUid);
    write(crashFileFd, writerBuffer, bufferPosition);
    bufferPosition = 0;

    /**
     * Write crash regs
     */
    // TODO:

    /**
     * Write crash thread. backtrace
     */
    // TODO:

    /**
     * Write other threads.
     */
    // TODO:
    free(writerBuffer);
    close(crashFileFd);
    return 0;
}
