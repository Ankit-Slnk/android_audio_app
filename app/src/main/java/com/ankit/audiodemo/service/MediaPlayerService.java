package com.ankit.audiodemo.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.ankit.audiodemo.R;
import com.ankit.audiodemo.listener.PlaybackListener;
import com.ankit.audiodemo.models.AudioPlaybackList;
import com.ankit.audiodemo.models.AudioPlaybackModel;
import com.ankit.audiodemo.models.PlaybackStatus;
import com.ankit.audiodemo.utility.PrefUtils;
import com.ankit.audiodemo.utility.Utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener {

    private static String tag = "MediaPlayerService";

    public static final String ACTION_PLAY = "com.audio.app.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.audio.app.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.audio.app.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.audio.app.ACTION_NEXT";
    public static final String ACTION_STOP = "com.audio.app.ACTION_STOP";
    private final String MEDIA_CHANNEL_ID = "media_playback_channel";

    private MediaPlayer mediaPlayer;

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSession mediaSession;
    private MediaController.TransportControls transportControls;

    //AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;

    //Used to pause/resume MediaPlayer
    private int resumePosition;

    //AudioFocus
    private AudioManager audioManager;

    // Binder given to clients
    private final IBinder iBinder = new LocalBinder();

    //List of available Audio files
    private List<AudioPlaybackModel> audioList;
    private int audioIndex = -1;
    private AudioPlaybackModel activeAudio; //an object on the currently playing audio

    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    private NotificationManager notificationManager;
    private PlaybackListener listener;
    boolean isTapToPlay = false;

    /**
     * Service lifecycle methods
     */
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onCreate() {
        Log.e(tag, "onCreate");
        super.onCreate();
        // Perform one-time setup procedures

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener();
        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver();
        //Listen for new Audio to play -- BroadcastReceiver
        register_playNewAudio();
        register_playAudio();
        register_StopAudio();
        register_PauseAudio();
        register_ResumeAudio();
        register_SkipNextAudio();
        register_SkipPreviousAudio();
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    //The system calls this method when an activity, requests the service be started
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(tag, "onStartCommand");
        try {
            //unregister BroadcastReceivers
            unregisterReceiver(becomingNoisyReceiver);
            unregisterReceiver(playNewAudio);
        } catch (Exception e) {

        }
        try {
            // You only need to create the channel on API 26+ devices
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createChannel();
            }
            //Load data from SharedPreferences
            AudioPlaybackList audioPlaybackList = PrefUtils.getAudioPlayback(MediaPlayerService.this);
            audioList = audioPlaybackList == null ? new ArrayList<>() : audioPlaybackList.playbacks;
            audioIndex = PrefUtils.getAudioPlaybackIndex(MediaPlayerService.this);
            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }
        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (!requestAudioFocus()) {
            //Could not gain focus
            stopSelf();
        }
        if (mediaSessionManager == null) {
            try {
                initMediaSession();
            } catch (RemoteException e) {
                Log.e("initMediaSession", e.toString());
                stopSelf();
            }
        }
        initMediaPlayer();
        buildNotification(PlaybackStatus.PLAYING);

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    public void initListener(PlaybackListener listener, boolean isTapToPlay) {
        Log.e(tag, "initListener");
        this.isTapToPlay = isTapToPlay;
        this.listener = listener;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        removeNotification();

        //unregister BroadcastReceivers
        try {
            unregisterReceiver(becomingNoisyReceiver);
        } catch (Exception e) {
            Log.e("unregisterReceiver", "becomingNoisyReceiver");
        }
        try {
            unregisterReceiver(playNewAudio);
        } catch (Exception e) {
            Log.e("unregisterReceiver", "playNewAudio");
        }

        //clear cached playlist
        PrefUtils.setAudioPlayback(new AudioPlaybackList(new ArrayList<>()), MediaPlayerService.this);
        PrefUtils.setAudioPlaybackIndex(-1, MediaPlayerService.this);
    }

    /**
     * Service Binder
     */
    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MediaPlayerService.this;
        }
    }

    /**
     * MediaPlayer callback methods
     */
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //Invoked indicating buffering status of
        //a media resource being streamed over the network.
        Log.e("onBufferingUpdate", percent + "");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mp.getCurrentPosition() != 0 || mp.getDuration() != 0) {
            if ((mp.getCurrentPosition() / 1000) != 0 || (mp.getDuration() / 1000) != 0) {
                if ((mp.getCurrentPosition() / 1000) == (mp.getDuration() / 1000)) {
                    skipToNext();
                }
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.e("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.e("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.e("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.e(tag, "onPrepared");
        if (isTapToPlay) {
            playMedia();
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.e(tag, "onSeekComplete");
        playMedia();
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        Log.e(tag, "onAudioFocusChange");
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) {
                    initMediaPlayer();
                } else if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                }
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    /**
     * AudioFocus
     */
    private boolean requestAudioFocus() {
        Log.e(tag, "requestAudioFocus");
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        //Focus gained
        boolean isFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        Log.e("isFocus", isFocus + "");
        return isFocus;
        //Could not gain focus
    }

    private boolean removeAudioFocus() {
        Log.e(tag, "removeAudioFocus");
        try {
            return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * MediaPlayer actions
     */
    private void initMediaPlayer() {
        Log.e(tag, "initMediaPlayer");
        if (mediaPlayer == null)
            mediaPlayer = new MediaPlayer();//new MediaPlayer instance

        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(activeAudio.audioUrl);
        } catch (IOException e) {
            Log.e("setDataSource", e.toString());
            stopSelf();
        }
        try {
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e("prepareAsync", e.toString());
            stopSelf();
        }
    }

    private void playMedia() {
        Log.e(tag, "playMedia");
        if (!mediaPlayer.isPlaying()) {
            Log.e(tag, "playMedia 1");
            mediaPlayer.start();
            listener.onPlay(mediaPlayer);
            updateMetaData();
        }
    }

    private BroadcastReceiver playAudioReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playMedia();
        }
    };

    private void register_playAudio() {
        Log.e(tag, "register_playAudio");
        //Register playNewMedia receiver
        try {
            unregisterReceiver(playAudioReceiver);
        } catch (Exception e) {

        }
        IntentFilter filter = new IntentFilter(Utility.Broadcast_PLAY_AUDIO);
        registerReceiver(playAudioReceiver, filter);
    }

    private void stopMedia() {
        Log.e(tag, "stopMedia");
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            listener.onStop(mediaPlayer);
        }
    }

    private void register_StopAudio() {
        Log.e(tag, "register_StopAudio");
        try {
            IntentFilter filter = new IntentFilter(Utility.Broadcast_STOP_AUDIO);
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    stopMedia();
                }
            }, filter);
        } catch (Exception e) {

        }
    }

    private void pauseMedia() {
        Log.e(tag, "pauseMedia");
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
            listener.onPause(mediaPlayer);
            buildNotification(PlaybackStatus.PAUSED);
        }
    }

    private void register_PauseAudio() {
        Log.e(tag, "register_PauseAudio");
        try {
            IntentFilter filter = new IntentFilter(Utility.Broadcast_PAUSE_AUDIO);
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    pauseMedia();
                }
            }, filter);
        } catch (Exception e) {

        }
    }

    private void resumeMedia() {
        Log.e(tag, "resumeMedia");
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
            listener.onResume(mediaPlayer);
            buildNotification(PlaybackStatus.PLAYING);
        }
    }

    private void register_ResumeAudio() {
        Log.e(tag, "register_ResumeAudio");
        try {
            IntentFilter filter = new IntentFilter(Utility.Broadcast_RESUME_AUDIO);
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    resumeMedia();
                }
            }, filter);
        } catch (Exception e) {

        }
    }

    private void skipToNext() {
        Log.e(tag, "skipToNext");
        if (audioIndex == audioList.size() - 1) {
            //if last in playlist
            audioIndex = 0;
            activeAudio = audioList.get(audioIndex);
        } else {
            //get next in playlist
            activeAudio = audioList.get(++audioIndex);
        }

        //Update stored index
        PrefUtils.setAudioPlaybackIndex(audioIndex, MediaPlayerService.this);

        listener.onSkip(mediaPlayer);

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();

        updateMetaData();
        buildNotification(PlaybackStatus.PLAYING);
    }

    private void register_SkipNextAudio() {
        Log.e(tag, "register_SkipNextAudio");
        //Register playNewMedia receiver
        try {
            IntentFilter filter = new IntentFilter(Utility.Broadcast_SKIP_NEXT_AUDIO);
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    skipToNext();
                }
            }, filter);
        } catch (Exception e) {

        }
    }

    private void skipToPrevious() {
        Log.e(tag, "skipToPrevious");
        if (audioIndex == 0) {
            //if first in playlist
            //set index to the last of audioList
            audioIndex = audioList.size() - 1;
            activeAudio = audioList.get(audioIndex);
        } else {
            //get previous in playlist
            activeAudio = audioList.get(--audioIndex);
        }

        //Update stored index
        PrefUtils.setAudioPlaybackIndex(audioIndex, MediaPlayerService.this);

        listener.onSkip(mediaPlayer);

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();

        updateMetaData();
        buildNotification(PlaybackStatus.PLAYING);
    }

    private void register_SkipPreviousAudio() {
        Log.e(tag, "register_SkipPreviousAudio");
        //Register playNewMedia receiver
        try {
            IntentFilter filter = new IntentFilter(Utility.Broadcast_SKIP_PREVIOUS_AUDIO);
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    skipToPrevious();
                }
            }, filter);
        } catch (Exception e) {

        }
    }

    /**
     * ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs
     */
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver() {
        Log.e(tag, "registerBecomingNoisyReceiver");
        //register after getting audio focus
        try {
            IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            registerReceiver(becomingNoisyReceiver, intentFilter);
        } catch (Exception e) {

        }
    }

    /**
     * Handle PhoneState changes
     */
    private void callStateListener() {
        Log.e(tag, "callStateListener");
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * MediaSession and Notification actions
     */
    private void initMediaSession() throws RemoteException {
        Log.e(tag, "initMediaSession");
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSession(getApplicationContext(), "AudioPlayer");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSession.Callback.
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Set mediaSession's MetaData
        updateMetaData();

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSession.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }

        });
    }

    private void updateMetaData() {
        Log.e(tag, "updateMetaData");
        String title = "";
        String artist = "";
        if (activeAudio.title != null) {
            if (!activeAudio.title.equals("null")) {
                title = activeAudio.title;
            }
        }
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadata.Builder()
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, getPoster())
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .build());
    }

    Bitmap getPoster() {
        final Bitmap[] albumArt = {BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_background)};
//        Glide.with(this)
//                .asBitmap()
//                .load(activeAudio.posterUrl)
//                .into(new CustomTarget<Bitmap>() {
//                    @Override
//                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
//                        albumArt[0] = resource;
//                    }
//
//                    @Override
//                    public void onLoadCleared(@Nullable Drawable placeholder) {
//                    }
//                });
        return albumArt[0];
    }

    private void buildNotification(PlaybackStatus playbackStatus) {
        /**
         * Notification actions -> playbackAction()
         *  0 -> Play
         *  1 -> Pause
         *  2 -> Next track
         *  3 -> Previous track
         */

        int notificationAction = R.drawable.ic_notification_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = R.drawable.ic_notification_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = R.drawable.ic_notification_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        String title = "";
        String artist = "";
        if (activeAudio.title != null) {
            if (!activeAudio.title.equals("null")) {
                title = activeAudio.title;
            }
        }
        // Create a new Notification
        Notification.Builder notificationBuilder = (Notification.Builder) new Notification.Builder(this, MEDIA_CHANNEL_ID)
                // Hide the timestamp
                .setShowWhen(false)
                // Set the Notification style
                .setStyle(new Notification.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compat view
                        .setShowActionsInCompactView(0, 1, 2))
                .setOngoing(true)
                // Set the Notification color
//                .setColor(getResources().getColor(R.color.appColor))
                // Set the large and small icons
                .setLargeIcon(getPoster())
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                // Set Notification content information
                .setContentTitle(title)
                .setContentInfo(artist)
                // Add playback actions
                .addAction(R.drawable.ic_notification_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(R.drawable.ic_notification_next, "next", playbackAction(2));

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private PendingIntent playbackAction(int actionNumber) {
        Log.e(tag, "playbackAction");
        Log.e(tag, "playbackAction - " + actionNumber);
        Intent playbackAction = new Intent(this, MediaPlayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0);
            default:
                break;
        }
        return null;
    }

    private void removeNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }

    /**
     * Play new Audio
     */
    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(tag, "playNewAudio");
            //Get the new media index form SharedPreferences
            audioIndex = PrefUtils.getAudioPlaybackIndex(MediaPlayerService.this);
            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }

            //A PLAY_NEW_AUDIO action received
            //reset mediaPlayer to play the new Audio
            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }
    };

    private void register_playNewAudio() {
        Log.e(tag, "register_playNewAudio");
        try {
            IntentFilter filter = new IntentFilter(Utility.Broadcast_PLAY_NEW_AUDIO);
            registerReceiver(playNewAudio, filter);
        } catch (Exception e) {

        }
    }

    private void createChannel() {
        Log.e(tag, "createChannel");
        // The id of the channel.
        String id = MEDIA_CHANNEL_ID;
        // The user-visible name of the channel.
        CharSequence name = "Media playback";
        // The user-visible description of the channel.
        String description = "Media playback controls";
        int importance;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            importance = NotificationManager.IMPORTANCE_LOW;
        } else {
            importance = NotificationManager.IMPORTANCE_DEFAULT;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(id, name, importance);
            // Configure the notification channel.
            mChannel.setDescription(description);
            mChannel.setShowBadge(false);
            mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(mChannel);
        }
    }
}
