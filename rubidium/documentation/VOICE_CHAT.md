# Voice Chat System

> **Document Purpose**: Complete reference for Rubidium's proximity and channel-based voice chat system.

## Overview

Rubidium's voice chat system provides real-time voice communication for players with:
- **Proximity Chat**: 3D spatial audio that attenuates with distance
- **Channel Chat**: Private channels for parties, teams, or custom groups
- **Admin Controls**: Mute, priority speaker, and moderation tools
- **Quality Settings**: Configurable codec, bitrate, and noise suppression

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       Client Side                                │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐                  │
│  │ Microphone│───►│ Encoder  │───►│ Network  │                  │
│  │ Capture  │    │ (Opus)   │    │ Send     │                  │
│  └──────────┘    └──────────┘    └──────────┘                  │
│                                                                  │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐                  │
│  │ Speaker  │◄───│ Decoder  │◄───│ Network  │                  │
│  │ Output   │    │ (Opus)   │    │ Receive  │                  │
│  └──────────┘    └──────────┘    └──────────┘                  │
├─────────────────────────────────────────────────────────────────┤
│                       Server Side                                │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                   VoiceChatManager                           ││
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       ││
│  │  │ Proximity│ │ Channel  │ │ Audio    │ │ Admin    │       ││
│  │  │ Manager  │ │ Manager  │ │ Router   │ │ Controls │       ││
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘       ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

## Core Classes

### VoiceChatManager

```java
package com.yellowtale.rubidium.voice;

public class VoiceChatManager {
    
    // Configuration
    private VoiceConfig config;
    
    // Sub-managers
    private ProximityManager proximityManager;
    private ChannelManager channelManager;
    private AudioRouter audioRouter;
    private VoiceMetrics metrics;
    
    // Player state
    private Map<UUID, VoiceState> playerStates;
    private Map<UUID, VoiceChannel> playerChannels;
    
    // Methods
    public void enable();
    public void disable();
    public void tick();
    
    // Player management
    public void onPlayerJoin(Player player);
    public void onPlayerQuit(Player player);
    public void onPlayerMove(Player player, Vector3d newPosition);
    
    // Voice control
    public void setMuted(UUID playerId, boolean muted);
    public void setDeafened(UUID playerId, boolean deafened);
    public void setPrioritySpeaker(UUID playerId, boolean priority);
    
    // Channel management
    public VoiceChannel createChannel(String name, ChannelType type);
    public void joinChannel(UUID playerId, VoiceChannel channel);
    public void leaveChannel(UUID playerId, VoiceChannel channel);
    
    // Audio routing
    public void routeAudio(UUID speaker, byte[] audioData);
}
```

### VoiceConfig

```java
public record VoiceConfig(
    // General
    boolean enabled,
    VoiceCodec codec,
    int sampleRate,
    int frameSize,
    int bitrate,
    
    // Proximity
    double proximityRange,
    double falloffStart,
    double falloffEnd,
    boolean enable3DAudio,
    
    // Quality
    NoiseSuppressionLevel noiseSuppression,
    boolean enableAGC,
    boolean enableEchoCancellation,
    
    // Channels
    int maxChannelMembers,
    int maxChannelsPerPlayer,
    boolean allowCrossWorldChat,
    
    // Moderation
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
            false,
            false,
            Duration.ofDays(7)
        );
    }
}
```

### VoiceState

```java
public class VoiceState {
    private UUID playerId;
    private Vector3d position;
    private Vector3d lookDirection;
    
    // Status flags
    private boolean speaking;
    private boolean muted;
    private boolean selfMuted;
    private boolean deafened;
    private boolean selfDeafened;
    private boolean prioritySpeaker;
    
    // Audio settings
    private float inputVolume;
    private float outputVolume;
    private NoiseSuppressionLevel noiseSuppression;
    
    // Statistics
    private long lastVoiceActivity;
    private long totalSpeakTimeMs;
    private int packetsReceived;
    private int packetsSent;
    private int packetsDropped;
}
```

### VoiceChannel

```java
public class VoiceChannel {
    private String id;
    private String name;
    private ChannelType type;
    private UUID owner;
    
    private Set<UUID> members;
    private Set<UUID> moderators;
    private Set<UUID> banned;
    
    private ChannelSettings settings;
    
    public enum ChannelType {
        PROXIMITY,      // 3D spatial audio
        GLOBAL,         // Server-wide
        PARTY,          // Party members only
        TEAM,           // Team members only
        PRIVATE,        // Invite-only
        ADMIN           // Staff only
    }
    
    public record ChannelSettings(
        boolean persistent,
        boolean requiresPassword,
        String password,
        int maxMembers,
        boolean allowSpectators,
        boolean pushToTalk
    ) {}
}
```

## Proximity Audio

### Distance Calculation

```java
public class ProximityManager {
    
    public double calculateVolume(Vector3d speaker, Vector3d listener) {
        double distance = speaker.distance(listener);
        
        if (distance <= config.falloffStart()) {
            return 1.0;
        } else if (distance >= config.falloffEnd()) {
            return 0.0;
        } else {
            double range = config.falloffEnd() - config.falloffStart();
            double normalized = (distance - config.falloffStart()) / range;
            return 1.0 - easeOutQuad(normalized);
        }
    }
    
    public Vector3d calculatePan(Vector3d speaker, Vector3d listener, Vector3d listenerForward) {
        Vector3d direction = speaker.subtract(listener).normalize();
        Vector3d listenerRight = listenerForward.cross(Vector3d.UP).normalize();
        
        double pan = direction.dot(listenerRight);
        double elevation = direction.dot(Vector3d.UP);
        double front = direction.dot(listenerForward);
        
        return new Vector3d(pan, elevation, front);
    }
}
```

### Spatial Audio Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `proximityRange` | Maximum hearing distance | 64 blocks |
| `falloffStart` | Distance where volume starts decreasing | 16 blocks |
| `falloffEnd` | Distance where volume reaches zero | 64 blocks |
| `enable3DAudio` | Enable left/right panning | true |

## Audio Encoding

### Opus Codec Settings

```java
public enum VoiceQuality {
    LOW(16000, 16000, 20),       // 16 kHz, 16 kbps
    MEDIUM(24000, 32000, 20),    // 24 kHz, 32 kbps
    HIGH(48000, 64000, 20),      // 48 kHz, 64 kbps
    ULTRA(48000, 128000, 10);    // 48 kHz, 128 kbps
    
    private final int sampleRate;
    private final int bitrate;
    private final int frameMs;
}
```

### Audio Frame Format

```
┌────────────────────────────────────────────────────┐
│ Voice Packet (UDP)                                  │
├────────────────────────────────────────────────────┤
│ Header (8 bytes)                                    │
│ ├── Packet Type (1 byte): 0x01 = Audio            │
│ ├── Sequence Number (4 bytes): uint32              │
│ ├── Timestamp (2 bytes): uint16 ms offset          │
│ └── Flags (1 byte): speaking, priority, etc.       │
├────────────────────────────────────────────────────┤
│ Audio Data (variable)                               │
│ ├── Speaker UUID (16 bytes)                        │
│ ├── Channel ID (4 bytes)                           │
│ ├── Frame Length (2 bytes)                         │
│ └── Opus Frame Data (variable)                      │
└────────────────────────────────────────────────────┘
```

## Commands

### Player Commands

```
/voice                          - Show voice chat status
/voice toggle                   - Toggle voice chat
/voice mute                     - Mute your microphone
/voice unmute                   - Unmute your microphone
/voice deafen                   - Deafen yourself
/voice undeafen                 - Undeafen yourself
/voice volume <0-200>           - Set output volume percentage
/voice inputvolume <0-200>      - Set input volume percentage
/voice ptt                      - Toggle push-to-talk mode
/voice quality <low|medium|high|ultra> - Set audio quality
```

### Channel Commands

```
/voice channel list             - List available channels
/voice channel join <name>      - Join a voice channel
/voice channel leave            - Leave current channel
/voice channel create <name>    - Create a new channel
/voice channel delete <name>    - Delete your channel
/voice channel invite <player>  - Invite player to channel
/voice channel kick <player>    - Kick player from channel
/voice channel password <pass>  - Set channel password
```

### Admin Commands

```
/voice admin mute <player>      - Server mute a player
/voice admin unmute <player>    - Server unmute a player
/voice admin priority <player>  - Toggle priority speaker
/voice admin record <player>    - Start recording player
/voice admin stoprecord <player> - Stop recording
/voice admin channels           - List all active channels
/voice admin stats              - Show voice system stats
```

## Integration Examples

### Party Integration

```java
partyManager.onPartyCreated((party) -> {
    VoiceChannel channel = voiceChat.createChannel(
        "party-" + party.getId(),
        ChannelType.PARTY
    );
    party.setVoiceChannel(channel);
    
    for (UUID member : party.getMembers()) {
        voiceChat.joinChannel(member, channel);
    }
});

partyManager.onMemberJoined((party, member) -> {
    voiceChat.joinChannel(member, party.getVoiceChannel());
});

partyManager.onMemberLeft((party, member) -> {
    voiceChat.leaveChannel(member, party.getVoiceChannel());
});
```

### Moderation Integration

```java
moderationService.onPlayerMuted((player, reason, duration) -> {
    voiceChat.setMuted(player.getId(), true);
    player.sendMessage("You have been voice muted: " + reason);
});

replayFeature.onRecordingStart((player, reason) -> {
    if (voiceConfig.enableVoiceRecording()) {
        voiceChat.startRecording(player.getId(), reason);
    }
});
```

## Performance Considerations

### Bandwidth Usage

| Quality | Per Player (kbps) | 10 Players (kbps) | 50 Players (kbps) |
|---------|-------------------|-------------------|-------------------|
| LOW | 16 | 160 | 800 |
| MEDIUM | 32 | 320 | 1,600 |
| HIGH | 64 | 640 | 3,200 |
| ULTRA | 128 | 1,280 | 6,400 |

### CPU Usage

- **Encoding**: ~1% CPU per active speaker
- **Decoding**: ~0.5% CPU per active listener
- **Routing**: O(n) per frame where n = active listeners
- **3D Audio**: +~0.1% CPU per listener

### Optimization Tips

1. Use `MEDIUM` quality for most servers
2. Limit proximity range to 32-64 blocks
3. Enable noise suppression to reduce packet rate
4. Use channel-based chat for large groups
5. Disable 3D audio if not needed
