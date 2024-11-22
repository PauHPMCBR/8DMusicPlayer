# Directory contents
Here, there should be one directory for each OS "type?" the application will be compiled to.
Right now, only arm64-v8a is compatible.
If more compatibility would be added, some things should change in CMakeLists.txt.

This directory already has all needed libraries to open sound files compiled for ARM64,
compiled using the instructions specified below.

# Compiling important libraries for the project
How to compile OpenAL for Android:
https://github.com/kcat/openal-soft/issues/824


To compile one cmake for Android, in Windows (pain):
specify `Release`, and `-O2` as flags

- param `CMAKE_TOOLCHAIN_FILE`:
`C:/Users/(usrename)/AppData/Local/Android/Sdk/ndk/(version)/build/cmake/android.toolchain.cmake`

- param `ANDROID_ABI`:
`arm64-v8a`

- ninja path:
`C:/Users/(username)/AppData/Local/Android/Sdk/cmake/(version)/bin/ninja.exe`
