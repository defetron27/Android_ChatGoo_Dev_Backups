package com.deffe.max.chatgoo.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

import com.deffe.max.chatgoo.R;
import com.deffe.max.chatgoo.Views.GraphicOverlayView;

public class FaceGraphic extends GraphicOverlayView.Graphic
{
    private static final String TAG = "FaceGraphic";

    private boolean mIsFrontFacing;

    private volatile FaceData mFaceData;

    private Drawable mMonkeyFace;
    private Drawable mJokerFace;

    private Context context;

    private int maskPosition;

    FaceGraphic(int maskPosition,GraphicOverlayView overlay, final Context context, boolean isFrontFacing)
    {
        super(overlay);
        mIsFrontFacing = isFrontFacing;
        this.context = context;

        this.maskPosition = maskPosition;

        prepareMask(maskPosition);
    }

    private void prepareMask(int position)
    {
        Resources resources = context.getResources();

        switch (position)
        {
            case 0:
                prepareJokerMask(resources);
                break;
            case 1:
                prepareMonkeyMask(resources);
                break;
            default:
                defaultMask(resources);
                break;
        }
    }

    private void prepareJokerMask(Resources resources)
    {
        mJokerFace = resources.getDrawable(R.drawable.joker1);
    }

    private void prepareMonkeyMask(Resources resources)
    {
        mMonkeyFace = resources.getDrawable(R.drawable.monkey1);
    }

    private void defaultMask(Resources resources)
    {

    }

    void update(FaceData faceData)
    {
        mFaceData = faceData;
        postInvalidate();
    }

    @Override
    public void draw(Canvas canvas)
    {
        FaceData faceData = mFaceData;
        if (faceData == null) {
            return;
        }
        PointF detectPosition = faceData.getPosition();
        PointF detectNoseBasePosition = faceData.getNoseBasePosition();

        if ((detectPosition == null) || (detectNoseBasePosition == null) )
        {
            return;
        }

        PointF position = new PointF(translateX(detectPosition.x), translateY(detectPosition.y));

        float width = scaleX(faceData.getWidth());
        float height = scaleY(faceData.getHeight());

        PointF noseBasePosition = new PointF(translateX(detectNoseBasePosition.x), translateY(detectNoseBasePosition.y));

        if (maskPosition == 0)
        {
            drawJokerFace(canvas, noseBasePosition,width,height);
        }
        else if (maskPosition == 1)
        {
            drawMonkeyFace(canvas, noseBasePosition,width,height);
        }
    }

    private void drawJokerFace(Canvas canvas, PointF noseBasePosition, float faceWidth, float faceHeight)
    {
        int left = (int)(noseBasePosition.x - (faceWidth / 2));
        int right = (int)(noseBasePosition.x + (faceWidth / 2));
        int top = (int)(noseBasePosition.y - (faceHeight / 2));
        int bottom = (int)(noseBasePosition.y + (faceHeight / 2));

        mJokerFace.setBounds(left, top, right, bottom);
        mJokerFace.draw(canvas);
    }

    private void drawMonkeyFace(Canvas canvas, PointF noseBasePosition, float faceWidth, float faceHeight)
    {
        int left = (int)(noseBasePosition.x - (faceWidth / 2));
        int right = (int)(noseBasePosition.x + (faceWidth / 2));
        int top = (int)(noseBasePosition.y - (faceHeight / 2));
        int bottom = (int)(noseBasePosition.y + (faceHeight / 2));

        mMonkeyFace.setBounds(left, top, right, bottom);
        mMonkeyFace.draw(canvas);
    }
}

