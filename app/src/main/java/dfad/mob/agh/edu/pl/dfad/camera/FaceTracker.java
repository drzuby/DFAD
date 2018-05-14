package dfad.mob.agh.edu.pl.dfad.camera;

import android.graphics.PointF;
import android.hardware.Camera;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.util.HashMap;
import java.util.Map;

public class FaceTracker extends Tracker<Face> implements Camera.FaceDetectionListener {

    private static final float EYE_CLOSED_THRESHOLD = 0.4f;

    // Record the previously seen proportions of the landmark locations relative to the bounding box
    // of the face.  These proportions can be used to approximate where the landmarks are within the
    // face bounding box if the eye landmark is missing in a future update.
    private Map<Integer, PointF> mPreviousProportions = new HashMap<>();

    private boolean mPreviousIsLeftOpen = true;
    private boolean mPreviousIsRightOpen = true;

    private final Listener listener;

    public FaceTracker(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera) {
    }

    /**
     * @param id
     * @param item
     *
     * This method is called to initially assert a new item when it is detected.
     */
    @Override
    public void onNewItem(int id, Face item) {
        listener.onFaceDetected();
    }

    /**
     * Called to indicate that the item associated with the ID previously reported via onNewItem(int, Object)
     * has been assumed to be gone forever.
     */
    @Override
    public void onDone() {
        listener.onFaceTimedOut();
    }

    @Override
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {

        PointF leftPosition = getLandmarkPosition(face, Landmark.LEFT_EYE);
        PointF rightPosition = getLandmarkPosition(face, Landmark.RIGHT_EYE);

        float leftOpenScore = face.getIsLeftEyeOpenProbability();
        boolean isLeftOpen;
        if (leftOpenScore == Face.UNCOMPUTED_PROBABILITY) {
            isLeftOpen = mPreviousIsLeftOpen;
        } else {
            isLeftOpen = (leftOpenScore > EYE_CLOSED_THRESHOLD);
            mPreviousIsLeftOpen = isLeftOpen;
        }

        float rightOpenScore = face.getIsRightEyeOpenProbability();
        boolean isRightOpen;
        if (rightOpenScore == Face.UNCOMPUTED_PROBABILITY) {
            isRightOpen = mPreviousIsRightOpen;
        } else {
            isRightOpen = (rightOpenScore > EYE_CLOSED_THRESHOLD);
            mPreviousIsRightOpen = isRightOpen;
        }

        listener.onLeftEyeChanged(isLeftOpen);
        listener.onRightEyeChanged(isRightOpen);
    }

    @Override
    public void onMissing(FaceDetector.Detections<Face> detectionResults) {

    }

    /**
     * Finds a specific landmark position, or approximates the position based on past observations
     * if it is not present.
     */
    private PointF getLandmarkPosition(Face face, int landmarkId) {
        for (Landmark landmark : face.getLandmarks()) {
            if (landmark.getType() == landmarkId) {
                return landmark.getPosition();
            }
        }

        PointF prop = mPreviousProportions.get(landmarkId);
        if (prop == null) {
            return null;
        }

        float x = face.getPosition().x + (prop.x * face.getWidth());
        float y = face.getPosition().y + (prop.y * face.getHeight());
        return new PointF(x, y);
    }
}
