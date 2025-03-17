//
// Created by pengcheng.tan on 2025/3/17.
//

#include <jni.h>

extern "C" JNIEXPORT jlong JNICALL
Java_com_tans_tapm_monitors_AnrMonitor_registerAnrMonitor(
        JNIEnv * env,
        jobject javaAnrMonitor) {
    // TODO:
    return 0L;
}

extern "C" JNIEXPORT void JNICALL
Java_com_tans_tapm_monitors_AnrMonitor_unregisterAnrMonitor(
        JNIEnv * env,
        jobject javaAnrMonitor,
        jlong anrMonitor) {
    // TODO:
}