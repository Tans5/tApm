//
// Created by pengcheng.tan on 2025/3/17.
//

#include <jni.h>
#include "anr/anr.h"
//#include "breakpad/client/linux/handler/exception_handler.h"
//
//// 崩溃回调函数（可选）
//bool DumpCallback(const google_breakpad::MinidumpDescriptor& descriptor,
//                  void* context,
//                  bool succeeded) {
//    return succeeded;
//}

extern "C" JNIEXPORT jlong JNICALL
Java_com_tans_tapm_monitors_AnrMonitor_registerAnrMonitorNative(
        JNIEnv * env,
        jobject javaAnrMonitor) {

//    google_breakpad::MinidumpDescriptor descriptor(0);
//    google_breakpad::ExceptionHandler eh(
//            descriptor,
//            NULL,         // FilterCallback（可选）
//            DumpCallback,  // 崩溃后的回调
//            NULL,          // 回调上下文
//            true,          // 是否启动子进程处理崩溃
//            -1             // 文件描述符（默认 -1）
//    );

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