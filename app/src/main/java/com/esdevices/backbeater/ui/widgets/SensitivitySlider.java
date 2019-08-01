package com.esdevices.backbeater.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.esdevices.backbeater.R;
import com.esdevices.backbeater.utils.Constants;

public class SensitivitySlider extends View {
    
    public interface ValueChangeListener {
        void onSensitivityValueChanged(int newValue);
        void onSensitivityEditChanged(int newValue);
    }
    
    
    private ValueChangeListener valueChangeListener;
    
    
    private TextPaint mTextPaint;
    private Paint mDrawPaint;
    private Drawable low;
    private Drawable high;
    private float level = 1.0f;
    private int circleRadius = 100;
    private final int PINK_COLOR;
    private int cX;
    private int left;
    private int right;
    private Rect textBounds = new Rect();
    private int height = 0;
    private int width = 0;

    public SensitivitySlider(Context context) {
        this(context, null, 0);
    }

    public SensitivitySlider(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SensitivitySlider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        PINK_COLOR = getResources().getColor(R.color.assent_color);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        mDrawPaint = new Paint();
        mDrawPaint.setColor(Color.BLACK);
        mDrawPaint.setStyle(Paint.Style.STROKE);
        mDrawPaint.setStrokeWidth(getResources().getDimension(R.dimen.sensitivity_stroke));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            low = getResources().getDrawable(R.drawable.sensitivity_low, context.getTheme());
            high = getResources().getDrawable(R.drawable.sensitivity_high, context.getTheme());
        } else {
            low = ContextCompat.getDrawable(context, R.drawable.sensitivity_low);
            high = ContextCompat.getDrawable(context, R.drawable.sensitivity_high);
        }
        

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }
    
    public void setValueChangeListener(ValueChangeListener valueChangeListener) {
        this.valueChangeListener = valueChangeListener;
    }
    
    public void setValue(int level) {
        this.level = Math.max(0f, Math.min(100f, (float)level/100f));
        invalidate();
    }
    
    public int getValue() {
        return Math.round(level*100f);
    }
    
    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setColor(-1);
        mTextPaint.setStrokeWidth(getResources().getDimension(R.dimen.sensitivity_stroke));
        if (!isInEditMode()) {
            mTextPaint.setTypeface(Constants.BBTypeface.FUTURA_ROUND_BOOK.getTypeface(getContext()));
        }
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;
        if (height != contentHeight && width != contentWidth) {
            height = contentHeight;
            width = contentWidth;
            low.setBounds(paddingLeft, paddingTop, contentHeight + paddingLeft, contentHeight+paddingTop);
            high.setBounds(width - paddingLeft-paddingRight-contentHeight, paddingTop, width-paddingRight, contentHeight+paddingTop);
            circleRadius = (int) (contentHeight/3.5);
            //mTextPaint.setTextSize((int)(circleRadius));
            //mTextPaint.getTextBounds("100",0,2,textBounds);
        }
        int cY = low.getBounds().centerY();
//        mTextPaint.setColor(Color.BLACK);
//        mTextPaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(low.getBounds().right, cY, high.getBounds().left, cY, mDrawPaint);
        low.draw(canvas);
        high.draw(canvas);
        mTextPaint.setColor(PINK_COLOR);
        mTextPaint.setStyle(Paint.Style.FILL);
        left = paddingLeft+contentHeight+circleRadius;
        right = getWidth()-paddingRight-contentHeight-circleRadius;
        cX = (int) (left+ (right-left)*level);
        canvas.drawCircle(cX,low.getBounds().centerY(),circleRadius,mTextPaint);
        mTextPaint.setColor(-1);
        mTextPaint.setStrokeWidth(mTextPaint.getStrokeWidth() / 2);
        //mTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(cX, cY, circleRadius, mTextPaint);

//        mTextPaint.setStrokeWidth(mTextPaint.getStrokeWidth() * 2);
//        mTextPaint.setStyle(Paint.Style.FILL);
//        canvas.drawText(""+getValue(),cX,cY-textBounds.exactCenterY(),mTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                int pX = (int) event.getX();
                if(Math.abs(pX-cX)>circleRadius*1.5){
                    return false;
                }break;
            case MotionEvent.ACTION_MOVE:
                level = (event.getX()-left)/(right-left);
                level = Math.min(level, 1f);
                level = Math.max(level,0f);
                if (valueChangeListener != null){
                    valueChangeListener.onSensitivityEditChanged(getValue());
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(true);
                if (valueChangeListener != null){
                    valueChangeListener.onSensitivityValueChanged(getValue());
                }
                break;
        }
        return true;
    }
}
