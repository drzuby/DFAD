package dfad.mob.agh.edu.pl.dfad.visualization;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.bosphere.filelogger.FL;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dfad.mob.agh.edu.pl.dfad.R;
import dfad.mob.agh.edu.pl.dfad.model.Measurement;

public class ActionVisualizer extends AppCompatActivity implements SensorEventListener {

    private LineChart chart;
    private List<Entry> entries = new ArrayList<>();
    private LineDataSet dataSetX;
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

        Measurement m = new Measurement(0,0,0);
        entries.add(new Entry(m.getTime(),0));
        dataSetX = new LineDataSet(entries, "X");
        dataSetX.setDrawFilled(true);
        dataSetX.setColors(ColorTemplate.COLORFUL_COLORS);

        lineData = new LineData(dataSetX);
        chart.setData(lineData);
        chart.invalidate();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void addEntry(Measurement m) {
        dataSetX.addEntry(new Entry(m.getTime(), (float) m.getxAcc())); // TODO: support other axis 2
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
}
