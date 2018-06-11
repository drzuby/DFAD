package dfad.mob.agh.edu.pl.dfad.detector;

public class ManeuverAnomaly {

    private ManeuverType maneuverType;

    public ManeuverAnomaly() {
    }

    public ManeuverAnomaly(ManeuverType maneuverType) {
        this.maneuverType = maneuverType;
    }

    public ManeuverType getManeuverType() {
        return maneuverType;
    }

    public void setManeuverType(ManeuverType maneuverType) {
        this.maneuverType = maneuverType;
    }
}
