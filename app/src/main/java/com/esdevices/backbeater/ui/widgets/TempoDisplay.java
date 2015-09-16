package com.esdevices.backbeater.ui.widgets;

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
import android.widget.TextView;

import com.esdevices.backbeater.R;

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
        int cY = drum.getBounds().height()/2+radius+getPaddingTop();
        paint.setColor(accentColor);
        canvas.drawCircle(cX, cY, radius, paint);
        //paint the circle
        drum.draw(canvas);
        float stroke = paint.getStrokeWidth();
        paint.setStrokeWidth(0);
        String t = ""+tempo;
        paint.setTextSize(radius);
        paint.getTextBounds(t, 0, t.length(), textBounds);
        paint.setColor(-1);
        canvas.drawText(t,cX,cY-textBounds.exactCenterY(),paint);
        paint.setStrokeWidth(stroke);

    }

    public int getTempo() {
        return tempo;
    }

    public void setTempo(int tempo) {
        this.tempo = tempo;
        drum.start();
        invalidate();
    }
}
