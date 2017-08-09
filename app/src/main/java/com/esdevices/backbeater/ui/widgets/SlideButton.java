package com.esdevices.backbeater.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
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
    
    //private enum State {COLLAPSED, EXPANDED_UP, EXPANDED_DOWN, EXPANDING_UP, EXPANDING_DOWN, MOVING_UP, MOVING_DOWN }
    
    //private State state = State.COLLAPSED;
    
    private final float DP = isInEditMode()?3f:getResources().getDisplayMetrics().density;
    private static final int TAP_TIMEOUT = 180;
    private static final long ANIMATION_DURATION = 300;
    
    private int slideValue = Constants.DEFAULT_TEMPO;
    private int tempSlideValue = 0;
    private TextPaint paint;
    private final Rect textBounds = new Rect();
    private float strokeWidth = 0;
    
    private long eventStartTime = 0;
    private float eventStartY = 0;
    private float eventY = 0;
    
    private int greyColor;
    private int whiteColor = -1;
    private int assentColor;
    
    private boolean selected = false;
    
    private StateChangeListener stateChangeListener;
    
    private Rect clickArea = null;
    
    
    


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
        strokeWidth = getResources().getDimension(R.dimen.circle_outline_width);
        paint.setStrokeWidth(strokeWidth);
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
    
    
    // deltaY = distance between MotionEvent.getY and view center y (viewCY), saved on prev ACTION_MOVE
    // value sign defines stretch direction (+ -> up, - -> down)
    float deltaY = 0;
    
    // current vertical center of oval and text, oval position is based on this value
    float controlCY = 0; // center Y of visible ui control (oval)
    float textCY = 0;    // center Y of text in ui control
    
    // animation(expand, collapse, move up/down) starting points
    // values used to calculate animation speed
    float fromControlCY = 0; // visible oval center Y
    float fromTextCY = 0;    // text center Y
    long animationStartTime = 0;  // animation starting time
    
    @Override
    protected void onDraw(Canvas canvas) {
        long now = System.currentTimeMillis();
    
        float W = getWidth();  // view width
        float H = getHeight(); // view height, expect W:H = 1:2
        float viewCX = W/2;    // view center X
        float viewCY = H/2;    // view center Y
        int radius = (int) (Math.min(viewCX, viewCY));
    
        // define collapsed click area, user only clicks on the visible part of the view
        clickArea = new Rect(0 ,(int)(viewCY-radius), (int)W, (int)(viewCY+radius));
    
        boolean isPressed = (eventStartY != 0) && (eventStartTime != 0);
        int borderColor = isPressed ? whiteColor : (selected ? assentColor : greyColor);
        int textColor = isPressed ? whiteColor : (selected ? whiteColor : greyColor);
    
    
        // deltaY = distance between MotionEvent.getY and view center Y (viewCY)
        // value sign defines stretch direction ('+' -> up, '-' -> down)
        // switching sign means switching expand direction
        float oldDeltaY = deltaY;
        deltaY = eventY - viewCY;
        
        
        // -------------- define animation start state ------------------
    
        if (motionEventAction == MotionEvent.ACTION_DOWN) {
            // ACTION_DOWN - no stretch yet, only switch to white color
            // prepare for ACTION_MOVE, define visible control center Y (controlCY)
            controlCY = viewCY;
            Log.d("1", "ACTION_DOWN: controlCY: " + controlCY);
        } else if (motionEventAction == MotionEvent.ACTION_MOVE) {
            // ACTION_MOVE - start stretching
            if  (animationStartTime == 0) {
                // button just got pressed, after TAP_DURATION, define animation start parameters
                animationStartTime = now;
                fromControlCY = controlCY;
                fromTextCY = controlCY;
                Log.d("1", "was animationStartTime == 0, now animationStartTime= "+animationStartTime);
            } else {
                // animation in progress or control expanded, user continues ACTION_MOVE
                if (oldDeltaY * deltaY <= 0){
                    // user finger crossed vertical center, switch stretch direction
                    animationStartTime = now;
                    fromControlCY = controlCY;
                    fromTextCY = textCY;
                    Log.d("1", "switch: oldDeltaY = " + oldDeltaY + ", deltaY = " + deltaY + ", animationStartTime= "+animationStartTime);
                } else {
                    // stretch same direction/ do nothing
                    Log.d("1", "same direction: oldDeltaY = " + oldDeltaY + ", deltaY = " + deltaY+ ", animationStartTime= "+animationStartTime);
                }
            }
        } else if (controlCY != viewCY && motionEventAction == MotionEvent.ACTION_UP){
            // ACTION_UP, if control was expanded, collapse it
            animationStartTime = now;
            fromControlCY = controlCY;
            fromTextCY = textCY;
            motionEventAction = -1;
            Log.d("1", "ACTION_UP, animationStartTime= "+animationStartTime);
            
        }
    
        // dTa = deltaTimeAnimation = time since animation started
        long dTa = now - animationStartTime;
    
    
        // -------------- calculate flat side of an oval depending on time since animation started (dTa) ------------------
    
        float sideLength; // always positive
        if (motionEventAction == MotionEvent.ACTION_DOWN) {
            // user tapped, no stretch yet
            sideLength = 0;
        } else if (isPressed) {
            // expaning now or expanded
            if (dTa > ANIMATION_DURATION) {
                // animation time expired, state expanded, maximum stretch
                sideLength = W/2;
            } else {
                // playing animation, still expanding, calculate length proportional to dTa
                sideLength = Math.round((float) dTa / (float) ANIMATION_DURATION * W/2);
            }
            Log.d("2", "isPressed");
        } else {
            // collapsing now or collapsed
            if (dTa > ANIMATION_DURATION) {
                // animation time expired, state collapsed, 0 stretch
                sideLength = 0;
            } else {
                // playing animation, still collapsing, calculate length inversely proportional to dTa
                sideLength = Math.round((1f - (float) dTa / (float) ANIMATION_DURATION) * W/2);
            }
            Log.d("2", "not pressed");
        }
        Log.d("2", "sideLength = "+sideLength);
    
    
        // -------------- draw oval ------------------
        
        // oval current position is defined by controlCY and sideLength
        // current controlCY positioned between fromControlCY and toControlCY proportional to dTa
        
        // collapsed completely
        boolean isCollapsed = (sideLength == 0);// motionEventAction == MotionEvent.ACTION_DOWN || (!isPressed && dTa > ANIMATION_DURATION);
    
        // border
        if (isCollapsed) {
            // draw circle
            paint.setColor(borderColor);
            canvas.drawCircle(viewCX, viewCY, radius, paint);
        } else {
            // animating or fully expanded
            
            // toControlCY - target control center Y position at the end of the animation
            float toControlCY = viewCY + (deltaY >= 0 ? -W/2 : W/2);
            if (dTa > ANIMATION_DURATION) {
                // animation ended, move to the target position
                controlCY = toControlCY;
            } else {
                // still animating
                // calculate current control center Y position proportional to dTa,
                // between fromControlCY and toControlCY
                controlCY = fromControlCY + (float)dTa/(float)ANIMATION_DURATION * (toControlCY - fromControlCY);
                Log.d("3", "controlCY = "+controlCY + ", toControlCY="+toControlCY+", fromY="+fromControlCY + ", fraction="+((float)dTa/(float)ANIMATION_DURATION)+ ", animationStartTime= "+animationStartTime+ ", dTa= "+dTa);
            }
            
            // flat side Y coordinates
            float sideTop = controlCY - sideLength/2;
            float sideBottom = controlCY + sideLength/2;
            // arc rectangles
            RectF arcRectTop = new RectF(0f, sideTop-(float)radius, W, sideTop+(float)radius);
            RectF arcRectBottom = new RectF(0f, sideBottom-(float)radius, W, sideBottom+(float)radius);
            
            Log.d("3","viewCY = "+viewCY+", controlCY = "+controlCY+", radius = "+radius);
            Log.d("3","sideTop = "+sideTop+", sideBottom = "+sideBottom+", sideLength = "+sideLength);
            Log.d("3","arcRectTop    = "+arcRectTop + ", "+arcRectTop.width()+" x "+arcRectTop.height());
            Log.d("3","arcRectBottom = "+arcRectBottom + ", "+arcRectBottom.width()+" x "+arcRectBottom.height());
            

        
            Path path = new Path();
            path.moveTo(0f, sideTop);
            path.arcTo(arcRectTop, 180, 180, false);
            path.lineTo(W, sideBottom);
            path.arcTo(arcRectBottom, 0, 180, false);
            path.close();
    
            
            canvas.drawPath(path, paint);
            
        }
    
    
        // -------------- draw text ------------------
    
        
        paint.setStrokeWidth(0);
        paint.setColor(textColor);
        String t = ""+ (tempSlideValue==0 ? slideValue : tempSlideValue);
        paint.setTextSize(radius);
        paint.getTextBounds(t,0,t.length(),textBounds);
        
        
        if (isCollapsed) {
            // draw text in the center
            canvas.drawText(t, viewCX, viewCY - textBounds.exactCenterY(), paint);
        } else {
            // expanded
            float toTextCY = controlCY + textBounds.height()/2;
            if (deltaY >= 0) {
                toTextCY = toTextCY - sideLength/2;
            } else {
                toTextCY = toTextCY + sideLength/2;
            }

            if (dTa > ANIMATION_DURATION) {
                // animation ended, move to the target position
                textCY = toTextCY;
            } else {
                // still animating
                // calculate current text center Y position proportional to dTa,
                // between fromTextCY and toTextCY
                textCY = fromTextCY + (float)dTa/(float)ANIMATION_DURATION * (toTextCY - fromTextCY); //fromTextCY +
            }
            canvas.drawText(t, viewCX, textCY, paint);
        }
        
        // reset stroke width
        paint.setStrokeWidth(strokeWidth);
    
    
    
        // -------------- invalidate if needed ------------------
    
        if (animationStartTime != 0) {
            if (dTa < ANIMATION_DURATION) {
                // re-draw for animation
                invalidate();
            } else if (!isPressed) {
                // animation completed and view is not pressed, reset and re-draw to initial state
                reset();
                invalidate();
            }
        }
    }
    
    
    private void reset() {
        eventY = 0;
        eventStartTime = 0;
        tempSlideValue = 0;
        eventStartY = 0;
        motionEventAction = -1;
        deltaY = 0;
        fromControlCY = 0;
        fromTextCY = 0;
        controlCY = 0; // center of visible ui control
        textCY = 0; // center of text in ui control
        animationStartTime = 0;
    }
    
    
    
    int motionEventAction = -1;
    boolean ignoreAction = false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        long now = System.currentTimeMillis();
        long clickDuration = 0;
        motionEventAction = event.getAction();
        
        switch (motionEventAction){
            case MotionEvent.ACTION_DOWN:
                // check if user tapped on visible circle (view height is twice the initial circle)
                if (clickArea != null && clickArea.contains((int)event.getX(),(int)event.getY())) {
                    // user tapped on visible circle
                    ignoreAction = false;
                } else {
                    // user tapped at outside the visible circle, ignore following ACTION_MOVE and  ACTION_UP
                    ignoreAction = true;
                    return true;
                }
                eventStartY = event.getY();
                eventStartTime = now;
                tempSlideValue = slideValue;
                break;
            case MotionEvent.ACTION_MOVE:
                clickDuration = now - eventStartTime;
                if(ignoreAction || clickDuration < TAP_TIMEOUT) {
                    // ignore tap outside the visible circle
                    // ignore ACTION_MOVE for TAP_TIMEOUT milliseconds, may be it is TAP event
                    return true;
                }
                eventY = event.getY();
                tempSlideValue = slideValue + (int) ((eventStartY-eventY) /(DP*5));
                tempSlideValue = Math.min(Constants.MAX_TEMPO, Math.max(Constants.MIN_TEMPO, tempSlideValue));
                break;
            case MotionEvent.ACTION_UP:
                if(ignoreAction) {
                    // ignore tap outside the visible circle
                    return true;
                }
                clickDuration = now - eventStartTime;
                if(clickDuration <= TAP_TIMEOUT) {
                    // TAP event has occurred, ignore value changed
                    handleClick();
                } else if (slideValue != tempSlideValue) {
                    // handle swipe
                    slideValue = tempSlideValue;
                    handleValueChange();
                    
                }
                eventY = 0;
                eventStartTime = 0;
                tempSlideValue = 0;
                eventStartY = 0;
                break;
            default:
                Log.v("slide","event "+event.getAction());
        }
        invalidate();
        return true;
    }
}
