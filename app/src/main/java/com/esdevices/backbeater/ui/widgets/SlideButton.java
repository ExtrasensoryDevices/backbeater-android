package com.esdevices.backbeater.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by aeboyd on 8/8/15.
 */
public class SlideButton extends View {
    private final float DP = isInEditMode()?3f:getResources().getDisplayMetrics().density;
    private static final int SLIDE_MAX =125;
    private static final int SLIDE_MIN =20;
    private int slideValue = 20;
    private int tempSlideValue = 0;
    private TextPaint p;
    private float eventStartY = 0;
    private final Rect textBounds = new Rect();


    public SlideButton(Context context) {
        this(context, null, 0);
    }

    public SlideButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setStrokeWidth(5*DP);
        p.setColor(-1);
        p.setStyle(Paint.Style.STROKE);
        p.setTextAlign(Paint.Align.CENTER);
        if(!isInEditMode()) {
            p.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "fonts/steelfish_rg.ttf"));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int cX = getWidth()/2;
        int cY = getWidth()/2;
        int radius = (int)(Math.min(cX,cY) - p.getStrokeWidth());
        canvas.drawCircle(cX,cY,radius,p);

        float stroke = p.getStrokeWidth();
        p.setStrokeWidth(0);
        String t = ""+ (tempSlideValue==0?slideValue:tempSlideValue);
        p.setTextSize(radius);
        p.getTextBounds(t,0,t.length(),textBounds);
        p.setColor(-1);
        canvas.drawText(t,cX,cY-textBounds.exactCenterY(),p);
        p.setStrokeWidth(stroke);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                eventStartY = event.getY();
            case MotionEvent.ACTION_MOVE:
                tempSlideValue = slideValue + (int)((eventStartY-event.getY())/(DP*5));
                tempSlideValue = Math.min(SLIDE_MAX,Math.max(SLIDE_MIN,tempSlideValue));
                Log.v("slide",tempSlideValue+"");
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                slideValue = tempSlideValue;
                tempSlideValue=0;
                eventStartY=0;
            default:
                Log.v("slide","event "+event.getAction());
        }
        return true;
    }
}
