
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

add_library(breakpad_client STATIC IMPORTED )
set_target_properties(breakpad_client PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libbreakpad_client.a)
set_target_properties(breakpad_client PROPERTIES INCLUDE_DIRECTORIES
        breakpad/common/android/include
        breakpad
        breakpad/third_party/lss)

add_library(
        tapm SHARED
        jni.cpp
        tapm_log.h
        anr/anr.h
        anr/anr.cpp
)
target_include_directories(tapm PRIVATE
        xhook
        breakpad/common/android/include
        breakpad
        breakpad/third_party/lss
)

target_link_libraries(
        tapm
        # android
        android
        log

        xhook
        breakpad_client)
