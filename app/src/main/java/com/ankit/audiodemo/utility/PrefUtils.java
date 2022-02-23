package com.ankit.audiodemo.utility;

import android.content.Context;

import com.ankit.audiodemo.models.AudioPlaybackList;

public class PrefUtils {
    public static AudioPlaybackList getAudioPlayback(Context ctx) {
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, "anchorUser", 0);
        AudioPlaybackList currentUser = complexPreferences.getObject("anchorPaybackList", AudioPlaybackList.class);
        return currentUser;
    }

    public static void setAudioPlayback(AudioPlaybackList currentUser, Context ctx) {
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, "anchorUser", 0);
        complexPreferences.putObject("anchorPaybackList", currentUser);
        complexPreferences.commit();
    }

    // playback audio index

    public static int getAudioPlaybackIndex(Context ctx) {
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, "anchorUser", 0);
        Integer currentUser = complexPreferences.getObject("anchorPaybackListIndex", Integer.class);
        return currentUser;
    }

    public static void setAudioPlaybackIndex(int currentUser, Context ctx) {
        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(ctx, "anchorUser", 0);
        complexPreferences.putObject("anchorPaybackListIndex", currentUser);
        complexPreferences.commit();
    }
}
