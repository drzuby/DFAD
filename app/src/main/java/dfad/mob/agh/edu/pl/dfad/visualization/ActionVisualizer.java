package dfad.mob.agh.edu.pl.dfad.visualization;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

import dfad.mob.agh.edu.pl.dfad.R;
import dfad.mob.agh.edu.pl.dfad.model.Measurement;

public class ActionVisualizer extends AppCompatActivity implements SensorEventListener {

    private static final int WINDOW_SIZE = 500;
    private static final String X_AXIS_NAME = "X";
    private static final String Y_AXIS_NAME = "Y";
    private static final String Z_AXIS_NAME = "Z";
    private static final boolean CHART_DRAW_FILLED = true;
    private static final boolean CHART_DRAW_CIRCLES = false;

    private LineChart chart;
    private List<Entry> entriesX = new ArrayList<>();
    private List<Entry> entriesY = new ArrayList<>();
    private List<Entry> entriesZ = new ArrayList<>();
    private LineDataSet dataSetX;
    private LineDataSet dataSetY;
    private LineDataSet dataSetZ;
    private LineData lineData;

    private SensorManager sensorManager;
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

        setContentView(R.layout.activity_action_visualizer);
        chart = findViewById(R.id.chart);

        loadInitEntries();
        setChartDataSetParameters();
        loadChart();
        registerSensorService();

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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void updateTrueAcceleration() {
        SensorManager.getRotationMatrix(rotationMatrix, null, gravityReading, magnetometerReading);
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
        Matrix.invertM(invertedRotationMatrix, 0, rotationMatrix, 0);
        Matrix.multiplyMV(trueAcceleration, 0, invertedRotationMatrix, 0, accelerometerReading, 0);
        Measurement m = new Measurement(trueAcceleration[0], trueAcceleration[1], trueAcceleration[2]);
        addEntry(m);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null)
            sensorManager.unregisterListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (sensorManager != null)
            sensorManager.unregisterListener(this);
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

    private void registerSensorService() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL);
    }
}
