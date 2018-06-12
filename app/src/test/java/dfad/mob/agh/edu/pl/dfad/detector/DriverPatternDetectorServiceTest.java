package dfad.mob.agh.edu.pl.dfad.detector;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DriverPatternDetectorServiceTest {

    @Test
    public void testInit() {
        Map<ManeuverType, List<ManeuverMeasurement>> trainingData = new HashMap<>();
        List<DrivingMeasurement> drivingMeasurements = new ArrayList<>();
        drivingMeasurements.add(new DrivingMeasurement(0, 1, 2, 3));
        drivingMeasurements.add(new DrivingMeasurement(1, 2, 3, 4));
        drivingMeasurements.add(new DrivingMeasurement(2, 3, 4, 2));

        List<ManeuverMeasurement> maneuvers = new ArrayList<>();
        maneuvers.add(new ManeuverMeasurement(ManeuverType.AGGRESIVE_LEFT, drivingMeasurements));

        trainingData.put(ManeuverType.AGGRESIVE_LEFT, maneuvers);

        DriverPatternDetectorService driverPatternDetectorService = new DriverPatternDetectorService(trainingData);

        assertEquals(1, driverPatternDetectorService.isAnomaly(drivingMeasurements).size());
    }

    @Test
    public void testInitNoAnomaly() {
        Map<ManeuverType, List<ManeuverMeasurement>> trainingData = new HashMap<>();
        List<DrivingMeasurement> drivingMeasurements = new ArrayList<>();
        drivingMeasurements.add(new DrivingMeasurement(0, 1, 2, 3));
        drivingMeasurements.add(new DrivingMeasurement(1, 2, 3, 4));
        drivingMeasurements.add(new DrivingMeasurement(2, 3, 4, 2));

        List<DrivingMeasurement> testMeasurements = new ArrayList<>();
        testMeasurements.add(new DrivingMeasurement(0, 8, 12, 2));
        testMeasurements.add(new DrivingMeasurement(1, 5, 7, 1));
        testMeasurements.add(new DrivingMeasurement(2, 3, 4, 1));

        List<ManeuverMeasurement> maneuvers = new ArrayList<>();
        maneuvers.add(new ManeuverMeasurement(ManeuverType.AGGRESIVE_LEFT, drivingMeasurements));

        trainingData.put(ManeuverType.AGGRESIVE_LEFT, maneuvers);

        DriverPatternDetectorService driverPatternDetectorService = new DriverPatternDetectorService(trainingData);

        assertEquals(0, driverPatternDetectorService.isAnomaly(testMeasurements).size());
    }

}