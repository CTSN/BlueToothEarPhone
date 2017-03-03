package com.xmg.bluetoothearphone.utlis;

import android.content.Context;
import android.media.MediaPlayer;

import com.xmg.bluetoothearphone.R;

import java.util.Timer;
import java.util.TimerTask;

public enum  AudioUtils {
    INSTANCE;
    private MediaPlayer mediaPlayer;
    private Timer mTimer;
    private TimerTask mTimerTask;

    public void playMedai(Context activity) {
        mediaPlayer= MediaPlayer.create(activity, R.raw.yiqianlengyiye);
        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {

            }
        };
        mTimer.schedule(mTimerTask, 0, 10);
        mediaPlayer.start();
    }

    public  void stopPlay (){
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }
}
