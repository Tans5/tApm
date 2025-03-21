//
// Created by pengcheng.tan on 2025/3/21.
//

#ifndef TAPM_CRASH_H
#define TAPM_CRASH_H
#include <jni.h>
#include "client/linux/handler/exception_handler.h"

typedef struct Crash {
    JavaVM  *jvm = nullptr;
    jobject jCrashMonitor = nullptr;

    google_breakpad::ExceptionHandler* crashHandler = nullptr;

    int32_t prepare(JNIEnv *jniEnv, jobject jCrashMonitorP, jstring crashFileDir);

    void release(JNIEnv *jniEnv);
} Crash;

#endif //TAPM_CRASH_H
