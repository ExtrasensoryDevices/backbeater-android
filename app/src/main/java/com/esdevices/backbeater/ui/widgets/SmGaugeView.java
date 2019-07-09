package com.esdevices.backbeater.ui.widgets;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

public class SmGaugeView extends View {

    private static final String TAG = SmGaugeView.class.getSimpleName();

    public static final double DEFAULT_MAX_SPEED = 8;
    public static final int DEFAULT_MAJOR_TICK_COUNT = 8;
    public static final int DEFAULT_MINOR_TICKS = 1;
    public static final int DEFAULT_LABEL_TEXT_SIZE_DP = 12;

    private double maxSpeed = DEFAULT_MAX_SPEED;
    private double speed = 4;
    private int defaultColor = Color.rgb(180, 180, 180);
    private int majorTickStep = DEFAULT_MAJOR_TICK_COUNT;
    private int minorTicks = DEFAULT_MINOR_TICKS;
    private int backWidth = 6;
    private int offsetY = 80;
    private int majorTicksLength = 48;

    private int labelColor1 = 0xFFB51A00;
    private int labelColor2 = 0xFF4F7A28;

    private Paint backgroundPaint;
    private Paint backgroundInnerPaint;
    private Paint backgroundOuterPaint;

    private Paint needlePaint;
    private Paint ticksPaint;
    private Paint txtPaint1;
    private Paint txtPaint2;
    private Paint txtPaint0;
    private Paint unitsPaint;
    private Paint colorLinePaint;
    private int labelTextSize;

    String labelArray[] = {"slow", "-3", "-2", "-1", "", "+1", "+2", "+3", "fast"};

    private int targetNumber = 120;
    int outerCenterWidth = 28;//  36;
    int innerCenterWidth = 20;// 26;
    int needleWidth = 6;

    public SmGaugeView(Context context) {
        super(context);
        init();

        float density = getResources().getDisplayMetrics().density;
        setLabelTextSize(Math.round(DEFAULT_LABEL_TEXT_SIZE_DP * density));
    }

    public SmGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();

        float density = getResources().getDisplayMetrics().density;
        setLabelTextSize(Math.round(DEFAULT_LABEL_TEXT_SIZE_DP * density));
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(double maxSpeed) {
        if (maxSpeed <= 0)
            throw new IllegalArgumentException("Non-positive value specified as max speed.");
        this.maxSpeed = maxSpeed;
        invalidate();
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        if (speed < 0)
            throw new IllegalArgumentException("Non-positive value specified as a speed.");
        if (speed > maxSpeed)
            speed = maxSpeed;
        this.speed = speed;
        invalidate();
    }

    @TargetApi(11)
    public ValueAnimator setSpeed(double progress, long duration, long startDelay) {
        if (progress < 0)
            throw new IllegalArgumentException("Negative value specified as a speed.");

        if (progress > maxSpeed)
            progress = maxSpeed;

        ValueAnimator va = ValueAnimator.ofObject(new TypeEvaluator<Double>() {
            @Override
            public Double evaluate(float fraction, Double startValue, Double endValue) {
                return startValue + fraction * (endValue - startValue);
            }
        }, Double.valueOf(getSpeed()), Double.valueOf(progress));

        va.setDuration(duration);
        va.setStartDelay(startDelay);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                Double value = (Double) animation.getAnimatedValue();
                if (value != null)
                    setSpeed(value);
            }
        });
        va.start();
        return va;
    }

    @TargetApi(11)
    public ValueAnimator setSpeed(double progress, boolean animate) {
        return setSpeed(progress, 1500, 200);
    }

    public int getDefaultColor() {
        return defaultColor;
    }

    public void setDefaultColor(int defaultColor) {
        this.defaultColor = defaultColor;
        invalidate();
    }

    public int getMajorTickStep() {
        return majorTickStep;
    }

    public void setMajorTickStep(int majorTickStep) {
        if (majorTickStep <= 0)
            throw new IllegalArgumentException("Non-positive value specified as a major tick step.");
        this.majorTickStep = majorTickStep;
        invalidate();
    }

    public int getMinorTicks() {
        return minorTicks;
    }

    public void setMinorTicks(int minorTicks) {
        this.minorTicks = minorTicks;
        invalidate();
    }

    public int getLabelTextSize() {
        return labelTextSize;
    }

    public void setLabelTextSize(int labelTextSize) {
        this.labelTextSize = labelTextSize;
        if (txtPaint1 != null) {
            txtPaint1.setTextSize(labelTextSize);
            invalidate();
        }

        if (txtPaint2 != null) {
            txtPaint2.setTextSize(labelTextSize);
            invalidate();
        }

        if (txtPaint0 != null) {
            txtPaint0.setTextSize(labelTextSize + 8);
            invalidate();
        }
    }

    public int getTargetNumbergetTargetNumber() { return targetNumber; }

    public void setTargetNumber(int number) {
        if (targetNumber != number) {
            targetNumber = number;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Clear canvas
        canvas.drawColor(Color.TRANSPARENT);

        // Draw Metallic Arc and background
        drawBackground(canvas);

        // Draw Ticks and colored arc
        drawTicks(canvas);

        // Draw Needle
        drawNeedle(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
            //Must be this size
            width = widthSize;
        } else {
            width = -1;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST) {
            //Must be this size
            height = heightSize;
        } else {
            height = -1;
        }

        if (height >= 0 && width >= 0) {
            width = Math.min(height, width);
            height = width*12/20;
        } else if (width >= 0) {
            height = width/2;
        } else if (height >= 0) {
            width = height*2;
        } else {
            width = 0;
            height = 0;
        }

        //MUST CALL THIS
        setMeasuredDimension(width, height);
    }

    private void drawNeedle(Canvas canvas) {
        RectF oval = getOval(canvas, 1);
        float radius = oval.width()*0.5f - majorTicksLength - backWidth - 28-backWidth*2;
        RectF outerOval = new RectF(oval.centerX() - outerCenterWidth, oval.centerY() - outerCenterWidth,
                oval.centerX() + outerCenterWidth, oval.centerY() + outerCenterWidth);
        RectF innerOval = new RectF(oval.centerX() - innerCenterWidth, oval.centerY() - innerCenterWidth,
                oval.centerX() + innerCenterWidth, oval.centerY() + innerCenterWidth);

        float angle = (float) (getSpeed()/ getMaxSpeed()*180);
        canvas.drawLine(
                (float) (oval.centerX() + Math.cos((180 - angle) / 180 * Math.PI) * outerOval.width() *0.5f),
                (float) (oval.centerY() - Math.sin(angle / 180 * Math.PI) * outerOval.width()*0.5f),
                (float) (oval.centerX() + Math.cos((180 - angle) / 180 * Math.PI) * (radius)),
                (float) (oval.centerY() - Math.sin(angle / 180 * Math.PI) * (radius)),
                needlePaint
        );

        canvas.drawArc(outerOval, 0, 360, true, backgroundOuterPaint);
        canvas.drawArc(innerOval, 0, 360, true, backgroundInnerPaint);
    }

    private void drawTicks(Canvas canvas) {
        float availableAngle = 180;
        float majorStep = (availableAngle / majorTickStep);
        float majorTicksLength = 48;

        RectF oval = getOval(canvas, 1);
        float radius = oval.width()*0.5f;

        float currentAngle = 0;

        float radius2 = radius - backWidth/2;
        float radius1 = radius2 - majorTicksLength;

        for (int iStep = 0; iStep <= majorTickStep; iStep ++) {

            canvas.drawLine(
                    (float) (oval.centerX() + Math.cos((180 - currentAngle) / 180 * Math.PI) * radius1),
                    (float) (oval.centerY() - Math.sin(currentAngle / 180 * Math.PI) * radius1),
                    (float) (oval.centerX() + Math.cos((180 - currentAngle) / 180 * Math.PI) * radius2),
                    (float) (oval.centerY() - Math.sin(currentAngle / 180 * Math.PI) * radius2),
                    ticksPaint
            );

            canvas.save();
            canvas.rotate(180 + currentAngle, oval.centerX(), oval.centerY());
            float txtX = oval.centerX() + radius1 - majorTicksLength / 2;
            float txtY = oval.centerY();
            canvas.rotate(180 - currentAngle, txtX, txtY);
            if (iStep < 4) {
                canvas.drawText(labelArray[iStep], txtX, txtY, txtPaint1);
            }
            else if (iStep > 4) {
                canvas.drawText(labelArray[iStep], txtX, txtY, txtPaint2);
            }
            else {
                canvas.drawText("" + targetNumber, txtX, txtY, txtPaint0);
            }

            canvas.restore();

            currentAngle += majorStep;
        }
    }

    private RectF getOval(Canvas canvas, float factor) {
        RectF oval;
        final int canvasWidth = canvas.getWidth() - getPaddingLeft() - getPaddingRight()-backWidth*2;
        final int canvasHeight = canvas.getHeight() - getPaddingTop() - getPaddingBottom();

        if (canvasHeight*2 >= canvasWidth) {
            oval = new RectF(backWidth, 0, canvasWidth*factor, canvasWidth*factor);
        } else {
            oval = new RectF(backWidth, 0, canvasHeight*2*factor, canvasHeight*2*factor);
        }

        oval.offset((canvasWidth-oval.width())/2 + getPaddingLeft(), (canvasHeight*2-oval.height())/2 + getPaddingTop());
        oval.top -= offsetY/2;
        oval.bottom -= offsetY/2;

        return oval;
    }

    private void drawBackground(Canvas canvas) {
        RectF oval = getOval(canvas, 1);
        canvas.drawArc(oval, 180, 180, false, backgroundPaint);
    }

    @SuppressWarnings("NewApi")
    private void init() {
        if (Build.VERSION.SDK_INT >= 11 && !isInEditMode()) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        backgroundPaint.setStyle(Paint.Style.FILL);

        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        backgroundPaint.setStrokeWidth(backWidth);

        backgroundPaint.setColor(Color.rgb(221, 47, 68));

        backgroundInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundInnerPaint.setStyle(Paint.Style.FILL);
        backgroundInnerPaint.setColor(Color.rgb(156, 156, 156));

        backgroundOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundOuterPaint.setStyle(Paint.Style.FILL);
        backgroundOuterPaint.setColor(Color.rgb(200, 200, 200));

        txtPaint0 = new Paint(Paint.ANTI_ALIAS_FLAG);
        txtPaint0.setColor(Color.WHITE);
        txtPaint0.setTextSize(labelTextSize + 8);
        txtPaint0.setTextAlign(Paint.Align.CENTER);
        txtPaint0.setLinearText(true);

        txtPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        txtPaint1.setColor(labelColor1);
        txtPaint1.setTextSize(labelTextSize);
        txtPaint1.setTextAlign(Paint.Align.CENTER);
        txtPaint1.setLinearText(true);

        txtPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        txtPaint2.setColor(labelColor2);
        txtPaint2.setTextSize(labelTextSize);
        txtPaint2.setTextAlign(Paint.Align.CENTER);
        txtPaint2.setLinearText(true);

        ticksPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ticksPaint.setStrokeWidth(3.0f);
        ticksPaint.setStyle(Paint.Style.STROKE);
        ticksPaint.setColor(defaultColor);

        colorLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        colorLinePaint.setStyle(Paint.Style.STROKE);
        colorLinePaint.setStrokeCap(Paint.Cap.ROUND);
        colorLinePaint.setStrokeWidth(needleWidth);
        colorLinePaint.setColor(defaultColor);

        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setStrokeWidth(needleWidth);
        needlePaint.setStyle(Paint.Style.STROKE);
        needlePaint.setStrokeCap(Paint.Cap.ROUND);
        needlePaint.setColor(Color.rgb(202, 202, 202));
    }


}
