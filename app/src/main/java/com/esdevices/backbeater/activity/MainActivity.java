package com.esdevices.backbeater.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.esdevices.backbeater.BuildConfig;
import com.esdevices.backbeater.R;
import com.esdevices.backbeater.audio.AudioService;
import com.esdevices.backbeater.model.Song;
import com.esdevices.backbeater.ui.widgets.BBTextView;
import com.esdevices.backbeater.ui.widgets.NumberButton;
import com.esdevices.backbeater.ui.widgets.PopoverView;
import com.esdevices.backbeater.ui.widgets.SensitivitySlider;
import com.esdevices.backbeater.ui.widgets.SlideButton;
import com.esdevices.backbeater.ui.widgets.SmGaugeView;
import com.esdevices.backbeater.ui.widgets.TempoDisplay;
import com.esdevices.backbeater.utils.Constants;
import com.esdevices.backbeater.utils.DialogHelper;
import com.esdevices.backbeater.utils.NetworkInfoHelper;
import com.esdevices.backbeater.utils.Preferences;
import com.esdevices.backbeater.utils.UsbScanner;
import com.flurry.android.FlurryAgent;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends Activity implements SlideButton.StateChangeListener, SensitivitySlider.ValueChangeListener,
    AudioService.AudioServiceBeatListener, PopoverView.PopoverViewDelegate {
    
    static final int EDIT_SONG_LIST_REQUEST = 1;

    private int sound = 1;
    private int window = 2;
    private int beat = 1;
    private int sensitivity = 100;
    
    private List<Song> songList;
    private int currentSongIndex = -1;
    
    private AudioService audioService;
    private Handler handler;
    
    
    @Bind(R.id.drawerLayout) DrawerLayout drawerLayout;
    
    @Bind(R.id.tempoDisplay) TempoDisplay tempoDisplay;
    @Bind(R.id.sensitivitySlider) SensitivitySlider sensitivitySlider;
    @Bind(R.id.window2) NumberButton window2Button;
    @Bind(R.id.window3) NumberButton window3Button;
    @Bind(R.id.window4) NumberButton window4Button;
    @Bind(R.id.window5) NumberButton window5Button;
    @Bind(R.id.beat1) NumberButton beat1Button;
    @Bind(R.id.beat2) NumberButton beat2Button;
    @Bind(R.id.beat3) NumberButton beat3Button;
    @Bind(R.id.beat4) NumberButton beat4Button;
    @Bind(R.id.soundDrum) ImageView drumButton;
    @Bind(R.id.soundSticks) ImageView sticksButton;
    @Bind(R.id.soundMetronom) ImageView metronomeButton;
    @Bind(R.id.soundSurprise) ImageView surpriseButton;
    @Bind(R.id.textVersion) BBTextView versionNumber;
    
    @Bind(R.id.getSensorButton) View getSensorButton;
    @Bind(R.id.setTempoButton) View setTempoButton;
    @Bind(R.id.tempoSlideButton) SlideButton tempoSlideButton;
    
    @Bind(R.id.songListButton) ImageView songListButton;
    @Bind(R.id.songListLayout) View songListView;
    @Bind(R.id.songNameText) BBTextView songNameText;
    @Bind(R.id.prevButton) ImageView prevButton;
    @Bind(R.id.nextButton) ImageView nextButton;

    @Bind(R.id.progressIndicator) ProgressBar progressIndicator;
    @Bind(R.id.gaugeView)  SmGaugeView gaugeView;

    private UsbScanner usbScanner;

    private final Object lock = new Object();

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            synchronized (MainActivity.this.lock) {
                // looking for thre microphone
                if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                    boolean hasMicrophone = intent.getIntExtra("microphone", -1) == 1;

                    // if found
                    if (hasMicrophone) {
                        int state = intent.getIntExtra("state", -1);
                        boolean detected = state == 1;
                        handleSensorDetected(detected, false);
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    UsbScanner usbScanner = MainActivity.this.usbScanner;
                    if (usbScanner != null) {
                        boolean shouldCheckForMic = usbScanner.isUsbClassAudioDeviceConnected();
                        if (shouldCheckForMic){
                            startWaitingForMic();
                            usbScanner.checkForUSBMic();
                        }
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    handleSensorDetected(false, false);
                }
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    
        drawerLayout.setScrimColor(getResources().getColor(R.color.main_color_transparent));
        
        handler = new Handler(getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

            }
        };
    
        audioService = new AudioService();
        audioService.setBeatListener(this);
        
        versionNumber.setText("Version "+ BuildConfig.VERSION_NAME+ "("+BuildConfig.VERSION_CODE+")");


        if (android.os.Build.VERSION.SDK_INT >= 23) {
            usbScanner = new UsbScanner(this, new UsbScanner.UsbMicrophoneListener() {
                @Override
                public void onUSBScanComplete(boolean microphoneFound) {
                    handleSensorDetected(microphoneFound, false);
                }
            });
        }
        
        // RESTORE SETTINGS
        handleSensorDetected(false, false);
        
        setSound(Preferences.getSound(sound));
        setWindow(Preferences.getWindow(window), false);
        setBeat(Preferences.getBeat(beat), false);
        
        setSensitivity(Preferences.getSensitivity(sensitivity));
        sensitivitySlider.setValue(sensitivity);
        sensitivitySlider.setValueChangeListener(this);
        
        tempoSlideButton.setStateChangeListener(this);
        
        updateSongList();

    }


   @Override
    protected void onResume() {
       super.onResume();

       synchronized (lock) {
           // check if USB mic is pluged in already
           if (usbScanner != null) {
               boolean shouldCheckForMic = usbScanner.isUsbClassAudioDeviceConnected();
               if (shouldCheckForMic){
                   startWaitingForMic();
               }
           }
       }

       // register sensor listener
       IntentFilter intentFilter = new IntentFilter();
       intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
       intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
       intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

       getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);
   }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister sensor listener
        try {getApplicationContext().unregisterReceiver(broadcastReceiver); } catch (Exception e) {};
        // stop audio service
        audioService.stopMe();
        if (tempoSlideButton.isSelected()) {
            tempoSlideButton.toggle();
            tempoDisplay.setMetronomeOff();
        }
        // save CPT
        Preferences.putCPT(tempoDisplay.getCPT());

        // stop waiting for USB mic
        if (usbScanner != null) {
            usbScanner.stopCheckingForUSBMic();
        }
        stopWaitingForMic(false);
    }


    private void startWaitingForMic() {
        getSensorButton.setVisibility(View.INVISIBLE);
        setTempoButton.setVisibility(View.INVISIBLE);
        progressIndicator.setVisibility(View.VISIBLE);
    }

    private void stopWaitingForMic(boolean micConnected){
        getSensorButton.setVisibility(micConnected ? View.INVISIBLE : View.VISIBLE);
        setTempoButton.setVisibility(micConnected ? View.VISIBLE : View.INVISIBLE);
        progressIndicator.setVisibility(View.INVISIBLE);
    }
    
    private boolean sensorPluggedIn = false;
    private void handleSensorDetected(boolean pluggedIn, boolean permissionCheckInProgress){

        if (this.sensorPluggedIn && audioService.isRunning() && pluggedIn) {
            // already listening to sound, do nothing
            return;
        }

        sensorPluggedIn = pluggedIn;

        if (permissionCheckInProgress) {
            stopWaitingForMic(false);
            tempoDisplay.handleTap = true;
            audioService.stopMe();
            return;
        }

        if (pluggedIn) {
            boolean allowed = permissionCheck();
            if (allowed) {
                // sensor plugged in
                stopWaitingForMic(true);
                tempoDisplay.handleTap = false;
                audioService.startMe();
            }
        } else {
            // sensor unplugged
            stopWaitingForMic(false);
            tempoDisplay.handleTap = true;
            audioService.stopMe();
        }
    }
    
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EDIT_SONG_LIST_REQUEST) {
            if (resultCode == RESULT_OK) { // data changed
                updateSongList();
            }
        }
    }
    
    
    //================================================================================
    //  Song list
    //================================================================================
    
    
    
    
    private void updateSongList() {
        songList = Preferences.getSongList();
        setCurrentSongIndex(0);
        if (currentSongIndex != -1) {
            tempoSlideButton.setValue(songList.get(currentSongIndex).tempo);
        }
    }
    
    private void updateSongListView() {
        int count = songList.size();
        if (count <= 0) {
            songListView.setVisibility(View.GONE);
            songListButton.setImageResource(R.drawable.tempo_list);
        } else {
            // count > 0
            songListView.setVisibility(View.VISIBLE);
            songListButton.setImageResource(R.drawable.tempo_list_select);
            songNameText.setText(songList.get(currentSongIndex).name.toUpperCase());
            prevButton.setVisibility(count == 1 ? View.GONE : View.VISIBLE);
            nextButton.setVisibility(count == 1 ? View.GONE : View.VISIBLE);
        }
    }
    
    private void setCurrentSongIndex(int index) {
        int count = songList.size();
        if (count <= 0) { // song list is empty
            currentSongIndex = -1;
        } else { // handle out of bounds
            currentSongIndex = (index + count) % count;
        }
        updateSongListView();
    }
    
    
    //================================================================================
    //  Hit processing
    //================================================================================
    
    public void setTempo(int tempo, boolean startMetronome) {
        tempo = Math.min(Constants.MAX_TEMPO, (Math.max(Constants.MIN_TEMPO, tempo)));
        tempoSlideButton.setValue(tempo);
        // update metronome if needed
        if (!tempoSlideButton.isSelected() && startMetronome) {
            tempoSlideButton.toggle();
        }
        if (tempoSlideButton.isSelected()) {
            gaugeView.setTargetNumber(tempoSlideButton.getValue());
            tempoDisplay.setMetronomeOn(Constants.Sound.fromIndex(sound), tempoSlideButton.getValue());
        }
    }
    
    
    
    @Override
    public void onBeat(final long hitTime){
        handler.post(new Runnable() {
            @Override
            public void run() {
                tempoDisplay.beat(hitTime);
            }
        });
    }

    
    //================================================================================
    //  Buttons
    //================================================================================
    
    @OnClick(R.id.getSensorButton)
    public void onGetSensorButtonClick(View v) {
        if (!NetworkInfoHelper.isNetworkAvailable()) {
            DialogHelper.showNoNetworkMessage(this);
            return;
        }
        
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(Constants.BUY_SENSOR_URL));
        startActivity(i);
    }
    
    @OnClick(R.id.setTempoButton)
    public void onSetTempoButtonClick(View v) {
        setTempo(tempoDisplay.getCPT(), true);
    }
    
    
    @OnClick(R.id.songListButton)
    public void onSongListButtonClick(View v) {
        Intent intent = new Intent(this, SongListActivity.class);
        startActivityForResult(intent, EDIT_SONG_LIST_REQUEST);
    }
    
    @OnClick(R.id.nextButton)
    public void onNextButtonClick(View v){
        setCurrentSongIndex(currentSongIndex + 1);
        setTempo(songList.get(currentSongIndex).tempo, false);
        
    }
    
    @OnClick(R.id.prevButton)
    public void onPrevButtonClick(View v){
        setCurrentSongIndex(currentSongIndex - 1);
        setTempo(songList.get(currentSongIndex).tempo, false);
    }
    
    @Override public void onToggle(boolean isOn) {
        if (isOn) {
            tempoDisplay.setMetronomeOn(Constants.Sound.fromIndex(sound), tempoSlideButton.getValue());
        } else {
            tempoDisplay.setMetronomeOff();
        }
    
        FlurryAgent.logEvent(Constants.FLURRY_METRONOME_STATE_CHANGED, Constants.buildFlurryParams("value", isOn? "1" : "0"));
    }
    
    // Tempo Slide Button
    @Override public void onValueChanged(int newValue) {
        if (tempoSlideButton.isSelected()) {
            gaugeView.setTargetNumber(tempoSlideButton.getValue());
            tempoDisplay.setMetronomeOn(Constants.Sound.fromIndex(sound), tempoSlideButton.getValue());
        }
        FlurryAgent.logEvent(Constants.FLURRY_METRONOME_TEMPO_VALUE_CHANGED, Constants.buildFlurryParams("value", ""+newValue));
    
    }
    
    @Override public void onSensitivityValueChanged(int newValue) {
        setSensitivity(newValue);
        FlurryAgent.logEvent(Constants.FLURRY_SENSITIVITY_VALUE_CHANGED, Constants.buildFlurryParams("value", ""+newValue));
    }
    
    
    
    
    //================================================================================
    //  NAV DRAWER
    //================================================================================
 
     public void setBeat(int beat, boolean logFlurry){
        beat1Button.enable(beat==1);
        beat2Button.enable(beat==2);
        beat3Button.enable(beat==3);
        beat4Button.enable(beat==4);
        this.beat = beat;
        Preferences.putBeat(beat);
        tempoDisplay.setBeat(beat);
        
        if (logFlurry) {
            FlurryAgent.logEvent(Constants.FLURRY_TIME_SIGNATURE_VALUE_CHANGED,
                Constants.buildFlurryParams("value", "" + beat));
        }
    
    
     }

    public void setWindow(int window, boolean logFlurry){
        window2Button.enable(window==2);
        window3Button.enable(window==3);
        window4Button.enable(window==4);
        window5Button.enable(window==5);
        this.window = window;
        Preferences.putWindow(window);
        tempoDisplay.setWindow(window);
        if (logFlurry) {
            FlurryAgent.logEvent(Constants.FLURRY_STRIKES_WINDOW_VALUE_CHANGED,
                Constants.buildFlurryParams("value", "" + window));
        }
    }

    public void setSound(int sound){
        drumButton.setColorFilter     (sound==0 ? Color.BLACK : Color.WHITE, Mode.MULTIPLY);
        sticksButton.setColorFilter   (sound==1 ? Color.BLACK : Color.WHITE, Mode.MULTIPLY);
        metronomeButton.setColorFilter(sound==2 ? Color.BLACK : Color.WHITE, Mode.MULTIPLY);
        surpriseButton.setColorFilter (sound==3 ? Color.BLACK : Color.WHITE, Mode.MULTIPLY);
        this.sound = sound;
        Preferences.putSound(sound);
        tempoDisplay.setMetronomeSond(Constants.Sound.fromIndex(this.sound));
    }
    
    public void setSensitivity(int sensitivity) {
        this.sensitivity = sensitivity;
        audioService.setSensitivity(this.sensitivity);
        Preferences.putSensitivity(sensitivity);
    }
    
    @OnClick({R.id.window2, R.id.window3, R.id.window4, R.id.window5,
        R.id.beat1, R.id.beat2, R.id.beat3, R.id.beat4,
        R.id.soundDrum, R.id.soundSticks, R.id.soundMetronom, R.id.soundSurprise,
        R.id.aboutButton,R.id.avgHelpButton, R.id.beatHelpButton,
        R.id.menuButton})
    public void onSettingsClick(View v) {
        switch (v.getId()){
            case R.id.soundDrum:
                setSound(0);
                break;
            case R.id.soundSticks:
                setSound(1);
                break;
            case R.id.soundMetronom:
                setSound(2);
                break;
            case R.id.soundSurprise:
                setSound(3);
                break;
            case R.id.window2:
                setWindow(2, true);
                break;
            case R.id.window3:
                setWindow(3, true);
                break;
            case R.id.window4:
                setWindow(4, true);
                break;
            case R.id.window5:
                setWindow(5, true);
                break;
            case R.id.beat1:
                setBeat(1, true);
                break;
            case R.id.beat2:
                setBeat(2, true);
                break;
            case R.id.beat3:
                setBeat(3, true);
                break;
            case R.id.beat4:
                setBeat(4, true);
                break;
            case R.id.aboutButton:
                drawerLayout.closeDrawer(Gravity.LEFT);
                showAbout();
                break;
            case R.id.menuButton:
                drawerLayout.openDrawer(Gravity.LEFT);
                break;
            case R.id.avgHelpButton:
                showHelpPopover(R.id.avgHelpButton);
                break;
            case R.id.beatHelpButton:
                showHelpPopover(R.id.beatHelpButton);
                break;
        }
    }
    
    private void showAbout() {
        if (!NetworkInfoHelper.isNetworkAvailable()) {
            DialogHelper.showNoNetworkMessage(this);
            return;
        }
        
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    private void showHelpPopover(int buttonId) {
        RelativeLayout rootView = findViewById(R.id.pop_bar);

        PopoverView popoverView = new PopoverView(this, R.layout.popover_showed_view);
        int resid;
        View v = findViewById(buttonId);
        if (buttonId == R.id.avgHelpButton) {
            popoverView.setContentSizeForViewInPopover(new Point(480, 220));
            resid = R.string.avg_help_text;
        }
        else {
            popoverView.setContentSizeForViewInPopover(new Point(480, 340));
            resid = R.string.beat_help_text;
        }

        popoverView.setDelegate(null);

        popoverView.showPopoverFromRectInViewGroup(rootView, resid, PopoverView.getFrameForView(v), PopoverView.PopoverArrowDirectionAny, true);
    }
    
    //================================================================================
    //  PERMISSIONS
    //================================================================================
    
    
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1001;
    private boolean permissionCheck() {
        // thisActivity is the current activity
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                handleSensorDetected(false, true);
                showRequestPermissionRationale(R.string.dlg_mic_permission_msg_1);
                return false;
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
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
                    handleSensorDetected(sensorPluggedIn, false);
                } else {
                    // permission denied
                    handleSensorDetected(false, false);
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

    @Override
    public void popoverViewWillShow(PopoverView view) {
        Log.i("POPOVER", "Will show");
    }

    @Override
    public void popoverViewDidShow(PopoverView view) {
        Log.i("POPOVER", "Did show");
    }

    @Override
    public void popoverViewWillDismiss(PopoverView view) {
        Log.i("POPOVER", "Will dismiss");
    }

    @Override
    public void popoverViewDidDismiss(PopoverView view) {
        Log.i("POPOVER", "Did dismiss");
    }
}
