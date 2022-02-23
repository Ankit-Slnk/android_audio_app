package com.ankit.audiodemo.listener;

import android.media.MediaPlayer;

public interface PlaybackListener {
    public void onPlay(MediaPlayer mediaPlayer);

    public void onPause(MediaPlayer mediaPlayer);

    public void onStop(MediaPlayer mediaPlayer);

    public void onResume(MediaPlayer mediaPlayer);

    public void onSkip(MediaPlayer mediaPlayer);
}
