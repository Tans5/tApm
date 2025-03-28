
cmake_minimum_required(VERSION 3.22.1)

project("tapm")

add_library(xhook SHARED
        xhook/queue.h
        xhook/tree.h
        xhook/xh_core.c
        xhook/xh_core.h
        xhook/xh_elf.c
        xhook/xh_elf.h
        xhook/xh_errno.h
        xhook/xh_jni.c
        xhook/xh_log.c
        xhook/xh_log.h
        xhook/xh_util.c
        xhook/xh_util.h
        xhook/xh_version.c
        xhook/xh_version.h
        xhook/xhook.h
        xhook/xhook.c)

target_link_libraries(xhook
        android
        log)

add_library(
        tapm SHARED
        jni.cpp
        tapm_log.h
        time/tapm_time.h
        time/tapm_time.cpp
        linkedlist/linked_list.h
        linkedlist/linked_list.cpp
        thread/tapm_thread.h
        thread/tapm_thread.cpp
        anr/anr.h
        anr/anr.cpp
        crash/thread_control.h
        crash/thread_control.cpp
        crash/memory_maps.h
        crash/memory_maps.cpp
        crash/t_regs.h
        crash/t_regs.cpp
        crash/process_read.h
        crash/process_read.cpp
        crash/file_mmap.h
        crash/file_mmap.cpp
        crash/t_elf.h
        crash/t_elf.cpp
        crash/crash.h
        crash/crash.cpp
)
target_include_directories(tapm PRIVATE
        xhook
)

target_link_libraries(
        tapm
        # android
        android
        log

        xhook)
