package dfad.mob.agh.edu.pl.dfad.detector;

public class DrivingMeasurement {

    private double xTrueAcceleration;
    private double yTrueAcceleration;
    private double zTrueAcceleration;

    public DrivingMeasurement() {
    }

    public DrivingMeasurement(double xTrueAcceleration, double yTrueAcceleration, double zTrueAcceleration) {
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
}
