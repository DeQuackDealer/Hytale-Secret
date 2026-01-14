package rubidium.voice;

import java.util.Arrays;

public class RNNoiseProcessor {
    
    private final int frameSize;
    private final int sampleRate;
    private final VoiceConfig.NoiseSuppressionLevel level;
    
    private double[] noiseProfile;
    private double[] smoothedSpectrum;
    private double adaptationRate = 0.02;
    private double suppressionFactor;
    
    private static final int FFT_SIZE = 512;
    private static final int OVERLAP = FFT_SIZE / 2;
    
    private double[] previousFrame;
    private double[] overlapBuffer;
    private double[] window;
    
    public RNNoiseProcessor(VoiceConfig config) {
        this.frameSize = config.frameSize();
        this.sampleRate = config.sampleRate();
        this.level = config.noiseSuppression();
        this.suppressionFactor = level.getStrength();
        
        initialize();
    }
    
    public RNNoiseProcessor(int frameSize, int sampleRate, VoiceConfig.NoiseSuppressionLevel level) {
        this.frameSize = frameSize;
        this.sampleRate = sampleRate;
        this.level = level;
        this.suppressionFactor = level.getStrength();
        
        initialize();
    }
    
    private void initialize() {
        this.noiseProfile = new double[FFT_SIZE / 2 + 1];
        this.smoothedSpectrum = new double[FFT_SIZE / 2 + 1];
        this.previousFrame = new double[OVERLAP];
        this.overlapBuffer = new double[OVERLAP];
        this.window = createHannWindow(FFT_SIZE);
        
        Arrays.fill(noiseProfile, 0.0001);
    }
    
    private double[] createHannWindow(int size) {
        double[] w = new double[size];
        for (int i = 0; i < size; i++) {
            w[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (size - 1)));
        }
        return w;
    }
    
    public byte[] process(byte[] audioData) {
        if (level == VoiceConfig.NoiseSuppressionLevel.OFF) {
            return audioData;
        }
        
        short[] samples = bytesToShorts(audioData);
        short[] processed = process(samples);
        return shortsToBytes(processed);
    }
    
    public short[] process(short[] samples) {
        if (level == VoiceConfig.NoiseSuppressionLevel.OFF) {
            return samples;
        }
        
        double[] input = new double[samples.length];
        for (int i = 0; i < samples.length; i++) {
            input[i] = samples[i] / 32768.0;
        }
        
        double[] output = processSpectral(input);
        
        short[] result = new short[output.length];
        for (int i = 0; i < output.length; i++) {
            double sample = Math.max(-1.0, Math.min(1.0, output[i]));
            result[i] = (short) (sample * 32767);
        }
        
        return result;
    }
    
    private double[] processSpectral(double[] input) {
        double[] output = new double[input.length];
        
        int hopSize = FFT_SIZE / 2;
        int numFrames = (input.length - FFT_SIZE) / hopSize + 1;
        
        for (int frame = 0; frame < numFrames; frame++) {
            int offset = frame * hopSize;
            
            double[] segment = new double[FFT_SIZE];
            for (int i = 0; i < FFT_SIZE && offset + i < input.length; i++) {
                segment[i] = input[offset + i] * window[i];
            }
            
            double[] magnitude = computeMagnitude(segment);
            double[] phase = computePhase(segment);
            
            updateNoiseProfile(magnitude);
            
            double[] suppressedMag = applySpectralSubtraction(magnitude);
            
            double[] processed = reconstructSignal(suppressedMag, phase);
            
            for (int i = 0; i < FFT_SIZE && offset + i < output.length; i++) {
                output[offset + i] += processed[i] * window[i];
            }
        }
        
        return output;
    }
    
    private double[] computeMagnitude(double[] signal) {
        double[] mag = new double[FFT_SIZE / 2 + 1];
        
        for (int k = 0; k <= FFT_SIZE / 2; k++) {
            double real = 0, imag = 0;
            for (int n = 0; n < FFT_SIZE; n++) {
                double angle = -2 * Math.PI * k * n / FFT_SIZE;
                real += signal[n] * Math.cos(angle);
                imag += signal[n] * Math.sin(angle);
            }
            mag[k] = Math.sqrt(real * real + imag * imag);
        }
        
        return mag;
    }
    
    private double[] computePhase(double[] signal) {
        double[] phase = new double[FFT_SIZE / 2 + 1];
        
        for (int k = 0; k <= FFT_SIZE / 2; k++) {
            double real = 0, imag = 0;
            for (int n = 0; n < FFT_SIZE; n++) {
                double angle = -2 * Math.PI * k * n / FFT_SIZE;
                real += signal[n] * Math.cos(angle);
                imag += signal[n] * Math.sin(angle);
            }
            phase[k] = Math.atan2(imag, real);
        }
        
        return phase;
    }
    
    private void updateNoiseProfile(double[] magnitude) {
        for (int i = 0; i < noiseProfile.length && i < magnitude.length; i++) {
            noiseProfile[i] = noiseProfile[i] * (1 - adaptationRate) + 
                              magnitude[i] * adaptationRate;
        }
    }
    
    private double[] applySpectralSubtraction(double[] magnitude) {
        double[] result = new double[magnitude.length];
        
        double overSubtraction = 1.0 + suppressionFactor;
        double spectralFloor = 0.01;
        
        for (int i = 0; i < magnitude.length; i++) {
            double subtracted = magnitude[i] - overSubtraction * noiseProfile[i];
            result[i] = Math.max(subtracted, spectralFloor * magnitude[i]);
        }
        
        return result;
    }
    
    private double[] reconstructSignal(double[] magnitude, double[] phase) {
        double[] signal = new double[FFT_SIZE];
        
        for (int n = 0; n < FFT_SIZE; n++) {
            for (int k = 0; k <= FFT_SIZE / 2; k++) {
                double angle = 2 * Math.PI * k * n / FFT_SIZE + phase[k];
                signal[n] += magnitude[k] * Math.cos(angle) / FFT_SIZE;
            }
        }
        
        return signal;
    }
    
    private short[] bytesToShorts(byte[] bytes) {
        short[] shorts = new short[bytes.length / 2];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) ((bytes[i * 2] & 0xFF) | (bytes[i * 2 + 1] << 8));
        }
        return shorts;
    }
    
    private byte[] shortsToBytes(short[] shorts) {
        byte[] bytes = new byte[shorts.length * 2];
        for (int i = 0; i < shorts.length; i++) {
            bytes[i * 2] = (byte) (shorts[i] & 0xFF);
            bytes[i * 2 + 1] = (byte) ((shorts[i] >> 8) & 0xFF);
        }
        return bytes;
    }
    
    public void reset() {
        Arrays.fill(noiseProfile, 0.0001);
        Arrays.fill(smoothedSpectrum, 0);
        Arrays.fill(previousFrame, 0);
        Arrays.fill(overlapBuffer, 0);
    }
    
    public double getNoiseLevel() {
        double sum = 0;
        for (double v : noiseProfile) {
            sum += v;
        }
        return sum / noiseProfile.length;
    }
}
