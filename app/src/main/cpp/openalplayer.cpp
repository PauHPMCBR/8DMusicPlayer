#include <string>
#include <cmath>
#include <unistd.h>
#include <thread>
#include <atomic>
#include <map>
#include <mutex>

#include <jni.h>
#include <AL/al.h>
#include <AL/alc.h>
#include <AL/alext.h>
#include <android/log.h>

#define APPNAME "8D Music Player"

#include "utils.h"
#include "soundLoader.h"
#include "openalInitializer.h"

#define SAMPLE_RATE 44100
#define DURATION 1 // 1 second

ALCdevice* device;
ALCcontext* context;

ALfloat listenerOri[] = { 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f };

std::map<std::string, ALuint_p> fileSources;
std::mutex sourcesMutex;

float STEREO_ANGLE = M_PI / 6;

std::atomic<bool> stopFlag(false);
JavaVM* javaVM;
jobject globalCallback;

void playMusicThread(const std::string& filePath) {
    ALuint_p sources = fileSources[filePath];
    ALint state;

    alSourcePlay(sources.first);
    if (sources.second != AL_NONE) alSourcePlay(sources.second);
    do {
        sleep(1);
        alGetSourcei(sources.first, AL_SOURCE_STATE, &state);
    } while (alGetError() == AL_NO_ERROR && state != AL_STOPPED && !stopFlag.load());

    // Notify Java that the sound has finished
    JNIEnv* env;
    javaVM->AttachCurrentThread(&env, NULL);

    jclass callbackClass = env->GetObjectClass(globalCallback);
    jmethodID onSoundFinishedMethod = env->GetMethodID(callbackClass, "onSoundFinished", "(Ljava/lang/String;)V");

    jstring jFilePath = env->NewStringUTF(filePath.c_str());
    env->CallVoidMethod(globalCallback, onSoundFinishedMethod, jFilePath);

    env->DeleteLocalRef(jFilePath);
    javaVM->DetachCurrentThread();

    // Clean up
    {
        std::lock_guard<std::mutex> lock(sourcesMutex);
        alDeleteSources(1, &sources.first);
        if (sources.second != AL_NONE) alDeleteSources(1, &sources.second);
        fileSources.erase(filePath);
    }
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    javaVM = vm;
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL
Java_com_pmb_openal_OpenALManager_setCallback(JNIEnv* env, jclass /* this */, jobject callback) {
    if (globalCallback != NULL) {
        env->DeleteGlobalRef(globalCallback);
    }
    globalCallback = env->NewGlobalRef(callback);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_pmb_openal_OpenALManager_initOpenAL(JNIEnv* env, jclass /* this */, jstring jselectedHrtf) {
    const char *selectedHrtfChars = env->GetStringUTFChars(jselectedHrtf, 0);
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Initialising OpenAL with HRTF: %s", selectedHrtfChars);
    device = alcOpenDevice(nullptr);
    if (!device) return JNI_FALSE;
    loadHRTF(device, selectedHrtfChars);
    context = alcCreateContext(device, nullptr);
    if (!context) return JNI_FALSE;
    if (!alcMakeContextCurrent(context)) return JNI_FALSE;
    alListener3f(AL_POSITION, 0, 0, 1.0f);
    alListener3f(AL_VELOCITY, 0, 0, 0);
    alListenerfv(AL_ORIENTATION, listenerOri);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_pmb_openal_OpenALManager_cleanupOpenAL(JNIEnv* env, jclass /* this */) {
    alcMakeContextCurrent(nullptr);
    alcDestroyContext(context);
    alcCloseDevice(device);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pmb_openal_OpenALManager_setPlaybackPosition(JNIEnv* env, jclass /* this */, jstring filePath, jfloat seconds) {
    std::string filePathStr = jstringToString(env, filePath);

    std::lock_guard<std::mutex> lock(sourcesMutex);
    if (fileSources.find(filePathStr) != fileSources.end()) {
        ALuint_p sources = fileSources[filePathStr];
        alSourcef(sources.first, AL_SEC_OFFSET, seconds);
        if (sources.second != AL_NONE) alSourcef(sources.second, AL_SEC_OFFSET, seconds);

        ALenum error = alGetError();
        if (error != AL_NO_ERROR) {
            __android_log_print(ANDROID_LOG_ERROR, APPNAME, "Error setting playback position: %s",
                                alGetString(error));
        }
    } else {
        __android_log_print(ANDROID_LOG_ERROR, APPNAME, "Source not found for file: %s", filePathStr.c_str());
    }
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_pmb_openal_OpenALManager_getPlaybackPosition(JNIEnv* env, jclass /* this */, jstring filePath) {
    std::string filePathStr = jstringToString(env, filePath);

    std::lock_guard<std::mutex> lock(sourcesMutex);
    if (fileSources.find(filePathStr) != fileSources.end()) {
        ALuint_p sources = fileSources[filePathStr];
        ALfloat seconds = 0.0f;
        alGetSourcef(sources.first, AL_SEC_OFFSET, &seconds);

        ALenum error = alGetError();
        if (error != AL_NO_ERROR) {
            //__android_log_print(ANDROID_LOG_ERROR, APPNAME, "Error getting playback position: %s", alGetString(error));
            return -1;
        }
        return seconds;
    }
    else {
        //__android_log_print(ANDROID_LOG_ERROR, APPNAME, "Source not found for file: %s", filePathStr.c_str());
        return -1;
    }
}

extern "C" JNIEXPORT float JNICALL
Java_com_pmb_openal_OpenALManager_playMusic(JNIEnv* env, jclass /* this */, jstring filePath) {
    const char *filePathChars = env->GetStringUTFChars(filePath, 0);
    std::string filePathStr(filePathChars);
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Loading file: %s", filePathChars);

    ALuint_p buffers = LoadSound(filePathChars);
    env->ReleaseStringUTFChars(filePath, filePathChars);
    if (!buffers.first) {
        __android_log_print(ANDROID_LOG_ERROR, APPNAME, "Failed to load sound");
        return 0;
    }

    ALuint_p sources;
    alGenSources(1, &sources.first);
    alSourcei(sources.first, AL_SOURCE_RELATIVE, AL_TRUE);
    alSource3f(sources.first, AL_POSITION, 0.0f, 0.0f, -1.0f);
    alSourcei(sources.first, AL_BUFFER, (ALint)buffers.first);
    if (buffers.second) {
        alGenSources(1, &sources.second);
        alSourcei(sources.second, AL_SOURCE_RELATIVE, AL_TRUE);
        alSource3f(sources.second, AL_POSITION, 0.0f, 0.0f, -1.0f);
        alSourcei(sources.second, AL_BUFFER, (ALint)buffers.second);
    }
    else sources.second = AL_NONE;

    assert(alGetError()==AL_NO_ERROR && "Failed to setup sound source");

    {
        std::lock_guard<std::mutex> lock(sourcesMutex);
        fileSources[filePathStr] = sources;
    }

    stopFlag.store(false);

    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Starting thread for %s", filePathStr.c_str());
    std::thread musicThread(playMusicThread, filePathStr);
    musicThread.detach();
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Detached from thread for %s", filePathStr.c_str());

    return getDurationSeconds(buffers.first);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pmb_openal_OpenALManager_updateSourcePosition(JNIEnv* env, jclass /* this */, jstring filePath, jfloat angle, jfloat radius, jfloat height) {
    std::string filePathStr = jstringToString(env, filePath);

    std::lock_guard<std::mutex> lock(sourcesMutex);

    //angle += M_PI/2;

    if (fileSources.find(filePathStr) != fileSources.end()) {
        ALuint_p sources = fileSources[filePathStr];

        alcSuspendContext(context);

        if (sources.second == AL_NONE) {
            setPosition(sources.first, angle, radius, height);
        }
        else {
            setPosition(sources.first, angle - STEREO_ANGLE/2, radius, height);
            setPosition(sources.second, angle + STEREO_ANGLE/2, radius, height);
        }

        alcProcessContext(context);

        ALenum error = alGetError();
        if (error != AL_NO_ERROR) {
            __android_log_print(ANDROID_LOG_ERROR, APPNAME, "Error updating source position: %s",
                                alGetString(error));
        }
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_pmb_openal_OpenALManager_setStereoAngle(JNIEnv* env, jclass /*this*/, jfloat angle) {
    STEREO_ANGLE = angle;
}

extern "C" JNIEXPORT void JNICALL
Java_com_pmb_openal_OpenALManager_pauseMusic(JNIEnv* env, jclass /* this */, jstring filePath) {
    std::string filePathStr = jstringToString(env, filePath);

    std::lock_guard<std::mutex> lock(sourcesMutex);
    if (fileSources.find(filePathStr) != fileSources.end()) {
        ALuint_p sources = fileSources[filePathStr];
        if (isSourcePlaying(sources.first)) {
            alSourcePause(sources.first);
            if (sources.second != AL_NONE) alSourcePause(sources.second);
        }
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_pmb_openal_OpenALManager_resumeMusic(JNIEnv* env, jclass /* this */, jstring filePath) {
    std::string filePathStr = jstringToString(env, filePath);

    std::lock_guard<std::mutex> lock(sourcesMutex);
    if (fileSources.find(filePathStr) != fileSources.end()) {
        ALuint_p sources = fileSources[filePathStr];
        ALint state;
        alGetSourcei(sources.first, AL_SOURCE_STATE, &state);
        if (state == AL_PAUSED) {
            alSourcePlay(sources.first);
            if (sources.second != AL_NONE) alSourcePlay(sources.second);
        }
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_pmb_openal_OpenALManager_stopMusic__(JNIEnv* env, jclass /* this */) {
    std::lock_guard<std::mutex> lock(sourcesMutex);
    for (auto fn_sources : fileSources) {
        alSourceStop(fn_sources.second.first);
        alDeleteSources(1, &fn_sources.second.first);
        if (fn_sources.second.second != AL_NONE) {
            alSourceStop(fn_sources.second.second);
            alDeleteSources(1, &fn_sources.second.second);
        }
    }
    fileSources.clear();
}

extern "C" JNIEXPORT void JNICALL
Java_com_pmb_openal_OpenALManager_stopMusic__Ljava_lang_String_2(JNIEnv* env, jclass /* this */, jstring filePath) {
    std::string filePathStr = jstringToString(env, filePath);

    std::lock_guard<std::mutex> lock(sourcesMutex);
    if (fileSources.find(filePathStr) != fileSources.end()) {
        ALuint_p sources = fileSources[filePathStr];
        alSourceStop(sources.first);
        alDeleteSources(1, &sources.first);
        if (sources.second != AL_NONE) {
            alSourceStop(sources.second);
            alDeleteSources(1, &sources.second);
        }
        fileSources.erase(filePathStr);
    }
}
