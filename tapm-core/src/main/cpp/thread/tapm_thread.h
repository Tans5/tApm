//
// Created by pengcheng.tan on 2025/3/25.
//

#ifndef TAPM_TAPM_THREAD_H
#define TAPM_TAPM_THREAD_H
#include <sys/types.h>
#include "../linkedlist/linked_list.h"
#include "../tapm_size.h"

typedef struct tApmThread {
    char threadName[MAX_STR_SIZE] {};
    pid_t tid = 0;
} tApmThread;

void forEachThreads(pid_t pid, void * context, bool (*action)(const pid_t tid, const char * threadName, int threadNameSize, void * context));

void getProcessThreads(pid_t pid, LinkedList *output);

int findThreadByName(pid_t pid, const char * threadName, tApmThread *output);

int findThreadByTid(pid_t pid, pid_t tid, tApmThread * output);

int getCmdline(pid_t pid, char *output);

void recycleProcessThreads(LinkedList *toRelease);

#endif //TAPM_TAPM_THREAD_H
