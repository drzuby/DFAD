package dfad.mob.agh.edu.pl.dfad.detector;

public class ManeuverClassification {

    private ManeuverType maneuverType;
    private boolean isAnomaly;

    public ManeuverClassification() {
    }

    public ManeuverClassification(ManeuverType maneuverType, boolean isAnomaly) {
        this.maneuverType = maneuverType;
        this.isAnomaly = isAnomaly;
    }

    public ManeuverType getManeuverType() {
        return maneuverType;
    }

    public void setManeuverType(ManeuverType maneuverType) {
        this.maneuverType = maneuverType;
    }

    public boolean isAnomaly() {
        return isAnomaly;
    }

    public void setAnomaly(boolean anomaly) {
        isAnomaly = anomaly;
    }
}
