package com.deffe.max.chatgoo.Views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.gms.vision.CameraSource;

import java.util.HashSet;
import java.util.Set;

public class GraphicOverlayView extends View
{
    private final Object mLock = new Object();
    private int mPreviewWidth;
    protected float mWidthScaleFactor = 1.0f;
    private int mPreviewHeight;
    protected float mHeightScaleFactor = 1.0f;
    private int mFacing = CameraSource.CAMERA_FACING_BACK;
    private Set<Graphic> mGraphics = new HashSet<>();

    public static abstract class Graphic
    {

        private GraphicOverlayView mOverlay;

        protected Graphic(GraphicOverlayView overlay) {
            mOverlay = overlay;
        }

        protected abstract void draw(Canvas canvas);

        public float scaleX(float horizontal) {
            return horizontal * mOverlay.mWidthScaleFactor;
        }

        public float scaleY(float vertical) {
            return vertical * mOverlay.mHeightScaleFactor;
        }

        protected float translateX(float x)
        {
            if (mOverlay.mFacing == CameraSource.CAMERA_FACING_FRONT)
            {
                return mOverlay.getWidth() - scaleX(x);
            }
            else
            {
                return scaleX(x);
            }
        }

        protected float translateY(float y) {
            return scaleY(y);
        }

        protected void postInvalidate() {
            mOverlay.postInvalidate();
        }

    }

    public GraphicOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void clear() {
        synchronized (mLock) {
            mGraphics.clear();
        }
        postInvalidate();
    }

    public void add(Graphic graphic) {
        synchronized (mLock) {
            mGraphics.add(graphic);
        }
        postInvalidate();
    }

    public void remove(Graphic graphic) {
        synchronized (mLock) {
            mGraphics.remove(graphic);
        }
        postInvalidate();
    }

    public void setCameraInfo(int previewWidth, int previewHeight, int facing) {
        synchronized (mLock) {
            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;
            mFacing = facing;
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (mLock) {
            if ((mPreviewWidth != 0) && (mPreviewHeight != 0)) {
                mWidthScaleFactor = (float) canvas.getWidth() / (float) mPreviewWidth;
                mHeightScaleFactor = (float) canvas.getHeight() / (float) mPreviewHeight;
            }

            for (Graphic graphic : mGraphics) {
                graphic.draw(canvas);
            }
        }
    }

}
