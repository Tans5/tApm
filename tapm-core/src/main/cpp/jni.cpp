//
// Created by pengcheng.tan on 2025/3/17.
//

#include <jni.h>
#include "anr/anr.h"

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