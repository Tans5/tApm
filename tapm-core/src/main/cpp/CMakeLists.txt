
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
        anr/anr.h
        anr/anr.cpp
        anr/xhook.h
)

target_link_libraries(
        tapm
        # android
        android
        log

        xhook)
