
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("musicplayer")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#
# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
add_library(openal SHARED IMPORTED)
set_target_properties(openal PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jni/${ANDROID_ABI}/libopenal.so)

add_library(sndfile SHARED IMPORTED)
set_target_properties(sndfile PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jni/${ANDROID_ABI}/libsndfile.a)

add_library(ogg SHARED IMPORTED)
set_target_properties(ogg PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jni/${ANDROID_ABI}/libogg.a)

add_library(FLAC SHARED IMPORTED)
set_target_properties(FLAC PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jni/${ANDROID_ABI}/libFLAC.a)

add_library(vorbis SHARED IMPORTED)
set_target_properties(vorbis PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jni/${ANDROID_ABI}/libvorbis.a)
add_library(vorbisenc SHARED IMPORTED)
set_target_properties(vorbisenc PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jni/${ANDROID_ABI}/libvorbisenc.a)
add_library(vorbisfile SHARED IMPORTED)
set_target_properties(vorbisfile PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jni/${ANDROID_ABI}/libvorbisfile.a)

add_library(opus SHARED IMPORTED)
set_target_properties(opus PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jni/${ANDROID_ABI}/libopus.a)

add_library(LAME SHARED IMPORTED)
set_target_properties(LAME PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jni/${ANDROID_ABI}/libmp3lame.a)

add_library(mpg123 SHARED IMPORTED)
set_target_properties(mpg123 PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jni/${ANDROID_ABI}/libmpg123.a)


add_library(${CMAKE_PROJECT_NAME} SHARED
    # List C/C++ source files with relative paths to this CMakeLists.txt.
    openalplayer.cpp)

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
        ${CMAKE_SOURCE_DIR}/include)

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(${CMAKE_PROJECT_NAME}
    # List libraries link to the target library
        openal
        ogg
        FLAC
        vorbis
        vorbisenc
        vorbisfile
        opus
        LAME
        mpg123
        sndfile
        android
        log
        m)
