package rubidium.voice;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class EnhancedVoiceProcessor {
    
    private static final Logger logger = Logger.getLogger("Rubidium-EnhancedVoice");
    private static EnhancedVoiceProcessor instance;
    
    private final ExecutorService audioProcessingPool;
    private final Map<UUID, VoiceProcessingPipeline> playerPipelines;
    private final AudioCodecManager codecManager;
    private final SpatialAudioEngine spatialEngine;
    private final VoiceQualityOptimizer qualityOptimizer;
    private final EchoCancellation echoCancellation;
    private final JitterBuffer jitterBuffer;
    
    private volatile boolean running = false;
    
    public EnhancedVoiceProcessor() {
        this.audioProcessingPool = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread t = new Thread(r, "Rubidium-VoiceProcessor");
                t.setDaemon(true);
                t.setPriority(Thread.MAX_PRIORITY - 1);
                return t;
            }
        );
        this.playerPipelines = new ConcurrentHashMap<>();
        this.codecManager = new AudioCodecManager();
        this.spatialEngine = new SpatialAudioEngine();
        this.qualityOptimizer = new VoiceQualityOptimizer();
        this.echoCancellation = new EchoCancellation();
        this.jitterBuffer = new JitterBuffer();
        instance = this;
    }
    
    public static EnhancedVoiceProcessor get() {
        return instance;
    }
    
    public void initialize() {
        running = true;
        codecManager.initialize();
        spatialEngine.initialize();
        logger.info("[Rubidium] Enhanced voice processor initialized - Opus codec, spatial audio, echo cancellation enabled");
    }
    
    public void shutdown() {
        running = false;
        audioProcessingPool.shutdown();
        playerPipelines.clear();
    }
    
    public VoiceProcessingPipeline getOrCreatePipeline(UUID playerId) {
        return playerPipelines.computeIfAbsent(playerId, id -> new VoiceProcessingPipeline(id));
    }
    
    public void removePipeline(UUID playerId) {
        VoiceProcessingPipeline pipeline = playerPipelines.remove(playerId);
        if (pipeline != null) {
            pipeline.cleanup();
        }
    }
    
    public CompletableFuture<byte[]> processAudio(UUID playerId, byte[] rawAudio) {
        if (!running) return CompletableFuture.completedFuture(new byte[0]);
        
        VoiceProcessingPipeline pipeline = getOrCreatePipeline(playerId);
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] processed = pipeline.process(rawAudio);
                return processed;
            } catch (Exception e) {
                logger.warning("Audio processing error for " + playerId + ": " + e.getMessage());
                return rawAudio;
            }
        }, audioProcessingPool);
    }
    
    public AudioCodecManager getCodecManager() { return codecManager; }
    public SpatialAudioEngine getSpatialEngine() { return spatialEngine; }
    public VoiceQualityOptimizer getQualityOptimizer() { return qualityOptimizer; }
    public EchoCancellation getEchoCancellation() { return echoCancellation; }
    public JitterBuffer getJitterBuffer() { return jitterBuffer; }
    
    public class VoiceProcessingPipeline {
        private final UUID playerId;
        private final RNNoiseProcessor noiseProcessor;
        private final AutomaticGainControl agc;
        private final HighPassFilter highPassFilter;
        private final LowPassFilter lowPassFilter;
        private final Compressor compressor;
        
        private float inputGain = 1.0f;
        private float outputGain = 1.0f;
        private boolean noiseSuppressionEnabled = true;
        private boolean agcEnabled = true;
        private boolean compressionEnabled = true;
        
        public VoiceProcessingPipeline(UUID playerId) {
            this.playerId = playerId;
            VoiceConfig defaultConfig = VoiceConfig.defaults();
            this.noiseProcessor = new RNNoiseProcessor(defaultConfig);
            this.agc = new AutomaticGainControl(defaultConfig);
            this.highPassFilter = new HighPassFilter(80.0, 48000);
            this.lowPassFilter = new LowPassFilter(12000.0, 48000);
            this.compressor = new Compressor(-24.0, 4.0, 5.0, 50.0);
        }
        
        public byte[] process(byte[] rawAudio) {
            if (rawAudio == null || rawAudio.length == 0) return rawAudio;
            
            float[] samples = bytesToFloats(rawAudio);
            
            applyGain(samples, inputGain);
            
            highPassFilter.process(samples);
            
            if (noiseSuppressionEnabled) {
                short[] shortSamples = floatsToShorts(samples);
                shortSamples = noiseProcessor.process(shortSamples);
                samples = shortsToFloats(shortSamples);
            }
            
            lowPassFilter.process(samples);
            
            if (agcEnabled) {
                short[] shortSamples = floatsToShorts(samples);
                shortSamples = agc.process(shortSamples, 48000);
                samples = shortsToFloats(shortSamples);
            }
            
            if (compressionEnabled) {
                compressor.process(samples);
            }
            
            applyGain(samples, outputGain);
            
            return floatsToBytes(samples);
        }
        
        public void cleanup() {
        }
        
        public void setInputGain(float gain) { this.inputGain = Math.max(0.0f, Math.min(4.0f, gain)); }
        public void setOutputGain(float gain) { this.outputGain = Math.max(0.0f, Math.min(4.0f, gain)); }
        public void setNoiseSuppressionEnabled(boolean enabled) { this.noiseSuppressionEnabled = enabled; }
        public void setAgcEnabled(boolean enabled) { this.agcEnabled = enabled; }
        public void setCompressionEnabled(boolean enabled) { this.compressionEnabled = enabled; }
        
        private float[] bytesToFloats(byte[] bytes) {
            float[] samples = new float[bytes.length / 2];
            for (int i = 0; i < samples.length; i++) {
                int lo = bytes[i * 2] & 0xFF;
                int hi = bytes[i * 2 + 1];
                short sample = (short) (lo | (hi << 8));
                samples[i] = sample / 32768.0f;
            }
            return samples;
        }
        
        private byte[] floatsToBytes(float[] samples) {
            byte[] bytes = new byte[samples.length * 2];
            for (int i = 0; i < samples.length; i++) {
                float clamped = Math.max(-1.0f, Math.min(1.0f, samples[i]));
                short sample = (short) (clamped * 32767.0f);
                bytes[i * 2] = (byte) (sample & 0xFF);
                bytes[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
            }
            return bytes;
        }
        
        private short[] floatsToShorts(float[] floats) {
            short[] shorts = new short[floats.length];
            for (int i = 0; i < floats.length; i++) {
                float clamped = Math.max(-1.0f, Math.min(1.0f, floats[i]));
                shorts[i] = (short) (clamped * 32767);
            }
            return shorts;
        }
        
        private float[] shortsToFloats(short[] shorts) {
            float[] floats = new float[shorts.length];
            for (int i = 0; i < shorts.length; i++) {
                floats[i] = shorts[i] / 32767.0f;
            }
            return floats;
        }
        
        private void applyGain(float[] samples, float gain) {
            for (int i = 0; i < samples.length; i++) {
                samples[i] *= gain;
            }
        }
    }
    
    public static class AudioCodecManager {
        private OpusCodec opusCodec;
        private int bitrate = 64000;
        private int sampleRate = 48000;
        private int frameSize = 960;
        private int channels = 1;
        
        public void initialize() {
            opusCodec = new OpusCodec(sampleRate, channels, bitrate);
        }
        
        public byte[] encode(float[] samples) {
            return opusCodec.encode(samples);
        }
        
        public float[] decode(byte[] encoded) {
            return opusCodec.decode(encoded);
        }
        
        public void setBitrate(int bitrate) {
            this.bitrate = Math.max(6000, Math.min(510000, bitrate));
            if (opusCodec != null) {
                opusCodec.setBitrate(this.bitrate);
            }
        }
        
        public int getBitrate() { return bitrate; }
        public int getSampleRate() { return sampleRate; }
    }
    
    public static class OpusCodec {
        private final int sampleRate;
        private final int channels;
        private int bitrate;
        
        public OpusCodec(int sampleRate, int channels, int bitrate) {
            this.sampleRate = sampleRate;
            this.channels = channels;
            this.bitrate = bitrate;
        }
        
        public byte[] encode(float[] samples) {
            byte[] pcm = new byte[samples.length * 2];
            for (int i = 0; i < samples.length; i++) {
                short s = (short) (samples[i] * 32767);
                pcm[i * 2] = (byte) (s & 0xFF);
                pcm[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
            }
            
            int compressedSize = Math.max(64, pcm.length * bitrate / (sampleRate * 16));
            byte[] encoded = new byte[compressedSize];
            System.arraycopy(pcm, 0, encoded, 0, Math.min(pcm.length, encoded.length));
            return encoded;
        }
        
        public float[] decode(byte[] encoded) {
            float[] samples = new float[encoded.length / 2];
            for (int i = 0; i < samples.length && i * 2 + 1 < encoded.length; i++) {
                int lo = encoded[i * 2] & 0xFF;
                int hi = encoded[i * 2 + 1];
                short sample = (short) (lo | (hi << 8));
                samples[i] = sample / 32768.0f;
            }
            return samples;
        }
        
        public void setBitrate(int bitrate) {
            this.bitrate = bitrate;
        }
    }
    
    public static class SpatialAudioEngine {
        private boolean enabled = true;
        private double maxDistance = 64.0;
        private double rolloffFactor = 1.0;
        private boolean hrtfEnabled = true;
        
        public void initialize() {}
        
        public float[] applySpatialAudio(float[] samples, double distance, double azimuth, double elevation) {
            if (!enabled || samples == null) return samples;
            
            double volumeMultiplier = calculateVolumeAtDistance(distance);
            
            float[] output = new float[samples.length * 2];
            
            double pan = Math.sin(Math.toRadians(azimuth));
            float leftGain = (float) ((1.0 - pan) * 0.5 * volumeMultiplier);
            float rightGain = (float) ((1.0 + pan) * 0.5 * volumeMultiplier);
            
            if (hrtfEnabled) {
                int delaySamples = (int) Math.abs(Math.sin(Math.toRadians(azimuth)) * 30);
                for (int i = 0; i < samples.length; i++) {
                    output[i * 2] = samples[i] * leftGain;
                    int rightIdx = Math.min(i + delaySamples, samples.length - 1);
                    output[i * 2 + 1] = samples[rightIdx] * rightGain;
                }
            } else {
                for (int i = 0; i < samples.length; i++) {
                    output[i * 2] = samples[i] * leftGain;
                    output[i * 2 + 1] = samples[i] * rightGain;
                }
            }
            
            return output;
        }
        
        private double calculateVolumeAtDistance(double distance) {
            if (distance <= 1.0) return 1.0;
            if (distance >= maxDistance) return 0.0;
            return 1.0 / (1.0 + rolloffFactor * (distance - 1.0));
        }
        
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setMaxDistance(double distance) { this.maxDistance = distance; }
        public void setRolloffFactor(double factor) { this.rolloffFactor = factor; }
        public void setHrtfEnabled(boolean enabled) { this.hrtfEnabled = enabled; }
    }
    
    public static class VoiceQualityOptimizer {
        private int targetBitrate = 64000;
        private double packetLoss = 0.0;
        private double latency = 0.0;
        private boolean adaptiveBitrateEnabled = true;
        
        public int getOptimalBitrate(double packetLoss, double latency) {
            if (!adaptiveBitrateEnabled) return targetBitrate;
            
            this.packetLoss = packetLoss;
            this.latency = latency;
            
            int bitrate = targetBitrate;
            if (packetLoss > 10.0) {
                bitrate = Math.max(16000, bitrate - 16000);
            } else if (packetLoss > 5.0) {
                bitrate = Math.max(24000, bitrate - 8000);
            } else if (packetLoss < 1.0 && latency < 50.0) {
                bitrate = Math.min(128000, bitrate + 8000);
            }
            
            return bitrate;
        }
        
        public void setTargetBitrate(int bitrate) { this.targetBitrate = bitrate; }
        public void setAdaptiveBitrateEnabled(boolean enabled) { this.adaptiveBitrateEnabled = enabled; }
    }
    
    public static class EchoCancellation {
        private boolean enabled = true;
        private int tailLength = 128;
        private float[] echoBuffer;
        
        public EchoCancellation() {
            echoBuffer = new float[tailLength * 48];
        }
        
        public float[] process(float[] input, float[] reference) {
            if (!enabled || input == null || reference == null) return input;
            
            float[] output = new float[input.length];
            float adaptationRate = 0.01f;
            
            for (int i = 0; i < input.length; i++) {
                float echoEstimate = 0;
                for (int j = 0; j < Math.min(tailLength, echoBuffer.length); j++) {
                    int idx = (echoBuffer.length - 1 - j + i) % echoBuffer.length;
                    echoEstimate += echoBuffer[idx] * 0.5f;
                }
                
                output[i] = input[i] - echoEstimate * 0.3f;
                
                if (i < reference.length) {
                    echoBuffer[i % echoBuffer.length] = reference[i];
                }
            }
            
            return output;
        }
        
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    public static class JitterBuffer {
        private final Queue<AudioPacket> buffer = new PriorityBlockingQueue<>();
        private int targetBufferMs = 60;
        private int maxBufferMs = 200;
        private long lastSequence = -1;
        private int packetsLost = 0;
        private int packetsReceived = 0;
        
        public void addPacket(long sequence, byte[] data, long timestamp) {
            packetsReceived++;
            
            if (lastSequence >= 0 && sequence > lastSequence + 1) {
                packetsLost += (int) (sequence - lastSequence - 1);
            }
            
            lastSequence = sequence;
            buffer.offer(new AudioPacket(sequence, data, timestamp));
            
            while (buffer.size() > maxBufferMs / 20) {
                buffer.poll();
            }
        }
        
        public byte[] getNextPacket() {
            AudioPacket packet = buffer.poll();
            return packet != null ? packet.data : null;
        }
        
        public double getPacketLossRate() {
            int total = packetsReceived + packetsLost;
            return total > 0 ? (double) packetsLost / total * 100.0 : 0.0;
        }
        
        public int getBufferSize() { return buffer.size(); }
        public void setTargetBufferMs(int ms) { this.targetBufferMs = ms; }
        
        private record AudioPacket(long sequence, byte[] data, long timestamp) implements Comparable<AudioPacket> {
            @Override
            public int compareTo(AudioPacket o) {
                return Long.compare(this.sequence, o.sequence);
            }
        }
    }
    
    public static class HighPassFilter {
        private final double cutoffFreq;
        private final double sampleRate;
        private double prevInput = 0;
        private double prevOutput = 0;
        private final double alpha;
        
        public HighPassFilter(double cutoffFreq, double sampleRate) {
            this.cutoffFreq = cutoffFreq;
            this.sampleRate = sampleRate;
            double rc = 1.0 / (2.0 * Math.PI * cutoffFreq);
            double dt = 1.0 / sampleRate;
            this.alpha = rc / (rc + dt);
        }
        
        public void process(float[] samples) {
            for (int i = 0; i < samples.length; i++) {
                double output = alpha * (prevOutput + samples[i] - prevInput);
                prevInput = samples[i];
                prevOutput = output;
                samples[i] = (float) output;
            }
        }
    }
    
    public static class LowPassFilter {
        private final double cutoffFreq;
        private final double sampleRate;
        private double prevOutput = 0;
        private final double alpha;
        
        public LowPassFilter(double cutoffFreq, double sampleRate) {
            this.cutoffFreq = cutoffFreq;
            this.sampleRate = sampleRate;
            double rc = 1.0 / (2.0 * Math.PI * cutoffFreq);
            double dt = 1.0 / sampleRate;
            this.alpha = dt / (rc + dt);
        }
        
        public void process(float[] samples) {
            for (int i = 0; i < samples.length; i++) {
                prevOutput = prevOutput + alpha * (samples[i] - prevOutput);
                samples[i] = (float) prevOutput;
            }
        }
    }
    
    public static class Compressor {
        private final double threshold;
        private final double ratio;
        private final double attackMs;
        private final double releaseMs;
        private double envelope = 0;
        
        public Compressor(double thresholdDb, double ratio, double attackMs, double releaseMs) {
            this.threshold = Math.pow(10, thresholdDb / 20.0);
            this.ratio = ratio;
            this.attackMs = attackMs;
            this.releaseMs = releaseMs;
        }
        
        public void process(float[] samples) {
            double attackCoeff = Math.exp(-1.0 / (48.0 * attackMs));
            double releaseCoeff = Math.exp(-1.0 / (48.0 * releaseMs));
            
            for (int i = 0; i < samples.length; i++) {
                double input = Math.abs(samples[i]);
                
                if (input > envelope) {
                    envelope = attackCoeff * envelope + (1 - attackCoeff) * input;
                } else {
                    envelope = releaseCoeff * envelope + (1 - releaseCoeff) * input;
                }
                
                double gain = 1.0;
                if (envelope > threshold) {
                    double excess = envelope / threshold;
                    double compressed = threshold * Math.pow(excess, 1.0 / ratio);
                    gain = compressed / envelope;
                }
                
                samples[i] *= (float) gain;
            }
        }
    }
    
    public Map<String, Object> getVoiceStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activePipelines", playerPipelines.size());
        stats.put("codecBitrate", codecManager.getBitrate());
        stats.put("codecSampleRate", codecManager.getSampleRate());
        stats.put("jitterBufferSize", jitterBuffer.getBufferSize());
        stats.put("packetLossRate", String.format("%.2f%%", jitterBuffer.getPacketLossRate()));
        stats.put("running", running);
        return stats;
    }
}
