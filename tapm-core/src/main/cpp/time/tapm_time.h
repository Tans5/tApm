//
// Created by pengcheng.tan on 2025/3/24.
//

#ifndef TAPM_TAPM_TIME_H
#define TAPM_TAPM_TIME_H
#include <ctime>

int64_t nowInMillis();

void formatTime(int64_t timeInMillis, char * outputBuffer, int outputSize);

#endif //TAPM_TAPM_TIME_H
