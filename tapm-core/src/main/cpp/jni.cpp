//
// Created by pengcheng.tan on 2025/3/17.
//

#include <jni.h>
#include <cstdlib>
#include "anr/anr.h"
#include "crash/crash.h"
#include <android/set_abort_message.h>

extern "C" JNIEXPORT jlong JNICALL
Java_com_tans_tapm_monitors_AnrMonitor_registerAnrMonitorNative(
        JNIEnv * env,
        jobject javaAnrMonitor,
        jstring anrFileDir) {

    auto anr = new Anr;
    auto ret = anr->prepare(env, javaAnrMonitor, anrFileDir);
    if (ret == 0) {
        return (int64_t) anr;
    } else {
        anr->release(env);
        delete anr;
        return 0L;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_tans_tapm_monitors_AnrMonitor_unregisterAnrMonitorNative(
        JNIEnv * env,
        jobject javaAnrMonitor,
        jlong anrPtr) {
    if (anrPtr != 0L) {
        auto anr = reinterpret_cast<Anr *>(anrPtr);
        anr ->release(env);
        delete anr;
    }
}


extern "C" JNIEXPORT jlong JNICALL
Java_com_tans_tapm_monitors_NativeCrashMonitor_registerNativeCrashMonitorNative(
        JNIEnv *env,
        jobject javaAnrMonitor,
        jstring crashFileDir,
        jstring fingerprint) {
    auto crash = new Crash;
    auto ret = crash->prepare(env, javaAnrMonitor, crashFileDir, fingerprint);
    if (ret == 0) {
        return (int64_t) crash;
    } else {
        crash->release(env);
        delete crash;
        return 0L;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_tans_tapm_monitors_NativeCrashMonitor_testNativeCrash(
        JNIEnv *env,
        jobject javaAnrMonitor) {
    Crash *c = nullptr;
    c->crashOutputDir = "Hello, NativeCrash.";
//      android_set_abort_message("Test abort msg");
//      abort();
}

extern "C" JNIEXPORT void JNICALL
Java_com_tans_tapm_monitors_NativeCrashMonitor_unregisterNativeCrashMonitorNative(
        JNIEnv *env,
        jobject javaAnrMonitor,
        jlong crashPtr) {
    if (crashPtr != 0L) {
        auto crash = reinterpret_cast<Crash *>(crashPtr);
        crash ->release(env);
        delete crash;
    }
}