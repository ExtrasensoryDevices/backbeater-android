package com.esdevices.backbeater;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;

import com.esdevices.backbeater.ui.widgets.BBTextView;
import com.esdevices.backbeater.ui.widgets.NumberButton;
import com.esdevices.backbeater.ui.widgets.TempoDisplay;

import butterknife.Bind;
import butterknife.ButterKnife;


public class MainActivity extends Activity implements View.OnClickListener{

    private static final int BLACK = -16777216;
    public static final String PREFS_NAME = "Prefs";

    private int sound = 1;
    private int window = 16;
    private int beat = 4;
    private int sensitivity = 100;

    AudioService as;
    Handler mHandle;
    @Bind(R.id.tempo) TempoDisplay tempoDisplay;
    @Bind(R.id.window2) NumberButton window2Button;
    @Bind(R.id.window4) NumberButton window4Button;
    @Bind(R.id.window8) NumberButton window8Button;
    @Bind(R.id.window16) NumberButton window16Button;
    @Bind(R.id.beat1) NumberButton beat1Button;
    @Bind(R.id.beat2) NumberButton beat2Button;
    @Bind(R.id.beat3) NumberButton beat3Button;
    @Bind(R.id.beat4) NumberButton beat4Button;
    @Bind(R.id.soundDrum) ImageView drum;
    @Bind(R.id.soundSticks) ImageView sticks;
    @Bind(R.id.soundMetronom) ImageView metronome;
    @Bind(R.id.soundSurprise) ImageView surprise;
    @Bind(R.id.textVersion) BBTextView versionNumber;
    @Bind(R.id.drawerLayout) DrawerLayout mDrawerLayout;
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        as = new AudioService(this);
        ButterKnife.bind(this);
        mHandle = new Handler(getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

            }
        };
        versionNumber.setText("Version "+BuildConfig.VERSION_NAME+ "("+BuildConfig.VERSION_CODE+")");
        beat1Button.setOnClickListener(this);
        beat2Button.setOnClickListener(this);
        beat3Button.setOnClickListener(this);
        beat4Button.setOnClickListener(this);
        window2Button.setOnClickListener(this);
        window4Button.setOnClickListener(this);
        window8Button.setOnClickListener(this);
        window16Button.setOnClickListener(this);
        drum.setOnClickListener(this);
        sticks.setOnClickListener(this);
        metronome.setOnClickListener(this);
        surprise.setOnClickListener(this);

        settings = getSharedPreferences(PREFS_NAME, 0);
        setSound(settings.getInt("sound", sound));
        setWindow(settings.getInt("window", window));
        setBeat(settings.getInt("beat", beat));
        sensitivity = settings.getInt("sensitivity", sensitivity);

        findViewById(R.id.menuButton).setOnClickListener(this);
    }


    public void beat(final double beatsPerMinute){
        mHandle.post(new Runnable() {
            @Override
            public void run() {
                tempoDisplay.setTempo((int) (beatsPerMinute));
//                ObjectAnimator.ofFloat(circle,"scaleX",0,1f,0).start();
//                ObjectAnimator.ofFloat(circle,"scaleY",0,1f,0).start();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
//        as.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        as.stopMe();
    }


    public void setBeat(int beat){
        beat1Button.enable(beat==1);
        beat2Button.enable(beat==2);
        beat3Button.enable(beat==3);
        beat4Button.enable(beat==4);
        this.beat = beat;
        settings.edit().putInt("beat",beat).commit();
    }

    public void setWindow(int window){
        window16Button.enable(window==16);
        window2Button.enable(window==2);
        window4Button.enable(window==4);
        window8Button.enable(window==8);
        this.window = window;
        settings.edit().putInt("window",window).commit();

    }

    public void setSound(int sound){
        drum.setColorFilter(sound==0?BLACK:-1,android.graphics.PorterDuff.Mode.MULTIPLY);
        sticks.setColorFilter(sound==1?BLACK:-1,android.graphics.PorterDuff.Mode.MULTIPLY);
        metronome.setColorFilter(sound==2?BLACK:-1,android.graphics.PorterDuff.Mode.MULTIPLY);
        surprise.setColorFilter(sound==3?BLACK:-1,android.graphics.PorterDuff.Mode.MULTIPLY);
        this.sound = sound;
        settings.edit().putInt("sound",sound).commit();

    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.soundSurprise:
                setSound(3);
                break;
            case R.id.soundDrum:
                setSound(0);
                break;
            case R.id.soundSticks:
                setSound(1);
                break;
            case R.id.soundMetronom:
                setSound(2);
                break;
            case R.id.window2:
                setWindow(2);
                break;
            case R.id.window4:
                setWindow(4);
                break;
            case R.id.window8:
                setWindow(8);
                break;
            case R.id.window16:
                setWindow(16);
                break;
            case R.id.beat1:
                setBeat(1);
                break;
            case R.id.beat2:
                setBeat(2);
                break;
            case R.id.beat3:
                setBeat(3);
                break;
            case R.id.beat4:
                setBeat(4);
                break;
            case R.id.menuButton:
                mDrawerLayout.openDrawer(Gravity.LEFT);
                break;

        }
    }
}
