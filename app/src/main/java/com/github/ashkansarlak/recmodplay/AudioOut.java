package com.github.ashkansarlak.recmodplay;

import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Ashkan on 12/30/2015.
 */
public class AudioOut {
    private static final String TAG = AudioOut.class.getSimpleName();
    private static final long TIMEOUT_US = 1000000;
    private final String mime;
    private final MediaFormat format;

    private MediaCodec codec;
    private final MediaExtractor extractor;
    private ByteBuffer[] codecInputBuffers;
    private ByteBuffer[] codecOutputBuffers;
    private boolean sawInputEOS;
    private MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

    private int minBufferSize = AudioTrack.getMinBufferSize(
            Const.SAMPLING_FREQUENCY,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT);

    private AudioTrack audioTrack = new AudioTrack(
            AudioManager.STREAM_MUSIC,
            Const.SAMPLING_FREQUENCY,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize,
            AudioTrack.MODE_STREAM);

    private boolean sawOutputEOS;
    private double gain;
    private float volume;

    public AudioOut() {
        AssetFileDescriptor sampleFD = App.get().getResources().openRawResourceFd(R.raw.music);

        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(sampleFD.getFileDescriptor(), sampleFD.getStartOffset(), sampleFD.getLength());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, String.format("TRACKS #: %d", extractor.getTrackCount()));
        format = extractor.getTrackFormat(0);
        mime = format.getString(MediaFormat.KEY_MIME);
        Log.d(TAG, String.format("MIME TYPE: %s", mime));

        try {
            codec = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
        codec.start();
        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();

        extractor.selectTrack(0); // <= You must select a track. You will read samples from the media from this track!
    }

    public void start() {
        playing = true;
        audioTrack.play();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isPlaying()) {
                    input();
                    output();
                }
            }
        }).start();
    }

    private boolean playing = false;

    public boolean isPlaying() {
        return !sawInputEOS && playing;
    }

    public void stop() {
        playing = false;
        audioTrack.stop();
    }

    public void input() {
        int inputBufIndex = codec.dequeueInputBuffer(TIMEOUT_US);
        if (inputBufIndex >= 0) {
            ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

            int sampleSize = extractor.readSampleData(dstBuf, 0);
            long presentationTimeUs = 0;
            if (sampleSize < 0) {
                sawInputEOS = true;
                sampleSize = 0;
            } else {
                presentationTimeUs = extractor.getSampleTime();
            }

            codec.queueInputBuffer(inputBufIndex,
                    0, //offset
                    sampleSize,
                    presentationTimeUs,
                    sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
            if (!sawInputEOS) {
                extractor.advance();
            }
        }
    }

    public void output() {
        final int res = codec.dequeueOutputBuffer(info, TIMEOUT_US);
        if (res >= 0) {
            int outputBufIndex = res;
            ByteBuffer buf = codecOutputBuffers[outputBufIndex];

            final byte[] chunk = new byte[info.size];
            final byte[] leftChunk = new byte[info.size/2];
            buf.get(chunk); // Read the buffer all at once
            buf.clear(); // ** MUST DO!!! OTHERWISE THE NEXT TIME YOU GET THIS SAME BUFFER BAD THINGS WILL HAPPEN
            getLeftChannel(chunk, leftChunk);

            if (leftChunk.length > 0) {
                gain = Gain.of(leftChunk);
                audioTrack.write(leftChunk, 0, leftChunk.length);
            }
            codec.releaseOutputBuffer(outputBufIndex, false /* render */);

            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                sawOutputEOS = true;
            }
        } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            codecOutputBuffers = codec.getOutputBuffers();
        } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            final MediaFormat oformat = codec.getOutputFormat();
            Log.d(TAG, "Output format has changed to " + oformat);
            audioTrack.setPlaybackRate(oformat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
        }
    }

    private void getLeftChannel(byte[] chunk, byte[] leftChunk) {
        for (int i = 0; i < leftChunk.length - 1; i+=2) {
            // because it's 16-bit
            leftChunk[i]     = chunk[2 * i];
            leftChunk[i + 1] = chunk[2 * i + 1];
        }
    }

    public double getGain() {
        return gain;
    }

    public double getScaledGain() {
        return gain * volume;
    }

    public void setVolume(float volume) {
        this.volume = volume > 1 ? 1 : volume < 0 ? 0 : volume;
        audioTrack.setStereoVolume(volume, volume);
    }

    public float getVolume() {
        return volume;
    }
}
