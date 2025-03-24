//
// Created by pengcheng.tan on 2025/3/24.
//
#include <jni.h>
#include "crash/crash.h"


extern "C" JNIEXPORT jlong JNICALL
Java_com_tans_tapm_breakpad_BreakpadNativeCrashMonitor_registerNativeCrashMonitorNative(
        JNIEnv *env,
        jobject javaAnrMonitor,
        jstring crashFileDir) {
    auto crash = new Crash;
    auto ret = crash->prepare(env, javaAnrMonitor, crashFileDir);
    if (ret == 0) {
        return (int64_t) crash;
    } else {
        crash->release(env);
        return 0L;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_tans_tapm_breakpad_BreakpadNativeCrashMonitor_testNativeCrash(
        JNIEnv *env,
        jobject javaAnrMonitor) {
    Crash *c = nullptr;
    JNIEnv *e = nullptr;
    c->jvm->GetEnv(reinterpret_cast<void **>(&e), JNI_VERSION_1_6);
}

extern "C" JNIEXPORT void JNICALL
Java_com_tans_tapm_breakpad_BreakpadNativeCrashMonitor_unregisterNativeCrashMonitorNative(
        JNIEnv *env,
        jobject javaAnrMonitor,
        jlong crashPtr) {
    if (crashPtr != 0L) {
        reinterpret_cast<Crash *>(crashPtr)->release(env);
    }
}
