package com.ankit.audiodemo.screens;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ankit.audiodemo.R;
import com.ankit.audiodemo.adapter.FullScreenPlaybackAdapter;
import com.ankit.audiodemo.listener.PlaybackListener;
import com.ankit.audiodemo.listener.RecyclerViewListener;
import com.ankit.audiodemo.models.AudioPlaybackList;
import com.ankit.audiodemo.models.AudioPlaybackModel;
import com.ankit.audiodemo.service.MediaPlayerService;
import com.ankit.audiodemo.utility.PrefUtils;
import com.ankit.audiodemo.utility.Utility;

import java.util.ArrayList;
import java.util.List;

public class FullScreenPlaybackControlActivity extends AppCompatActivity {

    ImageView imgPrevious, imgNext, imgShuffle;// imgPoster
    //    ProgressBar progress;
    SeekBar seekBar;
    TextView tvCurrentTime, tvTotalTime;
    LinearLayout llPause, llPlay;
    RecyclerView recyclerView;
    AudioPlaybackModel audioPlaybackModel;
    Toolbar toolbar;
    String tempPosterUrl = "";
    private Handler mHandler = new Handler();
    MediaPlayer mMediaPlayer;
    FullScreenPlaybackAdapter fullScreenPlaybackAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_playback_control);

//        imgPoster = findViewById(R.id.imgPoster);
//        progress = findViewById(R.id.progress);
        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        imgPrevious = findViewById(R.id.imgPrevious);
        llPause = findViewById(R.id.llPause);
        llPlay = findViewById(R.id.llPlay);
        imgNext = findViewById(R.id.imgNext);
        imgShuffle = findViewById(R.id.imgShuffle);
        recyclerView = findViewById(R.id.recyclerView);
        toolbar = findViewById(R.id.toolbar);

        recyclerView.setLayoutManager(new LinearLayoutManager(FullScreenPlaybackControlActivity.this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        imgPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioPlaybackModel != null) {
                    sendBroadcast(new Intent(Utility.Broadcast_SKIP_PREVIOUS_AUDIO));
                }
            }
        });

        imgNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioPlaybackModel != null) {
                    sendBroadcast(new Intent(Utility.Broadcast_SKIP_NEXT_AUDIO));
                }
            }
        });

        imgShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioPlaybackModel != null) {

                }
            }
        });

        llPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioPlaybackModel != null) {
                    sendBroadcast(new Intent(Utility.Broadcast_PLAY_AUDIO));
                }
            }
        });

        llPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioPlaybackModel != null) {
                    sendBroadcast(new Intent(Utility.Broadcast_PAUSE_AUDIO));
                }
            }
        });

        setView(false);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mMediaPlayer != null && fromUser) {
                    mMediaPlayer.seekTo(progress * 1000);
                }
            }
        });
    }

    public void setView(boolean isTapToPlay) {
        Intent playerIntent = new Intent(FullScreenPlaybackControlActivity.this, MediaPlayerService.class);
        Log.e("isTapToPlay 1", isTapToPlay + "");
        Log.e("isMyServiceRunning 1", Utility.isMyServiceRunning(FullScreenPlaybackControlActivity.this, MediaPlayerService.class) + "");
        if (isTapToPlay) {
            startService(playerIntent);
        } else if (!Utility.isMyServiceRunning(FullScreenPlaybackControlActivity.this, MediaPlayerService.class)) { // IMPORTANT
            startService(playerIntent);
        }
        bindService(playerIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
                MediaPlayerService mediaPlayerService = binder.getService();

                if (mediaPlayerService.getMediaPlayer() != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mediaPlayerService.getMediaPlayer() != null) {
                                mMediaPlayer = mediaPlayerService.getMediaPlayer();
                                updateDurationView();
                            }
                            mHandler.postDelayed(this, 1000);
                        }
                    });
                    updateView();
                    setAudioPlaybackList();
                }

                mediaPlayerService.initListener(new PlaybackListener() {
                    @Override
                    public void onPlay(MediaPlayer mediaPlayer) {
                        mMediaPlayer = mediaPlayer;
                        updateView();
                    }

                    @Override
                    public void onPause(MediaPlayer mediaPlayer) {
                        mMediaPlayer = mediaPlayer;
                        updateView();
                    }

                    @Override
                    public void onStop(MediaPlayer mediaPlayer) {
                        mMediaPlayer = mediaPlayer;
                        updateView();
                    }

                    @Override
                    public void onResume(MediaPlayer mediaPlayer) {
                        mMediaPlayer = mediaPlayer;
                        updateView();
                    }

                    @Override
                    public void onSkip(MediaPlayer mediaPlayer) {
                        mMediaPlayer = mediaPlayer;
                        updateView();
                        setAudioPlaybackList();
                    }
                }, isTapToPlay);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        }, Context.BIND_AUTO_CREATE);
    }

    void setAudioPlaybackList() {
        List<AudioPlaybackModel> playbacks = new ArrayList<>();
        AudioPlaybackList audioPlaybackList;
        audioPlaybackList = PrefUtils.getAudioPlayback(FullScreenPlaybackControlActivity.this);
        if (audioPlaybackList != null) {
            playbacks = audioPlaybackList.playbacks;
            if (playbacks.size() > 0) {
                fullScreenPlaybackAdapter = new FullScreenPlaybackAdapter(FullScreenPlaybackControlActivity.this, playbacks, new RecyclerViewListener() {
                    @Override
                    public void onItemViewTap(int index) {
                        PrefUtils.setAudioPlaybackIndex(index, FullScreenPlaybackControlActivity.this);
                        setView(true);
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.e("tapToPlay", "handler 1");
                                sendBroadcast(new Intent(Utility.Broadcast_PLAY_NEW_AUDIO));
                            }
                        }, 300);
                        fullScreenPlaybackAdapter.notifyDataSetChanged();
                    }
                });
                recyclerView.setAdapter(fullScreenPlaybackAdapter);
            }
        }
    }

    public void updateView() {
        togglePlayPauseView();
        audioPlaybackModel = Utility.getCurrentPlaybackAudio(FullScreenPlaybackControlActivity.this);

        if (audioPlaybackModel != null) {
//            if (!tempPosterUrl.equals(audioPlaybackModel.posterUrl)) {
//                // not to update image on every event
//                tempPosterUrl = audioPlaybackModel.posterUrl;
//                new GlideImageLoader(imgPoster, progress).load(tempPosterUrl, Utility.getGlideRequestOptions());
//            }
            toolbar.setTitle(audioPlaybackModel.title.equals("null") ? "" : audioPlaybackModel.title);
            updateDurationView();
        }
    }

    public void updateDurationView() {
        if (mMediaPlayer != null) {
            tvTotalTime.setText(Utility.milliSecondsToTimer(mMediaPlayer.getDuration()));
            tvCurrentTime.setText(Utility.milliSecondsToTimer(mMediaPlayer.getCurrentPosition()));
            seekBar.setMax(mMediaPlayer.getDuration() / 1000);
            seekBar.setProgress(mMediaPlayer.getCurrentPosition() / 1000);
        }
    }

    public void togglePlayPauseView() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                llPause.setVisibility(View.VISIBLE);
                llPlay.setVisibility(View.GONE);
            } else {
                llPause.setVisibility(View.GONE);
                llPlay.setVisibility(View.VISIBLE);
            }
        }
    }
}