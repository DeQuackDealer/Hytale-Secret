package rubidium.voice;

import java.time.Duration;

public record VoiceConfig(
    boolean enabled,
    VoiceCodec codec,
    int sampleRate,
    int frameSize,
    int bitrate,
    int channels,
    double proximityRange,
    double falloffStart,
    double falloffEnd,
    boolean enable3DAudio,
    NoiseSuppressionLevel noiseSuppression,
    boolean enableAGC,
    double agcTargetLevel,
    double agcMaxGain,
    boolean enableEchoCancellation,
    boolean enableVAD,
    double vadThreshold,
    int vadHangTime,
    ActivationMode defaultActivationMode,
    int maxChannelMembers,
    int maxChannelsPerPlayer,
    int maxGroupsPerPlayer,
    boolean allowCrossWorldChat,
    boolean autoJoinGlobal,
    boolean logVoiceActivity,
    boolean enableVoiceRecording,
    Duration recordingRetention,
    boolean enableWhisperMode,
    double whisperRange,
    int udpPort,
    int maxPacketSize,
    int jitterBufferSize
) {
    public static VoiceConfig defaults() {
        return new VoiceConfig(
            true,
            VoiceCodec.OPUS,
            48000,
            960,
            64000,
            1,
            64.0,
            16.0,
            64.0,
            true,
            NoiseSuppressionLevel.MODERATE,
            true,
            -18.0,
            30.0,
            true,
            true,
            0.003,
            300,
            ActivationMode.VOICE_ACTIVATION,
            32,
            5,
            10,
            false,
            true,
            false,
            false,
            Duration.ofDays(7),
            true,
            8.0,
            24454,
            1400,
            6
        );
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public enum VoiceCodec {
        OPUS(true, 6, 510),
        OPUS_MUSIC(true, 10, 510);
        
        private final boolean stereoSupport;
        private final int minBitrate;
        private final int maxBitrate;
        
        VoiceCodec(boolean stereoSupport, int minBitrate, int maxBitrate) {
            this.stereoSupport = stereoSupport;
            this.minBitrate = minBitrate;
            this.maxBitrate = maxBitrate;
        }
        
        public boolean supportsStereo() { return stereoSupport; }
        public int getMinBitrate() { return minBitrate * 1000; }
        public int getMaxBitrate() { return maxBitrate * 1000; }
    }
    
    public enum NoiseSuppressionLevel {
        OFF(0.0),
        LOW(0.25),
        MODERATE(0.5),
        HIGH(0.75),
        AGGRESSIVE(1.0);
        
        private final double strength;
        
        NoiseSuppressionLevel(double strength) {
            this.strength = strength;
        }
        
        public double getStrength() { return strength; }
    }
    
    public enum ActivationMode {
        PUSH_TO_TALK,
        VOICE_ACTIVATION,
        HYBRID
    }
    
    public static class Builder {
        private boolean enabled = true;
        private VoiceCodec codec = VoiceCodec.OPUS;
        private int sampleRate = 48000;
        private int frameSize = 960;
        private int bitrate = 64000;
        private int channels = 1;
        private double proximityRange = 64.0;
        private double falloffStart = 16.0;
        private double falloffEnd = 64.0;
        private boolean enable3DAudio = true;
        private NoiseSuppressionLevel noiseSuppression = NoiseSuppressionLevel.MODERATE;
        private boolean enableAGC = true;
        private double agcTargetLevel = -18.0;
        private double agcMaxGain = 30.0;
        private boolean enableEchoCancellation = true;
        private boolean enableVAD = true;
        private double vadThreshold = 0.003;
        private int vadHangTime = 300;
        private ActivationMode defaultActivationMode = ActivationMode.VOICE_ACTIVATION;
        private int maxChannelMembers = 32;
        private int maxChannelsPerPlayer = 5;
        private int maxGroupsPerPlayer = 10;
        private boolean allowCrossWorldChat = false;
        private boolean autoJoinGlobal = true;
        private boolean logVoiceActivity = false;
        private boolean enableVoiceRecording = false;
        private Duration recordingRetention = Duration.ofDays(7);
        private boolean enableWhisperMode = true;
        private double whisperRange = 8.0;
        private int udpPort = 24454;
        private int maxPacketSize = 1400;
        private int jitterBufferSize = 6;
        
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder codec(VoiceCodec codec) { this.codec = codec; return this; }
        public Builder sampleRate(int sampleRate) { this.sampleRate = sampleRate; return this; }
        public Builder frameSize(int frameSize) { this.frameSize = frameSize; return this; }
        public Builder bitrate(int bitrate) { this.bitrate = bitrate; return this; }
        public Builder channels(int channels) { this.channels = channels; return this; }
        public Builder proximityRange(double proximityRange) { this.proximityRange = proximityRange; return this; }
        public Builder falloffStart(double falloffStart) { this.falloffStart = falloffStart; return this; }
        public Builder falloffEnd(double falloffEnd) { this.falloffEnd = falloffEnd; return this; }
        public Builder enable3DAudio(boolean enable3DAudio) { this.enable3DAudio = enable3DAudio; return this; }
        public Builder noiseSuppression(NoiseSuppressionLevel noiseSuppression) { this.noiseSuppression = noiseSuppression; return this; }
        public Builder enableAGC(boolean enableAGC) { this.enableAGC = enableAGC; return this; }
        public Builder agcTargetLevel(double agcTargetLevel) { this.agcTargetLevel = agcTargetLevel; return this; }
        public Builder agcMaxGain(double agcMaxGain) { this.agcMaxGain = agcMaxGain; return this; }
        public Builder enableEchoCancellation(boolean enableEchoCancellation) { this.enableEchoCancellation = enableEchoCancellation; return this; }
        public Builder enableVAD(boolean enableVAD) { this.enableVAD = enableVAD; return this; }
        public Builder vadThreshold(double vadThreshold) { this.vadThreshold = vadThreshold; return this; }
        public Builder vadHangTime(int vadHangTime) { this.vadHangTime = vadHangTime; return this; }
        public Builder defaultActivationMode(ActivationMode defaultActivationMode) { this.defaultActivationMode = defaultActivationMode; return this; }
        public Builder maxChannelMembers(int maxChannelMembers) { this.maxChannelMembers = maxChannelMembers; return this; }
        public Builder maxChannelsPerPlayer(int maxChannelsPerPlayer) { this.maxChannelsPerPlayer = maxChannelsPerPlayer; return this; }
        public Builder maxGroupsPerPlayer(int maxGroupsPerPlayer) { this.maxGroupsPerPlayer = maxGroupsPerPlayer; return this; }
        public Builder allowCrossWorldChat(boolean allowCrossWorldChat) { this.allowCrossWorldChat = allowCrossWorldChat; return this; }
        public Builder autoJoinGlobal(boolean autoJoinGlobal) { this.autoJoinGlobal = autoJoinGlobal; return this; }
        public Builder logVoiceActivity(boolean logVoiceActivity) { this.logVoiceActivity = logVoiceActivity; return this; }
        public Builder enableVoiceRecording(boolean enableVoiceRecording) { this.enableVoiceRecording = enableVoiceRecording; return this; }
        public Builder recordingRetention(Duration recordingRetention) { this.recordingRetention = recordingRetention; return this; }
        public Builder enableWhisperMode(boolean enableWhisperMode) { this.enableWhisperMode = enableWhisperMode; return this; }
        public Builder whisperRange(double whisperRange) { this.whisperRange = whisperRange; return this; }
        public Builder udpPort(int udpPort) { this.udpPort = udpPort; return this; }
        public Builder maxPacketSize(int maxPacketSize) { this.maxPacketSize = maxPacketSize; return this; }
        public Builder jitterBufferSize(int jitterBufferSize) { this.jitterBufferSize = jitterBufferSize; return this; }
        
        public VoiceConfig build() {
            return new VoiceConfig(
                enabled, codec, sampleRate, frameSize, bitrate, channels,
                proximityRange, falloffStart, falloffEnd, enable3DAudio,
                noiseSuppression, enableAGC, agcTargetLevel, agcMaxGain,
                enableEchoCancellation, enableVAD, vadThreshold, vadHangTime,
                defaultActivationMode, maxChannelMembers, maxChannelsPerPlayer,
                maxGroupsPerPlayer, allowCrossWorldChat, autoJoinGlobal,
                logVoiceActivity, enableVoiceRecording, recordingRetention,
                enableWhisperMode, whisperRange, udpPort, maxPacketSize, jitterBufferSize
            );
        }
    }
}
