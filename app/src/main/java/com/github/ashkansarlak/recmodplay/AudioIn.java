package com.github.ashkansarlak.recmodplay;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by Ashkan on 12/30/2015.
 */
public class AudioIn {

    private static final String TAG = AudioIn.class.getSimpleName();

    int minBufferSize = AudioRecord.getMinBufferSize(
            Const.SAMPLING_FREQUENCY,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);

    short[] audioData = new short[minBufferSize];

    AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
            Const.SAMPLING_FREQUENCY,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize);
    private boolean recording = false;
    private double gain;

    public AudioIn() {
    }

    public void start() {
        recording = true;
        audioRecord.startRecording();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (recording) {
                    int readShorts = audioRecord.read(audioData, 0, minBufferSize);
                    gain = Gain.of(audioData, readShorts);
                }
            }
        }).start();
    }

    public void stop() {
        recording = false;
        audioRecord.stop();
    }

    public double getGain() {
        return gain;
    }
}
