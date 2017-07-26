package com.esdevices.backbeater.utils;

import android.support.annotation.RawRes;
import com.esdevices.backbeater.R;

/**
 * Created by Alina Kholcheva on 2017-06-27.
 */

public class Constants {

public static final String HELP_URL = "http://backbeater.com/apphelp/?app=android";
public static final String BUY_SENSOR_URL = "http://backbeater.com/appbuy/?app=android";
    
public static final int DEFAULT_TEMPO = 120;
public static final int MAX_TEMPO = 200;
public static final int MIN_TEMPO = 20;
    
    
public enum Sound {
    SIDE_STICK(0, R.raw.side_stick),
    STICKS(1, R.raw.stick),
    METRONOME(2, R.raw.metronome),
    SURPRIZE(3, R.raw.surprise);
    
    public final int index;
    public final @RawRes int resourceId;
    
    Sound(int index, @RawRes int resourceId) {
        this.index = index;
        this.resourceId = resourceId;
    }
    
    @Override
    public String toString() {
        return "Sound("+index+")"+resourceId;
    }
    public static Sound fromIndex(final int index) {
        switch (index) {
            case 0: return SIDE_STICK;
            case 1: return STICKS;
            case 2: return METRONOME;
            case 3: return SURPRIZE;
            default: return SIDE_STICK;
        }
    }
    
}
    
}
