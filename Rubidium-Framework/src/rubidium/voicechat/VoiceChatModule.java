package rubidium.voicechat;

import rubidium.api.RubidiumModule;
import rubidium.api.player.Player;
import rubidium.api.server.Server;
import rubidium.hud.HUDRegistry;
import rubidium.settings.PlayerSettings;
import rubidium.settings.SettingsRegistry;
import rubidium.ui.components.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceChatModule implements RubidiumModule {
    
    private static VoiceChatModule instance;
    
    private final Map<UUID, VoiceState> voiceStates = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> mutedPlayers = new ConcurrentHashMap<>();
    private boolean enabled = true;
    private float proximityRadius = 50.0f;
    
    @Override
    public String getId() { return "rubidium-voicechat"; }
    
    @Override
    public String getName() { return "Voice Chat"; }
    
    @Override
    public String getVersion() { return "1.0.0"; }
    
    @Override
    public void onEnable() {
        instance = this;
        
        HUDRegistry.get().registerWidget(new VoiceChatHUDWidget());
        
        log("Voice chat module enabled - proximity radius: " + proximityRadius + " blocks");
    }
    
    @Override
    public void onDisable() {
        voiceStates.clear();
        mutedPlayers.clear();
        instance = null;
    }
    
    public static VoiceChatModule getInstance() {
        return instance;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setProximityRadius(float radius) {
        this.proximityRadius = radius;
    }
    
    public float getProximityRadius() {
        return proximityRadius;
    }
    
    public void startSpeaking(UUID playerId) {
        VoiceState state = voiceStates.computeIfAbsent(playerId, VoiceState::new);
        state.setSpeaking(true);
        state.setLastSpeakTime(System.currentTimeMillis());
        log("Player " + playerId + " started speaking");
    }
    
    public void stopSpeaking(UUID playerId) {
        VoiceState state = voiceStates.get(playerId);
        if (state != null) {
            state.setSpeaking(false);
        }
    }
    
    public boolean isSpeaking(UUID playerId) {
        VoiceState state = voiceStates.get(playerId);
        return state != null && state.isSpeaking();
    }
    
    public void mutePlayer(UUID playerId, UUID targetId) {
        mutedPlayers.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(targetId);
    }
    
    public void unmutePlayer(UUID playerId, UUID targetId) {
        Set<UUID> muted = mutedPlayers.get(playerId);
        if (muted != null) {
            muted.remove(targetId);
        }
    }
    
    public boolean isMuted(UUID playerId, UUID targetId) {
        Set<UUID> muted = mutedPlayers.get(playerId);
        return muted != null && muted.contains(targetId);
    }
    
    public List<UUID> getNearbySpeakers(UUID playerId, double x, double y, double z) {
        List<UUID> speakers = new ArrayList<>();
        
        for (Map.Entry<UUID, VoiceState> entry : voiceStates.entrySet()) {
            UUID speakerId = entry.getKey();
            VoiceState state = entry.getValue();
            
            if (!speakerId.equals(playerId) && state.isSpeaking()) {
                if (!isMuted(playerId, speakerId)) {
                    speakers.add(speakerId);
                }
            }
        }
        
        return speakers;
    }
    
    public VoiceState getVoiceState(UUID playerId) {
        return voiceStates.computeIfAbsent(playerId, VoiceState::new);
    }
    
    public static class VoiceState {
        private final UUID playerId;
        private boolean speaking;
        private boolean deafened;
        private boolean muted;
        private float volume = 1.0f;
        private long lastSpeakTime;
        
        public VoiceState(UUID playerId) {
            this.playerId = playerId;
        }
        
        public UUID getPlayerId() { return playerId; }
        
        public boolean isSpeaking() { return speaking; }
        public void setSpeaking(boolean speaking) { this.speaking = speaking; }
        
        public boolean isDeafened() { return deafened; }
        public void setDeafened(boolean deafened) { this.deafened = deafened; }
        
        public boolean isMuted() { return muted; }
        public void setMuted(boolean muted) { this.muted = muted; }
        
        public float getVolume() { return volume; }
        public void setVolume(float volume) { this.volume = volume; }
        
        public long getLastSpeakTime() { return lastSpeakTime; }
        public void setLastSpeakTime(long time) { this.lastSpeakTime = time; }
    }
    
    private class VoiceChatHUDWidget extends HUDRegistry.HUDWidget {
        
        public VoiceChatHUDWidget() {
            super("voicechat", "Voice Chat", "Shows nearby voice chat activity", true, true, true);
        }
        
        @Override
        public boolean isVisible(UUID playerId) {
            if (!enabled) return false;
            rubidium.settings.ServerSettings serverSettings = SettingsRegistry.get().getServerSettings();
            if (!serverSettings.isVoiceChatAllowed()) return false;
            
            PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
            return settings.isVoiceChatEnabled();
        }
        
        @Override
        public void render(UUID playerId, HUDRegistry.RenderContext ctx, PlayerSettings.HUDPosition position) {
            int x = ctx.resolveX(position);
            int y = ctx.resolveY(position);
            
            UIContainer vcPanel = new UIContainer("voicechat_hud")
                .setPosition(x, y)
                .setSize(position.getWidth(), position.getHeight())
                .setBackground(0x1E1E23CC);
            
            vcPanel.addChild(new UIText("vc_title")
                .setText("Voice Chat")
                .setFontSize(12)
                .setColor(0x8A2BE2)
                .setPosition(10, 5));
            
            PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
            String pttKey = settings.getPushToTalkKey();
            VoiceState myState = getVoiceState(playerId);
            
            String statusText = myState.isSpeaking() ? "Speaking..." : "Press " + pttKey + " to talk";
            int statusColor = myState.isSpeaking() ? 0x32CD32 : 0x808090;
            
            vcPanel.addChild(new UIText("vc_status")
                .setText(statusText)
                .setFontSize(10)
                .setColor(statusColor)
                .setPosition(10, 22));
            
            int speakerY = 38;
            for (Map.Entry<UUID, VoiceState> entry : voiceStates.entrySet()) {
                if (!entry.getKey().equals(playerId) && entry.getValue().isSpeaking()) {
                    Player speaker = Server.getPlayer(entry.getKey()).orElse(null);
                    String name = speaker != null ? speaker.getName() : "Unknown";
                    
                    vcPanel.addChild(new UIText("speaker_" + entry.getKey())
                        .setText("* " + name)
                        .setFontSize(10)
                        .setColor(0x32CD32)
                        .setPosition(10, speakerY));
                    
                    speakerY += 14;
                    if (speakerY > position.getHeight() - 20) break;
                }
            }
            
            vcPanel.addChild(new UIText("vc_volume")
                .setText("Vol: " + (int)(settings.getVoiceChatVolume() * 100) + "%")
                .setFontSize(9)
                .setColor(0x606070)
                .setPosition(10, position.getHeight() - 15));
        }
        
        @Override
        public int getDefaultWidth() { return 180; }
        
        @Override
        public int getDefaultHeight() { return 120; }
    }
}
