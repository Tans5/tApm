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

    int32_t signalCatcherTid = -1;

    sigset_t *oldBlockSigSets = nullptr;
    sigaction_p *oldQuitSigAction = nullptr;

    int32_t prepare(JNIEnv *jniEnv, jobject jAnrObject);

    void release(JNIEnv *jniEnv);
} Anr;

typedef struct AnrData{
    int64_t anrTime = 0L;
    bool isFromMe = false;
} AnrData;

#endif //TAPM_ANR_H
