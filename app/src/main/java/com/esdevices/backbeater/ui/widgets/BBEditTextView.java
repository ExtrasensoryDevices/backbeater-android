package com.esdevices.backbeater.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.support.v7.widget.AppCompatEditText;
import com.esdevices.backbeater.R;
import com.esdevices.backbeater.utils.Constants.BBTypeface;

/**
 * Created by Alina Kholcheva on 2017-06-29.
 */

public class BBEditTextView extends AppCompatEditText {
    
    private BBTypeface bbTypeface = BBTypeface.DEFAULT_TYPEFACE;
    
    
    
    public BBEditTextView(Context context) {
        this(context, null);
    }
    
    public BBEditTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public BBEditTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()) {
            return;
        }
    
        if (isInEditMode()) {
            return;
        }
        BBTypeface typeface = BBTypeface.DEFAULT_TYPEFACE;
        if (attrs != null) {
            // Load attributes
            TypedArray styleAttrs =
                getContext().obtainStyledAttributes(attrs, R.styleable.BBEditTextView, defStyleAttr, 0);
        
            if (styleAttrs != null) {
                final int fontIndex = styleAttrs.getInt(R.styleable.BBTextView_bbTypeface, BBTypeface.DEFAULT_TYPEFACE.index);
                typeface = BBTypeface.fromIndex(fontIndex);
            }
            styleAttrs.recycle();
        }
        setBBTypeface(typeface);
    }
    
    
    public void setBBTypeface(@NonNull BBTypeface bbTypeface) {
        this.bbTypeface = bbTypeface;
        super.setTypeface(bbTypeface.getTypeface(getContext()));
    }
    
    public BBTypeface getBBTypeface() {
        return bbTypeface;
    }
    
}
