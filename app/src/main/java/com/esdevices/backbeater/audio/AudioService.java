package com.esdevices.backbeater.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import com.esdevices.backbeater.activity.MainActivity;

/**
 * Created by aeboyd on 7/13/15.
 */
public class AudioService extends Thread {
    
    public interface AudioServiceBeatListener{
        void onBeat(double beatBpm);
    }
    
    private static final String TAG = "AudioRecorder";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int SAMPLES_PER_FRAME = 1024;
    private static final int AVERAGE_SAMPLE = 4;
    private static final short HIGH_THRESHOLD = 4000;
    private static final short LOW_THRESHOLD = 1000;
    private static final double SECONDS_PER_SAMPLE = 1.0/SAMPLE_RATE;
    private final short[] buffer;
    private boolean running = false;

    private byte bpmAvgSize = 4;
    private double[] bpmAvgA = new double[]{0,0,0,0};
    private int bpmI =0;
    
    private AudioServiceBeatListener beatListener;


    private AudioRecord audioRecord;

    public AudioService() {
        int min_buffer_size = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        int buffer_size = SAMPLES_PER_FRAME * 10;
        if (buffer_size < min_buffer_size) {
            buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
        }
        buffer = new short[buffer_size];
        setupAudioRecord();
    }

    private void setupAudioRecord() {

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,       // source
                SAMPLE_RATE,                         // sample rate, hz
                CHANNEL_CONFIG,                      // channels
                AUDIO_FORMAT,                        // audio format
                buffer.length);                        // buffer size (bytes)
    }
    
    public void setBeatListener(AudioServiceBeatListener beatListener) {
        this.beatListener = beatListener;
    }
    
    public void stopMe() {
        running = false;
    }

    @Override
    public void run() {
        running = true;
        audioRecord.startRecording();
        short max = 0;
        double avg = 0;
        long sample = -1;
        short[] lasts = new short[AVERAGE_SAMPLE];
        for (int i = 0; i < AVERAGE_SAMPLE; i++) {
            lasts[i] = 0;
        }
        boolean inBeat = false;
        double maxMax = 0;
        int aI = 0;
        while (running) {
            int read = audioRecord.read(buffer, 0, buffer.length);
            if (read < 0) {
                Log.e(TAG, "some error while trying to read the audio record " + read);
                break;
            }
            for (int i = 0; i < read; i++) {
                sample++;
                short on = buffer[i];
                if (on > max) {
                    max = on;
                } else if (on <= 0 && max > 0) {
                    aI = (aI + 1) % AVERAGE_SAMPLE;
                    avg -= lasts[(aI + 1) % AVERAGE_SAMPLE] / (double) AVERAGE_SAMPLE;
                    avg += max / (double) AVERAGE_SAMPLE;
                    lasts[aI] = max;
                    if (avg > HIGH_THRESHOLD & !inBeat) {
                        inBeat = true;
                        bpmI = (bpmI+1)%bpmAvgSize;
                        bpmAvgA[bpmI]=sample*SECONDS_PER_SAMPLE;
                        double seconds = 0;
                        for(int i2=0;i2<bpmAvgSize;i2++){
                            seconds+=bpmAvgA[i2];
                        }
                        
                        if (beatListener != null) {
                            beatListener.onBeat(bpmAvgSize / seconds * 60);
                        }
                        sample = 0;
                    }else if(avg<LOW_THRESHOLD && inBeat){
                        inBeat=false;
                    }
                    if (avg > maxMax)
                        maxMax = avg;
                    max = 0;
                }
            }

        }
    }
}
