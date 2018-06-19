package dfad.mob.agh.edu.pl.dfad.detector;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.sql.Timestamp;

import dfad.mob.agh.edu.pl.dfad.MainActivity;
import dfad.mob.agh.edu.pl.dfad.helper.TimestampHelper;

public class ComputationTask{

    private static final int NOTIF_BREAK = 6000;

    private final DriverPatternDetectorService driverPatternDetectorService;
    private Timestamp lastNotification;
    private int i = 0;

    public ComputationTask(DriverPatternDetectorService driverPatternDetectorService) {
        this.driverPatternDetectorService = driverPatternDetectorService;
        lastNotification = TimestampHelper.getCurrentTimestamp();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void checkAnomaly(DrivingWindow drivingWindow) {
        i++;
        if (i % 100 == 0) {
            boolean condition1 = TimestampHelper.diff(lastNotification, TimestampHelper.getCurrentTimestamp()) > NOTIF_BREAK;
            if (condition1) {
                boolean condition2 = driverPatternDetectorService.isAnomalyByRegression(drivingWindow.getDrivingMeasurements()).size() > 0;
                if (condition2) {
                    MainActivity.getInstance().runSoundNotification();
                    lastNotification = TimestampHelper.getCurrentTimestamp();
                }
            }
        }
        resetIterator();
    }

    private void resetIterator() {
        if (i >= Integer.MAX_VALUE)
            i = 1;
    }

}
