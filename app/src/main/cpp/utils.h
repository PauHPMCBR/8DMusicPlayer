//
// Created by PauMB on 08/09/2024.
//

#ifndef INC_8DMUSICPLAYER_UTILS_H
#define INC_8DMUSICPLAYER_UTILS_H

#include <AL/al.h>

std::string jstringToString(JNIEnv* env, jstring js) {
    const char *filePathChars = env->GetStringUTFChars(js, 0);
    std::string filePathStr(filePathChars);
    env->ReleaseStringUTFChars(js, filePathChars);
    return filePathChars;
}

bool isSourcePlaying(ALuint source) {
    ALint state;
    alGetSourcei(source, AL_SOURCE_STATE, &state);
    return (state == AL_PLAYING);
}

float getDurationSeconds(ALuint buffer) {
    ALint sizeInBytes;
    ALint channels;
    ALint bits;
    alGetBufferi(buffer, AL_SIZE, &sizeInBytes);
    alGetBufferi(buffer, AL_CHANNELS, &channels);
    alGetBufferi(buffer, AL_BITS, &bits);

    float lengthInSamples = sizeInBytes * 8 / (channels * bits);

    ALint frequency;
    alGetBufferi(buffer, AL_FREQUENCY, &frequency);

    return (float)lengthInSamples / (float)frequency;
}

void setPosition(ALuint source, float angle, float radius, float height) {
    while (angle > M_PI) angle -= M_PI*2.0;
    while (angle < M_PI) angle += M_PI*2.0;
    alSource3f(source, AL_POSITION, radius * cos(angle), height, radius * sin(angle));
}

#endif //INC_8DMUSICPLAYER_UTILS_H
