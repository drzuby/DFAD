package dfad.mob.agh.edu.pl.dfad.detector;

import android.os.Build;
import android.support.annotation.RequiresApi;

import dfad.mob.agh.edu.pl.dfad.MainActivity;

public class ComputationTask{

    private final DriverPatternDetectorService driverPatternDetectorService;
    int i = 0;

    public ComputationTask(DriverPatternDetectorService driverPatternDetectorService) {
        this.driverPatternDetectorService = driverPatternDetectorService;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void checkAnomaly(DrivingWindow drivingWindow) {
        i++;
        if (i % 100 == 0) {
            if (driverPatternDetectorService.isAnomalyByRegression(drivingWindow.getDrivingMeasurements()).size() > 0)
                MainActivity.getInstance().runSoundNotification();
        }
        if (i >= Integer.MAX_VALUE)
            i = 1;
    }

}
