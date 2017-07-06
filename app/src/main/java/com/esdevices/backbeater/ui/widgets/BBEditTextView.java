package com.esdevices.backbeater.ui.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Created by Alina Kholcheva on 2017-06-29.
 */

public class BBEditTextView extends EditText {
    
    public BBEditTextView(Context context) {
        this(context, null);
    }
    
    public BBEditTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public BBEditTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if(!isInEditMode()) {
            setTypeface(Typeface.createFromAsset(getResources().getAssets(), "fonts/futura_round_demi.ttf"));
        }
    }
    
    
}
