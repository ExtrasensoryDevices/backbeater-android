package com.esdevices.backbeater.ui.widgets;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import android.widget.Button;
import com.esdevices.backbeater.R;
import com.esdevices.backbeater.utils.Constants;

/**
 * Created by aeboyd on 7/15/15.
 */
public class NumberButton extends Button implements View.OnClickListener {
    TextPaint paint;
    private final static int TEXT_COLOR = -1;
    private final int BACK_COLOR;
    private float transistionPct = 0f;
    private boolean on = false;
    private final Rect textBounds = new Rect();
    private OnClickListener listener;
    public NumberButton(Context context) {
        this(context, null, 0);
    }

    public NumberButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        BACK_COLOR = getResources().getColor(R.color.assent_color);
        paint.setColor(-1);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(getResources().getDimension(R.dimen.circle_outline_width));
        if (!isInEditMode()) {
            paint.setTypeface(Constants.BBTypeface.FUTURA_ROUND_DEMI.getTypeface(context));
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(getTextSize());
        paint.getTextBounds("2", 0, 1, textBounds);
        super.setOnClickListener(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int cX = getWidth() / 2;
        int cY = getHeight() / 2;
        int backColor = mixTwoColors(TEXT_COLOR, BACK_COLOR, transistionPct);
        int textColor = mixTwoColors(BACK_COLOR, TEXT_COLOR, transistionPct);
        float stroke = paint.getStrokeWidth();
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(backColor);
        canvas.drawCircle(cX, cY, cY - paint.getStrokeWidth(), paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(TEXT_COLOR);
        canvas.drawCircle(cX, cY, cY - paint.getStrokeWidth(), paint);

        paint.setColor(textColor);
        paint.setStrokeWidth(0);
        canvas.drawText(getText().toString(), cX, cY - textBounds.centerY(), paint);
        paint.setStrokeWidth(stroke);

    }

    public void setTransistionPct(float pct) {
        transistionPct = pct;
        invalidate();
    }


    public static int mixTwoColors(int color1, int color2, float amount) {
        final byte ALPHA_CHANNEL = 24;
        final byte RED_CHANNEL = 16;
        final byte GREEN_CHANNEL = 8;
        final byte BLUE_CHANNEL = 0;

        final float inverseAmount = 1.0f - amount;

        int a = ((int) (((float) (color1 >> ALPHA_CHANNEL & 0xff) * amount) +
                ((float) (color2 >> ALPHA_CHANNEL & 0xff) * inverseAmount))) & 0xff;
        int r = ((int) (((float) (color1 >> RED_CHANNEL & 0xff) * amount) +
                ((float) (color2 >> RED_CHANNEL & 0xff) * inverseAmount))) & 0xff;
        int g = ((int) (((float) (color1 >> GREEN_CHANNEL & 0xff) * amount) +
                ((float) (color2 >> GREEN_CHANNEL & 0xff) * inverseAmount))) & 0xff;
        int b = ((int) (((float) (color1 & 0xff) * amount) +
                ((float) (color2 & 0xff) * inverseAmount))) & 0xff;

        return a << ALPHA_CHANNEL | r << RED_CHANNEL | g << GREEN_CHANNEL | b << BLUE_CHANNEL;
    }

    public void enable(boolean state){
        if (!state) {
            ObjectAnimator.ofFloat(this, "transistionPct", transistionPct, 0).start();
            on = false;
        } else {
            on = true;
            ObjectAnimator.ofFloat(this, "transistionPct", transistionPct, 1).start();
        }
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        super.setOnClickListener(l);
    }

    @Override
    public void onClick(View v) {
        enable(!on);
        if(listener!=null){
            listener.onClick(v);
        }
    }
}
