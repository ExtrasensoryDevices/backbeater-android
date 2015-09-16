package com.esdevices.backbeater;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.esdevices.backbeater.ui.widgets.BBTextView;
import com.esdevices.backbeater.ui.widgets.NumberButton;
import com.esdevices.backbeater.ui.widgets.TempoDisplay;

import butterknife.Bind;
import butterknife.ButterKnife;


public class MainActivity extends Activity {

    AudioService as;
    Handler mHandle;
    @Bind(R.id.tempo) TempoDisplay tempoDisplay;
    @Bind(R.id.window2) NumberButton windowButton;
    @Bind(R.id.beat1) NumberButton beatButton;
    @Bind(R.id.textVersion) BBTextView versionNumber;
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
//        as.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        as.stopMe();
    }


}
