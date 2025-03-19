//
// Created by pengcheng.tan on 2025/3/18.
//

#ifndef TAPM_ANR_H
#define TAPM_ANR_H
#include <jni.h>
#include <csignal>

#ifndef SIGNAL_CATCHER_BUFFER_SIZE
#define SIGNAL_CATCHER_BUFFER_SIZE 4096
#endif

typedef struct sigaction sigaction_p;

typedef struct Anr {
    /**
     * Java
     */
    JavaVM *jvm = nullptr;
    jobject jAnrObject = nullptr;
    const char * anrTraceOutputDir = nullptr;

    int32_t signalCatcherTid = -1;

    sigset_t *oldBlockSigSets = nullptr;
    sigaction_p *oldQuitSigAction = nullptr;

    int32_t prepare(JNIEnv *jniEnv, jobject jAnrObject, jstring anrOutputDir);

    void release(JNIEnv *jniEnv);
} Anr;

typedef struct AnrData{
    int64_t anrTime = 0L;
    char * anrFilePath = nullptr;
    bool isSigFromMe = false;
    volatile int anrFileFd = -1;
    volatile int anrFileWriteFinishNotifyFd = -1;
    volatile int epollFd = -1;

    int32_t prepare(const char *baseDir);

    void release();
} AnrData;

#endif //TAPM_ANR_H
