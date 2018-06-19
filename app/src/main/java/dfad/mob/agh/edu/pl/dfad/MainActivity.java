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
import android.provider.Telephony;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import dfad.mob.agh.edu.pl.dfad.camera.FaceDetectionCamera;
import dfad.mob.agh.edu.pl.dfad.camera.FrontCameraRetriever;
import dfad.mob.agh.edu.pl.dfad.detector.DriverPatternDetectorService;
import dfad.mob.agh.edu.pl.dfad.detector.DrivingMeasurement;
import dfad.mob.agh.edu.pl.dfad.detector.DrivingWindow;
import dfad.mob.agh.edu.pl.dfad.detector.ManeuverMeasurement;
import dfad.mob.agh.edu.pl.dfad.detector.ManeuverType;
import dfad.mob.agh.edu.pl.dfad.gsm.CallsBroadcastReceiver;
import dfad.mob.agh.edu.pl.dfad.gsm.SmsMmsBroadcastReceiver;
import dfad.mob.agh.edu.pl.dfad.model.Measurement;
import dfad.mob.agh.edu.pl.dfad.notification.SoundNotificationService;

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
    private DrivingWindow drivingWindow = new DrivingWindow();
    private DriverPatternDetectorService driverPatternDetectorService;

    private LineChart chart;
    private TextView xResTextView;
    private TextView yResTextView;
    private TextView zResTextView;
    private TextView smsMmsTextView;
    private TextView incomingCallsTextView;
    private TextView outgoingCallsTextView;
    private TextView missedCallsTextView;
    private TextView leftEyeTextView;
    private TextView rightEyeTextView;
    private TextView cameraTextView;
    private CheckBox keepScreenOnCheckBox;

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

    private static final int WINDOW_SIZE = 500;
    private static final String X_AXIS_NAME = "X";
    private static final String Y_AXIS_NAME = "Y";
    private static final String Z_AXIS_NAME = "Z";
    private static final boolean CHART_DRAW_FILLED = true;
    private static final boolean CHART_DRAW_CIRCLES = false;
    private List<Entry> entriesX = new ArrayList<>();
    private List<Entry> entriesY = new ArrayList<>();
    private List<Entry> entriesZ = new ArrayList<>();
    private LineDataSet dataSetX;
    private LineDataSet dataSetY;
    private LineDataSet dataSetZ;
    private LineData lineData;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        assignWidgets();
        initializeChart();
        keepScreenOnCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });

        registerSmsMmsBroadcastReceiver();
        registerCallsBroadcastReceiver();

        try {
            initializeDetector();
        } catch (IOException ignored) {
        }

        FrontCameraRetriever.retrieveFor(this);
        registerSensorService();
    }

    private void initializeChart() {
        loadInitEntries();
        setChartDataSetParameters();
        loadChart();
    }

    public void addEntry(Measurement m) {
        if (dataSetX.getValues().size() > WINDOW_SIZE) {
            dataSetX.removeFirst();
            dataSetY.removeFirst();
            dataSetZ.removeFirst();
        }

        dataSetX.addEntry(new Entry(m.getTime(), (float) m.getXAcc()));
        dataSetY.addEntry(new Entry(m.getTime(), (float) m.getYAcc()));
        dataSetZ.addEntry(new Entry(m.getTime(), (float) m.getZAcc()));

        lineData.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void loadInitEntries() {
        Measurement m = new Measurement(0, 0, 0);
        entriesX.add(new Entry(m.getTime(), 0));
        entriesY.add(new Entry(m.getTime(), 0));
        entriesZ.add(new Entry(m.getTime(), 0));
    }

    private void setChartDataSetParameters() {
        dataSetX = new LineDataSet(entriesX, X_AXIS_NAME);
        dataSetX.setDrawFilled(CHART_DRAW_FILLED);
        dataSetX.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
        dataSetX.setDrawCircles(CHART_DRAW_CIRCLES);

        dataSetY = new LineDataSet(entriesY, Y_AXIS_NAME);
        dataSetY.setDrawFilled(CHART_DRAW_FILLED);
        dataSetY.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
        dataSetY.setDrawCircles(CHART_DRAW_CIRCLES);

        dataSetZ = new LineDataSet(entriesZ, Z_AXIS_NAME);
        dataSetZ.setDrawFilled(CHART_DRAW_FILLED);
        dataSetZ.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorGreen));
        dataSetZ.setDrawCircles(CHART_DRAW_CIRCLES);
    }

    private void loadChart() {
        lineData = new LineData(dataSetX, dataSetY, dataSetZ);
        chart.setData(lineData);
        chart.invalidate();
    }

    private void registerSmsMmsBroadcastReceiver() {
        try {
            unregisterReceiver(smsMmsBroadcastReceiver);
        } catch (Exception ignored) {
        }
        smsMmsBroadcastReceiver = new SmsMmsBroadcastReceiver();
        IntentFilter smsMmsIntentFilter = new IntentFilter();
        smsMmsIntentFilter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        smsMmsIntentFilter.addAction(Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION);
        registerReceiver(smsMmsBroadcastReceiver, smsMmsIntentFilter);
        smsMmsBroadcastReceiver.setSmsListener((sender, body) -> smsMmsTextView.setText(String.valueOf(++smsMmsAmount)));
        smsMmsBroadcastReceiver.setMmsListener(sender -> smsMmsTextView.setText(String.valueOf(++smsMmsAmount)));
    }

    private void registerCallsBroadcastReceiver() {
        try {
            unregisterReceiver(callsBroadcastReceiver);
        } catch (Exception ignored) {
        }
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
                incomingCallsTextView.setText(String.valueOf(--incomingCallsAmount));
                incomingCallsTextView.setTextColor(Color.BLACK);
            }
        });
    }

    private void registerSensorService() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initializeDetector() throws IOException {
        Map<ManeuverType, List<ManeuverMeasurement>> trainingData = new HashMap<>();
        List<DrivingMeasurement> drivingMeasurements1 = new ArrayList<>();
        List<String> linesTraining1 = loadFromFile(drivingMeasurements1, R.raw.first_stop);

        parseDrivingMeasurement(drivingMeasurements1, linesTraining1);

        List<ManeuverMeasurement> maneuvers = new ArrayList<>();
        maneuvers.add(new ManeuverMeasurement(ManeuverType.AGGRESIVE_STOP, drivingMeasurements1));
        trainingData.put(ManeuverType.AGGRESIVE_STOP, maneuvers);

        driverPatternDetectorService = new DriverPatternDetectorService(trainingData);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean isAnomalyDetected() {
        return driverPatternDetectorService.isAnomalyByRegression(drivingWindow.getDrivingMeasurements()).size() > 0;
    }

    private void assignWidgets() {
        chart = findViewById(R.id.chart);

        xResTextView = findViewById(R.id.xRes);
        yResTextView = findViewById(R.id.yRes);
        zResTextView = findViewById(R.id.zRes);

        smsMmsTextView = findViewById(R.id.smsMms);
        incomingCallsTextView = findViewById(R.id.incomingCalls);
        outgoingCallsTextView = findViewById(R.id.outgoingCalls);
        missedCallsTextView = findViewById(R.id.missedCalls);

        leftEyeTextView = findViewById(R.id.leftEye);
        rightEyeTextView = findViewById(R.id.rightEye);
        cameraTextView = findViewById(R.id.cameraTextView);

        keepScreenOnCheckBox = findViewById(R.id.keepScreenOn);
    }

    private void runSoundNotification() {
        Intent soundNotificationIntent = new Intent(this, SoundNotificationService.class);
        soundNotificationIntent.setAction(SoundNotificationService.ACTION_PLAY);
        startService(soundNotificationIntent);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void parseDrivingMeasurement(List<DrivingMeasurement> measurements, List<String> lines) {

        final AtomicInteger indexHolder = new AtomicInteger();

        for (String l : lines) {
            double accX = Double.parseDouble(l.split(";")[0]);
            double accY = Double.parseDouble(l.split(";")[1]);
            double accZ = Double.parseDouble(l.split(";")[2]);
            measurements.add(new DrivingMeasurement(indexHolder.getAndIncrement(), accX, accY, accZ));
        }
    }

    private List<String> loadFromFile(List<DrivingMeasurement> drivingMeasurements, int resourceId) throws IOException {
        InputStream in = getResource(resourceId);
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charset.forName(StandardCharsets.UTF_8.name())))) {
            String line;
            while ((line = reader.readLine()) != null)
                lines.add(line);
        }
        return lines;
    }

    private InputStream getResource(int resourceId) {
        return this.getResources().openRawResource(resourceId);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, 3);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            System.arraycopy(event.values, 0, gravityReading,
                    0, gravityReading.length);
        }
        updateTrueAcceleration();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void updateTrueAcceleration() {
        SensorManager.getRotationMatrix(rotationMatrix, null, gravityReading, magnetometerReading);
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
        Matrix.invertM(invertedRotationMatrix, 0, rotationMatrix, 0);
        Matrix.multiplyMV(trueAcceleration, 0, invertedRotationMatrix, 0, accelerometerReading, 0);

        xResTextView.setText(String.format(Locale.ENGLISH, "x: %f", trueAcceleration[0]));
        yResTextView.setText(String.format(Locale.ENGLISH, "y: %f", trueAcceleration[1]));
        zResTextView.setText(String.format(Locale.ENGLISH, "z: %f", trueAcceleration[2]));

        Measurement curMeasurement = new Measurement(trueAcceleration[0], trueAcceleration[1], trueAcceleration[2]);
        addEntry(curMeasurement);
        drivingWindow.addMeasurement(curMeasurement);

        if (isAnomalyDetected()) {
            runSoundNotification();
        }
    }

    @Override
    public void onLoaded(FaceDetectionCamera camera) {
        try {
            camera.initialise(this, getApplicationContext());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFailedToLoadFaceDetectionCamera() {
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
        unregisterReceiver(smsMmsBroadcastReceiver);
        unregisterReceiver(callsBroadcastReceiver);
        sensorManager.unregisterListener(this);
        super.onStop();
    }
}