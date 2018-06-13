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

import static weka.core.Instances.mergeInstances;

public class DriverPatternDetectorService extends Service {

    public static final String X_ATTRIBUTE_NAME = "xTrueAcceleration";
    public static final String Y_ATTRIBUTE_NAME = "yTrueAcceleration";
    public static final String Z_ATTRIBUTE_NAME = "zTrueAcceleration";
    private static final double BASE_THRESHOLD = 0.7;
    private static final double THRESHOLD_LOSS_PERCENTAGE = 10;
    private static int PENALTY_COUNT = 0;

    private Map<ManeuverType, Map<Attribute, LibSVM>> svrMap;
    private Map<ManeuverType, Map<Attribute, Instances>> datasets;
    private Attribute x;
    private Attribute y;
    private Attribute z;
    private Attribute measurementIndex;
    private ArrayList<Attribute> attributes;

    private Map<ManeuverType, LibSVM> svcMap;

    final IBinder mBinder = new DriverPatternNotificationBinder();

    @RequiresApi(api = Build.VERSION_CODES.N)
    public DriverPatternDetectorService(Map<ManeuverType, List<ManeuverMeasurement>> maneuverData) {
        initializeRegression(maneuverData);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void initializeSVR(Map<ManeuverType, List<ManeuverMeasurement>> maneuverData, Map<ManeuverType, List<ManeuverMeasurement>> nonManeuverData) {
        svcMap = new HashMap<>();

        trainClassifier(maneuverData, nonManeuverData);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void trainClassifier(Map<ManeuverType, List<ManeuverMeasurement>> maneuverData, Map<ManeuverType, List<ManeuverMeasurement>> nonManeuverData) {
        Map<ManeuverType, Instances> anomalyInstances = createClassificationDataset(maneuverData);
        Map<ManeuverType, Instances> nonAnomalyInstances = createClassificationDataset(nonManeuverData);

        maneuverData.entrySet().forEach(entry -> {
            ManeuverType maneuverType = entry.getKey();

            svcMap.put(maneuverType, createSVC());
            Instances anomalyInst = fillClassifierInstances(anomalyInstances.get(maneuverType), entry.getValue(), 1);
            Instances nonAnomalyInst = fillClassifierInstances(nonAnomalyInstances.get(maneuverType), nonManeuverData.get(maneuverType), 0);
            Instances trainingInstances = mergeInstances(anomalyInst, nonAnomalyInst);
            try {
                svcMap.get(maneuverType).buildClassifier(trainingInstances);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Instances fillClassifierInstances(Instances dataset, List<ManeuverMeasurement> maneuverMeasurements, int classValue) {

        maneuverMeasurements.forEach(maneuverMeasurement -> {
            List<DrivingMeasurement> drivingMeasurements = maneuverMeasurement.getDrivingMeasurements();

            Instance instance = new DenseInstance(dataset.numAttributes());
            instance.setDataset(dataset);
            int i = 0;
            for(; i < dataset.numAttributes(); i+=3) {
                instance.setValue(i, drivingMeasurements.get(i).getxTrueAcceleration());
                instance.setValue(i+1, drivingMeasurements.get(i+1).getyTrueAcceleration());
                instance.setValue(i+2, drivingMeasurements.get(i+2).getzTrueAcceleration());
            }

            instance.setValue(i-2, classValue);

            dataset.add(instance);
        });

        return dataset;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initializeRegression(Map<ManeuverType, List<ManeuverMeasurement>> maneuverData) {
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

        trainRegression(maneuverData);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<ManeuverAnomaly> isAnomalyByRegression(List<DrivingMeasurement> drivingMeasurements) {
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
                .mapToDouble(attribute -> average(drivingMeasurements, attribute))
                .sum();
    }

    private double calculateThreshold(double norm) {
        return norm * (BASE_THRESHOLD * penalty());
    }

    private double penalty() {
        double valueFractionAfterPenalty = 1;
        for(int i=0; i < PENALTY_COUNT; i++) {
            valueFractionAfterPenalty *= (1.0 + THRESHOLD_LOSS_PERCENTAGE / 100);
        }
        return valueFractionAfterPenalty;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private double average(List<DrivingMeasurement> drivingMeasurements, Attribute attribute) {
        return drivingMeasurements.stream()
                .mapToDouble(measurement -> measurement.getAccelerationByAttribute(attribute))
                .reduce(0, (a, b) -> a + Math.abs(b))/drivingMeasurements.size();
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
    private void trainRegression(Map<ManeuverType, List<ManeuverMeasurement>> maneuverData) {

        maneuverData.keySet().forEach(this::initializeSVR);

        attributes.forEach(attribute -> maneuverData.entrySet().forEach(entry -> {
            Instances maneuverTrainingDataset = createRegressionDataset(entry.getKey(), attribute);

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
        return createSVM("-S 3 -K 2 -C 10000");
    }

    private LibSVM createSVC() {
        return createSVM("-S 0 -K 2 -C 10000");
    }

    private LibSVM createSVM(String params) {
        LibSVM libSVM = new LibSVM();
        try {
            libSVM.setOptions(params.split(" "));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return libSVM;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initializeSVR(ManeuverType maneuverType) {

        if(!svrMap.containsKey(maneuverType)) {
            svrMap.put(maneuverType, new HashMap<>());
        }
        attributes.forEach(attribute -> svrMap.get(maneuverType).put(attribute, createSVR()));

    }

    @TargetApi(Build.VERSION_CODES.N)
    private Instances createTrainingInstances(ManeuverMeasurement maneuverMeasurement, Attribute attribute) {
        Instances dataset = createRegressionDataset(maneuverMeasurement.getManeuverType(), attribute);

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

    private Instances createRegressionDataset(ManeuverType maneueverType, Attribute attribute) {
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
    private Map<ManeuverType, Instances> createClassificationDataset(Map<ManeuverType, List<ManeuverMeasurement>> maneuverData) {
        Map<ManeuverType, Instances> toRet = new HashMap<>();

        maneuverData.entrySet().forEach(entry -> {
            ManeuverType maneuverType = entry.getKey();
            List<ManeuverMeasurement> maneuverMeasurements = entry.getValue();

            ArrayList<Attribute> classificationAttributes = new ArrayList<>();

            int measurementCount = maneuverMeasurements.get(0).getDrivingMeasurements().size();


            for(int i = 0; i < measurementCount; i++) {
                classificationAttributes.add(new Attribute(String.format("x%d", i)));
                classificationAttributes.add(new Attribute(String.format("y%d", i)));
                classificationAttributes.add(new Attribute(String.format("z%d", i)));
            }

            Attribute classAttribute = new Attribute("anomaly");

            classificationAttributes.add(classAttribute);

            Instances instances = new Instances(maneuverType.name(), classificationAttributes, 0);
            instances.setClass(classAttribute);

            toRet.put(entry.getKey(), instances);
        });

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
