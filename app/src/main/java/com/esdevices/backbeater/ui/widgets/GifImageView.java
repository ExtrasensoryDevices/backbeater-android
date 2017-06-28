package com.esdevices.backbeater.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.net.Uri;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by Alina Kholcheva on 2017-06-27.
 */

public class GifImageView extends View {
    
    private InputStream mInputStream;
    private Movie mMovie;
    private int mWidth, mHeight;
    private long mStart;
    private Context mContext;
    
    private int duration = 1000;
    
    public GifImageView(Context context) {
        super(context);
        this.mContext = context;
    }
    
    public GifImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public GifImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        
        int count = attrs.getAttributeCount();
        for (int i=0; i<count; i++) {
            if (attrs.getAttributeName(i).equals("background")) {
                int id = Integer.parseInt(attrs.getAttributeValue(i).substring(1));
                setGifImageResource(id);
            }
        }
    }
    
    private void init() {
        setFocusable(false);
        mMovie = Movie.decodeStream(mInputStream);
        mWidth = mMovie.width();
        mHeight = mMovie.height();
        
        requestLayout();
    }
    
    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mWidth, mHeight);
    }
    
    @Override protected void onDraw(Canvas canvas) {
        
        long now = SystemClock.uptimeMillis();
        
        if (mStart == 0) {
            mStart = now;
        }
        
        if (mMovie != null) {
            
            int duration = mMovie.duration();
            if (duration == 0) {
                duration = 1000;
            }
            
            int relTime = (int) ((now - mStart) % duration);
            
            mMovie.setTime(relTime);
            
            mMovie.draw(canvas, 0, 0);
            invalidate();
        }
    }
    
    public void setGifImageResource(int id) {
        if (this.isInEditMode()) {
            return;
        }
        mInputStream = mContext.getResources().openRawResource(id);
        init();
    }
    
    public void setGifImageUri(Uri uri) {
        try {
            mInputStream = mContext.getContentResolver().openInputStream(uri);
            init();
        } catch (FileNotFoundException e) {
            Log.e("GIfImageView", "File not found");
        }
    }
}
