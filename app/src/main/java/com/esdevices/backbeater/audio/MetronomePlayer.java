package com.esdevices.backbeater.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import com.esdevices.backbeater.App;
import com.esdevices.backbeater.utils.Constants.Sound;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Alina Kholcheva on 2017-07-12.
 */

public class MetronomePlayer {
    private SoundPool soundPool;
    private Sound currentSound = Sound.fromIndex(0);
    
    private int[] soundIds;
    
    private static final int COUNT = 4;

    Timer soundTimer = null;

    public MetronomePlayer(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)

                    .build();
            soundPool = new SoundPool.Builder().setMaxStreams(COUNT).setAudioAttributes(attrs).build();
        } else {
            soundPool = new SoundPool(COUNT, AudioManager.STREAM_MUSIC, 0);
        }
        // init sounds
        soundIds = new int[COUNT];
        Context context = App.getContext();
        for (int i=0; i<COUNT; i++) {
            soundIds[i] = soundPool.load(context, Sound.fromIndex(i).resourceId, 1);
        }
    }
    
    public void setCurrentSound(Sound sound) {
        this.currentSound = sound;
    }

    public void play(float duration) {
        if (soundTimer != null) {
            soundTimer.cancel();
            soundTimer = null;
        }
        soundTimer = new Timer();
        soundTimer.scheduleAtFixedRate( new TimerTask() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override public void run() {
                        soundPool.play(soundIds[currentSound.index],1,1,1,0,1f);
                    }
                }).start();
            }
        }, 50, (int)(duration*1000) );
/*
        new Thread(new Runnable() {
            @Override public void run() {
                soundPool.setLoop(soundIds[currentSound.index], -1);
                soundPool.play(soundIds[currentSound.index],1,1,1,-1,1f);
            }
        }).start();
*/
    }

    public void stop() {
        if (soundTimer != null) {
            soundTimer.cancel();
            soundTimer = null;
        }
        soundPool.stop(soundIds[currentSound.index]);
    }
}
