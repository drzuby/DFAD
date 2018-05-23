package dfad.mob.agh.edu.pl.dfad;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import com.bosphere.filelogger.FL;
import com.bosphere.filelogger.FLConfig;
import com.bosphere.filelogger.FLConst;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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
public class MainActivity extends Activity implements FrontCameraRetriever.Listener, FaceDetectionCamera.Listener, SensorEventListener {

    private static final String TAG = "FDT" + MainActivity.class.getSimpleName();

    private TextView helloWorldTextView;
    private SensorManager sensorManager;

    private TextView leftEyeTextView;
    private TextView rightEyeTextView;

    private TextView xAccTextView;
    private TextView yAccTextView;
    private TextView zAccTextView;

    private TextView xGyroTextView;
    private TextView yGyroTextView;
    private TextView zGyroTextView;

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        helloWorldTextView = findViewById(R.id.helloWorldTextView);
        xAccTextView = findViewById(R.id.xAcc);
        yAccTextView = findViewById(R.id.yAcc);
        zAccTextView = findViewById(R.id.zAcc);
        xGyroTextView = findViewById(R.id.xPos);
        yGyroTextView = findViewById(R.id.yPos);
        zGyroTextView = findViewById(R.id.zPos);
        leftEyeTextView = findViewById(R.id.leftEye);
        rightEyeTextView = findViewById(R.id.rightEye);

        FL.init(new FLConfig.Builder(this)
                .minLevel(FLConst.Level.V)
                .logToFile(true)
                .dir(new File(Environment.getExternalStorageDirectory(), "DFAD"))
                .retentionPolicy(FLConst.RetentionPolicy.FILE_COUNT)
                .build());
        FL.setEnabled(true);

        // Go get the front facing camera of the device
        // best practice is to do this asynchronously
        FrontCameraRetriever.retrieveFor(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
            xAccTextView.setText(String.format(Locale.ENGLISH, "x: %f", event.values[0]));
            yAccTextView.setText(String.format(Locale.ENGLISH, "y: %f", event.values[1]));
            zAccTextView.setText(String.format(Locale.ENGLISH, "z: %f", event.values[2]));
            FL.d("acc\t%f\t%f\t%f", event.values[0], event.values[1], event.values[2]);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }
        updateOrientationAngles();
        FL.d("pos\t%f\t%f\t%f", mOrientationAngles[0], mOrientationAngles[1], mOrientationAngles[2]);

    }

    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        // "mOrientationAngles" now has up-to-date information.
        xGyroTextView.setText(String.format(Locale.ENGLISH, "x: %f", mOrientationAngles[0]));
        yGyroTextView.setText(String.format(Locale.ENGLISH, "y: %f", mOrientationAngles[1]));
        zGyroTextView.setText(String.format(Locale.ENGLISH, "z: %f", mOrientationAngles[2]));
    }

    @Override
    public void onLoaded(FaceDetectionCamera camera) {
        // When the front facing camera has been retrieved
        // then initialise it i.e turn face detection on

        try {
            camera.initialise(this, getApplicationContext());
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helloWorldTextView.setText(R.string.face_detected_message);
            }
        });
    }

    @Override
    public void onFaceTimedOut() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                helloWorldTextView.setText(R.string.face_detected_then_lost_message);
                leftEyeTextView.setText(R.string.default_eye_label);
                rightEyeTextView.setText(R.string.default_eye_label);
            }
        });
    }

    @Override
    public void onFaceDetectionNonRecoverableError() {
        // This can happen if
        // Face detection not supported on this device
        // Something went wrong in the Android api
        // or our app or another app failed to release the camera properly
        helloWorldTextView.setText(R.string.error_with_face_detection);
    }

    @Override
    public void onLeftEyeChanged(final boolean isOpen) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isOpen) {
                    leftEyeTextView.setText(R.string.left_eye_open);
                } else {
                    leftEyeTextView.setText(R.string.left_eye_closed);
                }
            }
        });
    }

    @Override
    public void onRightEyeChanged(final boolean isOpen) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isOpen) {
                    rightEyeTextView.setText(R.string.right_eye_open);
                } else {
                    rightEyeTextView.setText(R.string.right_eye_closed);
                }
            }
        });
    }

    //https://stackoverflow.com/questions/37458157/error-failed-to-connect-to-camera-service-android-marshmallow
    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissions = new ArrayList<>();
            int hasCameraPermission = checkSelfPermission(Manifest.permission.CAMERA);
            int hasWriteExternalStoragePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int hasReadExternalStoragePermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

            if (hasCameraPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.CAMERA);
            }
            if (hasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (hasReadExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }

            if (!permissions.isEmpty()) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), 111);
            }
        }
    }
}