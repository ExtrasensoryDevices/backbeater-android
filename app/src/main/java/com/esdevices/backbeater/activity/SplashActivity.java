package com.esdevices.backbeater.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.esdevices.backbeater.R;

public class SplashActivity extends Activity {

    private final int SPLASH_DISPLAY_LENGTH = 1800;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    
    
        // testing
        //Intent i = new Intent(SplashActivity.this, CalibrationActivity.class);
        //startActivity(i);
        //finish();
        //if (true) {return;}

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                overridePendingTransition( 0, 0);
                startActivity(intent);
                overridePendingTransition( 0, 0);
                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}