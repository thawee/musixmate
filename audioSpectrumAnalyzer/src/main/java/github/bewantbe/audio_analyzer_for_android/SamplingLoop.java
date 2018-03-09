package github.bewantbe.audio_analyzer_for_android;

/**
 * Created by e1022387 on 2/28/2018.
 */

public abstract class SamplingLoop {
    /** inverse max short value as float **/
    protected final float MAX_VALUE = 1.0f / Short.MAX_VALUE;
    volatile double wavSecRemain;
    volatile double wavSec = 0;
    protected STFT stft;   // use with care
    protected AnalyzerViews mAnalyzerViews;
    protected AnalyzerParameters analyzerParam;
    protected double[] spectrumDBcopy;   // XXX, transfers data from SamplingLoop to AnalyzerGraphic

    public double getWavSec() {
        return wavSec;
    }

    public double getWavSecRemain() {
        return wavSecRemain;
    }

    protected void setupView(AnalyzerViews analyzerViews) {
        this.mAnalyzerViews = analyzerViews;
        this.mAnalyzerViews.setupView(analyzerParam);
    }

    protected void setupSTFT() {
        stft = new STFT(analyzerParam);
        stft.setAWeighting(analyzerParam.isAWeighting);
        if (spectrumDBcopy == null || spectrumDBcopy.length != analyzerParam.fftLen/2+1) {
            spectrumDBcopy = new double[analyzerParam.fftLen/2+1];
        }
    }

    public abstract void analyze(AnalyzerViews analyzerViews);


    public float[] floatMe(short[] pcms) {
        float[] floaters = new float[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            floaters[i] = pcms[i];
        }
        return floaters;
    }

    public  short[] shortMe(float[] pcms) {
        short[] shorters = new short[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            shorters[i] = (short)pcms[i];
        }
        return shorters;
    }
}
