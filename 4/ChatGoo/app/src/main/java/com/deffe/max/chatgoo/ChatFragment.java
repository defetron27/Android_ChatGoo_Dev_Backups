package com.deffe.max.chatgoo;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.deffe.max.chatgoo.Adapters.ChattingRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatFragment extends Fragment
{

    private String onlineUserId;

    private View view;

    private ChattingRecyclerAdapter adapter;

    private Set<String> finalizedNumbers = new HashSet<>();

    public ChatFragment()
    {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        view = inflater.inflate(R.layout.fragment_chat, container, false);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null)
        {
            onlineUserId = firebaseUser.getUid();
        }

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        final Set<String> chattingUsers = new HashSet<>(getContactNumbers());

        final RecyclerView chattingUsersRecyclerView = view.findViewById(R.id.chatting_users_recycler_view);
        chattingUsersRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext(),LinearLayoutManager.VERTICAL,false));

        ValueEventListener eventListener = new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                finalizedNumbers.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren())
                {
                    String userKey = dataSnapshot.getKey();

                    if (userKey != null && !userKey.equals(onlineUserId))
                    {
                        String mobileNumber = dataSnapshot.child("user_number").getValue().toString();
                        String mobileNumberWithPlus = dataSnapshot.child("user_number_with_plus").getValue().toString();

                        for (String number : chattingUsers)
                        {
                            if (number.equals(mobileNumber) || number.equals(mobileNumberWithPlus))
                            {
                                finalizedNumbers.add(userKey);
                            }
                        }
                    }
                }
                adapter = new ChattingRecyclerAdapter(finalizedNumbers, view.getContext(),getActivity());
                chattingUsersRecyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {
                Crashlytics.log(databaseError.getMessage());
            }
        };

        usersRef.addValueEventListener(eventListener);

        return view;
    }

    private Set<String> getContactNumbers()
    {
        Set<String> mobileNumbers = new HashSet<>();

        Cursor cursor = view.getContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,null,null,null);

        if (cursor != null)
        {
            while (cursor.moveToNext())
            {
                String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                mobileNumbers.add(number);
            }
        }

        if (cursor != null)
        {
            cursor.close();
        }

        return mobileNumbers;
    }
}
