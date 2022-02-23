package com.ankit.audiodemo.models;

import java.util.ArrayList;
import java.util.List;

public class AudioPlaybackList {
    public List<AudioPlaybackModel> playbacks = new ArrayList<>();

    public AudioPlaybackList(List<AudioPlaybackModel> playbacks) {
        this.playbacks = playbacks;
    }
}
