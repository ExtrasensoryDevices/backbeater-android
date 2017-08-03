package com.esdevices.backbeater.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import com.esdevices.backbeater.App;

/**
 * Created by aeboyd on 7/13/15.
 */
public class AudioService {
    
    public interface AudioServiceBeatListener{
        void onBeat(long hitTime);
    }
    
    private static final String TAG = "AudioRecorder";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int SAMPLES_PER_FRAME = 1024;
    private int buffer_size;
    private boolean running = false;
    
    private double HIGH_THRESHOLD = 4000*4000;
    private double LOW_THRESHOLD = 1000*1000    ;
    
    private AudioServiceBeatListener beatListener;
    
    private EnergyFunction energyFunction = new EnergyFunction();
    
    
    private AudioRecord audioRecord;

    public AudioService() {
        updateThreshold();
        int min_buffer_size = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        buffer_size = SAMPLES_PER_FRAME * 10;
        if (buffer_size < min_buffer_size) {
            buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
        }
    
        // make sure MetronomePlayer plays through speakers when sensor is plugged in
        AudioManager am = (AudioManager) App.getInstance().getSystemService(App.getContext().AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        am.setSpeakerphoneOn(true);
    
        setupAudioRecord();
    }

    private void setupAudioRecord() {
    
        reset();
        
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,       // source
                SAMPLE_RATE,                         // sample rate, hz
                CHANNEL_CONFIG,                      // channels
                AUDIO_FORMAT,                        // audio format
                buffer_size);                        // buffer size (bytes)
    }
    
    private void reset() {
        if (audioRecord != null) {
            Log.e(TAG, "--------------------- RESET ---------------------");
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }
    
    public void setBeatListener(AudioServiceBeatListener beatListener) {
        this.beatListener = beatListener;
    }
    
    public void setSensitivity(int sensitivity) {
        if (this.sensitivity != sensitivity) {
            this.sensitivity = sensitivity;
            updateThreshold();
        }
    }
    
    public void startMe() {
        energyFunction.clear();
        subscribe();
    }
    
    public void stopMe() {
        running = false;
        energyFunction.clear();
    }
    
    
    private void updateThreshold(){
        //    float sensitivity = (float)Settings.sharedInstance.sensitivity;
        //    float A = 0;
        //    float B = 0;
        //    float C = 0;
        //
        //    if (sensitivity < 20){
        //        A = 7; B = 197; C = 19;
        //    } else if (sensitivity < 90) {
        //        A = 1; B = 95; C = 25;
        //    } else {
        //        A = 3; B = 310; C = 200;
        //    }
        //
        //    _startThreshold = - (A * sensitivity - B ) / C;
        
        startThreshold = START_THRESHOLD_ARRAY[sensitivity];
        endThreshold = 1.1 * startThreshold;
        //    NSLog(@"%f - %f", _startThreshold, _endThreshold);
    }
    
    private boolean inBeat = false;
    private void processDataInput(short[] buffer, int dataLength) {
        for (int i = 0; i < dataLength; i++) {
            short dataSample = buffer[i];
            double energyLevel = energyFunction.push(dataSample);
            if (energyLevel > HIGH_THRESHOLD & !inBeat) { // TODO: HIGH_THRESHOLD -> startThreshold
                inBeat = true;
                Log.e(TAG, "inBeat " + energyLevel);
            }else if(energyLevel<LOW_THRESHOLD && inBeat){ // TODO: LOW_THRESHOLD -> endThreshold
                inBeat=false;
                Log.e(TAG, "outBeat " + energyLevel);
                if (beatListener != null) {
                    beatListener.onBeat(System.currentTimeMillis());
                }
            }
        }
    }

   
    
    private void subscribe() {
    
        new Thread(new Runnable() {
            @Override public void run() {
                running = true;
                setupAudioRecord();
                audioRecord.startRecording();
                short[] buffer = new short[buffer_size];
    
                while (running) {
                    int dataLength = audioRecord.read(buffer, 0, buffer.length);
                    if (dataLength < 0) {
                        Log.e(TAG, "some error while trying to read the audio record " + dataLength);
                        break;
                    }
                    Log.e(TAG, "Processing " + dataLength);
                    processDataInput(buffer, dataLength);
                }
                reset();
            }
        }).start();
    }
    
    
    
    
    
    
    
    
    private int sensitivity = 100;
    private double startThreshold;
    private double endThreshold;
    
    private static final double[] START_THRESHOLD_ARRAY = new double[] {10, 9, 8, 7, 6, 5, 4.5, 4, 3.5, 3, 2.5, 2.4, 2.3,
        2.2, 2.1, 2, 1.95, 1.9, 1.85, 1.8, 1.75, 1.7, 1.65, 1.6, 1.55, 1.54, 1.53, 1.52, 1.51, 1.5, 1.475, 1.45, 1.425,
        1.4, 1.39, 1.38, 1.37, 1.36, 1.35, 1.34, 1.33, 1.32, 1.31, 1.3, 1.29, 1.28, 1.27, 1.26, 1.25, 1.24, 1.23, 1.22,
        1.21, 1.2, 1.19, 1.18, 1.17, 1.16, 1.15, 1.14, 1.12, 1.1, 1.07, 1.03, 1, 0.95, 0.9, 0.83, 0.75, 0.65, 0.55, 0.45,
        0.35, 0.25, 0.15, 0.1, 0.09, 0.08, 0.06, 0.02, 0.015, 0.01, 0.0075, 0.005, 0.003, 0.002, 0.0015, 0.001, 0.0009,
        0.0008, 0.0007, 0.0006, 0.0005, 0.00045, 0.0004, 0.00035, 0.0003, 0.00025, 0.0002, 0.00015, 0.0001};
    
    
    
    
    
    //boolean inStrike = false;
    //private static long MAX_STRIKE_DURATION = 200000000; // 200 000 000ns = 200ms
    //private long strikeEndTime = 0;
    //private void processData( short[] data, int numFrames) {
    //    int i;
    //    for (i=0; i<numFrames; i++) {
    //
    //        double energyLevel = energyFunction.push(data[i]);
    //        long now = System.nanoTime();
    //        long timeElapsedNs = now - strikeEndTime;
    //
    //        if (timeElapsedNs >= MAX_STRIKE_DURATION) {
    //            // constant long noise, ignore
    //            inStrike = false;
    //            return;
    //        }
    //
    //
    //        // timeout ended, handle energy level
    //        if (!inStrike && energyLevel >= startThreshold) {
    //            inStrike = true;
    //            //strikeStartTime = now;
    //        } else if (inStrike && energyLevel <= endThreshold) {
    //            inStrike = false;
    //            strikeEndTime = now;
    //            onBeat();
    //        }
    //    }
    //}
    //
    //private long lastBeatTime = 0;
    //private int tapCount = 0;
    //private void onBeat(){
    //    long now = System.nanoTime();
    //    long timeElapsedNs = now - lastBeatTime;
    //    double delayFator = 0.1;
    //    double timeElapsedInSec = (double)timeElapsedNs * 10.0e-6 * delayFator;
    //
    //    boolean isNewTapSeq = (timeElapsedInSec >= Constants.IDLE_TIMEOUT_IN_MS) ? true : false;
    //
    //    if (isNewTapSeq) {
    //        tapCount = 0;
    //        // flash sensitivity
    //        // TODO: Report first beat
    //    } else {
    //        double bpm = 60.0 / timeElapsedInSec;
    //        if (beatListener != null) {
    //            beatListener.onBeat(bpm);
    //        }
    //    }
    //
    //    lastBeatTime = now;
    //    tapCount += 1;
    //
    //}
}
