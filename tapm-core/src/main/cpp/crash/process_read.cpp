//
// Created by pengcheng.tan on 2025/3/27.
//
#include <sys/types.h>
#include <bits/sysconf.h>
#include <sys/uio.h>
#include <unistd.h>
#include "process_read.h"

size_t processRead(pid_t pid, uintptr_t remote_addr, void* dst, size_t dst_len) {
    // 获取系统页大小，通常是 4096
    auto page_size = (size_t)sysconf(_SC_PAGE_SIZE);
    struct iovec src_iovs[64];
    uintptr_t cur_remote_addr = remote_addr;
    size_t total_read = 0;

    // 一个一个内存页面读取，最大支持 64 个内存页面，单次最大读取的数据大小是 64 * page_size，超过了进入下次循环再次读取。
    while (dst_len > 0) {
        // the destination
        struct iovec dst_iov = {.iov_base = &((uint8_t *)dst)[total_read], .iov_len = dst_len};

        // the source
        size_t iovecs_used = 0;
        while (dst_len > 0) {
            // fill iov_base
            src_iovs[iovecs_used].iov_base = (void *)cur_remote_addr;

            // fill iov_len (one page at a time, page boundaries aligned)
            // 计算页内地址偏移
            uintptr_t misalignment = cur_remote_addr & (page_size - 1);
            // 当前页剩余可读长度
            size_t iov_len = page_size - misalignment;
            src_iovs[iovecs_used].iov_len = (iov_len > dst_len ? dst_len : iov_len);

            // 判断是否溢出
            if (__builtin_add_overflow(cur_remote_addr, iov_len, &cur_remote_addr)) return total_read;
            dst_len -= iov_len;

            if(64 == ++iovecs_used) break;
        }

        // read from source to destination
        ssize_t rc;
        rc = process_vm_readv(pid, &dst_iov, 1, src_iovs, iovecs_used, 0);
        if(-1 == rc) return total_read;

        total_read += (size_t)rc;
    }

    return total_read;
}
