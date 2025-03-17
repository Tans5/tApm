
cmake_minimum_required(VERSION 3.18.1)

project("tapm")

add_library(
        tapm SHARED
        jni.cpp
)
target_link_libraries(
        tapm
        # android
        ${log-lib}
        ${android-lib}
)
