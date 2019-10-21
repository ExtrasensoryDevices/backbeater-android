package com.esdevices.backbeater.audio;

import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

import com.esdevices.backbeater.App;
import com.esdevices.backbeater.activity.MainActivity;
import com.esdevices.backbeater.ui.widgets.TempoDisplay;
import com.esdevices.backbeater.utils.Constants.Sound;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Alina Kholcheva on 2017-07-12.
 */

public class MetronomePlayer {
    Timer soundTimer = null;
    TimerTask timerTask = null;
    float duration = 0;

    public MainActivity parent = null;

    private MediaPlayer mediaPlayer = null;

    public MetronomePlayer(){
    }
    
    public void setCurrentSound(final Sound sound) {
        MediaPlayer mp = MediaPlayer.create(App.getContext(), sound.resourceId);
        mp.setVolume(0.22f, 0.22f);
        mp.seekTo(0);
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = mp;
    }

    long lastPlayTime = 0;

    public void play(float duration) {
        int delay = 0;
        if (soundTimer != null) {
            soundTimer.cancel();
            soundTimer = null;
        }
        this.duration = duration;
        soundTimer = new Timer();

        Log.i("TAG", "#### start play = " + (int)(this.duration*1000));


        final int dur = (int)(this.duration * 1000);

        lastPlayTime = System.currentTimeMillis();
        TempoDisplay.lastPlayTime = System.currentTimeMillis();
        mediaPlayer.setVolume(0.2f, 0.2f);
        mediaPlayer.start();

        final Handler handler1 = new Handler();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                final long curTime =  System.currentTimeMillis();
                if (parent != null) {
                    handler1.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            TempoDisplay.lastPlayTime = System.currentTimeMillis();
                            parent.invalidateTempo();
                        }
                    }, 10);
                }
                if (mediaPlayer != null) {
                    try {
//                        if (mediaPlayer.isPlaying()) {
//                            mediaPlayer.stop();
//                            mediaPlayer.seekTo(0);
//                        }
                        mediaPlayer.setVolume(0.15f, 0.15f);
                        mediaPlayer.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (parent != null) {

                    long curTime2 = System.currentTimeMillis();

//                    parent.setLogText("play = " + (curTime - lastPlayTime) + " > " + (curTime2 - curTime));

//                    Log.e("TAG", "##### play = " + (curTime - lastPlayTime) + " > " + (curTime2 - curTime));
                }
                lastPlayTime = curTime;
            }
        };

        soundTimer.scheduleAtFixedRate(timerTask, dur, dur );
    }

    public void stop() {
        if (soundTimer != null) {
            soundTimer.cancel();
            soundTimer = null;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = null;
    }
}
