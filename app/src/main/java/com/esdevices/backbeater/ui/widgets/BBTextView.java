package com.esdevices.backbeater.ui.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.support.v7.widget.AppCompatTextView;
import com.esdevices.backbeater.R;

import com.esdevices.backbeater.utils.Constants.BBTypeface;

/**
 * Created by aeboyd on 7/15/15.
 */
public class BBTextView extends AppCompatTextView {
    
    private BBTypeface bbTypeface = BBTypeface.DEFAULT_TYPEFACE;
    
    
    public BBTextView(Context context) {
        this(context, null, 0);
    }

    public BBTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    @TargetApi(21)
    public BBTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context, attrs, defStyleAttr);
    }

    public BBTextView(Context context, AttributeSet attrs, int defStyleAttr) {
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
                getContext().obtainStyledAttributes(attrs, R.styleable.BBTextView, defStyleAttr, 0);
        
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
    
    
    // redraw if does not fit
    
    boolean redraw = false;
    int redrawC = 0;
    
    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getLineCount() > getMaxLines() && redrawC < 3) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, getTextSize() - 2);
            redraw = true;
            redrawC++;
        }
    }
    
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (redraw) {
            requestLayout();
            redraw = false;
        }
    }
}
