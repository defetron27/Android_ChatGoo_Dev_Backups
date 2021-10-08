package com.deffe.max.chatgoo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity
{
    static
    {
        System.loadLibrary("native-lib");
    }

    private DatabaseReference userRef;

    private String onlineUserId;

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userRef = FirebaseDatabase.getInstance().getReference().child("Users");

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null)
        {
            onlineUserId = firebaseUser.getUid();
        }

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        getSupportFragmentManager().beginTransaction().addToBackStack(null).replace(R.id.main_fragment,new AssistantFragment()).commit();


        bottomNavigationView = findViewById(R.id.main_bottom_navigation_view);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item)
            {
                Fragment fragment = null;

                switch (item.getItemId())
                {
                    case R.id.chat_with_bot:
                        fragment = new AssistantFragment();
                        break;
                    case R.id.chat_with_friends:
                        fragment = new ChatFragment();
                        break;
                }

                getSupportFragmentManager().beginTransaction().addToBackStack(null).replace(R.id.main_fragment,fragment).commit();

                return true;
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        if (onlineUserId == null)
        {
            Intent mainIntent = new Intent(MainActivity.this,LoginActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);
            finish();
        }
        else
        {
            userRef.child(onlineUserId).child("status").setValue("active").addOnFailureListener(new OnFailureListener()
            {
                @Override
                public void onFailure(@NonNull Exception e)
                {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onBackPressed()
    {
        if (bottomNavigationView.getSelectedItemId() == R.id.chat_with_bot)
        {
            super.onBackPressed();
        }
        else
        {
            bottomNavigationView.setSelectedItemId(R.id.chat_with_bot);
        }
    }
}