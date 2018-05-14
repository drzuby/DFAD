package dfad.mob.agh.edu.pl.dfad.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.view.SurfaceHolder;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.IOException;

/**
 * Manages the android camera and sets it up for face detection
 * can throw an error if face detection is not supported on this device
 */
public class FaceDetectionCamera implements Listener {

    private final Camera camera;

    private Listener listener;
    private Context context;

    public FaceDetectionCamera(Camera camera) {
        this.camera = camera;
    }

    /**
     * Use this to detect faces without an on screen preview
     *
     * @param listener the {@link Listener} for when faces are detected
     */
    public void initialise(Listener listener, Context context) throws IOException {
        initialise(new DummySurfaceHolder(), listener, context);
    }

    private void initialise(SurfaceHolder holder, Listener listener, Context context) throws IOException {
        this.listener = listener;
        FaceDetector faceDetector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .setProminentFaceOnly(true)
                .build();

        faceDetector.setProcessor(
                new LargestFaceFocusingProcessor(
                        faceDetector,
                        new FaceTracker(this)));

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        else {
            CameraSource cameraSource = new CameraSource.Builder(context, faceDetector)
                    .setFacing(CameraSource.CAMERA_FACING_FRONT)
                    .setRequestedPreviewSize(320, 240)
                    .build()
                    .start();
        }

//        try {
//            camera.stopPreview();
//        } catch (Exception swallow) {
//            // ignore: tried to stop a non-existent preview
//        }
//        try {
//            camera.setPreviewDisplay(holder);
//            camera.startPreview();
//            camera.setFaceDetectionListener(new OneShotFaceDetectionListener(this));
//            camera.startFaceDetection();
//        } catch (IOException e) {
//            this.listener.onFaceDetectionNonRecoverableError();
//        }
    }

    @Override
    public void onFaceDetected() {
        listener.onFaceDetected();
    }

    @Override
    public void onFaceTimedOut() {
        listener.onFaceTimedOut();
    }

    @Override
    public void onLeftEyeChanged(boolean isOpen) {
        listener.onLeftEyeChanged(isOpen);
    }

    @Override
    public void onRightEyeChanged(boolean isOpen) {
        listener.onRightEyeChanged(isOpen);
    }

    public void recycle() {
        if (camera != null) {
            camera.release();
        }
    }

    public interface Listener {
        void onFaceDetected();

        void onFaceTimedOut();

        void onFaceDetectionNonRecoverableError();

        void onLeftEyeChanged(boolean isOpen);

        void onRightEyeChanged(boolean isOpen);
    }
}