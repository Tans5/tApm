//
// Created by pengcheng.tan on 2025/3/26.
//
#include <sys/ptrace.h>
#include <sys/ucontext.h>
#include <sys/wait.h>
#include <cerrno>
#include "thread_control.h"
#include "../tapm_log.h"


void initThreadStatus(LinkedList *inputThreads, pid_t crashThreadTid, LinkedList *outputThreadsStatus, ThreadStatus **outputCrashThreadStatus) {
    Iterator threadsIterator;
    inputThreads->iterator(&threadsIterator);
    while (threadsIterator.containValue()) {
        auto t = static_cast<tApmThread *>(threadsIterator.value());
        auto threadStatus = new ThreadStatus;
        threadStatus->thread = t;
        outputThreadsStatus->addToLast(threadStatus);
        if (t->tid == crashThreadTid) {
            (*outputCrashThreadStatus) = threadStatus;
        }
        threadsIterator.next();
    }
}

void suspendThreads(LinkedList *inputThreadsStatus) {
    Iterator iterator;
    inputThreadsStatus->iterator(&iterator);
    while (iterator.containValue()) {
        auto threadAndStatus = static_cast<ThreadStatus *>(iterator.value());
        if (!threadAndStatus->isSuspend) {
            auto thread = threadAndStatus->thread;
            int r = ptrace(PTRACE_ATTACH, thread->tid, nullptr, nullptr);
            if (r != 0) {
                LOGE("Attach thread: %s, fail: %d", thread->threadName, r);
            } else {
                errno = 0;
                int ret = 0;
                while ((ret = waitpid(thread->tid, nullptr, __WALL)) < 0) {
                    if (EINTR != errno) {
                        ptrace(PTRACE_DETACH, thread->tid, nullptr, nullptr);
                        LOGE("Wait thread %s attach fail.", thread->threadName);
                        ret = -1;
                        break;
                    }
                    errno = 0;
                }
                if (ret >= 0) {
                    threadAndStatus->isSuspend = true;
                }
            }
        }
        iterator.next();
    }
}

void resumeThreads(LinkedList *inputThreadsStatus) {
    Iterator iterator;
    inputThreadsStatus->iterator(&iterator);
    while (iterator.containValue()) {
        auto status = static_cast<ThreadStatus *>(iterator.value());
        if (status->isSuspend) {
            ptrace(PTRACE_DETACH, status->thread->tid, nullptr, nullptr);
            status->isSuspend = false;
        }
        iterator.next();
    }
}

void readThreadsRegs(LinkedList *inputThreadsStatus, pid_t crashThreadTid, ucontext_t *crashThreadUContext) {
    Iterator iterator;
    inputThreadsStatus->iterator(&iterator);

    while (iterator.containValue()) {
        auto status = static_cast<ThreadStatus *>(iterator.value());
        if (status->isSuspend) {
            if (status->thread->tid == crashThreadTid) {
                readRegsFromUContext(crashThreadUContext, status->regs);
                status->isGetRegs = true;
                status->pc = getPc(status->regs);
                status->sp = getSp(status->regs);
            } else {
                if (readRegsFromPtrace(status->thread->tid, status->regs) == 0) {
                    status->isGetRegs = true;
                    status->pc = getPc(status->regs);
                    status->sp = getSp(status->regs);
                }
            }
        }
        iterator.next();
    }
}

ThreadStatus *findThreadStatus(LinkedList *inputThreadStatus, pid_t tid) {
    Iterator iterator;
    inputThreadStatus->iterator(&iterator);
    while (iterator.containValue()) {
        auto threadStatus = static_cast<ThreadStatus *>(iterator.value());
        if (threadStatus->thread->tid == tid) {
            return threadStatus;
        }
        iterator.next();
    }
    return nullptr;
}

void recycleThreadsStatus(LinkedList *toRecycle) {
    while (toRecycle->size > 0) {
        auto v = static_cast<ThreadStatus *>(toRecycle->popFirst());
        delete v;
    }
}
