package com.ankit.audiodemo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.ankit.audiodemo.R;
import com.ankit.audiodemo.listener.RecyclerViewListener;
import com.ankit.audiodemo.models.AlbumDetails;

import java.util.ArrayList;
import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.MyViewHolder> {
    private List<AlbumDetails> listData = new ArrayList<>();
    private List<AlbumDetails> tempListData = new ArrayList<>();
    Context context;
    RecyclerViewListener recyclerViewListener;

    public AlbumAdapter(Context context, List<AlbumDetails> listData, RecyclerViewListener recyclerViewListener) {
        this.context = context;
        this.listData = listData;
        this.recyclerViewListener = recyclerViewListener;
        this.tempListData = new ArrayList<>();
        this.tempListData.addAll(listData);
    }

    @Override
    public AlbumAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.album_item_view, parent, false);

        return new AlbumAdapter.MyViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(AlbumAdapter.MyViewHolder holder, int position) {

        AlbumDetails data = listData.get(position);

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
