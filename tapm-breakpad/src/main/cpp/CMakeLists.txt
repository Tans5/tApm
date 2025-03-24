cmake_minimum_required(VERSION 3.22.1)

project("tapmbreakpad")

add_library(breakpad_client STATIC IMPORTED )
set_target_properties(breakpad_client PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libbreakpad_client.a)
set_target_properties(breakpad_client PROPERTIES INCLUDE_DIRECTORIES
        breakpad/common/android/include
        breakpad
        breakpad/third_party/lss)


add_library(tapmbreakpad SHARED
        jni.cpp
        crash/crash.cpp
        crash/crash.h)

target_include_directories(tapmbreakpad PRIVATE
        breakpad/common/android/include
        breakpad
        breakpad/third_party/lss)

target_link_libraries(tapmbreakpad
        # List libraries link to the target library
        android
        log
        breakpad_client)