package dfad.mob.agh.edu.pl.dfad;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.bosphere.filelogger.FL;
import com.bosphere.filelogger.FLConfig;
import com.bosphere.filelogger.FLConst;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dfad.mob.agh.edu.pl.dfad.camera.FaceDetectionCamera;
import dfad.mob.agh.edu.pl.dfad.camera.FrontCameraRetriever;
import dfad.mob.agh.edu.pl.dfad.gsm.CallsBroadcastReceiver;
import dfad.mob.agh.edu.pl.dfad.gsm.SmsMmsBroadcastReceiver;
import dfad.mob.agh.edu.pl.dfad.notification.SoundNotificationService;
import dfad.mob.agh.edu.pl.dfad.visualization.ActionVisualizer;

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

    private SensorManager sensorManager;
    private SmsMmsBroadcastReceiver smsMmsBroadcastReceiver;
    private CallsBroadcastReceiver callsBroadcastReceiver;

    private TextView leftEyeTextView;
    private TextView rightEyeTextView;

    private TextView xAccTextView;
    private TextView yAccTextView;
    private TextView zAccTextView;

    private TextView xGraTextView;
    private TextView yGraTextView;
    private TextView zGraTextView;

    private TextView xMagTextView;
    private TextView yMagTextView;
    private TextView zMagTextView;

    private TextView xPosTextView;
    private TextView yPosTextView;
    private TextView zPosTextView;

    private TextView xResTextView;
    private TextView yResTextView;
    private TextView zResTextView;

    private CheckBox keepScreenOnCheckBox;
    private TextView cameraTextView;
    private Button barkButton;
    private Button visualizerButton;

    private TextView smsMmsTextView;
    private TextView incomingCallsTextView;
    private TextView outgoingCallsTextView;
    private TextView missedCallsTextView;

    private int smsMmsAmount;
    private int incomingCallsAmount;
    private int outgoingCallsAmount;
    private int missedCallsAmount;

    private final float[] accelerometerReading = new float[4];
    private final float[] magnetometerReading = new float[3];
    private final float[] gravityReading = new float[3];

    private final float[] rotationMatrix = new float[16];
    private final float[] invertedRotationMatrix = new float[16];
    private final float[] orientationAngles = new float[3];
    private final float[] trueAcceleration = new float[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        assignWidgets();

        FL.init(new FLConfig.Builder(this)
                .minLevel(FLConst.Level.V)
                .logToFile(true)
                .dir(new File(Environment.getExternalStorageDirectory(), "DFAD"))
                .retentionPolicy(FLConst.RetentionPolicy.FILE_COUNT)
                .build());
        FL.setEnabled(true);

        keepScreenOnCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
        barkButton.setOnClickListener(v -> runSoundNotification());

        visualizerButton.setOnClickListener(v -> showVisualizer());

        registerSmsMmsBroadcastReceiver();

        registerCallsBroadcastReceiver();

        // Go get the front facing camera of the device
        // best practice is to do this asynchronously
        FrontCameraRetriever.retrieveFor(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void registerSmsMmsBroadcastReceiver() {
        smsMmsBroadcastReceiver = new SmsMmsBroadcastReceiver();
        IntentFilter smsMmsIntentFilter = new IntentFilter();
        smsMmsIntentFilter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        smsMmsIntentFilter.addAction(Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION);
        registerReceiver(smsMmsBroadcastReceiver, smsMmsIntentFilter);
        smsMmsBroadcastReceiver.setSmsListener((sender, body) -> smsMmsTextView.setText(String.valueOf(++smsMmsAmount)));
        smsMmsBroadcastReceiver.setMmsListener(sender -> smsMmsTextView.setText(String.valueOf(++smsMmsAmount)));
    }

    private void registerCallsBroadcastReceiver() {
        callsBroadcastReceiver = new CallsBroadcastReceiver();
        IntentFilter callsIntentFilter = new IntentFilter();
        callsIntentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        callsIntentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        registerReceiver(callsBroadcastReceiver, callsIntentFilter);
        callsBroadcastReceiver.setCallsListener(new CallsBroadcastReceiver.CallsListener() {
            @Override
            public void onIncomingCallStarted() {
                incomingCallsTextView.setText(String.valueOf(++incomingCallsAmount));
                incomingCallsTextView.setTextColor(Color.RED);
            }

            @Override
            public void onOutgoingCallStarted() {
                outgoingCallsTextView.setText(String.valueOf(++outgoingCallsAmount));
                outgoingCallsTextView.setTextColor(Color.RED);
            }

            @Override
            public void onIncomingCallEnded() {
                incomingCallsTextView.setTextColor(Color.BLACK);
            }

            @Override
            public void onOutgoingCallEnded() {
                outgoingCallsTextView.setTextColor(Color.BLACK);
            }

            @Override
            public void onMissedCall() {
                missedCallsTextView.setText(String.valueOf(++missedCallsAmount));
            }
        });
    }

    private void showVisualizer() {
        Intent visualizerIntent = new Intent(this, ActionVisualizer.class);
        startActivity(visualizerIntent);
    }

    private void assignWidgets() {
        leftEyeTextView = findViewById(R.id.leftEye);
        rightEyeTextView = findViewById(R.id.rightEye);

        xAccTextView = findViewById(R.id.xAcc);
        yAccTextView = findViewById(R.id.yAcc);
        zAccTextView = findViewById(R.id.zAcc);

        xGraTextView = findViewById(R.id.xGra);
        yGraTextView = findViewById(R.id.yGra);
        zGraTextView = findViewById(R.id.zGra);

        xMagTextView = findViewById(R.id.xMag);
        yMagTextView = findViewById(R.id.yMag);
        zMagTextView = findViewById(R.id.zMag);

        xPosTextView = findViewById(R.id.xPos);
        yPosTextView = findViewById(R.id.yPos);
        zPosTextView = findViewById(R.id.zPos);

        xResTextView = findViewById(R.id.xRes);
        yResTextView = findViewById(R.id.yRes);
        zResTextView = findViewById(R.id.zRes);

        keepScreenOnCheckBox = findViewById(R.id.keepScreenOn);
        cameraTextView = findViewById(R.id.cameraTextView);
        barkButton = findViewById(R.id.barkButton);
        visualizerButton = findViewById(R.id.visualizerButton);

        smsMmsTextView = findViewById(R.id.smsMms);
        incomingCallsTextView = findViewById(R.id.incomingCalls);
        outgoingCallsTextView = findViewById(R.id.outgoingCalls);
        missedCallsTextView = findViewById(R.id.missedCalls);
    }

    private void runSoundNotification() {
        Intent soundNotificationIntent = new Intent(this, SoundNotificationService.class);
        soundNotificationIntent.setAction(SoundNotificationService.ACTION_PLAY);
        startService(soundNotificationIntent);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, 3);
            xAccTextView.setText(String.format(Locale.ENGLISH, "x: %f", event.values[0]));
            yAccTextView.setText(String.format(Locale.ENGLISH, "y: %f", event.values[1]));
            zAccTextView.setText(String.format(Locale.ENGLISH, "z: %f", event.values[2]));
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
            xMagTextView.setText(String.format(Locale.ENGLISH, "x: %f", event.values[0]));
            yMagTextView.setText(String.format(Locale.ENGLISH, "y: %f", event.values[1]));
            zMagTextView.setText(String.format(Locale.ENGLISH, "z: %f", event.values[2]));
        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            System.arraycopy(event.values, 0, gravityReading,
                    0, gravityReading.length);
            xGraTextView.setText(String.format(Locale.ENGLISH, "x: %f", event.values[0]));
            yGraTextView.setText(String.format(Locale.ENGLISH, "y: %f", event.values[1]));
            zGraTextView.setText(String.format(Locale.ENGLISH, "z: %f", event.values[2]));
        }
        updateTrueAcceleration();
    }

    public void updateTrueAcceleration() {
        SensorManager.getRotationMatrix(rotationMatrix, null, gravityReading, magnetometerReading);
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
        Matrix.invertM(invertedRotationMatrix, 0, rotationMatrix, 0);
        Matrix.multiplyMV(trueAcceleration, 0, invertedRotationMatrix, 0, accelerometerReading, 0);

        xPosTextView.setText(String.format(Locale.ENGLISH, "x: %f", orientationAngles[0]));
        yPosTextView.setText(String.format(Locale.ENGLISH, "y: %f", orientationAngles[1]));
        zPosTextView.setText(String.format(Locale.ENGLISH, "z: %f", orientationAngles[2]));

        xResTextView.setText(String.format(Locale.ENGLISH, "x: %f", trueAcceleration[0]));
        yResTextView.setText(String.format(Locale.ENGLISH, "y: %f", trueAcceleration[1]));
        zResTextView.setText(String.format(Locale.ENGLISH, "z: %f", trueAcceleration[2]));

        FL.d("%f\t%f\t%f", trueAcceleration[0], trueAcceleration[1], trueAcceleration[2]);
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
        cameraTextView.setText(R.string.error_with_face_detection);
    }

    @Override
    public void onFaceDetected() {
        runOnUiThread(() -> cameraTextView.setText(R.string.face_detected_message));
    }

    @Override
    public void onFaceTimedOut() {
        runOnUiThread(() -> {
            cameraTextView.setText(R.string.face_lost_message);
            leftEyeTextView.setText(R.string.default_eye_label);
            rightEyeTextView.setText(R.string.default_eye_label);
        });
    }

    @Override
    public void onFaceDetectionNonRecoverableError() {
        // This can happen if
        // Face detection not supported on this device
        // Something went wrong in the Android api
        // or our app or another app failed to release the camera properly
        cameraTextView.setText(R.string.error_with_face_detection);
    }

    @Override
    public void onLeftEyeChanged(final boolean isOpen) {
        runOnUiThread(() -> {
            if (isOpen) {
                leftEyeTextView.setText(R.string.left_eye_open);
            } else {
                leftEyeTextView.setText(R.string.left_eye_closed);
            }
        });
    }

    @Override
    public void onRightEyeChanged(final boolean isOpen) {
        runOnUiThread(() -> {
            if (isOpen) {
                rightEyeTextView.setText(R.string.right_eye_open);
            } else {
                rightEyeTextView.setText(R.string.right_eye_closed);
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
            int hasWakeLockPermission = checkSelfPermission(Manifest.permission.WAKE_LOCK);
            int hasReadSmsPermission = checkSelfPermission(Manifest.permission.READ_SMS);
            int hasReceiveSmsPermission = checkSelfPermission(Manifest.permission.RECEIVE_SMS);
            int hasReceiveMmsPermission = checkSelfPermission(Manifest.permission.RECEIVE_MMS);
            int hasReceiveWapPushPermission = checkSelfPermission(Manifest.permission.RECEIVE_WAP_PUSH);
            int hasReadPhoneStatePermission = checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
            int hasProcessOutoingCallsPermission = checkSelfPermission(Manifest.permission.PROCESS_OUTGOING_CALLS);

            if (hasCameraPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.CAMERA);
            }
            if (hasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (hasReadExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (hasWakeLockPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WAKE_LOCK);
            }
            if (hasReadSmsPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_SMS);
            }
            if (hasReceiveSmsPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.RECEIVE_SMS);
            }
            if (hasReceiveMmsPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.RECEIVE_MMS);
            }
            if (hasReceiveWapPushPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.RECEIVE_WAP_PUSH);
            }
            if (hasReadPhoneStatePermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_PHONE_STATE);
            }
            if (hasProcessOutoingCallsPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.PROCESS_OUTGOING_CALLS);
            }

            if (!permissions.isEmpty()) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), 111);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        registerSmsMmsBroadcastReceiver();
        registerCallsBroadcastReceiver();
    }

    @Override
    public void onStop() {
        try {
            unregisterReceiver(smsMmsBroadcastReceiver);
            unregisterReceiver(callsBroadcastReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        super.onStop();
    }
}