package com.esdevices.backbeater.ui.widgets;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
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


/**
 * Created by aeboyd on 7/15/15.
 */
public class TempoDisplay extends AppCompatTextView {

    public static final long MS_IN_MIN = 60000;
    public static final int WHITE_COLOR = -1;
//    private final int accentColor;
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
    private static final int DRUM_ANIMATION_DURATION = 500;
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

    public MainActivity mainActivity;

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

//        accentColor = getResources().getColor(R.color.assent_color);

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

        // (cX, cY), radius - centre and radius of the big circle,
        int cX = width/2+getPaddingLeft();
        int radius = (int) (width/2-paint.getStrokeWidth());
        if(height-drumBounds.height()/2<getWidth()){
            radius = (height - drumBounds.height()/2)/2- getPaddingLeft();
        }
        long now = System.currentTimeMillis();
        long timeSinceLastBeat = now - lastBeatTime;

        float density = getResources().getDisplayMetrics().density;

        int navbar = (int)(getNavigationBarHeight(getContext(), false) / density + 0.5f);
        int cY = drumBounds.height()/2+getPaddingTop() + contentHeight * 3/4;// width/2;//+radius + navbar/2;

        boolean oldIsIdle = isIdle;
        isIdle = Constants.IDLE_TIMEOUT_IN_MS - timeSinceLastBeat < 200;

        // draw drum
        drum.draw(canvas);
        drumFlash.draw(canvas);

        // draw CPT text
        String cptString = Constants.getTempoString(CPT);

        density *= 1.25f;
        if (density < 2.5f)  density = 2.5f;
        paint.setTextSize(radius/density);
        paint.getTextBounds(cptString, 0, cptString.length(), textBounds);
        canvas.drawText(cptString, cX, cY - textBounds.exactCenterY(), paint);

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
//        hit = false;
        isIdle = true;
        windowQueue.clear();
    }


    //================================================================================
    //  BPM processing
    //================================================================================


    private long lastTimerBeatTime = 0;
    private long lastBeatTime=0;
//    private boolean hit = false;
//    private double offDegree = 0;
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

        if (timeSinceLastBeat < 20) {
            return;
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

        if (this.CPT > 0) {
            int vCPT = Math.min(Constants.MAX_TEMPO, (Math.max(Constants.MIN_TEMPO, this.CPT)));
            int pos = this.CPT - metronomeTempo;
            if (pos > 4)        pos = 4;
            else if (pos < -4)  pos = -4;

            mainActivity.setSpeed(pos);

            if (pos == 0) {
                screenFlash();
            }

            double oneLapTime = getOneLapTime();
            long timeSinceLastTimerBeat = beatTime - lastTimerBeatTime;
            boolean hit = Math.abs(oneLapTime-timeSinceLastTimerBeat) > 150;

            if (Constants.isValidTempo(CPT)) {
                if (hit) {
                    leftStrike = !leftStrike;
                    strikeAnimation();
                }
            }
            else {
                if (hit) {
                    pulseAnimation();
                }
            }
            if (!isMetronomeOn()) {
                mainActivity.setTargetTemp(vCPT);
            }
        }
        invalidate();
    }

    ValueAnimator strikeAnimator = null;
    private void strikeAnimation() {
        if (strikeAnimator != null) {
            strikeAnimator.cancel();
            strikeAnimator = null;
        }
        ValueAnimator va = ValueAnimator.ofObject(new TypeEvaluator<Integer>() {
            @Override
            public Integer evaluate(float fraction, Integer startValue, Integer endValue) {
                return (int)(startValue + fraction * (endValue - startValue));
            }
        }, 0, DRUM_ANIMATION_FRAMES-1);

        va.setDuration(DRUM_ANIMATION_DURATION);
        va.setStartDelay(0);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer value = (Integer) animation.getAnimatedValue();
                if (value != null) {
                    int idx = 0;
                    if(!leftStrike) {
                        idx += DRUM_ANIMATION_FRAMES;
                    }
                    drum.selectDrawable(value + idx);
                    invalidate();
                }
            }
        });
        va.start();
        strikeAnimator = va;
    }

    ValueAnimator pulseAnimator = null;
    private void pulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        drum.selectDrawable(leftStrike ? DRUM_ANIMATION_FRAMES-1 : 2*DRUM_ANIMATION_FRAMES-1);

        ValueAnimator va = ValueAnimator.ofObject(new TypeEvaluator<Float>() {
            @Override
            public Float evaluate(float fraction, Float startValue, Float endValue) {
                return (startValue + fraction * (endValue - startValue));
            }
        }, 0.0f, 0.4f);
        va.setDuration(200);
        va.setStartDelay(0);
        va.setRepeatMode(ObjectAnimator.REVERSE);

        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Float value = (Float)animation.getAnimatedValue();
                if (value != null) {

                    int deltaY = (int) (value * DRUM_PULSE_DELTA_Y);
                    int deltaX = (int) ((float)deltaY*(float)drumBounds.width() / (float)drumBounds.height());

                    drumPulseBounds.set(drumBounds.left+deltaX, drumBounds.top+deltaY, drumBounds.right-deltaX, drumBounds.bottom-deltaY);
                    drum.setBounds(drumPulseBounds);
                    invalidate();
                }
            }
        });

        va.start();
        pulseAnimator = va;

//        float halfDrumPulseAnimation = DRUM_PULSE_ANIMATION_DURATION / 2f;
//        float timeElapsed = (float) timeSinceLastBeat - halfDrumPulseAnimation;
//        float coefficient = 0;
//        if (timeElapsed <= halfDrumPulseAnimation) {
//            // increase 0 -> 1
//            coefficient = timeElapsed / halfDrumPulseAnimation;
//        } else {
//            // decrease 1 -> 0
//            coefficient = timeElapsed / halfDrumPulseAnimation - 1f;
//        }
//
//        int deltaY = (int) (coefficient * DRUM_PULSE_DELTA_Y);
//        int deltaX = (int) ((float)deltaY*(float)drumBounds.width() / (float)drumBounds.height());

//        drumPulseBounds.set(drumBounds.left+deltaX, drumBounds.top+deltaY, drumBounds.right-deltaX, drumBounds.bottom-deltaY);
//        drum.setBounds(drumPulseBounds);


    }

    private void screenFlash() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (drumFlash.getAlpha() == 0) {
                ObjectAnimator animator = ObjectAnimator
                        .ofPropertyValuesHolder(drumFlash,
                                PropertyValuesHolder.ofInt("alpha", 0, 180));
                animator.setTarget(drumFlash);
                animator.setDuration(100);
                animator.setRepeatMode(ObjectAnimator.REVERSE);
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
        if (metronome == null) {
            metronome = new MetronomePlayer();
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
        metronome.stop();
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
