package com.esdevices.backbeater.ui.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by aeboyd on 7/15/15.
 */
public class BBTextView extends TextView {
    public BBTextView(Context context) {
        this(context, null, 0);
    }

    public BBTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BBTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if(!isInEditMode()) {
            setTypeface(Typeface.createFromAsset(getResources().getAssets(), "fonts/futura_round_demi.ttf"));
        }
    }
}
