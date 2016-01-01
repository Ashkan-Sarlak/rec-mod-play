package com.github.ashkansarlak.recmodplay;

/**
 * Created by Ashkan on 12/30/2015.
 */
public class Gain {
    public static double of(short[] audioData, int numOfShort) {
        double sumLevel = 0;
        for (int i = 0; i < numOfShort; i++) {
            sumLevel += Math.abs(audioData[i]);
        }
        return Math.abs((sumLevel / numOfShort));
    }

    public static double of(byte[] audioData) {
        double sumLevel = 0;
        for (int i = 0; i < audioData.length - 1; i+=2) {
            sumLevel += Math.abs(audioData[i] * Math.pow(2, 8) + audioData[i + 1]);
        }
        return Math.abs((sumLevel / audioData.length));
    }
}
