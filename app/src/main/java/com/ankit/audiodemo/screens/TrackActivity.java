package com.ankit.audiodemo.screens;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ankit.audiodemo.R;
import com.ankit.audiodemo.adapter.TrackAdapter;
import com.ankit.audiodemo.listener.RecyclerViewListener;
import com.ankit.audiodemo.models.AudioPlaybackModel;
import com.ankit.audiodemo.models.CommonView;
import com.ankit.audiodemo.models.TrackDetails;
import com.ankit.audiodemo.utility.BottomPlaybackControl;
import com.ankit.audiodemo.utility.Utility;

import java.util.ArrayList;
import java.util.List;

public class TrackActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    List<TrackDetails> trackDetails = new ArrayList<>();

    @Override
    protected void onResume() {
        super.onResume();
        refreshBottomPlayBack();
    }

    public void refreshBottomPlayBack() {
        CommonView.activity = this;
        CommonView.view = getWindow().getDecorView().getRootView();
        BottomPlaybackControl.getInstance().initViews();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        recyclerView = findViewById(R.id.recyclerView);

        setToolbar();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        trackDetails.add(new TrackDetails("Track 1 - 700 KB", "https://file-examples-com.github.io/uploads/2017/11/file_example_MP3_700KB.mp3"));
        trackDetails.add(new TrackDetails("Track 2 - 1 MB", "https://file-examples-com.github.io/uploads/2017/11/file_example_MP3_1MG.mp3"));
        trackDetails.add(new TrackDetails("Track 3 - 2 MB", "https://file-examples-com.github.io/uploads/2017/11/file_example_MP3_2MG.mp3"));
        trackDetails.add(new TrackDetails("Track 3 - 5 MB", "https://file-examples-com.github.io/uploads/2017/11/file_example_MP3_5MG.mp3"));

        TrackAdapter adapter = new TrackAdapter(TrackActivity.this, trackDetails, new RecyclerViewListener() {
            @Override
            public void onItemViewTap(int index) {
                List<AudioPlaybackModel> tempAudioList = new ArrayList<>();
                for (int i = 0; i < trackDetails.size(); i++) {
                    TrackDetails data = trackDetails.get(i);
                    String title = "";
                    String audioUrl = "";

                    title = data.name;
                    audioUrl = data.url;

                    tempAudioList.add(new AudioPlaybackModel(title + "", audioUrl + ""));
                }
                Utility.tapToPlay(TrackActivity.this, tempAudioList, index);
            }
        });

        recyclerView.setAdapter(adapter);
    }

    private void setToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Tracks");
        setSupportActionBar(toolbar);
    }
}