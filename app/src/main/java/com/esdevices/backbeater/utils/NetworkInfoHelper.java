package com.esdevices.backbeater.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.esdevices.backbeater.App;

/**
 * Created by Alina Kholcheva on 2017-02-27.
 */

public class NetworkInfoHelper {


public static boolean isNetworkAvailable() {
    ConnectivityManager cm = (ConnectivityManager) App.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
    return isConnected;
}
}
