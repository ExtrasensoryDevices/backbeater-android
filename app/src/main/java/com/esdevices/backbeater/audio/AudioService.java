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
    private static final String TAG = "AudioService";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int SAMPLES_PER_FRAME = 1024;
    private int buffer_size;
    private boolean running = false;
    
    private double startThreshold;
    private double endThreshold;
    private int sensitivity = 100;
    
    private AudioServiceBeatListener beatListener;
    private EnergyFunction energyFunction = new EnergyFunction();
    
    
    private AudioRecord audioRecord;
    
    
    private static final double[] START_THRESHOLD_ARRAY = new double[] {63619.17, 57277.19, 50935.21, 44593.23,
        38251.25, 31909.27, 28738.28, 25567.29, 22396.30, 19225.31, 16054.32, 15420.12, 14785.92, 14151.72, 13517.52,
        12883.33, 12566.23, 12249.13, 11932.03, 11614.93, 11297.83, 10980.73, 10663.63, 10346.53, 10029.44, 9966.02,
        9902.60, 9839.18, 9775.76, 9712.34, 9553.79, 9395.24, 9236.69, 9078.14, 9014.72, 8951.30, 8887.88, 8824.46,
        8761.04, 8697.62, 8634.20, 8570.78, 8507.36, 8443.94, 8380.52, 8317.10, 8253.68, 8190.26, 8126.84, 8063.42,
        8000.00, 7936.58, 7873.16, 7809.74, 7746.32, 7682.90, 7619.48, 7556.06, 7492.64, 7429.22, 7302.38, 7175.54,
        6985.28, 6731.61, 6541.35, 6224.25, 5907.15, 5463.21, 4955.85, 4321.65, 3687.46, 3053.26, 2419.06, 1784.86,
        1150.66, 833.56, 770.14, 706.72, 579.88, 326.21, 294.50, 262.79, 246.93, 231.08, 218.39, 212.05, 208.88,
        205.71, 205.07, 204.44, 203.81, 203.17, 202.54, 202.22, 201.90, 201.59, 201.27, 200.95, 200.63, 200.32, 200.00};
    
    
    
    
    
    public AudioService() {
        
        updateThreshold();
        
        int min_buffer_size = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        buffer_size = SAMPLES_PER_FRAME * 10;
        if (buffer_size < min_buffer_size) {
            buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
        }
    
        // make sure MetronomePlayer plays through speakers when sensor is plugged in
        //AudioManager am = (AudioManager) App.getInstance().getSystemService(App.getContext().AUDIO_SERVICE);
        //am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        //am.setSpeakerphoneOn(true);
    
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
            audioRecord.release();
            audioRecord = null;
        }
    }
    
    public void setBeatListener(AudioServiceBeatListener beatListener) {
        this.beatListener = beatListener;
    }
    
    public void setSensitivity(int sensitivity) {
        this.sensitivity = sensitivity;
        updateThreshold();
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
        startThreshold = START_THRESHOLD_ARRAY[sensitivity];
        endThreshold = 1.1 * startThreshold;
    }
    
    short lastPositiveDS = 0; // data sample, only consider positive part of the sound wave
    private boolean inBeat = false;
    
    // stats
    //short min=Short.MAX_VALUE, max=Short.MIN_VALUE;
    //double min_e=Double.MAX_VALUE, max_e=Double.MIN_VALUE;
    //short started_DS = 0, ended_DS = 0;
    //double started_e = 0, ended_e = 0;
    
    private void processDataInput(short[] buffer, int dataLength) {
        for (int i = 0; i < dataLength; i++) {
            short currentDS = buffer[i]; // data sample
            
            // stats
            //if (currentDS < min) { min = currentDS; }
            //if (currentDS > max) { max = currentDS; }
            
            if (currentDS > lastPositiveDS) {
                // step 1: get positive data sample
                lastPositiveDS = currentDS;
            } else if (currentDS <= 0 && lastPositiveDS > 0) {
                // step 2: when previous sample was positive and current is negative,
                //         check if lastPositiveDS was a start of the beat of end of the beat
    
                // energyLevel = average of the last 4 positive samples
                double energyLevel = energyFunction.push(lastPositiveDS);
    
                // stats
                //if (energyLevel < min_e) min_e = energyLevel;
                //if (energyLevel > max_e) max_e = energyLevel;
    
                if (energyLevel > startThreshold & !inBeat) {
                    // beat started
                    inBeat = true;
    
                    // stats
                    //started_DS = lastPositiveDS; started_e = energyLevel;
                    
                } else if (energyLevel < endThreshold && inBeat) {
                    // beat ended
                    
                    // stats
                    //ended_DS = lastPositiveDS; ended_e = energyLevel;
                    //
                    //if (calibrationBeatListener != null) {
                    //    calibrationBeatListener.onBeat(started_DS, started_e, ended_DS, ended_e);
                    //}
                    //min=Short.MAX_VALUE; max=Short.MIN_VALUE;
                    //min_e=Double.MAX_VALUE; max_e=Double.MIN_VALUE;
    
                    inBeat = false;
    
                    if (beatListener != null) {
                        beatListener.onBeat(System.currentTimeMillis());
                    }
                }
                lastPositiveDS = 0;
            }
        }
    }

   
    
    private void subscribe() {
    
        new Thread(new Runnable() {
            @Override public void run() {
                running = true;
                setupAudioRecord();
                Log.d("STATE", ""+audioRecord.getState());
                int state = audioRecord.getState();
                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.d("STATE", "NOT INITIALIZED");
                }
                audioRecord.startRecording();
                short[] buffer = new short[buffer_size];
    
                while (running) {
                    int dataLength = audioRecord.read(buffer, 0, buffer.length);
                    if (dataLength < 0) {
                        Log.e(TAG, "some error while trying to read the audio record " + dataLength);
                        break;
                    }
                    processDataInput(buffer, dataLength);
                }
                reset();
            }
        }).start();
    }
    
    /******************************************************
     *    Calibration helper
     *
     ******************************************************/

    //public interface AudioServiceCalibrationBeatListener{
    //    void onBeat(short started_DS, double started_e, short ended_DS, double ended_e);
    //}
    //
    //private AudioServiceCalibrationBeatListener calibrationBeatListener;
    
    
    //public void setCalibrationBeatListener(AudioServiceCalibrationBeatListener calibrationBeatListener) {
    //    this.calibrationBeatListener = calibrationBeatListener;
    //}
    
    
    
    //public void setTestStartThreshold(double START_THRESHOLD) {
    //    if (this.startThreshold != START_THRESHOLD) {
    //        this.startThreshold = START_THRESHOLD;
    //        this.endThreshold = 1.1 * this.startThreshold;
    //    }
    //}
    
    
}
