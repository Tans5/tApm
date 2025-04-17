//
// Created by pengcheng.tan on 2025/3/24.
//
#include "tapm_time.h"
#include <chrono>
#include <cstdio>
#include "../tapm_size.h"

int64_t nowInMillis() {
    using namespace std::chrono;
    return duration_cast<milliseconds>(system_clock::now().time_since_epoch()).count();
}

void formatTime(int64_t timeInMillis, char *outputBuffer) {
    if (outputBuffer == nullptr) return;

    auto seconds = static_cast<time_t>(timeInMillis / 1000);
    auto millis = static_cast<int>(timeInMillis % 1000);

    struct tm localTime{};
    localtime_r(&seconds, &localTime);

    char dataTime[20];
    strftime(dataTime, sizeof(dataTime), "%Y-%m-%dT%H:%M:%S", &localTime);

    auto timeOffsetInSeconds= localTime.tm_gmtoff;
    auto timeOffsetInHours = (int) (timeOffsetInSeconds / 3600);
    auto timeOffsetInMinus = (int) (timeOffsetInSeconds % 3600);

    snprintf(outputBuffer, MAX_STR_SIZE, "%s.%03d%+03d:%02d", dataTime, millis, timeOffsetInHours, timeOffsetInMinus);
}

void formatTimeNow(char *outputBuffer) {
    formatTime(nowInMillis(), outputBuffer);
}

void replaceChar(char targetChar, char newChar, const char *input, char *output) {
    for (int i = 0; i < MAX_STR_SIZE; i ++) {
        auto c = input[i];
        if (c == targetChar) {
            output[i] = newChar;
        } else {
            output[i] = c;
        }
        if (c == '\0') {
            break;
        }
    }
}

void replaceChar(char targetChar, char newChar, char *targetStr) {
    for (int i = 0; i < MAX_STR_SIZE; i ++) {
        auto c = targetStr[i];
        if (c == targetChar) {
            targetStr[i] = newChar;
        }
        if (c == '\0') {
            break;
        }
    }
}
