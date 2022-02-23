package com.ankit.audiodemo.utility;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ankit.audiodemo.R;
import com.ankit.audiodemo.listener.PlaybackListener;
import com.ankit.audiodemo.models.AudioPlaybackModel;
import com.ankit.audiodemo.models.CommonView;
import com.ankit.audiodemo.screens.FullScreenPlaybackControlActivity;
import com.ankit.audiodemo.service.MediaPlayerService;

public class BottomPlaybackControl {

    public String tag = "BottomPlaybackControl";
    LinearLayout llBottomPlaybackControl;
    ImageView imgBottomPoster, imgBottomPrevious, imgBottomPlayPause, imgBottomNext;
    ProgressBar bottomProgress;
    TextView tvBottomTitle;
    public Activity activity;
    AudioPlaybackModel audioPlaybackModel;

    private static final BottomPlaybackControl ourInstance = new BottomPlaybackControl();

    public static BottomPlaybackControl getInstance() {
        return ourInstance;
    }

    public void initViews(int... optionalFlag) {
        Log.e(tag, "initViews");
        activity = CommonView.activity;
        View view = CommonView.view;
        this.llBottomPlaybackControl = view.findViewById(R.id.llBottomPlaybackControl);
        this.imgBottomPoster = view.findViewById(R.id.imgBottomPoster);
        this.imgBottomPrevious = view.findViewById(R.id.imgBottomPrevious);
        this.imgBottomPlayPause = view.findViewById(R.id.imgBottomPlayPause);
        this.imgBottomNext = view.findViewById(R.id.imgBottomNext);
        this.bottomProgress = view.findViewById(R.id.bottomProgress);
        this.tvBottomTitle = view.findViewById(R.id.tvBottomTitle);

        setView(optionalFlag);
    }

    private void setView(int... optionalFlag) { // optionalFlag optional parameter
        Log.e(tag, "setView");
        int tapToPlay = optionalFlag.length >= 1 ? optionalFlag[0] : 0;
        // 0 - do not start service but check is service running
        // 1 - start service
        // 2 - app opening first time - so not start service
        setViewData();

        if (audioPlaybackModel == null) {
            this.llBottomPlaybackControl.setVisibility(View.GONE);
        } else {
            this.llBottomPlaybackControl.setVisibility(View.VISIBLE);
            Intent playerIntent = new Intent(activity, MediaPlayerService.class);
            Log.e("tapToPlay", tapToPlay + "");
            Log.e("isMyServiceRunning", Utility.isMyServiceRunning(activity, MediaPlayerService.class) + "");
            if (tapToPlay != 2) {
                if (tapToPlay == 1) {
                    activity.startService(playerIntent);
                } else if (!Utility.isMyServiceRunning(activity, MediaPlayerService.class)) { // IMPORTANT
                    activity.startService(playerIntent);
                }
            }
            activity.bindService(playerIntent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    // We've bound to LocalService, cast the IBinder and get LocalService instance
                    MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
                    MediaPlayerService mediaPlayerService = binder.getService();
                    if (mediaPlayerService.getMediaPlayer() != null) {
                        setPlaybackData(mediaPlayerService.getMediaPlayer());
                    }
                    mediaPlayerService.initListener(new PlaybackListener() {
                        @Override
                        public void onPlay(MediaPlayer mediaPlayer) {
                            setPlaybackData(mediaPlayer);
                        }

                        @Override
                        public void onPause(MediaPlayer mediaPlayer) {
                            setPlaybackData(mediaPlayer);
                        }

                        @Override
                        public void onStop(MediaPlayer mediaPlayer) {
                            setPlaybackData(mediaPlayer);
                        }

                        @Override
                        public void onResume(MediaPlayer mediaPlayer) {
                            setPlaybackData(mediaPlayer);
                        }

                        @Override
                        public void onSkip(MediaPlayer mediaPlayer) {
                            setPlaybackData(mediaPlayer);
                        }
                    }, tapToPlay == 1);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            }, Context.BIND_AUTO_CREATE);

            this.imgBottomPlayPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.sendBroadcast(new Intent(Utility.Broadcast_PLAY_NEW_AUDIO));
                }
            });

            setViewTap();
        }
    }

    public void setPlaybackData(MediaPlayer mediaPlayer) {
        Log.e(tag, "setPlaybackData");
        this.llBottomPlaybackControl.setVisibility(View.VISIBLE);
        setViewData();
        if (mediaPlayer.isPlaying()) {
            this.imgBottomPlayPause.setImageResource(R.drawable.pause);
        } else {
            this.imgBottomPlayPause.setImageResource(R.drawable.play);
        }
        this.imgBottomPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {
                    activity.sendBroadcast(new Intent(Utility.Broadcast_PAUSE_AUDIO));
                } else {
                    activity.sendBroadcast(new Intent(Utility.Broadcast_PLAY_AUDIO));
                }
            }
        });

        setViewTap();
    }

    private void setViewData() {
        audioPlaybackModel = Utility.getCurrentPlaybackAudio(activity);
        if (audioPlaybackModel != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    new GlideImageLoader(imgBottomPoster, bottomProgress).load(audioPlaybackModel.posterUrl, Utility.getGlideRequestOptions());
                    String title = "";
                    if (audioPlaybackModel.title != null) {
                        if (!audioPlaybackModel.title.equals("null")) {
                            title = audioPlaybackModel.title;
                        }
                    }
                    tvBottomTitle.setText(title);
                }
            });
        }
    }

    private void setViewTap() {
        this.imgBottomPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.sendBroadcast(new Intent(Utility.Broadcast_SKIP_PREVIOUS_AUDIO));
            }
        });

        this.imgBottomNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.sendBroadcast(new Intent(Utility.Broadcast_SKIP_NEXT_AUDIO));
            }
        });

        this.llBottomPlaybackControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startActivity(new Intent(activity, FullScreenPlaybackControlActivity.class));
            }
        });
    }
}
