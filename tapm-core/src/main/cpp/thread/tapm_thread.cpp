//
// Created by pengcheng.tan on 2025/3/25.
//
#include <cstdio>
#include <dirent.h>
#include <cstdlib>
#include <cerrno>
#include <fcntl.h>
#include <unistd.h>
#include <cstring>
#include "tapm_thread.h"

#define BUFFER_SIZE 4096

void forEachThreads(pid_t pid, void * context, bool (*action)(const pid_t tid, const char * threadName, int threadNameSize, void *context)) {
    char processPath[BUFFER_SIZE];
    int size = sprintf(processPath, "/proc/%d/task", pid);
    if (size >= BUFFER_SIZE) {
        return;
    }
    DIR * processDir = opendir(processPath);
    if (processDir != nullptr) {
        dirent *child = readdir(processDir);
        char fileNameFilePath[BUFFER_SIZE];
        char threadName[BUFFER_SIZE];
        while (child != nullptr) {
            auto childFileName = child->d_name;
            char * endPtr;
            long numToCheck = strtol(childFileName, &endPtr, 10);
            if (endPtr != childFileName && errno != ERANGE) {
                // success.
                pid_t tid = numToCheck;

                size = sprintf(fileNameFilePath, "%s/%s/comm", processPath, childFileName);
                if (size >= BUFFER_SIZE) {
                    continue;
                }
                int fd = open(fileNameFilePath, O_RDONLY);
                if (fd == -1) {
                    continue;
                }
                size = read(fd, threadName, BUFFER_SIZE);
                close(fd);
                auto isGoOn = action(tid, threadName, size, context);
                if (!isGoOn) {
                    break;
                }
            }
            child = readdir(processDir);
        }
        closedir(processDir);
    }
}

void getProcessThreads(pid_t pid, LinkedList *output) {
    // TODO:
}

typedef struct FindThreadByNameContext {
    const char * targetThreadName = nullptr;
    tApmThread * output = nullptr;
} FindThreadByNameContext;

int findThreadByName(pid_t pid, const char * threadName, tApmThread *output) {
    FindThreadByNameContext context {
        .targetThreadName = threadName,
        .output = output
    };
    auto action = [](const pid_t tid, const char * tName, int tNameSize, void* c) -> bool {
        auto context = static_cast<FindThreadByNameContext *>(c);
        if (strcmp(tName, context->targetThreadName) == 0) {
            context->output->tid = tid;
            memcpy(context->output->threadName, tName, tNameSize);
            return false;
        } else {
            return true;
        }
    };
    forEachThreads(pid, &context, action);
    if (output->tid != 0) {
        return 0;
    } else {
        return -1;
    }
}


int findThreadByTid(pid_t pid, pid_t tid, tApmThread * output) {
    // TODO:

    return -1;
}



