//
// Created by pengcheng.tan on 2025/3/21.
//
#include "crash.h"

typedef struct ReportJavaThreadArgs {
    Crash * crash = nullptr;
    const char * file = nullptr;
} ReportJavaThreadArgs;

void *reporterJavaThread(void *args) {
    auto castArgs = static_cast<ReportJavaThreadArgs *>(args);
    auto crash = castArgs->crash;
    auto path = castArgs->file;
    JNIEnv *env = nullptr;
    crash->jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    if (env == nullptr) {
        JavaVMAttachArgs jvmAttachArgs {
                .version = JNI_VERSION_1_6,
                .name = "NativeCrashReporter",
                .group = nullptr
        };
        auto result = crash->jvm->AttachCurrentThread(&env, &jvmAttachArgs);
        if (result != JNI_OK) {
            env = nullptr;
        }
    }
    if (env != nullptr) {
        auto jMonitor = crash->jCrashMonitor;
        auto jMonitorClazz = env->GetObjectClass(jMonitor);
        auto crashMethodId = env->GetMethodID(jMonitorClazz, "onNativeCrash", "(Ljava/lang/String;)V");
        auto crashStackFilePath = env->NewStringUTF(path);
        env->CallVoidMethod(jMonitor, crashMethodId, crashStackFilePath);
    }
    return nullptr;
}

static bool CrashCallback(const google_breakpad::MinidumpDescriptor& descriptor,
                  void* context,
                  bool succeeded) {
    LOGD("Receive native crash signal.");
    pthread_t t;
    ReportJavaThreadArgs args {
        .crash = static_cast<Crash *>(context),
        .file = descriptor.path()
    };
    pthread_create(&t, nullptr, reporterJavaThread, &args);
    pthread_join(t, nullptr);
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
