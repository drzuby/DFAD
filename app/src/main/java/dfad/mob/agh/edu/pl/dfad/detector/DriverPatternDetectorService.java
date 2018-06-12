package dfad.mob.agh.edu.pl.dfad.detector;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;


import weka.classifiers.functions.LibSVM;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class DriverPatternDetectorService extends Service {

    public static final String X_ATTRIBUTE_NAME = "xTrueAcceleration";
    public static final String Y_ATTRIBUTE_NAME = "yTrueAcceleration";
    public static final String Z_ATTRIBUTE_NAME = "zTrueAcceleration";
    private static final double BASE_THRESHOLD = 0.5;
    private static final double THRESHOLD_LOSS_PERCENTAGE = 10;
    private static int PENALTY_COUNT = 0;

    private Map<ManeuverType, Map<Attribute, LibSVM>> svrMap;
    private Map<ManeuverType, Map<Attribute, Instances>> datasets;
    private Attribute x;
    private Attribute y;
    private Attribute z;
    private Attribute measurementIndex;
    private ArrayList<Attribute> attributes;

    final IBinder mBinder = new DriverPatternNotificationBinder();

    @RequiresApi(api = Build.VERSION_CODES.N)
    public DriverPatternDetectorService(Map<ManeuverType, List<ManeuverMeasurement>> maneuverData) {
        initialize(maneuverData);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initialize(Map<ManeuverType, List<ManeuverMeasurement>> maneuverData) {
        svrMap = new HashMap<>();
        datasets = new HashMap<>();

        x = new Attribute(X_ATTRIBUTE_NAME);
        y = new Attribute(Y_ATTRIBUTE_NAME);
        z = new Attribute(Z_ATTRIBUTE_NAME);
        measurementIndex = new Attribute("measurementIndex");

        attributes = new ArrayList<>();
        attributes.add(x);
        attributes.add(y);
        attributes.add(z);

        trainClassifiers(maneuverData);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<ManeuverAnomaly> isAnomaly(List<DrivingMeasurement> drivingMeasurements) {
        List<ManeuverAnomaly> maneuverAnomalies = new ArrayList<>();

        svrMap.entrySet().forEach(entry -> {
            ManeuverType maneuverType = entry.getKey();

            int measurementCount = drivingMeasurements.size();

            List<DrivingMeasurement> maneuverRegressionModel = regressionModelForManeuver(maneuverType, measurementCount);

            double similarityX = computeDifference(drivingMeasurements, maneuverRegressionModel, x);
            double similarityY = computeDifference(drivingMeasurements, maneuverRegressionModel, y);
            double similarityZ = computeDifference(drivingMeasurements, maneuverRegressionModel, z);

            if(threshold(similarityX, similarityY, similarityZ, modelNorm(maneuverRegressionModel))) {
                ManeuverAnomaly maneuverAnomaly = new ManeuverAnomaly(maneuverType);
                maneuverAnomalies.add(maneuverAnomaly);
            }
        });

        return maneuverAnomalies;
    }

    private boolean threshold(double similarityX, double similarityY, double similarityZ, double modelNorm) {
        double threshold = calculateThreshold(modelNorm);

        return similarityX + similarityY + similarityZ < threshold;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private double modelNorm(List<DrivingMeasurement> drivingMeasurements) {

        return attributes.stream()
                .mapToDouble(attribute -> euclideanNorm(drivingMeasurements, attribute))
                .sum();
    }

    private double calculateThreshold(double norm) {
        return norm * (BASE_THRESHOLD * penalty());
    }

    private double penalty() {
        double valueFractionAfterPenalty = 1;
        for(int i=0; i < PENALTY_COUNT; i++) {
            valueFractionAfterPenalty *= (1 - THRESHOLD_LOSS_PERCENTAGE / 100);
        }
        return valueFractionAfterPenalty;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private double euclideanNorm(List<DrivingMeasurement> drivingMeasurements, Attribute attribute) {
        return Math.sqrt(drivingMeasurements.stream()
                .mapToDouble(measurement -> measurement.getAccelerationByAttribute(attribute))
                .reduce(0, (a, b) -> a + b*b));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private List<DrivingMeasurement> regressionModelForManeuver(ManeuverType maneuverType, int measurementCount) {
        List<DrivingMeasurement> drivingMeasurements = new ArrayList<>();

        IntStream.range(0, measurementCount)
                .forEach(index -> {
                    DrivingMeasurement drivingMeasurement = new DrivingMeasurement();
                    attributes.forEach(attribute -> {
                        Instance instance = new DenseInstance(1);
                        instance.setDataset(datasets.get(maneuverType).get(attribute));
                        instance.setValue(measurementIndex, index);
                        double result = -1;

                        try {
                            result = svrMap.get(maneuverType).get(attribute).classifyInstance(instance);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        drivingMeasurement.setAccelerationByAttribute(result, attribute);
                        drivingMeasurement.setMeasurementIndexInManeuver(index);
                    });
                    drivingMeasurements.add(drivingMeasurement);
                });

        return drivingMeasurements;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void trainClassifiers(Map<ManeuverType, List<ManeuverMeasurement>> maneuverData) {

        maneuverData.keySet().forEach(this::initializeClassifier);

        attributes.forEach(attribute -> maneuverData.entrySet().forEach(entry -> {
            Instances maneuverTrainingDataset = createDataset(entry.getKey(), attribute);

            ManeuverType maneuverType = entry.getKey();

            Instances attributeTrainingInstances = entry.getValue().stream()
                    .map(maneuverMeasurements -> createTrainingInstances(maneuverMeasurements, attribute))
                    .reduce(maneuverTrainingDataset, (inst1, inst2) -> {
                        if(inst2 == null) {
                            return inst1;
                        }
                        inst1.addAll(inst2.subList(0, inst2.size()));
                        return inst1;
                    });

            try {
                if(!datasets.containsKey(maneuverType)) {
                    datasets.put(maneuverType, new HashMap<>());
                }
                datasets.get(maneuverType).put(attribute, attributeTrainingInstances);

                svrMap.get(maneuverType).get(attribute).buildClassifier(attributeTrainingInstances);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

    }

    private LibSVM createSVR() {
        LibSVM libSVM = new LibSVM();
        try {
            libSVM.setOptions("-S 3 -K 2 -C 10000".split(" "));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return libSVM;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initializeClassifier(ManeuverType maneuverType) {

        if(!svrMap.containsKey(maneuverType)) {
            svrMap.put(maneuverType, new HashMap<>());
        }
        attributes.forEach(attribute -> svrMap.get(maneuverType).put(attribute, createSVR()));

    }

    @TargetApi(Build.VERSION_CODES.N)
    private Instances createTrainingInstances(ManeuverMeasurement maneuverMeasurement, Attribute attribute) {
        Instances dataset = createDataset(maneuverMeasurement.getManeuverType(), attribute);

        maneuverMeasurement.getDrivingMeasurements().stream()
                .map(maneuverMeasurementPom -> createInstance(maneuverMeasurementPom, attribute))
                .forEach(dataset::add);

        return dataset;
    }

    private Instance createInstance(DrivingMeasurement drivingMeasurement, Attribute attribute) {
        Instance instance = new DenseInstance(2);
        instance.setValue(measurementIndex, drivingMeasurement.getMeasurementIndexInManeuver());
        instance.setValue(attribute, drivingMeasurement.getAccelerationByAttribute(attribute));

        return instance;
    }

    private Instances createDataset(ManeuverType maneueverType, Attribute attribute) {
        Instances toRet;
        ArrayList<Attribute> atrributes = new ArrayList<>();
        atrributes.add(measurementIndex);
        atrributes.add(attribute);
        String instancesName = String.format(maneueverType.name() + ":%s", attribute.name());

        toRet = new Instances(instancesName, atrributes, 0);
        toRet.setClass(attribute);

        return toRet;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private float[] dtwParameter(List<DrivingMeasurement> drivingMeasurements, Attribute attribute) {
        float[] toRet = new float[drivingMeasurements.size()];

        for(DrivingMeasurement drivingMeasurement: drivingMeasurements) {
            toRet[drivingMeasurement.getMeasurementIndexInManeuver()] = (float) drivingMeasurement.getAccelerationByAttribute(attribute);
        }

        return toRet;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private double computeDifference(List<DrivingMeasurement> drivingMeasurements, List<DrivingMeasurement> maneuverRegressionModel, Attribute attribute) {
        DTW dtw = new DTW();
        return dtw.compute(dtwParameter(drivingMeasurements, attribute), dtwParameter(maneuverRegressionModel, attribute)).getDistance();
    }

    public static void incrementPenaltyCount() {
        PENALTY_COUNT++;
    }

    public static void decrementPenaltyCount() {
        PENALTY_COUNT--;
        if(PENALTY_COUNT < 0) PENALTY_COUNT = 0;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class DriverPatternNotificationBinder extends Binder {
        DriverPatternDetectorService getService() {
            return DriverPatternDetectorService.this;
        }
    }
}
