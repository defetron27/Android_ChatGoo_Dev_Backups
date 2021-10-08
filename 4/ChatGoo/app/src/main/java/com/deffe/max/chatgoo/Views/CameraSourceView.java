package com.deffe.max.chatgoo.Views;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.CameraSource;

import java.io.IOException;

public class CameraSourceView extends ViewGroup
{
    private final static String TAG = CameraSourceView.class.getSimpleName();

    private Context mContext;
    private SurfaceView mSurfaceView;
    private CameraSource mCameraSource;

    private GraphicOverlayView mOverlay;

    private boolean mStartRequested;
    private boolean mSurfaceAvailable;

    public CameraSourceView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        mContext = context;

        mStartRequested = false;
        mSurfaceAvailable = false;

        mSurfaceView = new SurfaceView(context);
        mSurfaceView.getHolder().addCallback(new SurfaceCallback());
        addView(mSurfaceView);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        final int layoutWidth = right - left;
        final int layoutHeight = bottom - top;

        for (int i = 0; i < getChildCount(); ++i)
        {
            getChildAt(i).layout(0, 0, layoutWidth, layoutHeight);
        }

        try
        {
            startIfReady();
        }
        catch (SecurityException se)
        {
            Log.e(TAG, "Do not have permission to start the camera", se);
        }
        catch (IOException e)
        {
            Log.e(TAG, "Could not start camera source.", e);
        }
    }

    public class SurfaceCallback implements SurfaceHolder.Callback
    {
        @Override
        public void surfaceCreated(SurfaceHolder holder)
        {
            mSurfaceAvailable = true;

            try
            {
                startIfReady();
            }
            catch (IOException e)
            {
                Log.e(TAG, "Could not start camera source.", e);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
        {
            mSurfaceAvailable = false;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }

    public void start(CameraSource cameraSource) throws IOException
    {
        if (cameraSource == null)
        {
            stop();
        }

        mCameraSource = cameraSource;

        if (mCameraSource != null)
        {
            mStartRequested = true;
            startIfReady();
        }
    }

    public void start(CameraSource cameraSource, GraphicOverlayView overlay) throws IOException
    {
        mOverlay = overlay;
        start(cameraSource);
    }

    public void stop()
    {
        if (mCameraSource != null)
        {
            mCameraSource.stop();
        }
    }

    private void startIfReady() throws IOException
    {
        if (mStartRequested && mSurfaceAvailable)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                {
                    mCameraSource.start(mSurfaceView.getHolder());
                }
            }
            else
            {
                mCameraSource.start(mSurfaceView.getHolder());

                if (mOverlay != null)
                {
                    Size size = mCameraSource.getPreviewSize();
                    int min = Math.min(size.getWidth(), size.getHeight());
                    int max = Math.max(size.getWidth(), size.getHeight());
                    if (isPortraitMode())
                    {
                        mOverlay.setCameraInfo(min, max, mCameraSource.getCameraFacing());
                    }
                    else
                    {
                        mOverlay.setCameraInfo(max, min, mCameraSource.getCameraFacing());
                    }
                    mOverlay.clear();
                }
            }
            mStartRequested = false;
        }
    }

    private boolean isPortraitMode()
    {
        int orientation = mContext.getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            return false;
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            return true;
        }

        Log.d(TAG, "isPortraitMode returning false by default");
        return false;
    }
}
