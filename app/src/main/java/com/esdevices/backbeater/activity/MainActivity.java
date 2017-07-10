package com.esdevices.backbeater.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;

import com.esdevices.backbeater.audio.AudioService;
import com.esdevices.backbeater.BuildConfig;
import com.esdevices.backbeater.R;
import com.esdevices.backbeater.model.Song;
import com.esdevices.backbeater.ui.widgets.BBTextView;
import com.esdevices.backbeater.ui.widgets.NumberButton;
import com.esdevices.backbeater.ui.widgets.TempoDisplay;

import butterknife.ButterKnife;
import butterknife.Bind;
import butterknife.OnClick;
import com.esdevices.backbeater.utils.Constants;
import com.esdevices.backbeater.utils.DialogHelper;
import com.esdevices.backbeater.utils.NetworkInfoHelper;
import com.esdevices.backbeater.utils.Preferences;
import java.util.List;

public class MainActivity extends Activity{
    
    static final int EDIT_SONG_LIST_REQUEST = 1;

    private static final int BLACK = -16777216;
    
    private int sound = 1;
    private int window = 16;
    private int beat = 4;
    private int sensitivity = 100;
    
    private List<Song> songList;
    private int currentSongIndex = -1;

    AudioService audioService;
    Handler handler;
    @Bind(R.id.tempoDisplay) TempoDisplay tempoDisplay;
    @Bind(R.id.window2) NumberButton window2Button;
    @Bind(R.id.window4) NumberButton window4Button;
    @Bind(R.id.window8) NumberButton window8Button;
    @Bind(R.id.window16) NumberButton window16Button;
    @Bind(R.id.beat1) NumberButton beat1Button;
    @Bind(R.id.beat2) NumberButton beat2Button;
    @Bind(R.id.beat3) NumberButton beat3Button;
    @Bind(R.id.beat4) NumberButton beat4Button;
    @Bind(R.id.soundDrum) ImageView drumButton;
    @Bind(R.id.soundSticks) ImageView sticksButton;
    @Bind(R.id.soundMetronom) ImageView metronomeButton;
    @Bind(R.id.soundSurprise) ImageView surpriseButton;
    @Bind(R.id.textVersion) BBTextView versionNumber;
    @Bind(R.id.drawerLayout) DrawerLayout mDrawerLayout;
    
    @Bind(R.id.getSensorButton) View getSensorButton;
    @Bind(R.id.setTempoButton) View setTempoButton;
    
    @Bind(R.id.songListButton) ImageView songListButton;
    @Bind(R.id.songListLayout) View songListView;
    @Bind(R.id.songNameText) BBTextView songNameText;
    @Bind(R.id.prevButton) ImageView prevButton;
    @Bind(R.id.nextButton) ImageView nextButton;
    
    
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                boolean hasMicrophone = intent.getIntExtra("microphone", -1) == 1;
                int state = intent.getIntExtra("state", -1);
                handleSensorDetected(hasMicrophone && state == 1);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        
        handler = new Handler(getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

            }
        };
    
        audioService = new AudioService(this);
        
        versionNumber.setText("Version "+ BuildConfig.VERSION_NAME+ "("+BuildConfig.VERSION_CODE+")");
        
        // RESTORE SETTINGS
        handleSensorDetected(false);
        
        setSound(Preferences.getSound(sound));
        setWindow(Preferences.getWindow(window));
        setBeat(Preferences.getBeat(beat));
        sensitivity = Preferences.getSensitivity(sensitivity);
        
        updateSongList();

    }


    public void beat(final double beatsPerMinute){
        handler.post(new Runnable() {
            @Override
            public void run() {
                tempoDisplay.setTempo((int) (beatsPerMinute));
//                ObjectAnimator.ofFloat(circle,"scaleX",0,1f,0).start();
//                ObjectAnimator.ofFloat(circle,"scaleY",0,1f,0).start();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // register sensor listener
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        getApplicationContext().registerReceiver(broadcastReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister sensor listener
        getApplicationContext().unregisterReceiver(broadcastReceiver);
        audioService.stopMe();
    }
    
    
    private void handleSensorDetected(boolean pluggedIn){
        if (pluggedIn) {
            // sensor plugged in
            getSensorButton.setVisibility(View.INVISIBLE);
            setTempoButton.setVisibility(View.VISIBLE);
            tempoDisplay.handleTap = false;
            audioService.start();
        } else {
            // sensor unplugged
            getSensorButton.setVisibility(View.VISIBLE);
            setTempoButton.setVisibility(View.INVISIBLE);
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
    
    
    private void updateSongList() {
        songList = Preferences.getSongList();
        setCurrentSongIndex(0);
    }
    
    private void updateSongListView() {
        int count = songList.size();
        if (count <= 0) {
            songListView.setVisibility(View.GONE);
            songListButton.setImageResource(R.drawable.tempo_list);
            return;
        }
        // count > 0
        songListView.setVisibility(View.VISIBLE);
        songListButton.setImageResource(R.drawable.tempo_list_select);
        songNameText.setText(songList.get(currentSongIndex).name);
        prevButton.setVisibility(count == 1 ? View.GONE : View.VISIBLE);
        nextButton.setVisibility(count == 1 ? View.GONE : View.VISIBLE);
    }
    
    private void setCurrentSongIndex(int index) {
        int count = songList.size();
        if (count <= 0) { // song list is empty
            currentSongIndex = -1;
        } else { // handle out of bounds
            currentSongIndex = (index + count) % count;
        }
        updateSongListView();
        setTempo(songList.get(currentSongIndex).tempo);
    }
    
    
    public void setTempo(int tempo) {
        //TODO:
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
        
    }
    
    
    @OnClick(R.id.songListButton)
    public void onSongListButtonClick(View v) {
        Intent intent = new Intent(this, SongListActivity.class);
        startActivityForResult(intent, EDIT_SONG_LIST_REQUEST);
    }
    
    @OnClick(R.id.nextButton)
    public void onNextButtonClick(View v){
        setCurrentSongIndex(currentSongIndex + 1);
        
    }
    
    @OnClick(R.id.prevButton)
    public void onPrevButtonClick(View v){
        setCurrentSongIndex(currentSongIndex - 1);
    }
    
    //================================================================================
    //  NAV DRAWER
    //================================================================================
 
     public void setBeat(int beat){
        beat1Button.enable(beat==1);
        beat2Button.enable(beat==2);
        beat3Button.enable(beat==3);
        beat4Button.enable(beat==4);
        this.beat = beat;
        Preferences.putBeat(beat);
    }

    public void setWindow(int window){
        window16Button.enable(window==16);
        window2Button.enable(window==2);
        window4Button.enable(window==4);
        window8Button.enable(window==8);
        this.window = window;
        Preferences.putWindow(window);

    }

    public void setSound(int sound){
        drumButton.setColorFilter(sound==0?BLACK:-1,android.graphics.PorterDuff.Mode.MULTIPLY);
        sticksButton.setColorFilter(sound==1?BLACK:-1,android.graphics.PorterDuff.Mode.MULTIPLY);
        metronomeButton.setColorFilter(sound==2?BLACK:-1,android.graphics.PorterDuff.Mode.MULTIPLY);
        surpriseButton.setColorFilter(sound==3?BLACK:-1,android.graphics.PorterDuff.Mode.MULTIPLY);
        this.sound = sound;
        Preferences.putSound(sound);

    }
    @OnClick({R.id.window2, R.id.window4, R.id.window8, R.id.window16,
        R.id.beat1, R.id.beat2, R.id.beat3, R.id.beat4,
        R.id.soundDrum, R.id.soundSticks, R.id.soundMetronom, R.id.soundSurprise,
        R.id.aboutButton,
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
                setWindow(2);
                break;
            case R.id.window4:
                setWindow(4);
                break;
            case R.id.window8:
                setWindow(8);
                break;
            case R.id.window16:
                setWindow(16);
                break;
            case R.id.beat1:
                setBeat(1);
                break;
            case R.id.beat2:
                setBeat(2);
                break;
            case R.id.beat3:
                setBeat(3);
                break;
            case R.id.beat4:
                setBeat(4);
                break;
            case R.id.aboutButton:
                mDrawerLayout.closeDrawer(Gravity.LEFT);
                showAbout();
                break;
            case R.id.menuButton:
                mDrawerLayout.openDrawer(Gravity.LEFT);
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
    
    
    
    
}
