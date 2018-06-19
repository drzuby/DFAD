package dfad.mob.agh.edu.pl.dfad.helper;

import java.sql.Timestamp;
import java.util.Date;

public class TimestampHelper {

    public static Timestamp getCurrentTimestamp() {
        Date today = new Date();
        return new Timestamp(today.getTime());
    }

    public static long diff(Date start, Date stop) {
        if (start.compareTo(stop) > 0) {
            Date tmp = start;
            start = stop;
            stop = tmp;
        }
        return stop.getTime() - start.getTime();
    }
}