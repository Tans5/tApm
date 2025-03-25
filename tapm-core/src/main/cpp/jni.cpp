//
// Created by pengcheng.tan on 2025/3/17.
//

#include <jni.h>
#include "anr/anr.h"
#include "crash/crash.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_tans_tapm_monitors_AnrMonitor_registerAnrMonitorNative(
        JNIEnv * env,
        jobject javaAnrMonitor) {

    auto anr = new Anr;
    auto ret = anr->prepare(env, javaAnrMonitor);
    if (ret == 0) {
        return (int64_t) anr;
    } else {
        anr->release(env);
        return 0L;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_tans_tapm_monitors_AnrMonitor_unregisterAnrMonitorNative(
        JNIEnv * env,
        jobject javaAnrMonitor,
        jlong anrPtr) {
    if (anrPtr != 0L) {
        reinterpret_cast<Anr *>(anrPtr)->release(env);
    }
}


extern "C" JNIEXPORT jlong JNICALL
Java_com_tans_tapm_monitors_NativeCrashMonitor_registerNativeCrashMonitorNative(
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
Java_com_tans_tapm_monitors_NativeCrashMonitor_testNativeCrash(
        JNIEnv *env,
        jobject javaAnrMonitor) {
    Crash *c = nullptr;
    JNIEnv *e = nullptr;
    c->jvm->GetEnv(reinterpret_cast<void **>(&e), JNI_VERSION_1_6);
}

extern "C" JNIEXPORT void JNICALL
Java_com_tans_tapm_monitors_NativeCrashMonitor_unregisterNativeCrashMonitorNative(
        JNIEnv *env,
        jobject javaAnrMonitor,
        jlong crashPtr) {
    if (crashPtr != 0L) {
        reinterpret_cast<Crash *>(crashPtr)->release(env);
    }
}