# Directory contents
Here, there should be one directory for each OS "type?" the application will be compiled to.
Right now, only arm64-v8a is compatible.
If more compatibility would be added, some things should change in CMakeLists.txt.

This directory already has all needed libraries to open sound files compiled for ARM64,
compiled using the instructions specified below.

## Compiled libraries included
This project already has the following compiled libraries:

- openal-soft: to play sound with spacial sound (3D/8D, with HRTF)
- libsndfile (to read sound files, depends on specific libraries to read files different than WAV)
- flac (to read flac files)
- lame (to write mp3 files, probably not necessary but just in case)
- mpg123 (to read mp3 files)
- opus (to read opus files)
- vorbis (to read vorbis and ogg files)

### Compiling openal-soft
The `openal-soft` library was compiled with the following arguments (on top of the default ones provided by the cmake file from the repo, and the ones specified in the next section):
- `ALSOFT_HRTF` + `ALSOFT_EMBED_HRTF_DATA` + `ALSOFT_INSTALL_HRTF_DATA` (i can't remember which ones actually do something)
- `AL_STEREO_ANGLES` / `AL_EXT_STEREO_ANGLES` (can't remember which one is correct, still can't use the library since it's bugged when you want to change distance from source to listener)

I have also modified the file that loads `alsoft.conf` to look for the Android root directory (`/storage/emulated/0/alsoft.conf`),
since it was the only way I found to load the configuration file that worked.

### Compiling libsndfile
The `libsndfile` library was compiled with the following arguments (on top of the default ones provided by the cmake file from the repo, and the ones specified in the next section):
- `ENABLE_MPEG`: true
- `FLAC_INCLUDE_DIR` (include directory) + `FLAC_LIBRARY` (`.a` file)
- `MP3LAME_INCLUDE_DIR` + `MP3LAME_LIBRARY` (same as before, I used version 3.100, 3.99 did not work)
- `OPUS_INCLUDE_DIR` + `OPUS_LIBRARY`
- `Vorbis_Vorbis_INCLUDE_DIR`  + `Vorbis_Vorbis_LIBRARY` (`libvorbis.a` file)
- `Vorbis_Enc_INCLUDE_DIR` (should be the same as the previous one) + `Vorbis_Enc_LIBRARY` (`libvorbisenc.a` file)
- `Vorbis_File_INCLUDE_DIR` (should be the same as the previous one) + `Vorbis_File_LIBRARY` (`libvorbisfile.a` file)
- `OGG_INCLUDE_DIR` + `OGG_LIBRARY` (comes from vorbis inside the `ogg` directory, file is `libogg.a`)
- `mpg123_INCLUDE_DIR` + `mpg123_LIBRARY`

# Compiling important libraries for the project
How to compile OpenAL for Android:
https://github.com/kcat/openal-soft/issues/824


To compile one cmake for Android, in Windows (pain):

- specify `Release`, and `-O2` as flags

- param `CMAKE_TOOLCHAIN_FILE`:
- `C:/Users/(usrename)/AppData/Local/Android/Sdk/ndk/(version)/build/cmake/android.toolchain.cmake`

- param `ANDROID_ABI`:
`arm64-v8a`

- ninja path (`CMAKE_MAKE_PROGRAM`, added after trying to "configure"):
`C:/Users/(username)/AppData/Local/Android/Sdk/cmake/(version)/bin/ninja.exe`
