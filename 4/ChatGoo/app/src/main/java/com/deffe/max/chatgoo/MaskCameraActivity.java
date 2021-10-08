package com.deffe.max.chatgoo;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.deffe.max.chatgoo.Views.CameraSourceView;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;
import java.util.ArrayList;

public class MaskCameraActivity extends AppCompatActivity
{
    private static final String TAG = MaskCameraActivity.class.getSimpleName();

    private CameraSourceView mPreview;
    private CameraSource mCameraSource = null;

    private boolean mIsFrontFacing = true;

    private ImageView captureBtn;

    private ArrayList<Integer> masks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_mask_camera);

        mPreview = findViewById(R.id.preview);

        captureBtn = findViewById(R.id.capture_btn);

        if (savedInstanceState != null)
        {
            mIsFrontFacing = savedInstanceState.getBoolean("IsFrontFacing");
        }

        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (rc == PackageManager.PERMISSION_GRANTED)
        {
            createCameraSource();
        }

        masks.add(R.drawable.joker1);
        masks.add(R.drawable.monkey1);
        masks.add(R.drawable.monkey2);
        masks.add(R.drawable.monkey3);
        masks.add(R.drawable.cat1);
        masks.add(R.drawable.thief_mask1);
        masks.add(R.drawable.horror_mask1);
        masks.add(R.drawable.queen1);

        ArrayList<Bitmap> thumbs = new ArrayList<>();

        for (Integer drawable : masks)
        {
            thumbs.add(BitmapFactory.decodeResource(getResources(),drawable));
        }

        RecyclerView masksRecyclerView = findViewById(R.id.masks_recycler_view);
        masksRecyclerView.setLayoutManager(new LinearLayoutManager(MaskCameraActivity.this, LinearLayoutManager.HORIZONTAL,false));
        MasksAdapter masksAdapter = new MasksAdapter(MaskCameraActivity.this, thumbs, new MasksAdapter.OnItemClickListener()
        {
            @Override
            public void onItemClick(int position)
            {

            }
        });
        masksRecyclerView.setAdapter(masksAdapter);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume called.");

        startCameraSource();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        mPreview.stop();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (mCameraSource != null)
        {
            mCameraSource.release();
        }
    }

    private void createCameraSource()
    {
        Log.d(TAG, "createCameraSource called.");

        int facing = CameraSource.CAMERA_FACING_FRONT;

        Context context = getApplicationContext();
        FaceDetector detector = createFaceDetector(context);

        if (!mIsFrontFacing)
        {
            facing = CameraSource.CAMERA_FACING_BACK;
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setFacing(facing)
                .setRequestedPreviewSize(320, 240)
                .setRequestedFps(60.0f)
                .setAutoFocusEnabled(true)
                .build();
    }

    private void startCameraSource()
    {
        Log.d(TAG, "startCameraSource called.");

        if (mCameraSource != null)
        {
            try
            {
                mPreview.start(mCameraSource);
            }
            catch (IOException e)
            {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    @NonNull
    private FaceDetector createFaceDetector(final Context context)
    {
        Log.d(TAG, "createFaceDetector called.");

        return new FaceDetector.Builder(context)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(true)
                .setMode(FaceDetector.FAST_MODE)
                .setProminentFaceOnly(mIsFrontFacing)
                .setMinFaceSize(mIsFrontFacing ? 0.35f : 0.15f)
                .build();
    }

    public static class MasksAdapter extends RecyclerView.Adapter<MasksAdapter.MasksViewHolder>
    {
        private Context context;
        private ArrayList<Bitmap> masks;
        private final OnItemClickListener listener;

        public interface OnItemClickListener
        {
            void onItemClick(int position);
        }

        MasksAdapter(Context context, ArrayList<Bitmap> masks, OnItemClickListener listener)
        {
            this.context = context;
            this.masks = masks;
            this.listener = listener;
        }

        @NonNull
        @Override
        public MasksViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
        {
            View view = LayoutInflater.from(context).inflate(R.layout.masks_items,viewGroup,false);
            return new MasksViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MasksViewHolder masksViewHolder, int i)
        {
            Bitmap bitmap = masks.get(i);

            masksViewHolder.bind(bitmap,listener,masksViewHolder);
        }

        @Override
        public int getItemCount()
        {
            return masks.size();
        }

        class MasksViewHolder extends RecyclerView.ViewHolder
        {
            View view;

            ImageView masksView;

            MasksViewHolder(@NonNull View itemView)
            {
                super(itemView);

                view = itemView;

                masksView = view.findViewById(R.id.masks_items);
            }

            void bind(final Bitmap item, final OnItemClickListener listener, final MasksViewHolder masksViewHolder)
            {
                masksView.setImageBitmap(item);
                itemView.setOnClickListener(new View.OnClickListener()
                {
                    @Override public void onClick(View v)
                    {
                        listener.onItemClick(masksViewHolder.getAdapterPosition());
                    }
                });
            }
        }
    }

}
