package com.esdevices.backbeater;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.crashlytics.android.Crashlytics;
import com.esdevices.backbeater.activity.MainActivity;
import com.esdevices.backbeater.activity.SplashActivity;
import com.esdevices.backbeater.utils.Constants;
import com.flurry.android.FlurryAgent;
import com.flurry.android.FlurryAgentListener;
import io.fabric.sdk.android.Fabric;
import java.util.Date;

/**
 * Created by Alina Kholcheva on 2017-02-21.
 */

public class App extends android.app.Application implements Application.ActivityLifecycleCallbacks {

    private static App instance;
    public App() {
        instance = this;
    }

    public static App getInstance() {
        return instance;
    }
    public static Context getContext() {
        return instance.getApplicationContext();
    }


    @Override
    public void onCreate() {
        super.onCreate();
    
    
        registerActivityLifecycleCallbacks(this);
    
        new FlurryAgent.Builder()
            .withLogEnabled(false)
            .withCaptureUncaughtExceptions(false)
            .withContinueSessionMillis(10000)
            .withListener(new FlurryAgentListener() {
                @Override public void onSessionStarted() {
        
                }
            })
            .build(this, getContext().getResources().getString(R.string.flurry_app_key));
    
        Fabric.with(this, new Crashlytics());
    }
    
    
    private boolean mainActivityIsInBackground = false;
    private static final int USER_QUIT_TIMEOUT = 500;
    private long sessionStarted;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runReportUserQuit = new Runnable() {
        @Override
        public void run() {
            long now = new Date().getTime();
            long sessionDuration = (now - sessionStarted) / 1000;
            FlurryAgent.logEvent(Constants.FLURRY_APP_CLOSED, Constants.buildFlurryParams("sessionLength", ""+sessionDuration));
            Log.d("Flurry", Constants.FLURRY_APP_CLOSED);
            // IMPORTANT!!!
            // TIMED EVENTS exist but duration does not show up anywhere on the Flurry dashboard
            // that is why we do not use timed events here
            //FlurryAgent.endTimedEvent(Constants.FLURRY_APP_CLOSED);
            FlurryAgent.onEndSession(getContext());
        }
    };

    
    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        handler.removeCallbacks(runReportUserQuit);
    }
    
    @Override public void onActivityStarted(Activity activity) {
        handler.removeCallbacks(runReportUserQuit);
        if (activity instanceof SplashActivity) {
            // ignore
        } else if (activity instanceof MainActivity) {
            if (mainActivityIsInBackground) {
                mainActivityIsInBackground = false;
            } else {
                sessionStarted = new Date().getTime();
                Log.d("Flurry", Constants.FLURRY_APP_OPENED);
                FlurryAgent.onStartSession(getContext());
                FlurryAgent.logEvent(Constants.FLURRY_APP_OPENED);
                // IMPORTANT!!!
                // TIMED EVENTS exist but duration does not show up anywhere on the Flurry dashboard
                // that is why we do not use timed events here
                //FlurryAgent.logEvent(Constants.FLURRY_APP_CLOSED, true);
            }
        } else {
            mainActivityIsInBackground = true;
        }
    }
    
    @Override public void onActivityResumed(Activity activity) {
        handler.removeCallbacks(runReportUserQuit);
    }
    
    @Override public void onActivityPaused(Activity activity) {
        handler.postDelayed(runReportUserQuit, USER_QUIT_TIMEOUT);
    }
    
    @Override public void onActivityStopped(Activity activity) {}
    
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    
    @Override public void onActivityDestroyed(Activity activity) {}
}