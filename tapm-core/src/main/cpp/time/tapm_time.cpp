//
// Created by pengcheng.tan on 2025/3/24.
//
#include "tapm_time.h"
#include <chrono>
#include <cstdio>

int64_t nowInMillis() {
    using namespace std::chrono;
    return duration_cast<milliseconds>(system_clock::now().time_since_epoch()).count();
}

void formatTime(int64_t timeInMillis, char *outputBuffer, int outputBufferSize) {
    if (outputBuffer == nullptr || outputBufferSize < 32) return;

    auto seconds = static_cast<time_t>(timeInMillis / 1000);
    auto millis = static_cast<int>(timeInMillis % 1000);

    struct tm localTime{};
    localtime_r(&seconds, &localTime);

    char dataTime[20];
    strftime(dataTime, sizeof(dataTime), "%Y-%m-%dT%H:%M:%S", &localTime);

    auto timeOffsetInSeconds= localTime.tm_gmtoff;
    auto timeOffsetInHours = (int) (timeOffsetInSeconds / 3600);
    auto timeOffsetInMinus = (int) (timeOffsetInSeconds % 3600);

    snprintf(outputBuffer, outputBufferSize, "%s.%03d%+03d:%02d", dataTime, millis, timeOffsetInHours, timeOffsetInMinus);
}

void formatTimeNow(char *outputBuffer, int outputBufferSize) {
    formatTime(nowInMillis(), outputBuffer, outputBufferSize);
}
