package dfad.mob.agh.edu.pl.dfad;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import dfad.mob.agh.edu.pl.dfad.camera.FaceDetectionCamera;
import dfad.mob.agh.edu.pl.dfad.camera.FrontCameraRetriever;

/**
 * Don't forget to add the permissions to the AndroidManifest.xml!
 * <p/>
 * <uses-feature android:name="android.hardware.camera" />
 * <uses-feature android:name="android.hardware.camera.front" />
 * <p/>
 * <uses-permission android:name="android.permission.CAMERA" />
 */
public class MainActivity extends Activity implements FrontCameraRetriever.Listener, FaceDetectionCamera.Listener {

    private static final String TAG = "FDT" + MainActivity.class.getSimpleName();

    private TextView helloWorldTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        helloWorldTextView = (TextView) findViewById(R.id.helloWorldTextView);
        // Go get the front facing camera of the device
        // best practice is to do this asynchronously
        FrontCameraRetriever.retrieveFor(this);
    }

    @Override
    public void onLoaded(FaceDetectionCamera camera) {
        // When the front facing camera has been retrieved
        // then initialise it i.e turn face detection on
        camera.initialise(this);
        // If you wanted to show a preview of what the camera can see
        // here is where you would do it
    }

    @Override
    public void onFailedToLoadFaceDetectionCamera() {
        // This can happen if
        // there is no front facing camera
        // or another app is using the camera
        // or our app or another app failed to release the camera properly
        Log.wtf(TAG, "Failed to load camera, what went wrong?");
        helloWorldTextView.setText(R.string.error_with_face_detection);
    }

    @Override
    public void onFaceDetected() {
        helloWorldTextView.setText(R.string.face_detected_message);
    }

    @Override
    public void onFaceTimedOut() {
        helloWorldTextView.setText(R.string.face_detected_then_lost_message);
    }

    @Override
    public void onFaceDetectionNonRecoverableError() {
        // This can happen if
        // Face detection not supported on this device
        // Something went wrong in the Android api
        // or our app or another app failed to release the camera properly
        helloWorldTextView.setText(R.string.error_with_face_detection);
    }

    //https://stackoverflow.com/questions/37458157/error-failed-to-connect-to-camera-service-android-marshmallow
    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            int hasCameraPermission = checkSelfPermission(Manifest.permission.CAMERA);

            List<String> permissions = new ArrayList<>();

            if (hasCameraPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.CAMERA);

            }
            if (!permissions.isEmpty()) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), 111);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 111: {
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        System.out.println("Permissions --> " + "Permission Granted: " + permissions[i]);


                    } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        System.out.println("Permissions --> " + "Permission Denied: " + permissions[i]);

                    }
                }
            }
            break;
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }
}