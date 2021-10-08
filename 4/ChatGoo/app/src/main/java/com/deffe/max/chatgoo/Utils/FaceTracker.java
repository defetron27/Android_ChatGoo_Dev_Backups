package com.deffe.max.chatgoo.Utils;

import android.content.Context;
import android.graphics.PointF;

import com.deffe.max.chatgoo.Views.GraphicOverlayView;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.util.HashMap;
import java.util.Map;

public class FaceTracker extends Tracker<Face>
{
    private static final String TAG = "FaceTracker";

    private GraphicOverlayView mOverlay;
    private FaceGraphic mFaceGraphic;
    private Context mContext;
    private boolean mIsFrontFacing;
    private FaceData mFaceData;

    private Map<Integer, PointF> mPreviousLandmarkPositions = new HashMap<>();

    private int position;

    private boolean mPreviousIsLeftEyeOpen = true;
    private boolean mPreviousIsRightEyeOpen = true;


    FaceTracker(int position, GraphicOverlayView overlay, Context context, boolean isFrontFacing)
    {
        this.position = position;
        mOverlay = overlay;
        mContext = context;
        mIsFrontFacing = isFrontFacing;
        mFaceData = new FaceData();
    }

    @Override
    public void onNewItem(int id, Face face)
    {
        mFaceGraphic = new FaceGraphic(position,mOverlay, mContext, mIsFrontFacing);
    }

    @Override
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
        mOverlay.add(mFaceGraphic);
        updatePreviousLandmarkPositions(face);

        // Get face dimensions.
        mFaceData.setPosition(face.getPosition());
        mFaceData.setWidth(face.getWidth());
        mFaceData.setHeight(face.getHeight());

        // Get head angles.
        mFaceData.setEulerY(face.getEulerY());
        mFaceData.setEulerZ(face.getEulerZ());

        // Get the positions of facial landmarks.
        mFaceData.setLeftEyePosition(getLandmarkPosition(face, Landmark.LEFT_EYE));
        mFaceData.setRightEyePosition(getLandmarkPosition(face, Landmark.RIGHT_EYE));
        mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.LEFT_CHEEK));
        mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.RIGHT_CHEEK));
        mFaceData.setNoseBasePosition(getLandmarkPosition(face, Landmark.NOSE_BASE));
        mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.LEFT_EAR));
        mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.LEFT_EAR_TIP));
        mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.RIGHT_EAR));
        mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.RIGHT_EAR_TIP));
        mFaceData.setMouthLeftPosition(getLandmarkPosition(face, Landmark.LEFT_MOUTH));
        mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.BOTTOM_MOUTH));
        mFaceData.setMouthRightPosition(getLandmarkPosition(face, Landmark.RIGHT_MOUTH));

        // Determine if eyes are open.
        final float EYE_CLOSED_THRESHOLD = 0.4f;
        float leftOpenScore = face.getIsLeftEyeOpenProbability();
        if (leftOpenScore == Face.UNCOMPUTED_PROBABILITY) {
            mFaceData.setLeftEyeOpen(mPreviousIsLeftEyeOpen);
        } else {
            mFaceData.setLeftEyeOpen(leftOpenScore > EYE_CLOSED_THRESHOLD);
            mPreviousIsLeftEyeOpen = mFaceData.isLeftEyeOpen();
        }
        float rightOpenScore = face.getIsRightEyeOpenProbability();
        if (rightOpenScore == Face.UNCOMPUTED_PROBABILITY) {
            mFaceData.setRightEyeOpen(mPreviousIsRightEyeOpen);
        } else {
            mFaceData.setRightEyeOpen(rightOpenScore > EYE_CLOSED_THRESHOLD);
            mPreviousIsRightEyeOpen = mFaceData.isRightEyeOpen();
        }

        // See if there's a smile!
        // Determine if person is smiling.
        final float SMILING_THRESHOLD = 0.8f;
        mFaceData.setSmiling(face.getIsSmilingProbability() > SMILING_THRESHOLD);

        mFaceGraphic.update(mFaceData);
    }

    @Override
    public void onMissing(FaceDetector.Detections<Face> detectionResults) {
        mOverlay.remove(mFaceGraphic);
    }

    @Override
    public void onDone() {
        mOverlay.remove(mFaceGraphic);
    }

    private PointF getLandmarkPosition(Face face, int landmarkId) {
        for (Landmark landmark : face.getLandmarks()) {
            if (landmark.getType() == landmarkId) {
                return landmark.getPosition();
            }
        }

        PointF landmarkPosition = mPreviousLandmarkPositions.get(landmarkId);
        if (landmarkPosition == null) {
            return null;
        }

        float x = face.getPosition().x + (landmarkPosition.x * face.getWidth());
        float y = face.getPosition().y + (landmarkPosition.y * face.getHeight());
        return new PointF(x, y);
    }

    private void updatePreviousLandmarkPositions(Face face) {
        for (Landmark landmark : face.getLandmarks()) {
            PointF position = landmark.getPosition();
            float xProp = (position.x - face.getPosition().x) / face.getWidth();
            float yProp = (position.y - face.getPosition().y) / face.getHeight();
            mPreviousLandmarkPositions.put(landmark.getType(), new PointF(xProp, yProp));
        }
    }
}
