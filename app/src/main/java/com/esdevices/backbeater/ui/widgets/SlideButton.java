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
import com.esdevices.backbeater.R;
import com.esdevices.backbeater.utils.Constants;

/**
 * Created by aeboyd on 8/8/15.
 */
public class SlideButton extends View {
    
    public interface StateChangeListener {
        void onToggle(boolean isOn);
        void onValueChanged(int newValue);
    }
    
    private final float DP = isInEditMode()?3f:getResources().getDisplayMetrics().density;
    private static final int MAX_CLICK_DURATION = 200;
    
    private int slideValue = Constants.DEFAULT_TEMPO;
    private int tempSlideValue = 0;
    private TextPaint paint;
    private final Rect textBounds = new Rect();
    
    private long eventStartTime = 0;
    private float eventStartY = 0;
    
    private int greyColor;
    private int whiteColor = -1;
    private int assentColor;
    
    private boolean selected = false;
    
    private StateChangeListener stateChangeListener;


    public SlideButton(Context context) {
        this(context, null, 0);
    }

    public SlideButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        
        greyColor = context.getResources().getColor(R.color.grey_color);
        assentColor = context.getResources().getColor(R.color.assent_color);
        
        paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(getResources().getDimension(R.dimen.circle_outline_width));
        paint.setStyle(Paint.Style.STROKE);
        paint.setTextAlign(Paint.Align.CENTER);
        if(!isInEditMode()) {
            paint.setTypeface(Constants.BBTypeface.FUTURA_ROUND_BOOK.getTypeface(context));
        }
    }
    
    public void setStateChangeListener(StateChangeListener stateChangeListener) {
        this.stateChangeListener = stateChangeListener;
    }
    
    public int getValue() {
        return slideValue;
    }
    
    public void setValue(int value) {
        this.slideValue = value;
        invalidate();
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public void toggle(){
        selected = !selected;
        invalidate();
    }
    
    private void handleClick(){
        toggle();
        if (stateChangeListener != null) {
            stateChangeListener.onToggle(selected);
        }
    }
    
    private void handleValueChange(){
        if (stateChangeListener != null) {
            stateChangeListener.onValueChanged(slideValue);
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        paint.setColor(selected ? assentColor : greyColor);
    
        int cX = getWidth()/2;
        int cY = getWidth()/2;
        int radius = (int)(Math.min(cX,cY) - paint.getStrokeWidth());
        canvas.drawCircle(cX,cY,radius, paint);

        float oldStrokeWidth = paint.getStrokeWidth();
        paint.setStrokeWidth(0);
        paint.setColor(selected ? whiteColor : greyColor);
        String t = ""+ (tempSlideValue==0 ? slideValue : tempSlideValue);
        paint.setTextSize(radius);
        paint.getTextBounds(t,0,t.length(),textBounds);
        canvas.drawText(t,cX,cY-textBounds.exactCenterY(), paint);
        paint.setStrokeWidth(oldStrokeWidth);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                eventStartY = event.getY();
                eventStartTime = System.currentTimeMillis();
                tempSlideValue = slideValue;
                break;
            case MotionEvent.ACTION_MOVE:
                tempSlideValue = slideValue + (int)((eventStartY-event.getY())/(DP*5));
                tempSlideValue = Math.min(Constants.MAX_TEMPO, Math.max(Constants.MIN_TEMPO, tempSlideValue));
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                boolean valueChanged = slideValue != tempSlideValue;
                slideValue = tempSlideValue;
                tempSlideValue = 0;
                eventStartY = 0;
                long clickDuration = System.currentTimeMillis() - eventStartTime;
                if(clickDuration < MAX_CLICK_DURATION) {
                    //click event has occurred
                    handleClick();
                }
                eventStartTime = 0;
                
                if (valueChanged) {
                    handleValueChange();
                }
                break;
            default:
                Log.v("slide","event "+event.getAction());
        }
        return true;
    }
}
