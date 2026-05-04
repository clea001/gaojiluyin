cmake_minimum_required(VERSION 3.22.1)
project("whisper_jni")

set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)

add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp whisper_build)

add_library(whisper_jni SHARED whisper_jni.cpp)
target_link_libraries(whisper_jni whisper log)
target_include_directories(whisper_jni PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp)
