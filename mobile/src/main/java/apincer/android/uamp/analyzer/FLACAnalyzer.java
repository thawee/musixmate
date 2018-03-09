package apincer.android.uamp.analyzer;

import android.app.Activity;

import net.galmiza.android.engine.sound.SoundEngine;
import net.galmiza.android.spectrogram.FrequencyView;

import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.PCMProcessor;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.ByteData;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by e1022387 on 3/5/2018.
 */

public class FLACAnalyzer extends Thread implements PCMProcessor {
    private final FLACDecoder decoder;
    private FrequencyView frequencyView;
    private SoundEngine nativeLib;
    private int fftResolution;
    private Activity activity;

    // Buffers
    private List<short[]> bufferStack; // Store trunks of buffers
    private short[] fftBuffer; // buffer supporting the fft process
    private float[] re; // buffer holding real part during fft process
    private float[] im; // buffer holding imaginary part during fft process

    /** number of channels **/
    private int channels;

    /** sample rate in Herz**/
    private float sampleRate;

    private int bitsPerSample;

    public FLACAnalyzer(Activity activity,FrequencyView frequencyView, int fftResolution, FileInputStream stream) {
        decoder = new FLACDecoder(stream);
        decoder.setSamplesDecoded(fftResolution);
        decoder.addPCMProcessor(this);
        this.frequencyView = frequencyView;
        this.fftResolution = fftResolution;
        this.activity = activity;
        // JNI interface
        nativeLib = new SoundEngine();
        nativeLib.initFSin();
    }

    @Override
    public void run() {
        try {
            bufferStack = new ArrayList<>();
            // Build buffers for runtime
            int n = fftResolution;
            fftBuffer = new short[n];
            re = new float[n];
            im = new float[n];
            decoder.decode();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processStreamInfo(StreamInfo streamInfo) {
        sampleRate = streamInfo.getSampleRate();
        channels = streamInfo.getChannels();
        bitsPerSample = streamInfo.getBitsPerSample();
    }

    @Override
    public void processPCM(ByteData pcm) {
        byte[] recordBuffer = pcm.getData();
        int n = fftResolution;

        // Trunks are consecutive n/2 length samples
        for (int i=0; i<bufferStack.size()-1; i++) {
            System.arraycopy(recordBuffer, n / 2 * i, bufferStack.get(i + 1), 0, n / 2);
        }
        // Build n length buffers for processing
        // Are build from consecutive trunks
        for (int i=0; i<bufferStack.size()-1; i++) {
            System.arraycopy(bufferStack.get(i), 0, fftBuffer, 0, n/2);
            System.arraycopy(bufferStack.get(i+1), 0, fftBuffer, n/2, n/2);
            process();
        }

        // Last item has not yet fully be used (only its first half)
        // Move it to first position in arraylist so that its last half is used
        if(!bufferStack.isEmpty()) {
            short[] first = bufferStack.get(0);
            short[] last = bufferStack.get(bufferStack.size() - 1);
            System.arraycopy(last, 0, first, 0, n / 2);
        }
    }


    /**
     * Processes the sound waves
     * Computes FFT
     * Update views
     */
    private void process() {
        int n = fftResolution;
        int log2_n = (int) (Math.log(n)/Math.log(2));

        nativeLib.shortToFloat(fftBuffer, re, n);
        nativeLib.clearFloat(im, n);	// Clear imaginary part

        // Windowing to reduce spectrum leakage

        String window = "Hamming";

        if (window.equals("Rectangular"))			nativeLib.windowRectangular(re, n);
        else if (window.equals("Triangular"))		nativeLib.windowTriangular(re, n);
        else if (window.equals("Welch"))			nativeLib.windowWelch(re, n);
        else if (window.equals("Hanning"))			nativeLib.windowHanning(re, n);
        else if (window.equals("Hamming"))			nativeLib.windowHamming(re, n);
        else if (window.equals("Blackman"))			nativeLib.windowBlackman(re, n);
        else if (window.equals("Nuttall"))			nativeLib.windowNuttall(re, n);
        else if (window.equals("Blackman-Nuttall"))	nativeLib.windowBlackmanNuttall(re, n);
        else if (window.equals("Blackman-Harris"))	nativeLib.windowBlackmanHarris(re, n);


        nativeLib.fft(re, im, log2_n, 0);	// Move into frquency domain
        nativeLib.toPolar(re, im, n);	// Move to polar base

        frequencyView.setMagnitudes(re);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                frequencyView.invalidate();
            }
        });
    }
}
