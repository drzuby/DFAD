package dfad.mob.agh.edu.pl.dfad.camera;

public interface Listener {

    void onFaceDetected();

    void onFaceTimedOut();

    void onLeftEyeChanged(boolean isOpen);

    void onRightEyeChanged(boolean isOpen);
}
