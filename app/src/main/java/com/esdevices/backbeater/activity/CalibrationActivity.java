package com.esdevices.backbeater.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import com.esdevices.backbeater.R;
import com.esdevices.backbeater.audio.AudioService;

/**
 * Created by Alina Kholcheva on 2017-08-04.
 */

public class CalibrationActivity  extends Activity implements AudioService.AudioServiceCalibrationBeatListener {
    
    @Bind(R.id.thresholdText) EditText thresholdText;
    @Bind(R.id.sensitivityText) EditText sensitivityText;
    @Bind(R.id.beatText) TextView beatText;
    
    private AudioService audioService;
    
    private Handler handler;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
        ButterKnife.bind(this);
    
        handler = new Handler(getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            
            }
        };
    
        audioService = new AudioService();
        audioService.setCalibrationBeatListener(this);
        
        audioService.startMe();
        
        
    }
    
    @Override protected void onPause() {
        super.onPause();
        audioService.stopMe();
    }
    
    @OnEditorAction(R.id.thresholdText)
    boolean thresholdEditorAction(int actionId) {
        if (thresholdText.hasFocus()) {
            handleEnter(thresholdText);
            return false;
        }
        return false;
    }
    
    @OnEditorAction(R.id.sensitivityText)
    boolean sensitivityEditorAction(int actionId) {
        if (sensitivityText.hasFocus()) {
            handleEnter(sensitivityText);
            return false;
        }
        return false;
    }
    
    private void handleEnter(EditText editText) {
        hideKeyboard();
        editText.clearFocus();
    
        String resThreshold = thresholdText.getText().toString().trim();
        String resSensitivity = sensitivityText.getText().toString().trim();
        try {
            int valueThreshold = Integer.parseInt(resThreshold);
            int valueSensitivity = Integer.parseInt(resSensitivity);
    
            if (editText == thresholdText) {
                //audioService.setTestStartThreshold(valueThreshold);
            } else if (editText == sensitivityText) {
                audioService.setSensitivity(valueSensitivity);
            }
        } catch (NumberFormatException e) {}
    }
    
    private void hideKeyboard(){
        InputMethodManager imm = (InputMethodManager) CalibrationActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindow().getDecorView().getRootView().getWindowToken(), 0);
    }
    
    @Override
    public void onBeat(final short started_DS, final double started_e, final short ended_DS, final double ended_e) {
        handler.post(new Runnable() {
            @Override public void run() {
                beatText.setText("Start: \n ds=" + started_DS + "\ne=" + started_e + "\nEnd:\nds=" + ended_DS + "\ne=" + ended_e);
            }
        });
    }
}
