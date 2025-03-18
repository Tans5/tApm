//
// Created by pengcheng.tan on 2025/3/18.
//

#ifndef TAPM_TAPM_LOG_H
#define TAPM_TAPM_LOG_H
#include <android/log.h>

#define LOG_TAG "tApmNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#endif //TAPM_TAPM_LOG_H
