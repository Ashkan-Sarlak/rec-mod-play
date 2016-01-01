package com.github.ashkansarlak.recmodplay;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.ashkansarlak.recmodplay.customviews.VolumeMeter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class CalibrationActivity extends AppCompatActivity {

    private static final double[] OG_IG =
            { 521.44, 352.57, 82.61, 30.48, 18.34, 11.41, 8.44, 6.40, 4.56, 3.55
                    , 3.27, 2.90, 2.52, 2.32, 2.11, 1.89, 1.85, 1.81, 1.81, 1.84
                    , 1.83, 1.80, 1.80, 1.82, 1.80, 1.81, 1.83, 1.79, 1.80, 1.80
                    , 1.80, 1.83, 1.82, 1.80, 1.84, 1.82, 1.82, 1.82, 1.82, 1.83
                    , 1.82, 1.84, 1.80, 1.85, 1.83, 1.83, 1.86, 1.84, 1.78, 1.87
                    , 1.80, 1.83, 1.82, 1.80, 1.82, 1.85, 1.84, 1.84, 1.84, 1.82
                    , 1.85, 1.87, 1.82, 1.84, 1.85, 1.82, 1.87, 1.84, 1.86, 1.84
                    , 1.81, 1.85, 1.85, 1.82, 1.85, 1.86, 1.89, 1.83, 1.87, 1.84
                    , 1.86, 1.83, 1.83, 1.86, 1.83, 1.86, 1.84, 1.85, 1.82, 1.88
                    , 1.86, 1.87, 1.84, 1.82, 1.89, 1.86, 1.86, 1.90, 1.87, 1.85, 1.86 };

    private static final double OG_IG_SAFTY_MARGIN = 0.20;

    private AudioOut audioOut = new AudioOut();
    private AudioIn audioIn = new AudioIn();
    private VolumeMeter inVolumeMeter;
    private VolumeMeter outVolumeMeter;
    private TextView outToIn;
    private TextView outToInAvg;
    private SeekBar outVolumeSeek;

    private double runningAvg = 0;
    private long numSamples = 0;

    private static final int MAX_GAIN = 5000;
    private static final int FOLLOWING_FACTOR = 60;
    private static final NumberFormat formatter = new DecimalFormat("#0.00");
    private Timer timer = new Timer();
    private MyTask task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
        inVolumeMeter = (VolumeMeter) findViewById(R.id.inVolume);
        outVolumeMeter = (VolumeMeter) findViewById(R.id.outVolume);
        outToIn = (TextView) findViewById(R.id.outToIn);
        outToInAvg = (TextView) findViewById(R.id.outToInAvg);
        outVolumeSeek = (SeekBar) findViewById(R.id.outVolumeSeek);

        inVolumeMeter.setMax(MAX_GAIN);
        outVolumeMeter.setMax(MAX_GAIN);

        audioOut.setVolume(0);

        outVolumeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioOut.setVolume(outVolumeSeek.getProgress() / 100f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    public void playOrStop(View view) {
        if (audioOut.isPlaying()) {
            audioOut.stop();
            audioIn.stop();
            task.cancel();
        } else {
            audioOut.start();
            audioIn.start();
            timer.scheduleAtFixedRate(task = new MyTask(), 0, 30);
        }
    }

    private class MyTask extends TimerTask {

        @Override
        public void run() {
            inVolumeMeter.setCurrent(audioIn.getGain());
            outVolumeMeter.setCurrent(audioOut.getScaledGain());
            outToIn.post(new Runnable() {
                @Override
                public void run() {
                    if (audioIn.getGain() == 0) return;
                    outToIn.setText(formatter.format(audioOut.getGain() / audioIn.getGain()));
                    runningAvg = (runningAvg * numSamples + audioOut.getGain() / audioIn.getGain()) / (numSamples + 1);
                    numSamples++;
                    outToInAvg.setText(formatter.format(runningAvg));

                    float target = (float) ((audioIn.getGain() - audioOut.getGain() / getOgIgFactor()) / MAX_GAIN);
                    audioOut.setVolume(audioOut.getVolume() + (target - audioOut.getVolume()) / FOLLOWING_FACTOR);

//                    runSilenceTestIteration();
//                    runInMaxGainTest();
                }
            });
        }
    }

    private int inMaxGainIterations = 0;
    private double inMaxGain = 0;
    private void runInMaxGainTest() {
        inMaxGain += audioIn.getGain();
        inMaxGainIterations++;

        if (inMaxGainIterations > 10000) {
            Log.v("IN MAX GAIN", "" + inMaxGain/inMaxGainIterations);
            playOrStop(null);
        }
    }

    private float testVol = 0.00f;
    private int iterationIndex = 0;
    private double[] oGiGSilent = new double[101];
    private void runSilenceTestIteration() {
        if (iterationIndex / 1000 > 100) return;

        audioOut.setVolume(testVol);
        oGiGSilent[iterationIndex / 1000] += audioOut.getGain() / audioIn.getGain();

        iterationIndex++;
        if (iterationIndex % 1000 == 0) {
            oGiGSilent[iterationIndex / 1000 - 1] /= 1000;
            Log.v("VOL - oGiG", testVol + " - " + oGiGSilent[iterationIndex / 1000 - 1]);
            testVol += 0.01;
        }

        if (iterationIndex / 1000 > 100) {
            Log.d("VOL - oGiG arr", arrToStr(oGiGSilent));
            playOrStop(null);
        }
    }

    private String arrToStr(double[] arr) {
        StringBuilder out = new StringBuilder();
        out.append("[ ");
        for (int i = 0; i < arr.length; i++) {
            if (i < arr.length - 1) {
                out.append(formatter.format(arr[i]) + ", ");
            } else {
                out.append(formatter.format(arr[i]) + " ]");
            }
        }
        return out.toString();
    }

    private double getOgIgFactor() {
        return OG_IG[((int) (audioOut.getVolume() * 100))] * (1 - OG_IG_SAFTY_MARGIN);
    }
}
