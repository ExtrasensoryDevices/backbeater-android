package com.esdevices.backbeater.activity;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import com.esdevices.backbeater.BuildConfig;
import com.esdevices.backbeater.R;
import com.esdevices.backbeater.audio.AudioService;

/**
 * Created by Alina Kholcheva on 2017-08-04.
 */

public class CalibrationActivity  extends Activity implements AudioService.AudioServiceCalibrationBeatListener {
    
    @Bind(R.id.thresholdText) EditText thresholdText;
    @Bind(R.id.sensitivityLabel) TextView sensitivityLabel;
    @Bind(R.id.sensitivityText) EditText sensitivityText;
    @Bind(R.id.beatText) TextView beatText;
    
    private AudioService audioService;
    
    private Handler handler;
    private ObjectAnimator anim;
    
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
    
        int startThreshold = 100;
        thresholdText.setText(""+startThreshold);
        audioService.setTestStartThreshold(startThreshold);
        
        //int startSensitivity = 75;
        //sensitivityText.setText(""+startSensitivity);
        //audioService.setSensitivity(startSensitivity);
        sensitivityLabel.setVisibility(View.GONE);
        sensitivityText.setVisibility(View.GONE);

        boolean allowed = permissionCheck();
        if (allowed) {
            audioService.startMeTest();
        }
    
        anim = ObjectAnimator.ofInt(beatText, "backgroundColor", Color.WHITE, Color.GREEN);
        anim.setDuration(100);
        anim.setEvaluator(new ArgbEvaluator());
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.setRepeatCount(1);
        
        beatText.setText("Version "+ BuildConfig.VERSION_NAME+ "("+BuildConfig.VERSION_CODE+")");
        
        
    }

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1001;
    private boolean permissionCheck() {
        // thisActivity is the current activity
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                showRequestPermissionRationale(R.string.dlg_mic_permission_msg_1);
                return false;
            } else {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.dlg_mic_permission_ttl);
                builder.setMessage(R.string.dlg_mic_permission_msg_3);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        ActivityCompat.requestPermissions(CalibrationActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                builder.show();

                // No explanation needed, we can request the permission.
                return false;
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    audioService.startMeTest();
                } else {
                    // permission denied
                    showRequestPermissionRationale(R.string.dlg_mic_permission_msg_2);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    private boolean showingRationaleDialog = false;
    private void showRequestPermissionRationale(@StringRes int msg) {
        if (showingRationaleDialog) { return; }
        showingRationaleDialog = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dlg_mic_permission_ttl);
        builder.setMessage(msg);
        builder.setPositiveButton(R.string.open_settings, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                showingRationaleDialog = false;
                openSettings();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                showingRationaleDialog = false;
            }
        });
        builder.show();
    }


    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
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
            double valueThreshold = Double.parseDouble(resThreshold);
            int valueSensitivity = Integer.parseInt(resSensitivity);
    
            if (editText == thresholdText) {
                audioService.setTestStartThreshold(valueThreshold);
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
                //beatText.setText("Start: \n ds=" + started_DS + "\ne=" + started_e + "\nEnd:\nds=" + ended_DS + "\ne=" + ended_e);
                String s = String.format("Start: \ne=%.2f\n\nEnd:\ne=%.2f", started_e, ended_e);
                beatText.setText(s);
                
                // animate
    
                anim.start();
                
                
            }
        });
    }
}
