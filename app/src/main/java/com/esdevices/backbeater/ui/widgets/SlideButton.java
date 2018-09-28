package com.esdevices.backbeater.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
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
    float controlTY = 0; // top Y of visible ui control (oval)
    float controlBY = 0; // bottom Y of visible ui control (oval)
    float textCY = 0;    // center Y of text in ui control
    
    // animation(expand, collapse, move up/down) starting points
    // values used to calculate animation speed
    float fromControlTY = 0; // visible oval top Y
    float fromControlBY = 0; // visible oval bottom Y
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
        float collapsedTY = viewCY - radius; // collapsed Top Y
        float collapsedBY = viewCY + radius; // collapsed Bottom Y
        float maxSideLength = W/2;
    
    
        // define collapsed click area, user only clicks on the visible part of the view
        clickArea = new Rect(0 ,(int) collapsedTY, (int)W, (int) collapsedBY);
    
        boolean isPressed = (eventStartY != 0) && (eventStartTime != 0);
        int borderColor = isPressed ? whiteColor : (selected ? assentColor : greyColor);
        int textColor = isPressed ? whiteColor : (selected ? whiteColor : greyColor);
    
    
        // deltaY = distance between MotionEvent.getY and view center Y (viewCY)
        // value sign defines stretch direction ('+' -> up, '-' -> down)
        // switching sign means switching expand direction
        float oldDeltaY = deltaY;
        deltaY = isPressed ? (eventY - viewCY) : 0;
        
        
        float toControlTY = collapsedTY;
        float toControlBY = collapsedBY;
        float toTextCY = viewCY;
    
        //Log.d("0", "----------------------");
        //Log.d("0", "Action: "+motionEventAction);
        //Log.d("0","viewCY = "+viewCY+", controlTY = "+controlTY+", controlBY = "+controlBY+", eventStartY = "+eventStartY+", eventY = "+eventY);
        //Log.d("0","collapsedTY = "+collapsedTY+", collapsedBY = "+collapsedBY);
    
        // -------------- define animation start state ------------------
    
        if (motionEventAction == MotionEvent.ACTION_DOWN) {
            // ACTION_DOWN - no stretch yet, only switch to white color
            // prepare for ACTION_MOVE, define visible control center Y (controlCY)
            controlTY = collapsedTY;
            controlBY = collapsedBY;
            fromControlTY = controlTY;
            fromControlBY = controlBY;
            toControlTY = collapsedTY;
            toControlBY = collapsedBY;
            //Log.d("1", "ACTION_DOWN: "+" controlTY = "+controlTY+", controlBY = "+controlBY);
        } else if (motionEventAction == MotionEvent.ACTION_MOVE) {
            // ACTION_MOVE - start stretching
    
            // define target position
            if (deltaY > 0) {
                toControlTY = collapsedTY - maxSideLength;
                toControlBY = collapsedBY;
                toTextCY = toControlTY + radius;
            } else {
                toControlTY = collapsedTY;
                toControlBY = collapsedBY + maxSideLength;
                toTextCY = toControlBY - radius;
            }
            
            if  (animationStartTime == 0) {
                // button just got pressed, after TAP_DURATION, define animation start parameters
                animationStartTime = now;
                fromControlTY = controlTY;
                fromControlBY = controlBY;
                fromTextCY = viewCY;
                //Log.d("1", "was animationStartTime == 0, now animationStartTime= "+animationStartTime);
            } else {
                // animation in progress or control expanded, user continues ACTION_MOVE
                if (oldDeltaY * deltaY <= 0){
                    // user finger crossed vertical center, switch stretch direction
                    animationStartTime = now;
                    fromControlTY = controlTY;
                    fromControlBY = controlBY;
                    fromTextCY = textCY;
                    ////Log.d("1", "switch: oldDeltaY = " + oldDeltaY + ", deltaY = " + deltaY + ", animationStartTime= "+animationStartTime);
                } else {
                    // stretch same direction/ do nothing
                    //Log.d("1", "same direction: oldDeltaY = " + oldDeltaY + ", deltaY = " + deltaY+ ", animationStartTime= "+animationStartTime);
                }
            }
        } else if ((motionEventAction == MotionEvent.ACTION_UP) && (controlTY != collapsedTY || controlBY != collapsedBY)){
            // ACTION_UP, if control was expanded, collapse it
            animationStartTime = now;
            fromControlTY = controlTY;
            fromControlBY = controlBY;
            fromTextCY = textCY;
            toControlTY = collapsedTY;
            toControlBY = collapsedBY;
            toTextCY = viewCY;
            motionEventAction = -1;
            //Log.d("1", "ACTION_UP, animationStartTime= "+animationStartTime);
        }
    
        //Log.d("1","controlTY = "+controlTY+", controlBY = "+controlBY);
    
    
    
        // dTa = deltaTimeAnimation = time since animation started
        long dTa = now - animationStartTime;
    
        boolean shouldPlayAnimation = animationStartTime != 0;
        
        if (shouldPlayAnimation) {
            if (dTa > ANIMATION_DURATION) {
                // animation time expired, define final position
                if (deltaY > 0) {
                    // completely expanded down
                    controlTY = collapsedTY - maxSideLength;
                    controlBY = collapsedBY;
                    textCY = toControlTY + radius;
                } else if (deltaY < 0) {
                    // completely expanded up
                    controlTY = collapsedTY;
                    controlBY = collapsedBY + maxSideLength;
                    textCY = toControlBY - radius;
                } else {
                    // completely collapsed
                    controlTY = collapsedTY;
                    controlBY = collapsedBY;
                    textCY = viewCY;
                }
                //Log.d("2","after anim: controlTY = "+controlTY+", controlBY = "+controlBY);
            } else {
                // playing animation, still collapsing or expanding, calculate length inversely proportional to dTa
                controlTY = fromControlTY + (float) dTa / (float) ANIMATION_DURATION * (toControlTY - fromControlTY);
                controlBY = fromControlBY + (float) dTa / (float) ANIMATION_DURATION * (toControlBY - fromControlBY);
                textCY = fromTextCY + (float) dTa / (float) ANIMATION_DURATION * (toTextCY - fromTextCY);
                //Log.d("2","inside anim: controlTY = "+controlTY+", controlBY = "+controlBY);
            }
        }
    
        
    
        // -------------- draw oval ------------------
        
        // oval current position is defined by controlCY and sideLength
        // current controlCY positioned between fromControlCY and toControlCY proportional to dTa
    
    
        // collapsed completely
        boolean isCollapsed =  (eventStartTime == 0 && !shouldPlayAnimation) ||(controlTY == collapsedTY && controlBY == collapsedBY); //(eventStartTime == 0  && !shouldPlayAnimation) ||
    
    
        // border
        if (isCollapsed) {
            // draw circle
            paint.setColor(borderColor);
            canvas.drawCircle(viewCX, viewCY, radius, paint);
        } else {
            // animating or fully expanded
    
            // flat side Y coordinates
            float sideTop = controlTY + radius;
            float sideBottom = controlBY - radius;
    
            // arc rectangles
            RectF arcRectTop = new RectF(0f, controlTY, W, sideTop+radius);
            RectF arcRectBottom = new RectF(0f, sideBottom-radius, W, controlBY);
    
            ////Log.d("3", "----------------------");
            //Log.d("3","controlTY = "+controlTY+", controlBY = "+controlBY);
            //Log.d("3","sideTop = "+sideTop+", sideBottom = "+sideBottom+", sideLength = "+(sideBottom-sideTop));
            //Log.d("3","arcRectTop    = "+arcRectTop + ", "+arcRectTop.width()+" x "+arcRectTop.height());
            //Log.d("3","arcRectBottom = "+arcRectBottom + ", "+arcRectBottom.width()+" x "+arcRectBottom.height());
            

        
            Path path = new Path();
            path.moveTo(0f, sideTop);
            path.arcTo(arcRectTop, 180, 180, false);
            path.lineTo(W, sideBottom);
            path.arcTo(arcRectBottom, 0, 180, false);
            path.close();
    
            canvas.drawPath(path, paint);
            
            paint.setColor(Color.RED);
            
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
            canvas.drawText(t, viewCX, textCY-textBounds.exactCenterY(), paint);
        }
        
        // reset stroke width
        paint.setStrokeWidth(strokeWidth);
    
    
    
        // -------------- invalidate if needed ------------------
    
        if (shouldPlayAnimation) {
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
        fromControlTY = 0;
        fromControlBY = 0;
        fromTextCY = 0;
        controlTY = 0; // center of visible ui control
        controlBY = 0; // center of visible ui control
        textCY = 0; // center of text in ui control
        animationStartTime = 0;
    }
    
    
    
    int motionEventAction = -1;
    boolean ignoreAction = false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        long now = System.currentTimeMillis();
        long clickDuration = 0;
    
        //motionEventAction = event.getAction();
        
        switch (event.getAction()){
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
                motionEventAction = MotionEvent.ACTION_DOWN;
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
                motionEventAction = MotionEvent.ACTION_MOVE;
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
                motionEventAction = MotionEvent.ACTION_UP;
                eventY = 0;
                eventStartTime = 0;
                tempSlideValue = 0;
                eventStartY = 0;
                break;
            default:
                //Log.v("slide","event "+event.getAction());
        }
        //Log.d("onTouchEvent", "Action: "+motionEventAction);
        invalidate();
        return true;
    }
}
