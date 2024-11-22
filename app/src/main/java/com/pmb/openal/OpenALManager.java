package com.pmb.openal;

public class OpenALManager {
    static {
        System.loadLibrary("openal");
        System.loadLibrary("musicplayer");
    }

    public interface OpenALCallback {
        void onSoundFinished(String filePath);
    }

    public static native boolean initOpenAL(String selectedHrtf);
    public static native void setCallback(OpenALCallback callback);
    public static native void cleanupOpenAL();

    public static native float playMusic(String filePath); //returns duration in seconds
    public static native void pauseMusic(String filePath);
    public static native void resumeMusic(String filePath);
    public static native void stopMusic();
    public static native void stopMusic(String filePath);

    public static native void updateSourcePosition(String filePath, float angle, float radius, float height);
    public static native void setStereoAngle(float angle);
    public static native void setPlaybackPosition(String filePath, float seconds);
    public static native float getPlaybackPosition(String filePath); //if the return value is negative it means it's not playing


    //public static void finishedPlaying(String filePath) { Log.d("OpenALManager", "received finished playing: " + filePath); }
}
