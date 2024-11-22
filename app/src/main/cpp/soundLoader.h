#include <assert.h>
#include <inttypes.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include "AL/al.h"
#include "AL/alext.h"
#include "sndfile.h"

#include <android/log.h>
#define C_SOUND_LOADER "C++ Sound Loader"

// Debug logging helper
#define LOG_DEBUG(...) __android_log_print(ANDROID_LOG_DEBUG, C_SOUND_LOADER, __VA_ARGS__)
#define LOG_ERROR(...) __android_log_print(ANDROID_LOG_ERROR, C_SOUND_LOADER, __VA_ARGS__)

typedef std::pair<ALuint, ALuint> ALuint_p;

enum FormatType {
    Int16,
    Float,
    IMA4,
    MSADPCM
};

ALenum getALFormat(FormatType sample_format) {
    if (sample_format == Int16)
        return AL_FORMAT_MONO16;
    else if (sample_format == Float)
        return AL_FORMAT_MONO_FLOAT32;
    else if (sample_format == IMA4)
        return AL_FORMAT_MONO_IMA4;
    else if (sample_format == MSADPCM)
        return AL_FORMAT_MONO_MSADPCM_SOFT;
    return AL_NONE;
}

//I need to load stereo sounds separately in 2 different buffers to have a custom stereo angles, since the one from the extension disables distance
template <typename T>
static ALuint_p processStereoSound(T* tempBuffer, SF_INFO sfinfo, ALenum format, ALsizei num_bytes) {
    ALuint_p buffers = {AL_NONE, AL_NONE};

    LOG_DEBUG("Allocating left and right buffers for stereo processing: frames = %lld, bytes allocated = %lld", sfinfo.frames, sfinfo.frames * sizeof(T));
    void *leftMembuf = malloc(sfinfo.frames * sizeof(T));
    void *rightMembuf = malloc(sfinfo.frames * sizeof(T));

    // Check for memory allocation failure
    if (!leftMembuf || !rightMembuf) {
        LOG_ERROR("Failed to allocate memory for left or right buffers");
        free(leftMembuf);
        free(rightMembuf);
        free(tempBuffer);
        return {AL_NONE, AL_NONE};
    }

    T *leftChannel = (T *)leftMembuf;
    T *rightChannel = (T *)rightMembuf;

    LOG_DEBUG("Processing stereo channels for %lld frames", sfinfo.frames);
    for (sf_count_t i = 0; i < sfinfo.frames; i++) {
        leftChannel[i] = tempBuffer[2 * i];      // left channel sample
        rightChannel[i] = tempBuffer[2 * i + 1]; // right channel sample
    }

    free(tempBuffer); // Free original tempBuffer after copying data to left/right buffers

    /* Generate OpenAL buffers for left and right channels */
    alGenBuffers(1, &buffers.first);
    alGenBuffers(1, &buffers.second);

    if (buffers.first == AL_NONE || buffers.second == AL_NONE) {
        LOG_ERROR("Failed to generate OpenAL buffers");
        free(leftMembuf);
        free(rightMembuf);
        return {AL_NONE, AL_NONE};
    }

    // Ensure correct data size
    LOG_DEBUG("Buffering left channel: num_bytes = %d", num_bytes);
    alBufferData(buffers.first, format, leftMembuf, num_bytes/2, sfinfo.samplerate);

    LOG_DEBUG("Buffering right channel: num_bytes = %d", num_bytes);
    alBufferData(buffers.second, format, rightMembuf, num_bytes/2, sfinfo.samplerate);

    // Clean up memory buffers
    free(leftMembuf);
    free(rightMembuf);

    /* Check for OpenAL errors */
    ALenum err = alGetError();
    if (err != AL_NO_ERROR) {
        LOG_ERROR("OpenAL Error after buffering: %s", alGetString(err));
        if (buffers.first && alIsBuffer(buffers.first))
            alDeleteBuffers(1, &buffers.first);
        if (buffers.second && alIsBuffer(buffers.second))
            alDeleteBuffers(1, &buffers.second);
        return {AL_NONE, AL_NONE};
    }

    LOG_DEBUG("Stereo sound processing completed successfully");
    return buffers;
}

static ALuint_p LoadSound(const char *filename) {
    enum FormatType sample_format = Int16;
    ALint byteblockalign = 0;
    ALint splblockalign = 0;
    sf_count_t num_frames;
    ALenum err, format;
    ALsizei num_bytes;
    SNDFILE *sndfile;
    SF_INFO sfinfo;
    ALuint_p buffers = {AL_NONE, AL_NONE};
    void *membuf;

    /* Open the audio file and check that it's usable. */
    sndfile = sf_open(filename, SFM_READ, &sfinfo);
    if(!sndfile)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, C_SOUND_LOADER, "Could not open audio in %s: %s\n", filename, sf_strerror(sndfile));
        return buffers;
    }
    if(sfinfo.frames < 1)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, C_SOUND_LOADER, "Bad sample count in %s (%" PRId64 ")\n", filename, sfinfo.frames);
        sf_close(sndfile);
        return buffers;
    }

    /* Detect a suitable format to load. Formats like Vorbis and Opus use float
     * natively, so load as float to avoid clipping when possible. Formats
     * larger than 16-bit can also use float to preserve a bit more precision.
     */
    switch((sfinfo.format&SF_FORMAT_SUBMASK))
    {
        case SF_FORMAT_PCM_24:
        case SF_FORMAT_PCM_32:
        case SF_FORMAT_FLOAT:
        case SF_FORMAT_DOUBLE:
        case SF_FORMAT_VORBIS:
        case SF_FORMAT_OPUS:
        case SF_FORMAT_ALAC_20:
        case SF_FORMAT_ALAC_24:
        case SF_FORMAT_ALAC_32:
        case 0x0080/*SF_FORMAT_MPEG_LAYER_I*/:
        case 0x0081/*SF_FORMAT_MPEG_LAYER_II*/:
        case 0x0082/*SF_FORMAT_MPEG_LAYER_III*/:
            if(alIsExtensionPresent("AL_EXT_FLOAT32"))
                sample_format = Float;
            break;
        case SF_FORMAT_IMA_ADPCM:
            /* ADPCM formats require setting a block alignment as specified in the
             * file, which needs to be read from the wave 'fmt ' chunk manually
             * since libsndfile doesn't provide it in a format-agnostic way.
             */
            if(sfinfo.channels <= 2 && (sfinfo.format&SF_FORMAT_TYPEMASK) == SF_FORMAT_WAV
               && alIsExtensionPresent("AL_EXT_IMA4")
               && alIsExtensionPresent("AL_SOFT_block_alignment"))
                sample_format = IMA4;
            break;
        case SF_FORMAT_MS_ADPCM:
            if(sfinfo.channels <= 2 && (sfinfo.format&SF_FORMAT_TYPEMASK) == SF_FORMAT_WAV
               && alIsExtensionPresent("AL_SOFT_MSADPCM")
               && alIsExtensionPresent("AL_SOFT_block_alignment"))
                sample_format = MSADPCM;
            break;
    }

    if(sample_format == IMA4 || sample_format == MSADPCM)
    {
        /* For ADPCM, lookup the wave file's "fmt " chunk, which is a
         * WAVEFORMATEX-based structure for the audio format.
         */
        SF_CHUNK_INFO inf = { "fmt ", 4, 0, NULL };
        SF_CHUNK_ITERATOR *iter = sf_get_chunk_iterator(sndfile, &inf);

        /* If there's an issue getting the chunk or block alignment, load as
         * 16-bit and have libsndfile do the conversion.
         */
        if(!iter || sf_get_chunk_size(iter, &inf) != SF_ERR_NO_ERROR || inf.datalen < 14)
            sample_format = Int16;
        else
        {
            ALubyte *fmtbuf = (ALubyte *)calloc(inf.datalen, 1);
            inf.data = fmtbuf;
            if(sf_get_chunk_data(iter, &inf) != SF_ERR_NO_ERROR)
                sample_format = Int16;
            else
            {
                /* Read the nBlockAlign field, and convert from bytes- to
                 * samples-per-block (verifying it's valid by converting back
                 * and comparing to the original value).
                 */
                byteblockalign = fmtbuf[12] | (fmtbuf[13]<<8);
                if(sample_format == IMA4)
                {
                    splblockalign = (byteblockalign/sfinfo.channels - 4)/4*8 + 1;
                    if(splblockalign < 1
                       || ((splblockalign-1)/2 + 4)*sfinfo.channels != byteblockalign)
                        sample_format = Int16;
                }
                else
                {
                    splblockalign = (byteblockalign/sfinfo.channels - 7)*2 + 2;
                    if(splblockalign < 2
                       || ((splblockalign-2)/2 + 7)*sfinfo.channels != byteblockalign)
                        sample_format = Int16;
                }
            }
            free(fmtbuf);
        }
    }

    if(sample_format == Int16)
    {
        splblockalign = 1;
        byteblockalign = sfinfo.channels * 2;
    }
    else if(sample_format == Float)
    {
        splblockalign = 1;
        byteblockalign = sfinfo.channels * 4;
    }

    /* Figure out the OpenAL format from the file and desired sample type. */
    format = getALFormat(sample_format);
    if(!format)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, C_SOUND_LOADER, "Unsupported channel count: %d\n", sfinfo.channels);
        sf_close(sndfile);
        return buffers;
    }

    if(sfinfo.frames/splblockalign > (sf_count_t)(INT_MAX/byteblockalign))
    {
        __android_log_print(ANDROID_LOG_VERBOSE, C_SOUND_LOADER, "Too many samples in %s (%" PRId64 ")\n", filename, sfinfo.frames);
        sf_close(sndfile);
        return buffers;
    }

    /* Decode the whole audio file to a buffer. */
    membuf = malloc((size_t)(sfinfo.frames / splblockalign * byteblockalign));

    if(sample_format == Int16)
        num_frames = sf_readf_short(sndfile, (short *)membuf, sfinfo.frames);
    else if(sample_format == Float)
        num_frames = sf_readf_float(sndfile, (float *)membuf, sfinfo.frames);
    else {
        sf_count_t count = sfinfo.frames / splblockalign * byteblockalign;
        num_frames = sf_read_raw(sndfile, membuf, count);
        if(num_frames > 0)
            num_frames = num_frames / byteblockalign * splblockalign;
    }

    sf_close(sndfile);

    if(num_frames < 1)
    {
        free(membuf);
        __android_log_print(ANDROID_LOG_VERBOSE, C_SOUND_LOADER, "Failed to read samples in %s (%" PRId64 ")\n", filename, num_frames);
        return buffers;
    }
    num_bytes = (ALsizei)(num_frames / splblockalign * byteblockalign);

    /* Buffer the audio data into a new buffer object, then free the data and
     * close the file.
     */
    if (sfinfo.channels > 2 || sfinfo.channels < 1) {
        free(membuf);
        __android_log_print(ANDROID_LOG_ERROR, C_SOUND_LOADER, "More than 2 channels (or 0) detected, can't play this file\n");
        return buffers;
    }
    else if (sfinfo.channels == 2) {
        if (sample_format == Int16)
            buffers = processStereoSound((short *)membuf, sfinfo, format, num_bytes);
        else if (sample_format == Float)
            buffers = processStereoSound((float *)membuf, sfinfo, format, num_bytes);
        else
            buffers = processStereoSound((char *)membuf, sfinfo, format, num_bytes); //is char correct here?
        //membuf gets free'd in processStereoSound function
    }
    else {
        alGenBuffers(1, &buffers.first);
        if(splblockalign > 1)
            alBufferi(buffers.first, AL_UNPACK_BLOCK_ALIGNMENT_SOFT, splblockalign);
        alBufferData(buffers.first, format, membuf, num_bytes, sfinfo.samplerate);
        free(membuf);
    }

    /* Check if an error occurred, and clean up if so. */
    err = alGetError();
    if(err != AL_NO_ERROR)
    {
        __android_log_print(ANDROID_LOG_VERBOSE, C_SOUND_LOADER, "OpenAL Error: %s\n", alGetString(err));
        if(buffers.first && alIsBuffer(buffers.first))
            alDeleteBuffers(1, &buffers.first);
        if(buffers.second && alIsBuffer(buffers.second))
            alDeleteBuffers(1, &buffers.second);
        return {AL_NONE, AL_NONE};
    }

    return buffers;
}
