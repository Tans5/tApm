//
// Created by pengcheng.tan on 2025/3/21.
//
#include "crash.h"
#include "../tapm_log.h"

static bool CrashCallback(const google_breakpad::MinidumpDescriptor& descriptor,
                  void* context,
                  bool succeeded) {
    LOGD("Receive native crash signal.");
    // TODO:
    return succeeded;
}


int32_t Crash::prepare(JNIEnv *jniEnv, jobject jCrashMonitorP, jstring crashFileDir) {
    jniEnv->GetJavaVM(&this->jvm);
    this->jCrashMonitor = jniEnv->NewGlobalRef(jCrashMonitorP);
    string dirStr(jniEnv->GetStringUTFChars(crashFileDir, JNI_FALSE));
    google_breakpad::MinidumpDescriptor descriptor(dirStr);
    google_breakpad::ExceptionHandler* handler = new google_breakpad::ExceptionHandler(
            descriptor,
            nullptr,
            CrashCallback,
            this,
            true, // Start new process to handle crash.
            -1
            );
    this->crashHandler = handler;

    return 0;
}

void Crash::release(JNIEnv *jniEnv) {
    this->jvm = nullptr;
    if (this->jCrashMonitor != nullptr) {
        jniEnv->DeleteGlobalRef(this->jCrashMonitor);
        this->jCrashMonitor = nullptr;
    }

    if (this->crashHandler != nullptr) {
        delete this->crashHandler;
        this->crashHandler = nullptr;
    }


    delete this;
}
