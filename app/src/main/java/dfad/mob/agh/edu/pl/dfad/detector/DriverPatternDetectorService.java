package dfad.mob.agh.edu.pl.dfad.detector;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.w3c.dom.Attr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class DriverPatternDetectorService extends Service {

//    private Map<ManeuverType, LibSVM> svrMap;
    private Attribute x;
    private Attribute y;
    private Attribute z;
    private ArrayList<Attribute> attributes;

    final IBinder mBinder = new DriverPatternNotificationBinder();

    public DriverPatternDetectorService() {
        initialize();
    }

    private void initialize() {
//        svrMap = new HashMap<>();
        x = new Attribute("xTrueAcceleration");
        y = new Attribute("yTrueAcceleration");
        z = new Attribute("zTrueAcceleration");
        attributes = new ArrayList<>();
        attributes.add(x);
        attributes.add(y);
        attributes.add(z);
        trainClassifiers(null);
    }

    private List<ManeuverClassification> classify(List<DrivingMeasurement> drivingMeasurements) {
        return null;
    }

    private void trainClassifiers(Map<ManeuverType, List<ManeuverMeasurement>> maneuverData) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            maneuverData.keySet().forEach(this::initializeClassifier);

            maneuverData.entrySet().stream().map(entry -> {
                ManeuverType maneuverType = entry.getKey();
//                List<Instances> trainingInstances = entry.getValue().stream().map(this::createTrainingInstances).collect(Collectors.toList());
//                svrMap.get(maneuverType).c .buildClassifier();
//                svrMap
                return null;
            });
        }
        else {
            return;
        }
    }

    private void initializeClassifier(ManeuverType maneuverType) {
//        LibSVM libSVM = new LibSVM();
//        try {
//            libSVM.setOptions("-S 2 -K 2 -C 1000".split(" "));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        svrMap.put(maneuverType, new LibSVM());
    }

    private Instances createTrainingInstances(ManeuverMeasurement maneuverMeasurement) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Instances dataset = new Instances(maneuverMeasurement.getManeuverType().name(), attributes, 0);

            maneuverMeasurement.getDrivingMeasurements().stream()
                    .map(this::createInstance)
                    .forEach(dataset::add);

            return dataset;
        }

        return null;
    }

    private Instance createInstance(DrivingMeasurement drivingMeasurement) {
        Instance instance = new DenseInstance(3);
        instance.setValue(x, drivingMeasurement.getxTrueAcceleration());
        instance.setValue(y, drivingMeasurement.getyTrueAcceleration());
        instance.setValue(z, drivingMeasurement.getzTrueAcceleration());

        return instance;
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
