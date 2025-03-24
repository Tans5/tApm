//
// Created by pengcheng.tan on 2025/3/21.
//

#ifndef TAPM_CRASH_H
#define TAPM_CRASH_H
#include <jni.h>
#include "client/linux/handler/exception_handler.h"

#include <android/log.h>
#define LOG_TAG "tApmNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef struct Crash {
    JavaVM  *jvm = nullptr;
    jobject jCrashMonitor = nullptr;

    google_breakpad::ExceptionHandler* crashHandler = nullptr;

    int32_t prepare(JNIEnv *jniEnv, jobject jCrashMonitorP, jstring crashFileDir);

    void release(JNIEnv *jniEnv);
} Crash;

#endif //TAPM_CRASH_H
