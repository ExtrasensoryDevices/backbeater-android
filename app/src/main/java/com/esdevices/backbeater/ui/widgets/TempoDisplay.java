package com.esdevices.backbeater.ui.widgets;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.support.v7.widget.AppCompatTextView;
import android.view.ViewConfiguration;

import com.esdevices.backbeater.R;

import com.esdevices.backbeater.activity.MainActivity;
import com.esdevices.backbeater.audio.MetronomePlayer;
import com.esdevices.backbeater.audio.WindowQueue;
import com.esdevices.backbeater.utils.Constants;
import com.esdevices.backbeater.utils.Constants.Sound;
import com.esdevices.backbeater.utils.Preferences;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by aeboyd on 7/15/15.
 */
public class TempoDisplay extends AppCompatTextView {

    public static final long MS_IN_MIN = 60000;
    public static final int WHITE_COLOR = -1;
    private final int accentColor;
    private final AnimationDrawable drum;
    private final Drawable drumFlash;
    private final TextPaint paint;
    private int height = 0;
    private int width = 0;
    private final Rect textBounds = new Rect();
    private Rect drumBounds = new Rect();
    private Rect drumPulseBounds = new Rect();
    private Rect drumFlashBounds = new Rect();

    private static final float PCT_DRUM = .4f;
    private static final float DRUM_ANIMATION_DURATION = 300f;
    private final int DRUM_ANIMATION_FRAMES;
    private boolean leftStrike = false;
    private static final float DRUM_PULSE_ANIMATION_DURATION = DRUM_ANIMATION_DURATION/10f;
    private final float DRUM_PULSE_DELTA_Y;



    private int CPT = Constants.DEFAULT_TEMPO;  // = Currently Playing Tempo (multiplied by Beat, averaged in Window) or Metronome Tempo
    private int metronomeTempo = Constants.DEFAULT_TEMPO;
    private int dCPT = 0;

    private MetronomePlayer metronome;

    private int beat = 1;
    private int window = 5;
    private WindowQueue windowQueue;

    public boolean handleTap = true;

    public MainActivity mainActivity;

    private boolean isPlaying = false;
    private Timer playTimer = null;
    public static long lastPlayTime = 0;

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

        drumFlash = getResources().getDrawable(R.drawable.style_button_circle);
        drumFlash.setAlpha(0);

        paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(WHITE_COLOR);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(getResources().getDimension(R.dimen.circle_outline_width));
        if(!isInEditMode()) {
            paint.setTypeface(Constants.BBTypeface.STEELFISH.getTypeface(context));
        }
        paint.setTextAlign(Paint.Align.CENTER);
    }

    //================================================================================
    //  Window queue settings
    //================================================================================

    public void setParent(MainActivity activity) {
        this.mainActivity = activity;
    }

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

    public static boolean hasNavBar(Context context) {
        // Kitkat and less shows container above nav bar
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return false;
        }
        // Emulator
        if (Build.FINGERPRINT.startsWith("generic")) {
            return true;
        }
        boolean hasMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        boolean hasNoCapacitiveKeys = !hasMenuKey && !hasBackKey;
        Resources resources = context.getResources();
        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        boolean hasOnScreenNavBar = id > 0 && resources.getBoolean(id);
        return hasOnScreenNavBar || hasNoCapacitiveKeys || getNavigationBarHeight(context, true) > 0;
    }

    public static int getNavigationBarHeight(Context context, boolean skipRequirement) {
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0 && (skipRequirement || hasNavBar(context))) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    int playPulseCount = 0;
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
            drumBounds.set(drumL, drumT, drumR, drumB);
            drum.setBounds(drumBounds);

            int h2 = (drumB - drumT) / 2;
            int cx = (drumL + drumR) / 2;

            drumFlashBounds.set(cx-h2, drumT, cx+h2, drumB);
            drumFlash.setBounds(drumFlashBounds);
        }

        long now = System.currentTimeMillis();
        long timeSinceLastBeat = now - lastBeatTime;

        // beat - beat registered
        // hit - beat in time according to CPT
        boolean beat = Constants.isValidTempo(CPT) && timeSinceLastBeat < DRUM_ANIMATION_DURATION;

        // draw big circle
        if (beat) {
            // select drim animation frame
            int drumAnimationFrameIndex = (int) (timeSinceLastBeat / DRUM_ANIMATION_DURATION * DRUM_ANIMATION_FRAMES);
            if(!leftStrike) {
                // right strike -> offset=DRUM_ANIMATION_FRAMES
                drumAnimationFrameIndex = drumAnimationFrameIndex + DRUM_ANIMATION_FRAMES;
            }
            drum.selectDrawable(drumAnimationFrameIndex);
        } else {
            drum.selectDrawable(leftStrike ? DRUM_ANIMATION_FRAMES - 1 : 2 * DRUM_ANIMATION_FRAMES - 1);
        }

        boolean oldIsIdle = isIdle;
        if (isPlaying) {
            // drum pulse animation
            if (!beat && timeSinceLastBeat < DRUM_PULSE_ANIMATION_DURATION) {

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
                int deltaX = (int) ((float) deltaY * (float) drumBounds.width() / (float) drumBounds.height());

                drumPulseBounds.set(drumBounds.left + deltaX, drumBounds.top + deltaY, drumBounds.right - deltaX, drumBounds.bottom - deltaY);
                drum.setBounds(drumPulseBounds);

                drumFlashBounds.set(drumBounds.centerX() - drumBounds.height() / 2, drumBounds.top, drumBounds.centerX() + drumBounds.height() / 2, drumBounds.bottom);
                drumFlash.setBounds(drumFlashBounds);
            } else {
                drum.setBounds(drumBounds);

                drumFlashBounds.set(drumBounds.centerX() - drumBounds.height() / 2, drumBounds.top, drumBounds.centerX() + drumBounds.height() / 2, drumBounds.bottom);
                drumFlash.setBounds(drumFlashBounds);
            }

            isIdle = Constants.IDLE_TIMEOUT_IN_MS - timeSinceLastBeat < 60;
        }
        else {
            if (isMetronomeOn()) {
                long sincePlayTime = now - lastPlayTime;

                if (sincePlayTime < 60 && playPulseCount < 1) {
                    playPulseCount++;
//                    Log.e("TAG", "#### tempo > " + lastPlayTime + " : " + sincePlayTime);
                    float halfDrumPulseAnimation = DRUM_PULSE_ANIMATION_DURATION / 2f;
                    float timeElapsed = (float) DRUM_PULSE_ANIMATION_DURATION - halfDrumPulseAnimation;
                    float coefficient = 0;
                    if (timeElapsed <= halfDrumPulseAnimation) {
                        // increase 0 -> 1
                        coefficient = timeElapsed / halfDrumPulseAnimation;
                    } else {
                        // decrease 1 -> 0
                        coefficient = timeElapsed / halfDrumPulseAnimation - 1f;
                    }

                    int deltaY = (int) (coefficient * DRUM_PULSE_DELTA_Y);
                    int deltaX = (int) ((float) deltaY * (float) drumBounds.width() / (float) drumBounds.height());

                    drumPulseBounds.set(drumBounds.left + deltaX, drumBounds.top + deltaY, drumBounds.right - deltaX, drumBounds.bottom - deltaY);
                    drum.setBounds(drumPulseBounds);

                } else {
                    if (sincePlayTime > 100)
                        playPulseCount = 0;
                    drum.setBounds(drumBounds);
                }

                isIdle = Constants.IDLE_TIMEOUT_IN_MS - sincePlayTime < 60;
            }
            else {
                isIdle = Constants.IDLE_TIMEOUT_IN_MS - timeSinceLastBeat < 60;
            }
        }

        // draw drum
        drum.draw(canvas);
        drumFlash.draw(canvas);

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

        if (timeSinceLastBeat < 100) {
            return;
        }

        if (isMetronomeOn()) {
            isPlaying = true;

            if (playTimer != null) {
                playTimer.cancel();
                playTimer = null;
            }

            playTimer = new Timer();
            playTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    isPlaying = false;
                }
            }, 2000);
        }

        double tapBpm = (double) (MS_IN_MIN /timeSinceLastBeat);
        processBeat(tapBpm, beatTime);
    }

    private void processBeat(double bpm, long beatTime) {
//        double multiplier = isMetronomeOn() ? 1 : (double) beat;
        double instantTempo = this.beat * bpm;
        CPT = windowQueue.enqueue(instantTempo).average();
        lastBeatTime = beatTime;

        mainActivity.setTargetLabel(bpm);

        offDegree = 0;
        if (this.CPT > 0) {
            int pos = (int)bpm - metronomeTempo;
            if (pos <= 2 && pos >= -2)       pos = 0;
            else if (pos <= 5 && pos > 2)    pos = 1;
            else if (pos >= -5 && pos < -2)  pos = -1;
            else if (pos <= 8 && pos > 5)    pos = 2;
            else if (pos >= -8 && pos < -5)  pos = -2;
            else if (pos <= 11 && pos > 8)   pos = 3;
            else if (pos >= -11 && pos < -8) pos = -3;
            else if (pos > 11)  pos = 4;
            else pos = -4;

            mainActivity.setSpeed(pos);

            if (pos == 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (drumFlash.getAlpha() == 0) {
                        ObjectAnimator animator = ObjectAnimator
                                .ofPropertyValuesHolder(drumFlash,
                                        PropertyValuesHolder.ofInt("alpha", 0, 180));
                        animator.setTarget(drumFlash);
                        animator.setDuration(100);
                        animator.start();

                        ObjectAnimator animator1 = ObjectAnimator
                                .ofPropertyValuesHolder(drumFlash,
                                        PropertyValuesHolder.ofInt("alpha", 180, 0));
                        animator1.setTarget(drumFlash);
                        animator1.setStartDelay(100);
                        animator1.setDuration(100);
                        animator1.start();
                    }
                }
                else {
                    ObjectAnimator animator = ObjectAnimator
                            .ofPropertyValuesHolder(drumFlash,
                                    PropertyValuesHolder.ofInt("alpha", 0, 180));
                    animator.setTarget(drumFlash);
                    animator.setDuration(100);
                    animator.start();

                    ObjectAnimator animator1 = ObjectAnimator
                            .ofPropertyValuesHolder(drumFlash,
                                    PropertyValuesHolder.ofInt("alpha", 180, 0));
                    animator1.setTarget(drumFlash);
                    animator1.setStartDelay(100);
                    animator1.setDuration(100);
                    animator1.start();
                }
            }

            double oneLapTime = getOneLapTime();
            long timeSinceLastTimerBeat = beatTime - lastTimerBeatTime;
            hit = Math.abs(oneLapTime-timeSinceLastTimerBeat) <= 100;
            if (Constants.isValidTempo(CPT)) {
                leftStrike = !leftStrike;
            }
            if (hit && Constants.isValidTempo(CPT)) {
//                Log.d("HIT", "#### --------- HIT ------------");
            } else {
                offDegree = (((double)timeSinceLastTimerBeat/oneLapTime) % 1) * 2*Math.PI + Math.PI;

            }
            if (!isMetronomeOn()) {
                mainActivity.setTargetTemp(this.CPT);
            }
            else {
                mainActivity.setTempoOnly(this.CPT);
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
    public void setMetronomeTempo(int tempo) {
        this.metronomeTempo = tempo;
    }

    public void setMetronomeOn(Sound sound, int metronomeTempo) {
        if (metronome != null) {
            metronome.stop();
            metronome = null;
        }
        if (metronome == null) {
            mainActivity.setLogText(null);
            metronome = new MetronomePlayer();
            metronome.parent = mainActivity;
        }
        float rTempo = 1;
        if (metronomeTempo > 0) {
            rTempo = 60.f / metronomeTempo;
        }
        metronome.setCurrentSound(sound);
        metronome.play(rTempo);
        setMetronomeTempo(metronomeTempo);
        lastTimerBeatTime = System.currentTimeMillis();
        invalidate();
    }

    public void setMetronomeOff() {
        mainActivity.setLogText(null);
        metronome.stop();
        metronome = null;
        reset();
        invalidate();
    }

    public void setMetronomeSound(Sound sound) {
        if (metronome != null) {
            metronome.setCurrentSound(sound);
        }
    }

    public boolean isMetronomeOn() {
        return metronome != null;
    }
}
