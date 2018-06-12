package dfad.mob.agh.edu.pl.dfad.detector;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DriverPatternDetectorServiceTest {

    private static final String RESOURCE1_FILE_NAME = "1stStop.txt";
    private static final String RESOURCE2_FILE_NAME = "2ndStop.txt";
    private static final String RESOURCE3_FILE_NAME= "1stNoStop.txt";

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

    @Test
    public void testAggressiveStopIsAnomaly() throws IOException {
        Map<ManeuverType, List<ManeuverMeasurement>> trainingData = new HashMap<>();
        List<DrivingMeasurement> drivingMeasurements = new ArrayList<>();
        List<DrivingMeasurement> testMeasurements = new ArrayList<>();
        List<String> linesTraining = loadFromFile(drivingMeasurements, RESOURCE1_FILE_NAME);
        List<String> linesTest = loadFromFile(testMeasurements, RESOURCE2_FILE_NAME);

        parseDrivingMeasurement(drivingMeasurements, linesTraining);
        parseDrivingMeasurement(testMeasurements, linesTest);

        List<ManeuverMeasurement> maneuvers = new ArrayList<>();
        maneuvers.add(new ManeuverMeasurement(ManeuverType.AGGRESIVE_STOP, drivingMeasurements));
        trainingData.put(ManeuverType.AGGRESIVE_STOP, maneuvers);

        DriverPatternDetectorService driverPatternDetectorService = new DriverPatternDetectorService(trainingData);

        assertEquals(1, driverPatternDetectorService.isAnomaly(testMeasurements).size());
    }

    @Test
    public void testAggressiveStopIsNotAnomaly() throws IOException {
        Map<ManeuverType, List<ManeuverMeasurement>> trainingData = new HashMap<>();
        List<DrivingMeasurement> drivingMeasurements = new ArrayList<>();
        List<DrivingMeasurement> testMeasurements = new ArrayList<>();
        List<String> linesTraining = loadFromFile(drivingMeasurements, RESOURCE1_FILE_NAME);
        List<String> linesTest = loadFromFile(testMeasurements, RESOURCE3_FILE_NAME);

        parseDrivingMeasurement(drivingMeasurements, linesTraining);
        parseDrivingMeasurement(testMeasurements, linesTest);

        List<ManeuverMeasurement> maneuvers = new ArrayList<>();
        maneuvers.add(new ManeuverMeasurement(ManeuverType.AGGRESIVE_STOP, drivingMeasurements));
        trainingData.put(ManeuverType.AGGRESIVE_STOP, maneuvers);

        DriverPatternDetectorService driverPatternDetectorService = new DriverPatternDetectorService(trainingData);

        assertEquals(0, driverPatternDetectorService.isAnomaly(testMeasurements).size());
    }

    private void parseDrivingMeasurement(List<DrivingMeasurement> measurements, List<String> lines) {

        final AtomicInteger indexHolder = new AtomicInteger();

        lines.forEach(l -> {
            double accX = Double.parseDouble(l.split(";")[0]);
            double accY = Double.parseDouble(l.split(";")[1]);
            double accZ = Double.parseDouble(l.split(";")[2]);
            measurements.add(new DrivingMeasurement(indexHolder.getAndIncrement(), accX, accY, accZ));
        });
    }

    private List<String> loadFromFile(List<DrivingMeasurement> drivingMeasurements, String resourceName) throws IOException {
        InputStream in = getResource(this, resourceName);
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charset.forName(StandardCharsets.UTF_8.name())))) {
            String line;
            while ((line = reader.readLine()) != null)
                lines.add(line);
        }
        return lines;
    }

    private InputStream getResource(Object obj, String resourceName) {
        return obj.getClass().getClassLoader().getResourceAsStream(resourceName);
    }

    private static File getFileFromPath(Object obj, String fileName) {
        ClassLoader classLoader = obj.getClass().getClassLoader();
        URL resource = classLoader.getResource(fileName);
        return new File(resource.getPath());
    }


}