package rubidium.voice;

public class AutomaticGainControl {
    
    private final double targetLevel;
    private final double maxGain;
    private final double minGain;
    private final double attackTime;
    private final double releaseTime;
    
    private double currentGain = 1.0;
    private double envelope = 0.0;
    private double peakHold = 0.0;
    private long lastPeakTime = 0;
    
    private static final double PEAK_HOLD_TIME_MS = 100;
    private static final double LIMITER_THRESHOLD = 0.95;
    
    public AutomaticGainControl(VoiceConfig config) {
        this.targetLevel = Math.pow(10, config.agcTargetLevel() / 20.0);
        this.maxGain = Math.pow(10, config.agcMaxGain() / 20.0);
        this.minGain = 0.1;
        this.attackTime = 0.01;
        this.releaseTime = 0.3;
    }
    
    public AutomaticGainControl(double targetLevelDb, double maxGainDb) {
        this.targetLevel = Math.pow(10, targetLevelDb / 20.0);
        this.maxGain = Math.pow(10, maxGainDb / 20.0);
        this.minGain = 0.1;
        this.attackTime = 0.01;
        this.releaseTime = 0.3;
    }
    
    public byte[] process(byte[] audioData, int sampleRate) {
        short[] samples = bytesToShorts(audioData);
        short[] processed = process(samples, sampleRate);
        return shortsToBytes(processed);
    }
    
    public short[] process(short[] samples, int sampleRate) {
        double attackCoef = Math.exp(-1.0 / (attackTime * sampleRate));
        double releaseCoef = Math.exp(-1.0 / (releaseTime * sampleRate));
        
        short[] output = new short[samples.length];
        
        for (int i = 0; i < samples.length; i++) {
            double input = samples[i] / 32768.0;
            double absInput = Math.abs(input);
            
            if (absInput > envelope) {
                envelope = attackCoef * envelope + (1 - attackCoef) * absInput;
            } else {
                envelope = releaseCoef * envelope + (1 - releaseCoef) * absInput;
            }
            
            updatePeakHold(absInput);
            
            double desiredGain = calculateDesiredGain();
            
            currentGain = smoothGain(currentGain, desiredGain, attackCoef, releaseCoef);
            
            double amplified = input * currentGain;
            
            amplified = softLimit(amplified);
            
            amplified = Math.max(-1.0, Math.min(1.0, amplified));
            output[i] = (short) (amplified * 32767);
        }
        
        return output;
    }
    
    private void updatePeakHold(double absInput) {
        long now = System.currentTimeMillis();
        
        if (absInput > peakHold || (now - lastPeakTime) > PEAK_HOLD_TIME_MS) {
            peakHold = absInput;
            lastPeakTime = now;
        }
    }
    
    private double calculateDesiredGain() {
        if (envelope < 0.0001) {
            return currentGain;
        }
        
        double desired = targetLevel / envelope;
        
        desired = Math.max(minGain, Math.min(maxGain, desired));
        
        if (peakHold * desired > LIMITER_THRESHOLD) {
            desired = LIMITER_THRESHOLD / peakHold;
        }
        
        return desired;
    }
    
    private double smoothGain(double current, double target, double attack, double release) {
        if (target < current) {
            return attack * current + (1 - attack) * target;
        } else {
            return release * current + (1 - release) * target;
        }
    }
    
    private double softLimit(double input) {
        if (Math.abs(input) <= LIMITER_THRESHOLD) {
            return input;
        }
        
        double sign = input >= 0 ? 1 : -1;
        double abs = Math.abs(input);
        
        return sign * (LIMITER_THRESHOLD + (1 - LIMITER_THRESHOLD) * Math.tanh((abs - LIMITER_THRESHOLD) / (1 - LIMITER_THRESHOLD)));
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
    
    public double getCurrentGainDb() {
        return 20 * Math.log10(currentGain);
    }
    
    public double getEnvelopeDb() {
        return envelope > 0 ? 20 * Math.log10(envelope) : -60;
    }
    
    public void reset() {
        currentGain = 1.0;
        envelope = 0.0;
        peakHold = 0.0;
        lastPeakTime = 0;
    }
}
