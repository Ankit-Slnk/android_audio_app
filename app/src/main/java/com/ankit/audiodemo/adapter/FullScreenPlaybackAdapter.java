package com.ankit.audiodemo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.ankit.audiodemo.R;
import com.ankit.audiodemo.listener.RecyclerViewListener;
import com.ankit.audiodemo.models.AudioPlaybackModel;
import com.ankit.audiodemo.utility.PrefUtils;

import java.util.List;

public class FullScreenPlaybackAdapter extends RecyclerView.Adapter<FullScreenPlaybackAdapter.MyViewHolder> {

    private List<AudioPlaybackModel> listData;
    Context context;
    RecyclerViewListener listener;

    public FullScreenPlaybackAdapter(Context context, List<AudioPlaybackModel> listData, RecyclerViewListener listener) {
        this.context = context;
        this.listData = listData;
        this.listener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.playback_song_item_view, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        AudioPlaybackModel data = listData.get(position);

//        new GlideImageLoader(holder.imgPoster, holder.progress).load(data.posterUrl, Utility.getGlideRequestOptions());

        holder.tvTitle.setText(data.title.equals("null") ? "" : data.title);

        int currentIndex = -1;
        try {
            if (PrefUtils.getAudioPlaybackIndex(context) == -1) {
                currentIndex = -1;
            } else {
                currentIndex = PrefUtils.getAudioPlaybackIndex(context);
            }
        } catch (Exception e) {
            currentIndex = -1;
        }

        holder.imgPlaying.setVisibility(View.GONE);

        if (currentIndex == position) {
            holder.imgPlaying.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemViewTap(position);
            }
        });
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPlaying;
        TextView tvTitle;

        public MyViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvTitle);
            imgPlaying = view.findViewById(R.id.imgPlaying);
        }
    }
}
