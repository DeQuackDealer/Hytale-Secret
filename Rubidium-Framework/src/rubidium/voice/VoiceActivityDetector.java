package rubidium.voice;

import java.util.Arrays;

public class VoiceActivityDetector {
    
    private final double threshold;
    private final int hangTime;
    private final int sampleRate;
    private final int frameSize;
    
    private double[] energyHistory;
    private int historyIndex = 0;
    private long lastVoiceDetected = 0;
    private boolean speaking = false;
    
    private double adaptiveThreshold;
    private double noiseFloor = 0.0001;
    private double signalLevel = 0.0;
    
    public VoiceActivityDetector(VoiceConfig config) {
        this.threshold = config.vadThreshold();
        this.hangTime = config.vadHangTime();
        this.sampleRate = config.sampleRate();
        this.frameSize = config.frameSize();
        this.energyHistory = new double[50];
        this.adaptiveThreshold = threshold;
    }
    
    public VoiceActivityDetector(double threshold, int hangTime, int sampleRate, int frameSize) {
        this.threshold = threshold;
        this.hangTime = hangTime;
        this.sampleRate = sampleRate;
        this.frameSize = frameSize;
        this.energyHistory = new double[50];
        this.adaptiveThreshold = threshold;
    }
    
    public boolean process(byte[] audioData) {
        double energy = calculateEnergy(audioData);
        
        updateEnergyHistory(energy);
        updateAdaptiveThreshold();
        
        signalLevel = energy;
        
        boolean voiceDetected = energy > adaptiveThreshold;
        
        long now = System.currentTimeMillis();
        
        if (voiceDetected) {
            lastVoiceDetected = now;
            speaking = true;
        } else if (speaking && (now - lastVoiceDetected) > hangTime) {
            speaking = false;
        }
        
        return speaking;
    }
    
    public boolean process(short[] samples) {
        double energy = calculateEnergy(samples);
        
        updateEnergyHistory(energy);
        updateAdaptiveThreshold();
        
        signalLevel = energy;
        
        boolean voiceDetected = energy > adaptiveThreshold;
        
        long now = System.currentTimeMillis();
        
        if (voiceDetected) {
            lastVoiceDetected = now;
            speaking = true;
        } else if (speaking && (now - lastVoiceDetected) > hangTime) {
            speaking = false;
        }
        
        return speaking;
    }
    
    private double calculateEnergy(byte[] audioData) {
        double sum = 0;
        int sampleCount = audioData.length / 2;
        
        for (int i = 0; i < audioData.length - 1; i += 2) {
            short sample = (short) ((audioData[i] & 0xFF) | (audioData[i + 1] << 8));
            double normalized = sample / 32768.0;
            sum += normalized * normalized;
        }
        
        return sampleCount > 0 ? Math.sqrt(sum / sampleCount) : 0;
    }
    
    private double calculateEnergy(short[] samples) {
        double sum = 0;
        
        for (short sample : samples) {
            double normalized = sample / 32768.0;
            sum += normalized * normalized;
        }
        
        return samples.length > 0 ? Math.sqrt(sum / samples.length) : 0;
    }
    
    private void updateEnergyHistory(double energy) {
        energyHistory[historyIndex] = energy;
        historyIndex = (historyIndex + 1) % energyHistory.length;
    }
    
    private void updateAdaptiveThreshold() {
        double[] sorted = energyHistory.clone();
        Arrays.sort(sorted);
        
        double median = sorted[sorted.length / 2];
        noiseFloor = noiseFloor * 0.99 + median * 0.01;
        
        adaptiveThreshold = Math.max(threshold, noiseFloor * 3.0);
    }
    
    public boolean isSpeaking() {
        return speaking;
    }
    
    public double getSignalLevel() {
        return signalLevel;
    }
    
    public double getNoiseFloor() {
        return noiseFloor;
    }
    
    public double getAdaptiveThreshold() {
        return adaptiveThreshold;
    }
    
    public void reset() {
        speaking = false;
        lastVoiceDetected = 0;
        historyIndex = 0;
        Arrays.fill(energyHistory, 0);
        noiseFloor = 0.0001;
        signalLevel = 0;
        adaptiveThreshold = threshold;
    }
}
