
cmake_minimum_required(VERSION 3.22.1)

project("tapm")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 11)

add_subdirectory(unwindstack)

# region xhook
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
# endregion

# region unwind

# unwind
add_library(libunwind STATIC IMPORTED )
set_target_properties(libunwind PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libunwind.a)

# unwind-a
add_library(libunwind-a STATIC IMPORTED )
set_target_properties(libunwind-a PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libunwind-${ANDROID_ABI}.a)

# ptrace
add_library(libunwind-ptrace STATIC IMPORTED )
set_target_properties(libunwind-ptrace PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libunwind-ptrace.a)

# dwarf-common
add_library(libunwind-dwarf-common STATIC IMPORTED)
set_target_properties(libunwind-dwarf-common PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libunwind-dwarf-common.a)

# dwarf-generic
add_library(libunwind-dwarf-generic STATIC IMPORTED)
set_target_properties(libunwind-dwarf-generic PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libunwind-dwarf-generic.a)

# dwarf-local
add_library(libunwind-dwarf-local STATIC IMPORTED)
set_target_properties(libunwind-dwarf-local PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libunwind-dwarf-local.a)

# elf
add_library(libunwind-elf STATIC IMPORTED)
set_target_properties(libunwind-elf PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libunwind-elf-${ANDROID_ABI}.a)

# setjmp
add_library(libunwind-setjmp STATIC IMPORTED)
set_target_properties(libunwind-setjmp PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libunwind-setjmp.a)


# endregion


# region tapm
add_library(
        tapm SHARED
        jni.cpp
        tapm_log.h
        tapm_size.h
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
        crash/t_unwind.h
        crash/t_unwind.cpp
        crash/crash.h
        crash/crash.cpp
        crash/crash_writer.cpp
        crash/crash_writer.h
)
target_include_directories(tapm PRIVATE
        xhook
        unwind
        unwindstack/libunwindstack/include
)

target_link_libraries(
        tapm
        # android
        android
        log
        z
        unwindstack

        libunwind
        libunwind-ptrace
        libunwind-a
        libunwind-dwarf-common
        libunwind-dwarf-generic
        libunwind-dwarf-local
        libunwind-elf
        libunwind-setjmp

        xhook)
# endregion
