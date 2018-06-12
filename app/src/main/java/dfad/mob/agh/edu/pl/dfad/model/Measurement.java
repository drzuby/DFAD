package dfad.mob.agh.edu.pl.dfad.model;

import java.time.LocalDateTime;
import java.util.Calendar;

public class Measurement {

    private final int time; // TODO: time instead?
    private final double xAcc;
    private final double yAcc;
    private final double zAcc;

    public Measurement(double xAcc, double yAcc, double zAcc) {
        time = Calendar.getInstance().get(Calendar.MINUTE)*60*1000 + Calendar.getInstance().get(Calendar.SECOND)*1000 + Calendar.getInstance().get(Calendar.MILLISECOND);
        System.out.println(time);
        this.xAcc = xAcc;
        this.yAcc = yAcc;
        this.zAcc = zAcc;
    }

    public int getTime() {
        return time;
    }

    public double getXAcc() {
        return xAcc;
    }

    public double getYAcc() {
        return yAcc;
    }

    public double getZAcc() {
        return zAcc;
    }
}
