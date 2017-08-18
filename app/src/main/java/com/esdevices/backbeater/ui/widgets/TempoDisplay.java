package com.esdevices.backbeater.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.support.v7.widget.AppCompatTextView;

import com.esdevices.backbeater.R;

import com.esdevices.backbeater.audio.MetronomePlayer;
import com.esdevices.backbeater.audio.WindowQueue;
import com.esdevices.backbeater.utils.Constants;
import com.esdevices.backbeater.utils.Constants.Sound;
import com.esdevices.backbeater.utils.Preferences;

/**
 * Created by aeboyd on 7/15/15.
 */
public class TempoDisplay extends AppCompatTextView {
    
    public static final long MS_IN_MIN = 60000;
    public static final int WHITE_COLOR = -1;
    private final int accentColor;
    private final AnimationDrawable drum;
    private final TextPaint paint;
    private int height = 0;
    private int width = 0;
    private final Rect textBounds = new Rect();
    private Rect drumBounds = new Rect();
    private static final float PCT_DRUM = .4f;
    private static final float DRUM_ANIMATION_DURATION = 600f;
    private final int DRUM_ANIMATION_FRAMES;
    private boolean leftStrike = false;
    private static final float DRUM_PULSE_ANIMATION_DURATION = DRUM_ANIMATION_DURATION/10f;
    private final float DRUM_PULSE_DELTA_Y;
    
    
    
    private int CPT = Constants.DEFAULT_TEMPO;  // = Currently Playing Tempo (multiplied by Beat, averaged in Window) or Metronome Tempo
    private int metronomeTempo = Constants.DEFAULT_TEMPO;
    
    private MetronomePlayer metronome;
    
    private int beat = 1;
    private int window = 5;
    private WindowQueue windowQueue;
    
    public boolean handleTap = true;
    
    
    
    
    public TempoDisplay(Context context) {
        this(context, null, 0);
    }

    public TempoDisplay(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TempoDisplay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        
        beat = Preferences.getBeat(beat);
        window = Preferences.getWindow(window);
        CPT = Preferences.getCPT(CPT);
        windowQueue = new WindowQueue(window);
    
        accentColor = getResources().getColor(R.color.assent_color);
        
        drum = (AnimationDrawable)getResources().getDrawable(R.drawable.drum_hit_animation);
        int totalFrames = drum.getNumberOfFrames();
        DRUM_ANIMATION_FRAMES = totalFrames / 2; // one pass = half of the frames
        DRUM_PULSE_DELTA_Y = getResources().getDimension(R.dimen.drum_animation_delta_y);
        drum.selectDrawable(totalFrames - 1); // last frame
        
        paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(WHITE_COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(getResources().getDimension(R.dimen.circle_outline_width));
        if(!isInEditMode()) {
            paint.setTypeface(Constants.BBTypeface.STEELFISH.getTypeface(context));
        }
        paint.setTextAlign(Paint.Align.CENTER);
    }
    
    //================================================================================
    //  Window queue settings
    //================================================================================
    
    
    
    public void setWindow(int window) {
        if (this.window != window) {
            reset();
            this.window = window;
            windowQueue.setCapacity(window);
        }
    }
    
    public void setBeat(int beat) {
        if (this.beat != beat) {
            reset();
            this.beat = beat;
        }
    }
    
    //================================================================================
    //  Currently Playing Tempo (use for saving state)
    //================================================================================
    
    public int getCPT() {
        return CPT;
    }
    
    
    //================================================================================
    //  Drawing
    //================================================================================
    
    
    
    @Override
    protected void onDraw(Canvas canvas) {
        boolean metronomeIsOn = isMetronomeOn();
        
        int contentWidth = getWidth()-getPaddingLeft()-getPaddingRight();
        int contentHeight = getHeight()-getPaddingBottom()-getPaddingTop();
        
        // size of the CPT display
        if (height != contentHeight || width != contentWidth) {
            height = contentHeight;
            width = contentWidth;
            int drumL = (int) (.5*width*(1-PCT_DRUM)+getPaddingLeft());
            int drumT = getPaddingTop();
            int drumR = (int) (.5*width*(1+PCT_DRUM)+getPaddingLeft());
            int drumB =(int)(width*PCT_DRUM*drum.getIntrinsicHeight()/drum.getIntrinsicWidth()+getPaddingTop());
            drumBounds = new Rect(drumL, drumT, drumR, drumB);
            drum.setBounds(drumBounds);
    
        }
        
        // (cX, cY), radius - centre and radius of the big circle,
        int cX = width/2+getPaddingLeft();
        int radius = (int) (width/2-paint.getStrokeWidth());
        if(height-drumBounds.height()/2<getWidth()){
            radius = (height - drumBounds.height()/2)/2- getPaddingLeft();
        }
        long now = System.currentTimeMillis();
        long timeSinceLastBeat = now - lastBeatTime;
        long timeSinceLastTimerBeat = now - lastTimerBeatTime;
        
        int cY = drumBounds.height()/2+radius+getPaddingTop();
        
        
        // beat - beat registered
        // hit - beat in time accourding to CPT
        boolean beat = Constants.isValidTempo(CPT) && timeSinceLastBeat < DRUM_ANIMATION_DURATION;
    
        // draw big circle
        if (beat && hit){
            // hit in right time
            
            // draw big circle (flash white)
            float halfDrumAnimation = DRUM_ANIMATION_DURATION / 2f;
            paint.setColor(NumberButton.mixTwoColors(accentColor, WHITE_COLOR,
                Math.abs((timeSinceLastBeat-halfDrumAnimation)/halfDrumAnimation)));
            canvas.drawCircle(cX, cY, radius, paint);
            paint.setColor(accentColor);
            
            // select drim animation frame
            int drumAnimationFrameIndex = (int) (timeSinceLastBeat / DRUM_ANIMATION_DURATION * DRUM_ANIMATION_FRAMES);
            if(!leftStrike) {
                // right strike -> offset=DRUM_ANIMATION_FRAMES
                drumAnimationFrameIndex = drumAnimationFrameIndex + DRUM_ANIMATION_FRAMES;
            }
            drum.selectDrawable(drumAnimationFrameIndex);
        }else {
            // missed the hit time
            
            // draw big circle with accent color
            paint.setColor(accentColor);
            canvas.drawCircle(cX, cY, radius, paint);
    
            // draw fading white circle
            if (beat && offDegree > 0) {
                paint.setColor(NumberButton.mixTwoColors(accentColor, WHITE_COLOR, timeSinceLastBeat / DRUM_ANIMATION_DURATION));
                paint.setAlpha((int) (255 - (timeSinceLastBeat / DRUM_ANIMATION_DURATION) * 255));
                int ocX = (int) (radius * Math.sin(offDegree) + cX);
                int ocY = (int) (radius * Math.cos(offDegree) + cY);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(ocX, ocY, 3 * paint.getStrokeWidth(), paint);
                // reset paint color
                paint.setColor(accentColor);
                paint.setAlpha(255);
            }
    
            drum.selectDrawable(leftStrike ? DRUM_ANIMATION_FRAMES-1 : 2*DRUM_ANIMATION_FRAMES-1);
        }
        
        // drum pulse animation
        if (beat && timeSinceLastBeat < DRUM_PULSE_ANIMATION_DURATION) {

            Rect drumPulseBounds = drumBounds;

            float halfDrumPulseAnimation = DRUM_PULSE_ANIMATION_DURATION / 2f;
            float timeElapsed = (float) timeSinceLastBeat - halfDrumPulseAnimation;
            float coefficient = 0;
            if (timeElapsed <= halfDrumPulseAnimation) {
                 // increase 0 -> 1
                coefficient = timeElapsed / halfDrumPulseAnimation;
            } else {
                // decrease 1 -> 0
                coefficient = timeElapsed / halfDrumPulseAnimation - 1f;
            }

            int deltaY = (int) (coefficient * DRUM_PULSE_DELTA_Y);
            int deltaX = (int) ((float)deltaY*(float)drumBounds.width() / (float)drumBounds.height());

            drumPulseBounds = new Rect(drumPulseBounds.left+deltaX, drumPulseBounds.top+deltaY, drumPulseBounds.right-deltaX, drumPulseBounds.bottom-deltaY);
            drum.setBounds(drumPulseBounds);
        } else {
            drum.setBounds(drumBounds);
        }
    
        boolean oldIsIdle = isIdle;
        isIdle = Constants.IDLE_TIMEOUT_IN_MS - timeSinceLastBeat < 200;
        
        int _CPT = metronomeIsOn ? metronomeTempo : CPT;
        boolean _cptIsValid = Constants.isValidTempo(_CPT);
    
        //paint the red circle that goes on the ring
        paint.setStyle(Paint.Style.FILL);
        if (_cptIsValid && (!isIdle || metronomeIsOn) ) {
    
            double oneLapTime = getOneLapTime();
            double degree = (((double)timeSinceLastTimerBeat/oneLapTime) % 1) * 2*Math.PI + Math.PI;
            
            int ocX = (int) (radius * Math.sin(degree) + cX);
            int ocY = (int) (radius * Math.cos(degree) + cY);
            canvas.drawCircle(ocX, ocY, 3 * paint.getStrokeWidth(), paint);
    
            
            if ((oneLapTime-timeSinceLastTimerBeat <= 5) || (timeSinceLastTimerBeat >= oneLapTime)) {
                lastTimerBeatTime = now;
                if (metronomeIsOn) {
                    metronome.play();
                }
            }
        }

        // draw drum
        drum.draw(canvas);
        
        // draw CPT text
        String cptString = Constants.getTempoString(CPT);
        
        paint.setStyle(Paint.Style.STROKE);
        float stroke = paint.getStrokeWidth();
        paint.setStrokeWidth(0);
        paint.setTextSize(radius);
        paint.getTextBounds(cptString, 0, cptString.length(), textBounds);
        paint.setColor(WHITE_COLOR);
        canvas.drawText(cptString, cX, cY - textBounds.exactCenterY(), paint);
        paint.setStrokeWidth(stroke);
    
    
        // if become idle
        if (!oldIsIdle && isIdle) {
            reset();
        }
        
        // repeat onDraw() if needed
        if (metronomeIsOn || (CPT > 0 && !isIdle)){
            invalidate();
        }

    }
    
    
    private void reset() {
        lastTimerBeatTime = 0;
        lastBeatTime=0;
        hit = false;
        offDegree = 0;
        isIdle = true;
        windowQueue.clear();
    }
    
    
    //================================================================================
    //  BPM processing
    //================================================================================
    
    
    private long lastTimerBeatTime = 0;
    private long lastBeatTime=0;
    private boolean hit = false;
    private double offDegree = 0;
    private boolean isIdle = true;
    
    private double getOneLapTime(){ // time to run one whole circle
        double _CPT = isMetronomeOn() ? (double)metronomeTempo : (double) this.CPT /(double)beat;
        return  (double) MS_IN_MIN / _CPT;
    }

    
    // beat handler
    public void beat(long beatTime) {
        if (lastBeatTime == 0){
            lastBeatTime = beatTime;
            if (!isMetronomeOn()) {
                lastTimerBeatTime = beatTime;
            }
            return;
        }
        
        long timeSinceLastBeat = beatTime - lastBeatTime;
        
        if (timeSinceLastBeat == 0) {
            return;
        }
        
        
        double tapBpm = (double) (MS_IN_MIN /timeSinceLastBeat);
        processBeat(tapBpm, beatTime);
    }
    
    private void processBeat(double bpm, long beatTime) {
        double multiplier = isMetronomeOn() ? 1 : (double) beat;
        double instantTempo = multiplier * bpm;
        CPT = windowQueue.enqueue(instantTempo).average();
        lastBeatTime = beatTime;
    
        offDegree = 0;
        if (this.CPT > 0) {
            double oneLapTime = getOneLapTime();
            long timeSinceLastTimerBeat = beatTime - lastTimerBeatTime;
            hit = Math.abs(oneLapTime-timeSinceLastTimerBeat) <= 10;
            if (hit && Constants.isValidTempo(CPT)) {
                //Log.d("HIT", "--------- HIT ------------");
                leftStrike = !leftStrike;
            } else {
                offDegree = (((double)timeSinceLastTimerBeat/oneLapTime) % 1) * 2*Math.PI + Math.PI;
    
            }
            
        }
        invalidate();
    }
    
    
    
    //================================================================================
    //  Touch handler
    //================================================================================
    
    
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (handleTap) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    beat(System.currentTimeMillis());
            }
        }
        return super.onTouchEvent(event);
    }
    
    
    
    //================================================================================
    //  METRONOME
    //================================================================================
    
    
    public void setMetronomeOn(Sound sound, int metronomeTempo) {
        if (metronome == null) {
            metronome = new MetronomePlayer();
        }
        metronome.setCurrentSound(sound);
        this.metronomeTempo = metronomeTempo;
        lastTimerBeatTime = System.currentTimeMillis();
        invalidate();
    }
    
    public void setMetronomeOff() {
        metronome = null;
        reset();
        invalidate();
    }
    
    public void setMetronomeSond(Sound sound) {
        if (metronome != null) {
            metronome.setCurrentSound(sound);
        }
    }
    
    public boolean isMetronomeOn() {
        return metronome != null;
    }
}
