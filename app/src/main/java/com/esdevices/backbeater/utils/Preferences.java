package com.esdevices.backbeater.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.esdevices.backbeater.App;
import com.esdevices.backbeater.model.Song;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alina Kholcheva on 2017-06-29.
 */

public class Preferences {
    
    public static ArrayList<Song> readSongList;
    
    
    private final static String SOUND = "SOUND";
    private final static String WINDOW = "WINDOW";
    private final static String BEAT = "BEAT";
    private final static String SENSITIVITY = "SENSITIVITY";
    private final static String SONG_LIST = "SONG_LIST";
    
    private static SharedPreferences prefs;
    
    private synchronized static SharedPreferences getPrefs() {
        if (prefs == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(App.getContext());
        }
        return prefs;
    }
    
    
    
    public static int getSound(int defaultValue) {
        return getPrefs().getInt(SOUND, defaultValue);
    }
    public static int getWindow(int defaultValue) {
        return getPrefs().getInt(WINDOW, defaultValue);
    }
    
    public static int getBeat(int defaultValue) {
        return getPrefs().getInt(BEAT, defaultValue);
    }
    
    public static int getSensitivity(int defaultValue) {
        return getPrefs().getInt(SENSITIVITY, defaultValue);
    }
    
    public static List<Song> getSongList() {
        Gson gson = new Gson();
        String json = getPrefs().getString(SONG_LIST, "");
        Type type = new TypeToken<List<Song>>(){}.getType();
        ArrayList<Song> res = gson.fromJson(json, type);
        if (res == null) {
            res = new ArrayList();
        }
        return res;
    }
    
    
    
    public static void putSound(int value) {
        getPrefs().edit().putInt(SOUND, value).commit();
    }
    
    public static void putWindow(int value) {
        getPrefs().edit().putInt(WINDOW, value).commit();
    }
    
    public static void putBeat(int value) {
        getPrefs().edit().putInt(BEAT, value).commit();
    }
    
    public static void putSensitivity(int value) {
        getPrefs().edit().putInt(SENSITIVITY, value).commit();
    }
    
    public static void putSongList(List<Song> list) {
        Gson gson = new Gson();
        String json = gson.toJson(list);
        getPrefs().edit().putString(SONG_LIST, json).commit();
    }
    
    
    
}
