package com.ankit.audiodemo.models;


import java.io.Serializable;

public class AudioPlaybackModel implements Serializable {

    public String title;
    public String audioUrl;

    public AudioPlaybackModel(String title, String audioUrl) {
        this.title = title;
        this.audioUrl = audioUrl;
    }
}
