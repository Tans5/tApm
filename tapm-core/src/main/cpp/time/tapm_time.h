//
// Created by pengcheng.tan on 2025/3/24.
//

#ifndef TAPM_TAPM_TIME_H
#define TAPM_TAPM_TIME_H
#include <ctime>

int64_t nowInMillis();

void formatTime(int64_t timeInMillis, char * outputBuffer);

void formatTimeNow(char * outputBuffer);

void replaceChar(char targetChar, char newChar, const char *input, char *output);

void replaceChar(char targetChar, char newChar, char *targetStr);

#endif //TAPM_TAPM_TIME_H
