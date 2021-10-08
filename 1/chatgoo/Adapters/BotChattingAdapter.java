package com.deffe.max.chatgoo.Adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class BotChattingAdapter extends RecyclerView.Adapter<BotChattingAdapter.BotChattingViewHolder>
{
    @NonNull
    @Override
    public BotChattingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull BotChattingViewHolder holder, int position)
    {

    }

    @Override
    public int getItemCount()
    {
        return 0;
    }

    class BotChattingViewHolder extends RecyclerView.ViewHolder
    {
        BotChattingViewHolder(View itemView)
        {
            super(itemView);
        }
    }
}
