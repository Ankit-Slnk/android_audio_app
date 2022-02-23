package com.ankit.audiodemo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.ankit.audiodemo.R;
import com.ankit.audiodemo.listener.RecyclerViewListener;
import com.ankit.audiodemo.models.TrackDetails;

import java.util.ArrayList;
import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.MyViewHolder> {
    private List<TrackDetails> listData = new ArrayList<>();
    private List<TrackDetails> tempListData = new ArrayList<>();
    Context context;
    RecyclerViewListener recyclerViewListener;

    public TrackAdapter(Context context, List<TrackDetails> listData, RecyclerViewListener recyclerViewListener) {
        this.context = context;
        this.listData = listData;
        this.recyclerViewListener = recyclerViewListener;
        this.tempListData = new ArrayList<>();
        this.tempListData.addAll(listData);
    }

    @Override
    public TrackAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.track_item_view, parent, false);

        return new TrackAdapter.MyViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(TrackAdapter.MyViewHolder holder, int position) {

        TrackDetails data = listData.get(position);

        holder.tvTitle.setText(data.name);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recyclerViewListener.onItemViewTap(position);
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
        TextView tvTitle;

        public MyViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvTitle);
        }
    }

}
