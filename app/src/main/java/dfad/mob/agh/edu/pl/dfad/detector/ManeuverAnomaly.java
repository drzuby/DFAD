package dfad.mob.agh.edu.pl.dfad.detector;

public class ManeuverAnomaly {

    private ManeuverType maneuverType;
    private double confidence;

    public ManeuverAnomaly() {
    }

    public ManeuverAnomaly(ManeuverType maneuverType, double confidence) {
        this.maneuverType = maneuverType;
        this.confidence = confidence;
    }

    public ManeuverType getManeuverType() {
        return maneuverType;
    }

    public void setManeuverType(ManeuverType maneuverType) {
        this.maneuverType = maneuverType;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}
