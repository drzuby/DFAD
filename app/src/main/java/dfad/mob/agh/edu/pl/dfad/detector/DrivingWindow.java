package dfad.mob.agh.edu.pl.dfad.detector;

import java.util.LinkedList;
import java.util.List;

import dfad.mob.agh.edu.pl.dfad.model.Measurement;


public class DrivingWindow {

    private static final int WINDOW_SIZE = 500;

    private List<DrivingMeasurement> drivingMeasurements = new LinkedList<>();

    public DrivingWindow() {
    }

    public void addMeasurement(Measurement measurement) {
        int lastIndex = 0;
        int drivingMeasurementsSize = drivingMeasurements.size();
        if (drivingMeasurementsSize > 0)
            lastIndex = drivingMeasurements.get(drivingMeasurementsSize).getMeasurementIndexInManeuver();
        DrivingMeasurement drivingMeasurement = new DrivingMeasurement(++lastIndex, measurement.getXAcc(), measurement.getYAcc(), measurement.getZAcc());
        drivingMeasurements.add(drivingMeasurement);
        if (drivingMeasurements.size() > WINDOW_SIZE)
            drivingMeasurements.remove(0);
    }

    public List<DrivingMeasurement> getDrivingMeasurements() {
        return drivingMeasurements;
    }
}
