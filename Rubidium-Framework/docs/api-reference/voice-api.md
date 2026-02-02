# Voice Chat API

> **Rubidium Plus Only** - This feature requires a Rubidium Plus subscription.

## Overview

The Enhanced Voice Processor API provides high-quality, real-time voice communication for multiplayer servers. It includes advanced audio processing features such as noise suppression, spatial audio, echo cancellation, and adaptive bitrate optimization.

## Package

```java
import rubidium.voice.EnhancedVoiceProcessor;
```

## Getting Started

```java
EnhancedVoiceProcessor voiceProcessor = EnhancedVoiceProcessor.get();
voiceProcessor.initialize();
```

---

## EnhancedVoiceProcessor

Main class for voice chat functionality. Manages player audio pipelines and audio processing components.

### Static Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `get()` | `EnhancedVoiceProcessor` | Returns the singleton instance |

### Instance Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `initialize()` | `void` | Initializes the voice processor with Opus codec and spatial audio |
| `shutdown()` | `void` | Shuts down the processor and cleans up resources |
| `getOrCreatePipeline(UUID playerId)` | `VoiceProcessingPipeline` | Gets or creates an audio pipeline for a player |
| `removePipeline(UUID playerId)` | `void` | Removes and cleans up a player's pipeline |
| `processAudio(UUID playerId, byte[] rawAudio)` | `CompletableFuture<byte[]>` | Asynchronously processes raw audio data |
| `getCodecManager()` | `AudioCodecManager` | Returns the codec manager |
| `getSpatialEngine()` | `SpatialAudioEngine` | Returns the spatial audio engine |
| `getQualityOptimizer()` | `VoiceQualityOptimizer` | Returns the quality optimizer |
| `getEchoCancellation()` | `EchoCancellation` | Returns the echo cancellation processor |
| `getJitterBuffer()` | `JitterBuffer` | Returns the jitter buffer |
| `getVoiceStats()` | `Map<String, Object>` | Returns voice processing statistics |

### Code Example

```java
EnhancedVoiceProcessor processor = EnhancedVoiceProcessor.get();
processor.initialize();

// Process audio for a player
UUID playerId = player.getUniqueId();
CompletableFuture<byte[]> processed = processor.processAudio(playerId, rawAudioData);

processed.thenAccept(data -> {
    // Send processed audio to other players
    broadcastVoice(playerId, data);
});

// Cleanup when player disconnects
processor.removePipeline(playerId);
```

---

## VoiceProcessingPipeline

Per-player audio processing pipeline with noise suppression, gain control, filtering, and compression.

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `process(byte[] rawAudio)` | `byte[]` | Processes raw audio through the pipeline |
| `cleanup()` | `void` | Cleans up pipeline resources |
| `setInputGain(float gain)` | `void` | Sets input gain (0.0 - 4.0) |
| `setOutputGain(float gain)` | `void` | Sets output gain (0.0 - 4.0) |
| `setNoiseSuppressionEnabled(boolean enabled)` | `void` | Enables/disables RNNoise suppression |
| `setAgcEnabled(boolean enabled)` | `void` | Enables/disables automatic gain control |
| `setCompressionEnabled(boolean enabled)` | `void` | Enables/disables dynamic compression |

### Code Example

```java
VoiceProcessingPipeline pipeline = processor.getOrCreatePipeline(playerId);

// Configure pipeline settings
pipeline.setInputGain(1.2f);
pipeline.setOutputGain(1.0f);
pipeline.setNoiseSuppressionEnabled(true);
pipeline.setAgcEnabled(true);
pipeline.setCompressionEnabled(true);

// Process audio
byte[] processedAudio = pipeline.process(rawAudio);
```

---

## AudioCodecManager

Manages Opus audio codec for high-quality, low-latency voice encoding.

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `initialize()` | `void` | Initializes the Opus codec |
| `encode(float[] samples)` | `byte[]` | Encodes PCM samples to Opus |
| `decode(byte[] encoded)` | `float[]` | Decodes Opus data to PCM samples |
| `setBitrate(int bitrate)` | `void` | Sets encoding bitrate (6000 - 510000 bps) |
| `getBitrate()` | `int` | Returns current bitrate |
| `getSampleRate()` | `int` | Returns sample rate (default: 48000 Hz) |

### Code Example

```java
AudioCodecManager codec = processor.getCodecManager();

// Set higher quality for premium users
codec.setBitrate(128000);

// Encode audio for transmission
byte[] encoded = codec.encode(pcmSamples);

// Decode received audio
float[] decoded = codec.decode(receivedData);
```

---

## SpatialAudioEngine

Provides 3D positional audio with HRTF (Head-Related Transfer Function) for immersive voice chat.

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `initialize()` | `void` | Initializes the spatial audio engine |
| `applySpatialAudio(float[] samples, double distance, double azimuth, double elevation)` | `float[]` | Applies 3D positioning to mono audio |
| `setEnabled(boolean enabled)` | `void` | Enables/disables spatial audio |
| `setMaxDistance(double distance)` | `void` | Sets maximum audible distance (default: 64 blocks) |
| `setRolloffFactor(double factor)` | `void` | Sets volume rolloff factor (default: 1.0) |
| `setHrtfEnabled(boolean enabled)` | `void` | Enables/disables HRTF processing |

### Code Example

```java
SpatialAudioEngine spatial = processor.getSpatialEngine();

// Configure spatial audio
spatial.setMaxDistance(100.0);
spatial.setRolloffFactor(1.5);
spatial.setHrtfEnabled(true);

// Apply spatial positioning
double distance = calculateDistance(speaker, listener);
double azimuth = calculateAzimuth(speaker, listener);
double elevation = calculateElevation(speaker, listener);

float[] stereoOutput = spatial.applySpatialAudio(monoSamples, distance, azimuth, elevation);
```

---

## VoiceQualityOptimizer

Adaptive bitrate control based on network conditions.

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getOptimalBitrate(double packetLoss, double latency)` | `int` | Calculates optimal bitrate for network conditions |
| `setTargetBitrate(int bitrate)` | `void` | Sets target bitrate |
| `setAdaptiveBitrateEnabled(boolean enabled)` | `void` | Enables/disables adaptive bitrate |

### Code Example

```java
VoiceQualityOptimizer optimizer = processor.getQualityOptimizer();

// Enable adaptive quality
optimizer.setAdaptiveBitrateEnabled(true);
optimizer.setTargetBitrate(64000);

// Adjust based on network conditions
double packetLoss = getPacketLossPercentage();
double latency = getAverageLatency();
int optimalBitrate = optimizer.getOptimalBitrate(packetLoss, latency);

codec.setBitrate(optimalBitrate);
```

---

## EchoCancellation

Acoustic echo cancellation to prevent feedback loops.

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `process(float[] input, float[] reference)` | `float[]` | Removes echo from input using reference signal |
| `setEnabled(boolean enabled)` | `void` | Enables/disables echo cancellation |

### Code Example

```java
EchoCancellation aec = processor.getEchoCancellation();
aec.setEnabled(true);

// Process with echo cancellation
float[] cleanAudio = aec.process(microphoneInput, speakerOutput);
```

---

## JitterBuffer

Network jitter compensation for smooth audio playback.

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `addPacket(long sequence, byte[] data, long timestamp)` | `void` | Adds an audio packet to the buffer |
| `getNextPacket()` | `byte[]` | Retrieves the next packet in sequence order |
| `getPacketLossRate()` | `double` | Returns packet loss percentage |
| `getBufferSize()` | `int` | Returns current buffer size |
| `setTargetBufferMs(int ms)` | `void` | Sets target buffer size in milliseconds |

### Code Example

```java
JitterBuffer buffer = processor.getJitterBuffer();
buffer.setTargetBufferMs(60);

// Add incoming packets
buffer.addPacket(sequenceNumber, audioData, timestamp);

// Retrieve packets for playback
byte[] nextAudio = buffer.getNextPacket();
if (nextAudio != null) {
    playAudio(nextAudio);
}

// Monitor quality
double lossRate = buffer.getPacketLossRate();
if (lossRate > 5.0) {
    logger.warning("High packet loss: " + lossRate + "%");
}
```

---

## Audio Filters

### HighPassFilter

Removes low-frequency noise (rumble, pops).

```java
HighPassFilter filter = new HighPassFilter(80.0, 48000); // 80Hz cutoff, 48kHz sample rate
filter.process(samples);
```

### LowPassFilter

Removes high-frequency noise and artifacts.

```java
LowPassFilter filter = new LowPassFilter(12000.0, 48000); // 12kHz cutoff
filter.process(samples);
```

### Compressor

Dynamic range compression for consistent volume levels.

```java
// threshold (dB), ratio, attack (ms), release (ms)
Compressor compressor = new Compressor(-24.0, 4.0, 5.0, 50.0);
compressor.process(samples);
```

---

## Performance Considerations

- **Thread Pool**: Voice processing uses a dedicated thread pool sized to half of available CPU cores (minimum 2)
- **Processing Priority**: Voice threads run at high priority (`Thread.MAX_PRIORITY - 1`)
- **Sample Rate**: Default 48kHz mono for optimal quality/bandwidth balance
- **Frame Size**: 960 samples (20ms frames) for low latency
- **Memory**: Each player pipeline allocates approximately 32KB for buffers
- **Latency**: Target end-to-end latency of 60-100ms with default settings

### Optimization Tips

1. **Disable unused features** - Turn off HRTF if not needed for proximity chat
2. **Adjust bitrate** - Use adaptive bitrate for variable network conditions
3. **Buffer sizing** - Increase jitter buffer for unstable networks, decrease for LAN
4. **Spatial distance** - Reduce max distance to limit processing overhead

---

## Statistics

```java
Map<String, Object> stats = processor.getVoiceStats();
// Returns:
// - activePipelines: Number of active player pipelines
// - codecBitrate: Current encoder bitrate
// - codecSampleRate: Sample rate (48000)
// - jitterBufferSize: Current buffer size
// - packetLossRate: Packet loss percentage
// - running: Whether processor is active
```
