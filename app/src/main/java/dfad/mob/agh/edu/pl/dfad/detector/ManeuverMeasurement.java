package dfad.mob.agh.edu.pl.dfad.detector;

import java.util.List;

public class ManeuverMeasurement {

    private ManeuverType maneuverType;
    private List<DrivingMeasurement> drivingMeasurements;

    public ManeuverMeasurement() {
    }

    public ManeuverMeasurement(ManeuverType maneuverType, List<DrivingMeasurement> drivingMeasurements) {
        this.maneuverType = maneuverType;
        this.drivingMeasurements = drivingMeasurements;
    }

    public ManeuverType getManeuverType() {
        return maneuverType;
    }

    public void setManeuverType(ManeuverType maneuverType) {
        this.maneuverType = maneuverType;
    }

    public List<DrivingMeasurement> getDrivingMeasurements() {
        return drivingMeasurements;
    }

    public void setDrivingMeasurements(List<DrivingMeasurement> drivingMeasurements) {
        this.drivingMeasurements = drivingMeasurements;
    }
}

