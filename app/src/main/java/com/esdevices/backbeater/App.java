package com.esdevices.backbeater;


import android.content.Context;

/**
 * Created by Alina Kholcheva on 2017-02-21.
 */

public class App extends android.app.Application{ //} implements Application.ActivityLifecycleCallbacks {

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


    //private static final int USER_QUIT_TIMEOUT = 1000;
    //private long sessionStarted;
    //private Handler handler;
    //private Runnable runReportUserQuit = new Runnable() {
    //    @Override
    //    public void run() {
    //        long now = DateTimeHelper.getCurrentTimeStamp();
    //        long sessionDuration = (now - sessionStarted - USER_QUIT_TIMEOUT) / 1000;
    //        Log.d("SESSION_DURATION", ""+sessionDuration);
    //        EventReporter.getInstance().trackSessionDurationEvent(sessionDuration);
    //    }
    //};


    @Override
    public void onCreate() {
        super.onCreate();

        //sessionStarted = DateTimeHelper.getCurrentTimeStamp();

        //registerActivityLifecycleCallbacks(this);
        //handler = new Handler(getMainLooper());
    }

    //@Override
    //public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    //    handler.removeCallbacks(runReportUserQuit);
    //}
    //
    //@Override
    //public void onActivityStarted(Activity activity) {
    //    handler.removeCallbacks(runReportUserQuit);
    //}
    //
    //@Override
    //public void onActivityResumed(Activity activity) {
    //    handler.removeCallbacks(runReportUserQuit);
    //}
    //
    //@Override
    //public void onActivityPaused(Activity activity) {
    //    handler.postDelayed(runReportUserQuit, USER_QUIT_TIMEOUT);
    //}
    //
    //@Override
    //public void onActivityStopped(Activity activity) {}
    //
    //@Override
    //public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    //
    //@Override
    //public void onActivityDestroyed(Activity activity) { }
    //
    //public String getVersionName() {
    //    String versionName = "";
    //    try {
    //        PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
    //        versionName = pInfo.versionName;
    //    } catch (PackageManager.NameNotFoundException e) {
    //        e.printStackTrace();
    //    }
    //    return versionName;
    //}
    //
    //public int getVersionCode() {
    //    int versionCode= 0;
    //    try {
    //        PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
    //        versionCode = pInfo.versionCode;
    //    } catch (PackageManager.NameNotFoundException e) {
    //        e.printStackTrace();
    //    }
    //    return versionCode;
    //}
}