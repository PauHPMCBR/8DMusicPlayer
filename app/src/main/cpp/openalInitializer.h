//
// Created by PauMB on 08/09/2024.
//

#ifndef INC_8DMUSICPLAYER_OPENALINITIALIZER_H
#define INC_8DMUSICPLAYER_OPENALINITIALIZER_H

#include "AL/al.h"
#include "AL/alext.h"

#include <android/log.h>
#include <string>

#define AL_INITIALIZER "C++ OpenAL Initializer"

static LPALCGETSTRINGISOFT alcGetStringiSOFT;
static LPALCRESETDEVICESOFT alcResetDeviceSOFT;

void loadHRTF(ALCdevice* device, const char* hrtfname) {
#define FUNCTION_CAST(T, ptr) reinterpret_cast<T>(ptr)
#define LOAD_PROC(d, T, x)  ((x) = FUNCTION_CAST(T, alcGetProcAddress((d), #x)))
    LOAD_PROC(device, LPALCGETSTRINGISOFT, alcGetStringiSOFT);
    LOAD_PROC(device, LPALCRESETDEVICESOFT, alcResetDeviceSOFT);
#undef LOAD_PROC
    /* Enumerate available HRTFs, and reset the device using one. */
    ALint  num_hrtf;
    alcGetIntegerv(device, ALC_NUM_HRTF_SPECIFIERS_SOFT, 1, &num_hrtf);
    if(!num_hrtf)
        __android_log_print(ANDROID_LOG_VERBOSE, AL_INITIALIZER, "No HRTFs found\n");
    else
    {
        ALCint attr[5];
        ALCint index = -1;
        ALCint i;

        //__android_log_print(ANDROID_LOG_VERBOSE, AL_INITIALIZER, "Available HRTFs:\n");
        for(i = 0;i < num_hrtf;i++)
        {
            const ALCchar *name = alcGetStringiSOFT(device, ALC_HRTF_SPECIFIER_SOFT, i);
            //__android_log_print(ANDROID_LOG_VERBOSE, AL_INITIALIZER, "    %d: %s\n", i, name);

            /* Check if this is the HRTF the user requested. */
            if(hrtfname && strcmp(name, hrtfname) == 0)
                index = i;
        }

        i = 0;
        attr[i++] = ALC_HRTF_SOFT;
        attr[i++] = ALC_TRUE;
        if(index == -1)
        {
            if(hrtfname)
                __android_log_print(ANDROID_LOG_VERBOSE, AL_INITIALIZER, "HRTF \"%s\" not found\n", hrtfname);
            __android_log_print(ANDROID_LOG_VERBOSE, AL_INITIALIZER, "Using default HRTF...\n");
        }
        else
        {
            __android_log_print(ANDROID_LOG_VERBOSE, AL_INITIALIZER, "Selecting HRTF %d...\n", index);
            attr[i++] = ALC_HRTF_ID_SOFT;
            attr[i++] = index;
        }
        attr[i] = 0;

        if(!alcResetDeviceSOFT(device, attr))
            __android_log_print(ANDROID_LOG_VERBOSE, AL_INITIALIZER, "Failed to reset device: %s\n", alcGetString(device, alcGetError(device)));
    }

    /* Check if HRTF is enabled, and show which is being used. */
    ALint hrtf_state;
    alcGetIntegerv(device, ALC_HRTF_SOFT, 1, &hrtf_state);
    if(!hrtf_state)
        __android_log_print(ANDROID_LOG_VERBOSE, AL_INITIALIZER, "HRTF not enabled!\n");
    else
    {
        const ALchar *name = alcGetString(device, ALC_HRTF_SPECIFIER_SOFT);
        __android_log_print(ANDROID_LOG_VERBOSE, AL_INITIALIZER, "HRTF enabled, using %s\n", name);
    }
    fflush(stdout);
}


#endif //INC_8DMUSICPLAYER_OPENALINITIALIZER_H
