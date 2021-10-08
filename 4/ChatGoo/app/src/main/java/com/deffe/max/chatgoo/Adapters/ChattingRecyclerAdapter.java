package com.deffe.max.chatgoo.Adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.deffe.max.chatgoo.ChatActivity;
import com.deffe.max.chatgoo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChattingRecyclerAdapter extends RecyclerView.Adapter<ChattingRecyclerAdapter.ChattingUsersViewHolder>
{
    private ArrayList<String> userIds;
    private Context context;
    private Activity activity;

    public ChattingRecyclerAdapter(Set<String> userIds, Context context,Activity activity)
    {
        this.userIds = new ArrayList<>(userIds);
        this.context = context;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ChattingUsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_items,parent,false);
        return new ChattingUsersViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ChattingUsersViewHolder holder, int position)
    {
        final String userKey = userIds.get(position);

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        usersRef.child(userKey).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                String thumb = dataSnapshot.child("user_profile_thumb_img").getValue().toString();
                String userName = dataSnapshot.child("user_name").getValue().toString();

                holder.userName.setText(userName);
                holder.setUserCircleImage(thumb);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {
                Crashlytics.log(databaseError.getMessage());
            }
        });

        holder.view.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent chatIntent = new Intent(context, ChatActivity.class);
                chatIntent.putExtra("receiver_id",userKey);
                context.startActivity(chatIntent);
                activity.overridePendingTransition(R.anim.slide_in_up,R.anim.slide_out_up);
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return userIds.size();
    }

    class ChattingUsersViewHolder extends RecyclerView.ViewHolder
    {
        TextView userName;

        View view;

        ChattingUsersViewHolder(View itemView)
        {
            super(itemView);

            view = itemView;

            userName = view.findViewById(R.id.chatting_user_name);
        }

        private void setUserCircleImage(final String thumbImg)
        {
            final CircleImageView thumb = view.findViewById(R.id.chatting_user_circle_image_view);

            Picasso.with(context).load(thumbImg).networkPolicy(NetworkPolicy.OFFLINE).into(thumb, new Callback()
            {
                @Override
                public void onSuccess()
                {

                }

                @Override
                public void onError()
                {
                    Picasso.with(context).load(thumbImg).placeholder(R.drawable.img_sel).into(thumb);
                }
            });
        }
    }
}