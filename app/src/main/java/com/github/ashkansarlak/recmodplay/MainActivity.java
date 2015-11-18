package com.github.ashkansarlak.recmodplay;

import android.content.Intent;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

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

    private static final int SAMPLING_FREQUENCY = 44100;
    private final static int RQS_OPEN_AUDIO_MP3 = 1;

    private FloatingActionButton recAndPlay;
    private boolean recording;
    private MediaPlayer mediaPlayer;
    private SeekBar followingFactorSeekbar;
    private ProgressBar envVol, headVol;
    private ImageButton browseAudio;
    private TextView songName;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mediaPlayer = MediaPlayer.create(this, R.raw.music);
        recAndPlay = (FloatingActionButton) findViewById(R.id.recAndPlay);
        recAndPlay.setOnClickListener(recAndPlayOnClickListener);
        followingFactorSeekbar = (SeekBar) findViewById(R.id.followingFactor);
        envVol = (ProgressBar) findViewById(R.id.envVol);
        headVol = (ProgressBar) findViewById(R.id.headVol);
        browseAudio = (ImageButton) findViewById(R.id.browseAudio);
        browseAudio.setOnClickListener(browseAudioClickListener);
        songName = (TextView) findViewById(R.id.songName);
    }

    private View.OnClickListener browseAudioClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (recording) return;

            Intent intent = new Intent();
            intent.setType("audio/mp3");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(
                    intent, "Select an MP3 file to play through headphones"), RQS_OPEN_AUDIO_MP3);

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == RQS_OPEN_AUDIO_MP3) {
                Uri audioFileUri = data.getData();
                mediaPlayer = MediaPlayer.create(this, audioFileUri);
                songName.setText(getFileName(audioFileUri));
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }


    private View.OnClickListener recAndPlayOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (recording) {
                recording = false;
                recAndPlay.setImageResource(R.drawable.ic_play_arrow_white_18dp);
            } else {
                recAndPlay.setImageResource(R.drawable.ic_stop_white_18dp);
                Thread recAndPlayThread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        recording = true;
                        recAndPlay();
                    }

                });

                recAndPlayThread.start();
            }
        }
    };

    private void recAndPlay() {
        int sampleFreq = SAMPLING_FREQUENCY;
        int minBufferSize = AudioRecord.getMinBufferSize(sampleFreq,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        short[] audioData = new short[minBufferSize];

        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleFreq,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize);

        audioRecord.startRecording();
        startMusic();

        while (recording) {
            int numberOfShort = audioRecord.read(audioData, 0, minBufferSize);
            Log.v("AUDIO_DATA", Arrays.toString(audioData));
            double gain = getGain(audioData, numberOfShort);
            Log.v("GAIN", String.valueOf(gain / 2000));
            setVolumeTarget(gain / 2000);
            showVolume(envVol, gain/ 2000);
        }

        audioRecord.stop();
        stopMusic();
    }

    private void showVolume(ProgressBar volume, double v) {
        volume.setProgress((int) ((v > 1 ? 1 : v) * 100));
    }

    private float currentVolume = 0;

    private void setVolumeTarget(double targetVolume) {
        if (mediaPlayer == null) return;

        currentVolume += (targetVolume - currentVolume) / getFollowingFactor();
        mediaPlayer.setVolume(currentVolume, currentVolume);
        showVolume(headVol, currentVolume);
        Log.v("HEADPHONE_GAIN", String.valueOf(currentVolume));
    }

    private double getFollowingFactor() {
        int factor = followingFactorSeekbar.getProgress();
        return factor < 1 ? 1 : factor;
    }

    private void stopMusic() {
        mediaPlayer.stop();
    }

    private void startMusic() {
        currentVolume = 0;
        mediaPlayer.start();
    }

    private double getGain(short[] audioData, int numOfShort) {
        double sumLevel = 0;
        for (int i = 0; i < numOfShort; i++) {
            sumLevel += Math.abs(audioData[i]);
        }
        return Math.abs((sumLevel / numOfShort));
    }

}
