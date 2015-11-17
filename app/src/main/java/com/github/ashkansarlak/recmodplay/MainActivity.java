package com.github.ashkansarlak.recmodplay;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    Integer[] freqset = {11025, 16000, 22050, 44100};
    private ArrayAdapter<Integer> adapter;

    Spinner spFrequency;
    Button startRec, stopRec, playBack, recAndPlay;

    Boolean recording;
    private MediaPlayer mediaPlayer;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startRec = (Button) findViewById(R.id.startrec);
        stopRec = (Button) findViewById(R.id.stoprec);
        playBack = (Button) findViewById(R.id.playback);
        recAndPlay = (Button) findViewById(R.id.recAndPlay);

        startRec.setOnClickListener(startRecOnClickListener);
        stopRec.setOnClickListener(stopRecOnClickListener);
        playBack.setOnClickListener(playBackOnClickListener);
        recAndPlay.setOnClickListener(recAndPlayOnClickListener);

        spFrequency = (Spinner) findViewById(R.id.frequency);
        adapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, freqset);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFrequency.setAdapter(adapter);
    }

    View.OnClickListener startRecOnClickListener
            = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {

            Thread recordThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    recording = true;
                    startRecord();
                }

            });

            recordThread.start();
        }
    };

    View.OnClickListener stopRecOnClickListener
            = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            recording = false;
        }
    };

    View.OnClickListener playBackOnClickListener
            = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            playRecord();
        }

    };

    private View.OnClickListener recAndPlayOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Thread recAndPlayThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    recording = true;
                    recAndPlay();
                }

            });

            recAndPlayThread.start();
        }
    };

    private void recAndPlay() {
        int shortSizeInBytes = Short.SIZE / Byte.SIZE;
        int sampleFreq = (Integer) spFrequency.getSelectedItem();
        int minBufferSize = AudioRecord.getMinBufferSize(sampleFreq,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        short[] audioData = new short[minBufferSize];

        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleFreq,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize);

//        AudioTrack audioTrack = new AudioTrack(
//                AudioManager.STREAM_MUSIC,
//                sampleFreq,
//                AudioFormat.CHANNEL_CONFIGURATION_MONO,
//                AudioFormat.ENCODING_PCM_16BIT,
//                shortSizeInBytes,
//                AudioTrack.MODE_STREAM);

        audioRecord.startRecording();
        startMusic();
//        audioTrack.play();
        int offset = 0;

        while (recording) {
            int numberOfShort = audioRecord.read(audioData, 0, minBufferSize);
//            audioTrack.write(audioData, offset, numberOfShort);
            offset += numberOfShort;
            Log.v("AUDIO_DATA", Arrays.toString(audioData));
            double gain = getGain(audioData, numberOfShort);
            Log.v("GAIN", String.valueOf(gain));
            mediaPlayer.setVolume((float) gain / 2000, (float) gain / 2000);
        }

        audioRecord.stop();
        stopMusic();
//        audioTrack.stop();
    }

    private void stopMusic() {
        mediaPlayer.stop();
    }

    private void startMusic() {
        mediaPlayer = MediaPlayer.create(this, R.raw.music);
        mediaPlayer.start();
    }

    private double getGain(short[] audioData, int numOfShort) {
        double sumLevel = 0;
        for (int i = 0; i < numOfShort; i++) {
            sumLevel += Math.abs(audioData[i]);
        }
        return Math.abs((sumLevel / numOfShort));
    }

    private void startRecord() {

        File file = new File(getCacheDir(), "test.pcm");

        int sampleFreq = (Integer) spFrequency.getSelectedItem();

        try {
            file.createNewFile();

            OutputStream outputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

            int minBufferSize = AudioRecord.getMinBufferSize(sampleFreq,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            short[] audioData = new short[minBufferSize];

            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    sampleFreq,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize);

            audioRecord.startRecording();

            while (recording) {
                int numberOfShort = audioRecord.read(audioData, 0, minBufferSize);
                for (int i = 0; i < numberOfShort; i++) {
                    dataOutputStream.writeShort(audioData[i]);
                }
            }

            audioRecord.stop();
            dataOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void playRecord() {

        File file = new File(getCacheDir(), "test.pcm");

        int shortSizeInBytes = Short.SIZE / Byte.SIZE;

        int bufferSizeInBytes = (int) (file.length() / shortSizeInBytes);
        short[] audioData = new short[bufferSizeInBytes];

        try {
            InputStream inputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            int i = 0;
            while (dataInputStream.available() > 0) {
                audioData[i] = dataInputStream.readShort();
                i++;
            }

            dataInputStream.close();

            int sampleFreq = (Integer) spFrequency.getSelectedItem();

            AudioTrack audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleFreq,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeInBytes,
                    AudioTrack.MODE_STREAM);

            audioTrack.play();
            audioTrack.write(audioData, 0, bufferSizeInBytes);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

