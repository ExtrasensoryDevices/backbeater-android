package com.esdevices.backbeater.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.esdevices.backbeater.R;

/**
 * TODO: document your custom view class.
 */
public class SensitivitySlider extends View {
    private TextPaint mTextPaint;
    private Drawable low;
    private Drawable high;
    private float level = .76f;

    private int height = 0;
    private int width = 0;

    public SensitivitySlider(Context context) {
        super(context);
        init(null, 0);
    }

    public SensitivitySlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public SensitivitySlider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.SensitivitySlider, defStyle, 0);

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        low = getResources().getDrawable(R.drawable.sensitivity_low);
        high = getResources().getDrawable(R.drawable.sensitivity_high);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setColor(-1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;
        if (height != contentHeight && width != contentWidth) {
            height = contentHeight;
            width = contentWidth;
            low.setBounds(paddingLeft, paddingTop, contentHeight + paddingLeft, contentHeight+paddingTop);
            high.setBounds(width -paddingLeft-paddingRight-contentHeight, paddingTop, width-paddingRight, contentHeight+paddingTop);
        }
        low.draw(canvas);
        high.draw(canvas);



    }

}
