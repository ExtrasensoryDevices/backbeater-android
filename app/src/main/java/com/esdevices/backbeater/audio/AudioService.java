package com.esdevices.backbeater.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

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
    private volatile boolean running = false;
    
    private double startThreshold;
    private double endThreshold;
    private int sensitivity = 100;
    
    private AudioServiceBeatListener beatListener;
    private EnergyFunction energyFunction = new EnergyFunction();
    
    // old array, converted from iOS
    //private static final double[] START_THRESHOLD_ARRAY = new double[] {63619.17, 57277.19, 50935.21, 44593.23,
    //    38251.25, 31909.27, 28738.28, 25567.29, 22396.30, 19225.31, 16054.32, 15420.12, 14785.92, 14151.72, 13517.52,
    //    12883.33, 12566.23, 12249.13, 11932.03, 11614.93, 11297.83, 10980.73, 10663.63, 10346.53, 10029.44, 9966.02,
    //    9902.60, 9839.18, 9775.76, 9712.34, 9553.79, 9395.24, 9236.69, 9078.14, 9014.72, 8951.30, 8887.88, 8824.46,
    //    8761.04, 8697.62, 8634.20, 8570.78, 8507.36, 8443.94, 8380.52, 8317.10, 8253.68, 8190.26, 8126.84, 8063.42,
    //    8000.00, 7936.58, 7873.16, 7809.74, 7746.32, 7682.90, 7619.48, 7556.06, 7492.64, 7429.22, 7302.38, 7175.54,
    //    6985.28, 6731.61, 6541.35, 6224.25, 5907.15, 5463.21, 4955.85, 4321.65, 3687.46, 3053.26, 2419.06, 1784.86,
    //    1150.66, 833.56, 770.14, 706.72, 579.88, 326.21, 294.50, 262.79, 246.93, 231.08, 218.39, 212.05, 208.88,
    //    205.71, 205.07, 204.44, 203.81, 203.17, 202.54, 202.22, 201.90, 201.59, 201.27, 200.95, 200.63, 200.32, 200.00};
    //
    
    private static final double[] START_THRESHOLD_ARRAY = new double[] {1000, 966.6666667, 933.3333333, 900, 866.6666667,
            833.3333333, 800, 775.5555556, 751.1111111, 726.6666667, 702.2222222, 677.7777778, 653.3333333, 628.8888889,
            604.4444444, 580, 570, 560, 550, 540, 530, 520, 510, 500, 490, 480, 473.75, 467.5, 461.25, 455, 448.75, 442.5,
            436.25, 430, 427.2727273, 424.5454545, 421.8181818, 419.0909091, 416.3636364, 413.6363636, 410.9090909, 408.1818182,
            405.4545455, 402.7272727, 400, 397.2727273, 394.5454545, 391.8181818, 389.0909091, 386.3636364, 383.6363636, 380.9090909,
            378.1818182, 375.4545455, 372.7272727, 370, 363, 356, 349, 342, 335, 328, 321, 314, 307, 300, 291, 282, 273, 264, 255,
            246, 237, 228, 219, 210, 201, 195.5384615, 190.0769231, 184.6153846, 179.1538462, 173.6923077, 168.2307692,
            162.7692308, 157.3076923, 151.8461538, 146.3846154, 140.9230769, 135.4615385, 130, 127.2727273, 124.5454545,
            121.8181818, 119.0909091, 116.3636364, 113.6363636, 110.9090909, 108.1818182, 105.4545455, 102.7272727, 100};
    
    
    public AudioService() {
        
        updateThreshold();
        
        int min_buffer_size = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        buffer_size = SAMPLES_PER_FRAME * 10;
        if (buffer_size < min_buffer_size) {
            buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
        }
    
        /* Commented out because Metronome should go through the same sensor output
    
        // make sure MetronomePlayer plays through speakers when sensor is plugged in
        AudioManager am = (AudioManager) App.getInstance().getSystemService(App.getContext().AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        am.setSpeakerphoneOn(true);
        
        */
    
    }

    private AudioRecord setupAudioRecord() {
    
        return new AudioRecord(
                MediaRecorder.AudioSource.MIC,  // source
                SAMPLE_RATE,                    // sample rate, hz
                CHANNEL_CONFIG,                 // channels
                AUDIO_FORMAT,                   // audio format
                buffer_size);                   // buffer size (bytes)
    }
     
    public void setBeatListener(AudioServiceBeatListener beatListener) {
        this.beatListener = beatListener;
    }
    
    public void setSensitivity(int sensitivity) {
        this.sensitivity = sensitivity;
        updateThreshold();
    }
    
    public void startMe() {
        stopMe();
        subscribe();
    }
    
    public void stopMe() {
        running = false;
        energyFunction.clear();
    }

    public boolean isRunning() {
        return running;
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

                    //if (calibrationBeatListener != null) {
                    //    calibrationBeatListener.onBeat(started_DS, started_e, ended_DS, ended_e);
                    //    energyFunction.clear();
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
                AudioRecord audioRecord = setupAudioRecord();
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
                        Log.e(TAG, "Some error while trying to read the audio record " + dataLength);
                        break;
                    }
                    processDataInput(buffer, dataLength);
                }
                audioRecord.release();
            }
    
    }).start();
    }
    
    /******************************************************
     *    Calibration helper
     *
     ******************************************************/

    public interface AudioServiceCalibrationBeatListener{
        void onBeat(short started_DS, double started_e, short ended_DS, double ended_e);
    }
    //
    private AudioServiceCalibrationBeatListener calibrationBeatListener;


    public void setCalibrationBeatListener(AudioServiceCalibrationBeatListener calibrationBeatListener) {
        this.calibrationBeatListener = calibrationBeatListener;
    }



    public void setTestStartThreshold(double START_THRESHOLD) {
        if (this.startThreshold != START_THRESHOLD) {
            this.startThreshold = START_THRESHOLD;
            this.endThreshold = 1.1 * this.startThreshold;
        }
        energyFunction.clear();
    }
    
    
    
    
}
