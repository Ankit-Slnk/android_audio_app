package com.ankit.audiodemo.screens;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ankit.audiodemo.R;
import com.ankit.audiodemo.adapter.AlbumAdapter;
import com.ankit.audiodemo.listener.RecyclerViewListener;
import com.ankit.audiodemo.models.AlbumDetails;
import com.ankit.audiodemo.models.CommonView;
import com.ankit.audiodemo.utility.BottomPlaybackControl;

import java.util.ArrayList;
import java.util.List;

public class AlbumsActivity extends AppCompatActivity {

    List<AlbumDetails> albumDetails = new ArrayList<>();
    RecyclerView recyclerView;

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
        setContentView(R.layout.activity_albums);

        recyclerView = findViewById(R.id.recyclerView);

        setToolbar();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        albumDetails.add(new AlbumDetails("Album 1"));
        albumDetails.add(new AlbumDetails("Album 2"));
        albumDetails.add(new AlbumDetails("Album 3"));
        albumDetails.add(new AlbumDetails("Album 4"));
        albumDetails.add(new AlbumDetails("Album 5"));
        albumDetails.add(new AlbumDetails("Album 6"));
        albumDetails.add(new AlbumDetails("Album 7"));
        albumDetails.add(new AlbumDetails("Album 8"));
        albumDetails.add(new AlbumDetails("Album 9"));
        albumDetails.add(new AlbumDetails("Album 10"));

        AlbumAdapter adapter = new AlbumAdapter(AlbumsActivity.this, albumDetails, new RecyclerViewListener() {
            @Override
            public void onItemViewTap(int index) {
                startActivity(new Intent(AlbumsActivity.this, TrackActivity.class));
            }
        });
        recyclerView.setAdapter(adapter);

    }

    private void setToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Albums");
        setSupportActionBar(toolbar);
    }
}