package rubidium.voice;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Audio mixer with 3D spatial processing and effects.
 */
public class AudioMixer {
    
    private static final Logger logger = Logger.getLogger("Rubidium-AudioMixer");
    
    private final ExecutorService processingPool;
    private final Map<UUID, AudioStream> activeStreams = new ConcurrentHashMap<>();
    
    private volatile boolean running = false;
    private volatile double processingLoad = 0.0;
    
    public AudioMixer() {
        this.processingPool = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "AudioMixer-Processor");
            t.setDaemon(true);
            return t;
        });
    }
    
    public void initialize() {
        running = true;
        logger.info("Audio mixer initialized");
    }
    
    public void shutdown() {
        running = false;
        processingPool.shutdown();
        try {
            if (!processingPool.awaitTermination(5, TimeUnit.SECONDS)) {
                processingPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            processingPool.shutdownNow();
        }
    }
    
    public double getProcessingLoad() {
        return processingLoad;
    }
    
    public byte[] processAudio(byte[] input, AudioProcessingParams params) {
        if (!running) return input;
        
        byte[] processed = input;
        
        if (params.applyNoiseSuppression) {
            processed = applyNoiseSuppression(processed);
        }
        
        if (params.applyEchoCancellation) {
            processed = applyEchoCancellation(processed);
        }
        
        if (params.volume != 1.0f) {
            processed = applyVolume(processed, params.volume);
        }
        
        return processed;
    }
    
    public byte[] applySpatialAudio(byte[] input, SpatialParams params) {
        if (!running) return input;
        
        float leftGain = calculateLeftGain(params);
        float rightGain = calculateRightGain(params);
        
        byte[] stereoOutput = new byte[input.length * 2];
        
        for (int i = 0; i < input.length; i += 2) {
            short sample = (short) ((input[i] & 0xFF) | (input[i + 1] << 8));
            
            short leftSample = (short) (sample * leftGain);
            short rightSample = (short) (sample * rightGain);
            
            int outIndex = i * 2;
            stereoOutput[outIndex] = (byte) (leftSample & 0xFF);
            stereoOutput[outIndex + 1] = (byte) ((leftSample >> 8) & 0xFF);
            stereoOutput[outIndex + 2] = (byte) (rightSample & 0xFF);
            stereoOutput[outIndex + 3] = (byte) ((rightSample >> 8) & 0xFF);
        }
        
        return stereoOutput;
    }
    
    private float calculateLeftGain(SpatialParams params) {
        double angle = Math.atan2(params.relativeZ, params.relativeX);
        float pan = (float) ((angle / Math.PI + 1.0) / 2.0);
        return (1.0f - pan) * params.volume;
    }
    
    private float calculateRightGain(SpatialParams params) {
        double angle = Math.atan2(params.relativeZ, params.relativeX);
        float pan = (float) ((angle / Math.PI + 1.0) / 2.0);
        return pan * params.volume;
    }
    
    private byte[] applyNoiseSuppression(byte[] input) {
        return input;
    }
    
    private byte[] applyEchoCancellation(byte[] input) {
        return input;
    }
    
    private byte[] applyVolume(byte[] input, float volume) {
        byte[] output = new byte[input.length];
        
        for (int i = 0; i < input.length; i += 2) {
            short sample = (short) ((input[i] & 0xFF) | (input[i + 1] << 8));
            sample = (short) Math.max(-32768, Math.min(32767, sample * volume));
            output[i] = (byte) (sample & 0xFF);
            output[i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        
        return output;
    }
    
    public void createStream(UUID playerId) {
        activeStreams.put(playerId, new AudioStream(playerId));
    }
    
    public void destroyStream(UUID playerId) {
        AudioStream stream = activeStreams.remove(playerId);
        if (stream != null) {
            stream.close();
        }
    }
    
    public record AudioProcessingParams(
        float volume,
        boolean applyNoiseSuppression,
        boolean applyEchoCancellation
    ) {
        public static AudioProcessingParams defaults() {
            return new AudioProcessingParams(1.0f, true, true);
        }
    }
    
    public record SpatialParams(
        double relativeX,
        double relativeY,
        double relativeZ,
        double distance,
        float volume
    ) {}
    
    private static class AudioStream {
        private final UUID playerId;
        private final Queue<byte[]> buffer = new ConcurrentLinkedQueue<>();
        private volatile boolean active = true;
        
        AudioStream(UUID playerId) {
            this.playerId = playerId;
        }
        
        void addFrame(byte[] frame) {
            if (active && buffer.size() < 100) {
                buffer.offer(frame);
            }
        }
        
        byte[] nextFrame() {
            return buffer.poll();
        }
        
        void close() {
            active = false;
            buffer.clear();
        }
    }
}
