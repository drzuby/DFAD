package dfad.mob.agh.edu.pl.dfad.detector;

import java.util.Objects;

import weka.core.Attribute;

public class DrivingMeasurement {

    private int measurementIndexInManeuver;     //can also be time e.g. Duration or miliseconds
    private double xTrueAcceleration;
    private double yTrueAcceleration;
    private double zTrueAcceleration;

    public DrivingMeasurement() {
    }

    public DrivingMeasurement(int measurementIndexInManeuver, double xTrueAcceleration, double yTrueAcceleration, double zTrueAcceleration) {
        this.measurementIndexInManeuver = measurementIndexInManeuver;
        this.xTrueAcceleration = xTrueAcceleration;
        this.yTrueAcceleration = yTrueAcceleration;
        this.zTrueAcceleration = zTrueAcceleration;
    }

    public double getxTrueAcceleration() {
        return xTrueAcceleration;
    }

    public void setxTrueAcceleration(double xTrueAcceleration) {
        this.xTrueAcceleration = xTrueAcceleration;
    }

    public double getyTrueAcceleration() {
        return yTrueAcceleration;
    }

    public void setyTrueAcceleration(double yTrueAcceleration) {
        this.yTrueAcceleration = yTrueAcceleration;
    }

    public double getzTrueAcceleration() {
        return zTrueAcceleration;
    }

    public void setzTrueAcceleration(double zTrueAcceleration) {
        this.zTrueAcceleration = zTrueAcceleration;
    }

    public double getAccelerationByAttribute(Attribute attribute) {
        if(Objects.equals(attribute.name(), DriverPatternDetectorService.X_ATTRIBUTE_NAME)) {
            return xTrueAcceleration;
        }
        if(Objects.equals(attribute.name(), DriverPatternDetectorService.Y_ATTRIBUTE_NAME)) {
            return yTrueAcceleration;
        }
        if(Objects.equals(attribute.name(), DriverPatternDetectorService.Z_ATTRIBUTE_NAME)) {
            return zTrueAcceleration;
        }

        return -1;
    }

    public int getMeasurementIndexInManeuver() {
        return measurementIndexInManeuver;
    }

    public void setMeasurementIndexInManeuver(int measurementIndexInManeuver) {
        this.measurementIndexInManeuver = measurementIndexInManeuver;
    }

    public void setAccelerationByAttribute(double newAcceleration, Attribute attribute) {
        if(Objects.equals(attribute.name(), DriverPatternDetectorService.X_ATTRIBUTE_NAME)) {
            this.xTrueAcceleration = newAcceleration;
        }
        if(Objects.equals(attribute.name(), DriverPatternDetectorService.Y_ATTRIBUTE_NAME)) {
            this.yTrueAcceleration = newAcceleration;
        }
        if(Objects.equals(attribute.name(), DriverPatternDetectorService.Z_ATTRIBUTE_NAME)) {
            this.zTrueAcceleration = newAcceleration;
        }
    }
}
