
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

add_library(breakpad STATIC
        breakpad/client/linux/crash_generation/crash_generation_client.cc
        breakpad/client/linux/dump_writer_common/thread_info.cc
        breakpad/client/linux/dump_writer_common/ucontext_reader.cc
        breakpad/client/linux/handler/exception_handler.cc
        breakpad/client/linux/handler/minidump_descriptor.cc
        breakpad/client/linux/log/log.cc
        breakpad/client/linux/microdump_writer/microdump_writer.cc
        breakpad/client/linux/minidump_writer/linux_dumper.cc
        breakpad/client/linux/minidump_writer/linux_ptrace_dumper.cc
        breakpad/client/linux/minidump_writer/minidump_writer.cc
        breakpad/client/linux/minidump_writer/pe_file.cc
        breakpad/client/minidump_file_writer.cc
        breakpad/common/convert_UTF.cc
        breakpad/common/md5.cc
        breakpad/common/string_conversion.cc
        breakpad/common/linux/breakpad_getcontext.S
        breakpad/common/linux/elfutils.cc
        breakpad/common/linux/file_id.cc
        breakpad/common/linux/guid_creator.cc
        breakpad/common/linux/linux_libc_support.cc
        breakpad/common/linux/memory_mapped_file.cc
        breakpad/common/linux/safe_readlink.cc
)

target_include_directories(breakpad PRIVATE
        breakpad/common/android/include
        breakpad
        breakpad/third_party/lss
)

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

        xhook
        breakpad)
