package com.esdevices.backbeater;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import android.widget.TextView;

import com.esdevices.backbeater.ui.widgets.TempoDisplay;


public class MainActivity extends Activity {

    AudioService as;
    Handler mHandle;
    TempoDisplay tempoDisplay;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        as = new AudioService(this);
        tempoDisplay = (TempoDisplay)findViewById(R.id.tempo);
        mHandle = new Handler(getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

            }
        };
    }


    public void beat(final double beatsPerMinute){
        mHandle.post(new Runnable() {
            @Override
            public void run() {
                tempoDisplay.setTempo((int)(beatsPerMinute));
//                ObjectAnimator.ofFloat(circle,"scaleX",0,1f,0).start();
//                ObjectAnimator.ofFloat(circle,"scaleY",0,1f,0).start();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        as.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        as.stopMe();
    }


}
