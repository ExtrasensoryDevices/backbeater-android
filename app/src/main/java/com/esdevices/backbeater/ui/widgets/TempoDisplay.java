package com.esdevices.backbeater.ui.widgets;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import com.esdevices.backbeater.R;

import java.util.Date;

/**
 * Created by aeboyd on 7/15/15.
 */
public class TempoDisplay extends TextView {
    private final int backgroundColor;
    private final int accentColor;
    private final AnimationDrawable drum;
    private final TextPaint paint;
    private int height = 0;
    private int width = 0;
    private final Rect textBounds = new Rect();
    private int tempo = 0;
    private final Point circleC = new Point();
    private static final float PCT_DRUM = .4f;
    private boolean leftStrike = false;
    private long lastBeat=0;
    private boolean hit = false;
    private float offDegree = 0;
    public TempoDisplay(Context context) {
        this(context, null, 0);
    }

    public TempoDisplay(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TempoDisplay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        backgroundColor = getResources().getColor(R.color.main_color);
        accentColor = getResources().getColor(R.color.assent_color);
        drum = (AnimationDrawable)getResources().getDrawable(R.drawable.left_animation);
        drum.selectDrawable(35);
        paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(-1);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(getResources().getDimension(R.dimen.circle_outline_width));
        if(!isInEditMode()) {
            paint.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "fonts/steelfish_rg.ttf"));
        }
        paint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int contentWidth = getWidth()-getPaddingLeft()-getPaddingRight();
        int contentHeight = getHeight()-getPaddingBottom()-getPaddingTop();
        if (height != contentHeight && width != contentWidth) {
            height = contentHeight;
            width = contentWidth;
            drum.setBounds((int) (.5*width*(1-PCT_DRUM)+getPaddingLeft()),getPaddingTop(),(int) (.5*width*(1+PCT_DRUM)+getPaddingLeft()),(int)(width*PCT_DRUM*drum.getIntrinsicHeight()/drum.getIntrinsicWidth()+getPaddingTop()));
        }
        int cX = width/2+getPaddingLeft();
        int radius = (int) (width/2-paint.getStrokeWidth());
        if(height-drum.getBounds().height()/2<getWidth()){
            radius = (height - drum.getBounds().height()/2)/2- getPaddingLeft();
        }
        long now = new Date().getTime();
        long timeDiff = now - lastBeat;
        int cY = drum.getBounds().height()/2+radius+getPaddingTop();
        if(timeDiff<600 && hit){
            paint.setColor(NumberButton.mixTwoColors(accentColor,-1,Math.abs((timeDiff-300)/300f)));
            canvas.drawCircle(cX, cY, radius, paint);
            paint.setColor(accentColor);
            if(leftStrike) {
                drum.selectDrawable((int) (timeDiff / 600f * 16));
            }else{
                drum.selectDrawable((int) (timeDiff / 600f * 20)+15);
            }
        }else {
            paint.setColor(accentColor);
            canvas.drawCircle(cX, cY, radius, paint);
            if(timeDiff<600&&offDegree>0) {
                paint.setColor(NumberButton.mixTwoColors(accentColor,-1, timeDiff / 600f));
                paint.setAlpha((int) (255-(timeDiff/600f)*255));
                int ocX = (int) (radius * Math.sin(offDegree) + cX);
                int ocY = (int) (radius * Math.cos(offDegree) + cY);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(ocX, ocY, 3 * paint.getStrokeWidth(), paint);
                paint.setColor(accentColor);
                paint.setAlpha(255);
            }
        }
        //paint the circles that go on the ring
        paint.setStyle(Paint.Style.FILL);
        if(tempo>0) {
            float bpmilisecond = 60000 / tempo;
            double degree = ((timeDiff / bpmilisecond) % 1) * 2 * Math.PI+ Math.PI;
            int ocX = (int) (radius * Math.sin(degree) + cX);
            int ocY = (int) (radius * Math.cos(degree) + cY);
            canvas.drawCircle(ocX, ocY, 3 * paint.getStrokeWidth(), paint);


        }else{


        }

        drum.draw(canvas);
        paint.setStyle(Paint.Style.STROKE);

        float stroke = paint.getStrokeWidth();
        paint.setStrokeWidth(0);
        String t = ""+tempo;
        paint.setTextSize(radius);
        paint.getTextBounds(t, 0, t.length(), textBounds);
        paint.setColor(-1);
        canvas.drawText(t, cX, cY - textBounds.exactCenterY(), paint);
        paint.setStrokeWidth(stroke);
        if(tempo>0 && now-lastBeat<4000){
            invalidate();
        }

    }

    public int getTempo() {
        return tempo;
    }


    public void beat(){
        long now = new Date().getTime();
        if(lastBeat==0){
            lastBeat=now;
            return;
        }
        int tempo = (int) (60000/(now-lastBeat));

        if(hit=(tempo==this.tempo)) {
            leftStrike = !leftStrike;
        }else if(this.tempo>0){
            offDegree= (float)((((now-lastBeat) / (60000f/this.tempo)) % 1) * 2 * Math.PI+ Math.PI);

        }
        lastBeat = now;
        setTempo(tempo);
    }

    public void setTempo(int tempo) {
        this.tempo = tempo;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                beat();
        }
        return super.onTouchEvent(event);
    }
}
