package com.esdevices.backbeater.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import com.esdevices.backbeater.R;

import com.esdevices.backbeater.audio.MetronomePlayer;
import com.esdevices.backbeater.audio.WindowQueue;
import com.esdevices.backbeater.utils.Constants;
import com.esdevices.backbeater.utils.Constants.Sound;
import com.esdevices.backbeater.utils.Preferences;

/**
 * Created by aeboyd on 7/15/15.
 */
public class TempoDisplay extends TextView {
    
    public static final long MS_IN_MIN = 60000;
    public static final long IDLE_TIMEOUT = 10000;
    public static final int WHITE_COLOR = -1;
    private final int accentColor;
    private final AnimationDrawable drum;
    private final TextPaint paint;
    private int height = 0;
    private int width = 0;
    private final Rect textBounds = new Rect();
    private static final float PCT_DRUM = .4f;
    private static final float DRUM_ANIMATION_DURATION = 600f;
    private final int DRUM_ANIMATION_FRAMES;
    private boolean leftStrike = false;
    
    
    
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
        this.window = window;
        windowQueue.setCapacity(window);
    }
    
    public void setBeat(int beat) {
        this.beat = beat;
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
        if (height != contentHeight && width != contentWidth) {
            height = contentHeight;
            width = contentWidth;
            drum.setBounds((int) (.5*width*(1-PCT_DRUM)+getPaddingLeft()),getPaddingTop(),(int) (.5*width*(1+PCT_DRUM)+getPaddingLeft()),(int)(width*PCT_DRUM*drum.getIntrinsicHeight()/drum.getIntrinsicWidth()+getPaddingTop()));
        }
        
        // (cX, cY), radius - centre and radius of the big circle,
        int cX = width/2+getPaddingLeft();
        int radius = (int) (width/2-paint.getStrokeWidth());
        if(height-drum.getBounds().height()/2<getWidth()){
            radius = (height - drum.getBounds().height()/2)/2- getPaddingLeft();
        }
        long now = System.currentTimeMillis();
        long timeSinceLastBeat = now - lastBeatTime;
        long timeSinceLastTimerBeat = now - lastTimerBeatTime;
        
        int cY = drum.getBounds().height()/2+radius+getPaddingTop();
        
        // draw big circle
        if(timeSinceLastBeat < DRUM_ANIMATION_DURATION && hit){
            // hit in right time
            
            // draw big circle (flash white)
            float halfDrumAnimation = DRUM_ANIMATION_DURATION / 2f;
            paint.setColor(NumberButton.mixTwoColors(accentColor,
                WHITE_COLOR,Math.abs((timeSinceLastBeat-halfDrumAnimation)/halfDrumAnimation)));
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
    
            Log.d("TIMER", "timeSinceLastBeat: "+timeSinceLastBeat+", condition: "+(timeSinceLastBeat < DRUM_ANIMATION_DURATION));
            // draw fading white circle
            if(timeSinceLastBeat < DRUM_ANIMATION_DURATION && offDegree > 0) {
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
        }
    
        boolean oldIsIdle = isIdle;
        isIdle = IDLE_TIMEOUT - timeSinceLastBeat < 200;
        
        // if become idle
        if (!oldIsIdle && isIdle) {
            lastBeatTime = 0;
        }
        
        int _CPT = metronomeIsOn ? metronomeTempo : CPT;
    
        //paint the red circle that goes on the ring
        paint.setStyle(Paint.Style.FILL);
        if (_CPT > 0 && (!isIdle || metronomeIsOn) ) {
    
            double oneLapTime = getOneLapTime();
            double degree = ((double)timeSinceLastTimerBeat * 2*Math.PI) / oneLapTime + Math.PI;
            //Log.d("TIMER", "timeSinceLastTimerBeat: "+timeSinceLastTimerBeat+", degree: "+degree);
    
            int ocX = (int) (radius * Math.sin(degree) + cX);
            int ocY = (int) (radius * Math.cos(degree) + cY);
            canvas.drawCircle(ocX, ocY, 3 * paint.getStrokeWidth(), paint);
    
            
            //Log.d("TIMER", "degree: "+degree+", time offset: "+(timeSinceLastTimerBeat-oneLapTime));
            if ((oneLapTime-timeSinceLastTimerBeat <= 5) || (timeSinceLastTimerBeat >= oneLapTime)) {
                Log.d("METRONOME", "----------------- BEEP ------------");
                lastTimerBeatTime = now;
                if (metronomeIsOn) {
                    metronome.play();
                }
            }
        }

        // draw drum
        drum.draw(canvas);
        
        // draw CPT text
        String cptString;
        if (CPT < Constants.MIN_TEMPO) {
            cptString = "MIN";
        } else if (CPT > Constants.MAX_TEMPO) {
            cptString = "MAX";
        } else {
            cptString = ""+ CPT;
        }
        
        paint.setStyle(Paint.Style.STROKE);
        float stroke = paint.getStrokeWidth();
        paint.setStrokeWidth(0);
        paint.setTextSize(radius);
        paint.getTextBounds(cptString, 0, cptString.length(), textBounds);
        paint.setColor(WHITE_COLOR);
        canvas.drawText(cptString, cX, cY - textBounds.exactCenterY(), paint);
        paint.setStrokeWidth(stroke);

        
        // repeat onDraw() if needed
        if (metronomeIsOn || (CPT > 0 && !isIdle)){
            invalidate();
        }

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
        double CPT = isMetronomeOn() ? (double)metronomeTempo : (double) this.CPT /(double)beat;
        return  (double) MS_IN_MIN / CPT;
    }
    
    // tap handler
    private double getBeatRate(){ //  hit frequency = CPT/beat
        return  (double) MS_IN_MIN / ((double) CPT /(double)beat);
    }
    
    // tap handler
    public void beat(){
        long now = System.currentTimeMillis();
        if (lastBeatTime == 0){
            lastBeatTime = now;
            if (!isMetronomeOn()) {
                lastTimerBeatTime = now;
            }
            return;
        }
        long timeSinceLastBeat = now - lastBeatTime;
        double tapBpm = (double) (MS_IN_MIN /timeSinceLastBeat);
        lastBeatTime = now;
        processBeat(tapBpm, now);
    }
    
    // audio handler
    public void setBPM(double drumBpm) {
        long now = System.currentTimeMillis();
        if (lastBeatTime == 0){
            lastBeatTime = now;
            if (!isMetronomeOn()) {
                lastTimerBeatTime = now;
            }
            return;
        }
        lastBeatTime = now;
        processBeat(drumBpm, now);
    }
    
    private void processBeat(double bpm, long beatTime) {
        double multiplier = isMetronomeOn() ? 1 : (double) beat;
        double instantTempo = multiplier * bpm;
        CPT = windowQueue.enqueue(instantTempo).average();
        
        offDegree = 0;
        if (this.CPT > 0) {
            double oneLapTime = getOneLapTime();
            long timeSinceLastTimerBeat = beatTime - lastTimerBeatTime;
            hit = (oneLapTime-timeSinceLastTimerBeat <= 5) || (timeSinceLastTimerBeat >= oneLapTime);
            if (!hit){
                offDegree = ((double)timeSinceLastTimerBeat * 2*Math.PI) / oneLapTime + Math.PI;
                leftStrike = !leftStrike;
            }
            Log.d("HIT", "---------------------");
            //Log.d("HIT", (hit? "HIT!!!!  ":"MISSED!!! ") +"   timeSinceLastBeat: "+timeSinceLastTimerBeat+", getBeatRate: "+ getBeatRate()+", offDegree: "+offDegree);
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
                    beat();
            }
        }
        return super.onTouchEvent(event);
    }
    
    
    
    //================================================================================
    //  METRONOME
    //================================================================================
    
    
    public void setMetronomeOn(Sound sound, int metronomeTempo) {
        //TODO: think of initializing MetronomePlayer in constructor
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
        lastTimerBeatTime = 0;
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
