//
// Created by pengcheng.tan on 2025/3/18.
//

#ifndef TAPM_ANR_H
#define TAPM_ANR_H
#include <jni.h>
#include <csignal>

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

#endif //TAPM_ANR_H
