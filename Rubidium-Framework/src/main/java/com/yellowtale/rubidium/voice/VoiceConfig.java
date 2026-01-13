package com.yellowtale.rubidium.voice;

import java.time.Duration;

public record VoiceConfig(
    boolean enabled,
    VoiceCodec codec,
    int sampleRate,
    int frameSize,
    int bitrate,
    double proximityRange,
    double falloffStart,
    double falloffEnd,
    boolean enable3DAudio,
    NoiseSuppressionLevel noiseSuppression,
    boolean enableAGC,
    boolean enableEchoCancellation,
    int maxChannelMembers,
    int maxChannelsPerPlayer,
    boolean allowCrossWorldChat,
    boolean autoJoinGlobal,
    boolean logVoiceActivity,
    boolean enableVoiceRecording,
    Duration recordingRetention
) {
    public static VoiceConfig defaults() {
        return new VoiceConfig(
            true,
            VoiceCodec.OPUS,
            48000,
            960,
            64000,
            64.0,
            16.0,
            64.0,
            true,
            NoiseSuppressionLevel.MODERATE,
            true,
            true,
            32,
            5,
            false,
            true,
            false,
            false,
            Duration.ofDays(7)
        );
    }
    
    public enum VoiceCodec {
        OPUS
    }
    
    public enum NoiseSuppressionLevel {
        OFF,
        LOW,
        MODERATE,
        HIGH,
        AGGRESSIVE
    }
}
