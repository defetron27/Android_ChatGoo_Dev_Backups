package com.deffe.max.chatgoo.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.deffe.max.chatgoo.Models.MessageTypesModel;
import com.deffe.max.chatgoo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class BotChattingAdapter extends RecyclerView.Adapter<BotChattingAdapter.BotChattingViewHolder>
{
    private Context context;
    private ArrayList<MessageTypesModel> messages;

    public BotChattingAdapter(Context context, ArrayList<MessageTypesModel> messages)
    {
        this.context = context;
        this.messages = messages;
    }

    @NonNull
    @Override
    public BotChattingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bot_chatting_text_layout_items,parent,false);

        return new BotChattingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BotChattingViewHolder holder, int position)
    {
        MessageTypesModel model = messages.get(position);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser user = firebaseAuth.getCurrentUser();

        String onlineUserId = null;

        String botId = "61d29dfb150b4378bf63895c95c6c75c";

        if (user != null)
        {
            onlineUserId = user.getUid();
        }

        if (model.getType().equals("text"))
        {
            if (model.getFrom().equals(botId))
            {
                holder.outcomeMessage.setVisibility(View.GONE);

                holder.incomeResponse.setVisibility(View.VISIBLE);
                holder.assistantImg.setVisibility(View.VISIBLE);

                holder.incomeResponse.setText(model.getMessage());
            }
            if (model.getFrom().equals(onlineUserId))
            {
                holder.incomeResponse.setVisibility(View.GONE);
                holder.assistantImg.setVisibility(View.GONE);

                holder.outcomeMessage.setVisibility(View.VISIBLE);

                holder.outcomeMessage.setText(model.getMessage());
            }
        }
    }

    @Override
    public int getItemCount()
    {
        return messages.size();
    }

    class BotChattingViewHolder extends RecyclerView.ViewHolder
    {
        private TextView incomeResponse,outcomeMessage;
        private ImageView assistantImg;

        BotChattingViewHolder(View itemView)
        {
            super(itemView);

            incomeResponse = itemView.findViewById(R.id.income_response);
            outcomeMessage = itemView.findViewById(R.id.outcome_message);
            assistantImg = itemView.findViewById(R.id.assistant_logo);
        }
    }
}
