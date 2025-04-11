//
// Created by pengcheng.tan on 2025/3/27.
//

#ifndef TAPM_PROCESS_READ_H
#define TAPM_PROCESS_READ_H

#include <sys/types.h>
#include "../tapm_size.h"

size_t processRead(pid_t pid, addr_t remote_addr, void* dst, size_t dst_len);

#endif //TAPM_PROCESS_READ_H
