package dfad.mob.agh.edu.pl.dfad.notification;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;

import com.bosphere.filelogger.FL;

import java.util.Objects;

import dfad.mob.agh.edu.pl.dfad.R;

// https://developer.android.com/guide/topics/media/mediaplayer
public class SoundNotificationService extends Service implements MediaPlayer.OnPreparedListener {

    public static final String ACTION_PLAY = "dfad.intent.notification.PLAY";
    MediaPlayer mMediaPlayer = null;
    final IBinder mBinder = new SoundNotificationBinder();

    public SoundNotificationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Objects.requireNonNull(intent.getAction()).equals(ACTION_PLAY)) {
            mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.example_sound2);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    public class SoundNotificationBinder extends Binder {
        SoundNotificationService getService() {
            return SoundNotificationService.this;
        }
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) mMediaPlayer.release();
    }

}
