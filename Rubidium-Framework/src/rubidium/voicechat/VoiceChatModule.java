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
            int width = position.getWidth();
            int height = position.getHeight();
            
            UIContainer vcPanel = new UIContainer("voicechat_hud")
                .setPosition(x, y)
                .setSize(width, height)
                .setBackground(0x1E1E23DD);
            
            UIContainer header = new UIContainer("vc_header")
                .setPosition(0, 0)
                .setSize(width, 24)
                .setBackground(0x14141ACC);
            
            header.addChild(new UIText("vc_icon")
                .setText("\u266A")
                .setFontSize(14)
                .setColor(0x8A2BE2)
                .setPosition(8, 4));
            
            header.addChild(new UIText("vc_title")
                .setText("Voice Chat")
                .setFontSize(11)
                .setColor(0xE0E0E5)
                .setPosition(26, 5));
            
            vcPanel.addChild(header);
            
            PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
            String pttKey = settings.getPushToTalkKey();
            VoiceState myState = getVoiceState(playerId);
            
            UIContainer statusBar = new UIContainer("vc_status_bar")
                .setPosition(8, 28)
                .setSize(width - 16, 22)
                .setBackground(myState.isSpeaking() ? 0x32CD3240 : 0x40404A);
            
            String micIcon = myState.isMuted() ? "\u2717" : (myState.isSpeaking() ? "\u25CF" : "\u25CB");
            int micColor = myState.isMuted() ? 0xFF4444 : (myState.isSpeaking() ? 0x32CD32 : 0x808090);
            
            statusBar.addChild(new UIText("mic_icon")
                .setText(micIcon)
                .setFontSize(12)
                .setColor(micColor)
                .setPosition(6, 4));
            
            String statusText = myState.isMuted() ? "Muted" : 
                               (myState.isSpeaking() ? "Speaking..." : "[" + pttKey + "] Push to Talk");
            
            statusBar.addChild(new UIText("vc_status")
                .setText(statusText)
                .setFontSize(10)
                .setColor(myState.isSpeaking() ? 0x32CD32 : 0xA0A0AA)
                .setPosition(22, 5));
            
            if (myState.isDeafened()) {
                statusBar.addChild(new UIText("deafen_icon")
                    .setText("\u2298")
                    .setFontSize(11)
                    .setColor(0xFF6B6B)
                    .setPosition(width - 30, 4));
            }
            
            vcPanel.addChild(statusBar);
            
            vcPanel.addChild(new UIText("nearby_label")
                .setText("Nearby Players")
                .setFontSize(9)
                .setColor(0x707080)
                .setPosition(10, 54));
            
            int speakerY = 66;
            int speakerCount = 0;
            for (Map.Entry<UUID, VoiceState> entry : voiceStates.entrySet()) {
                if (!entry.getKey().equals(playerId)) {
                    Player speaker = Server.getPlayer(entry.getKey()).orElse(null);
                    String name = speaker != null ? speaker.getName() : "Unknown";
                    boolean isSpeaking = entry.getValue().isSpeaking();
                    boolean isMutedByMe = isMuted(playerId, entry.getKey());
                    
                    UIContainer speakerRow = new UIContainer("speaker_row_" + speakerCount)
                        .setPosition(8, speakerY)
                        .setSize(width - 16, 16)
                        .setBackground(isSpeaking ? 0x32CD3220 : 0x00000000);
                    
                    String speakerIcon = isMutedByMe ? "\u2717" : (isSpeaking ? "\u25CF" : "\u25CB");
                    int iconColor = isMutedByMe ? 0xFF4444 : (isSpeaking ? 0x32CD32 : 0x505060);
                    
                    speakerRow.addChild(new UIText("speaker_icon_" + speakerCount)
                        .setText(speakerIcon)
                        .setFontSize(10)
                        .setColor(iconColor)
                        .setPosition(4, 2));
                    
                    speakerRow.addChild(new UIText("speaker_name_" + speakerCount)
                        .setText(name)
                        .setFontSize(10)
                        .setColor(isMutedByMe ? 0x808090 : 0xD0D0D5)
                        .setPosition(18, 2));
                    
                    vcPanel.addChild(speakerRow);
                    
                    speakerY += 18;
                    speakerCount++;
                    if (speakerY > height - 30) break;
                }
            }
            
            if (speakerCount == 0) {
                vcPanel.addChild(new UIText("no_players")
                    .setText("No players nearby")
                    .setFontSize(9)
                    .setColor(0x505060)
                    .setPosition(10, speakerY));
            }
            
            UIContainer footer = new UIContainer("vc_footer")
                .setPosition(0, height - 20)
                .setSize(width, 20)
                .setBackground(0x14141ACC);
            
            int volPercent = (int)(settings.getVoiceChatVolume() * 100);
            footer.addChild(new UIText("vol_label")
                .setText("Vol: " + volPercent + "%")
                .setFontSize(9)
                .setColor(0x808090)
                .setPosition(8, 4));
            
            footer.addChild(new UIText("radius_label")
                .setText("Range: " + (int)proximityRadius + "m")
                .setFontSize(9)
                .setColor(0x808090)
                .setPosition(width - 70, 4));
            
            vcPanel.addChild(footer);
        }
        
        @Override
        public int getDefaultWidth() { return 180; }
        
        @Override
        public int getDefaultHeight() { return 140; }
    }
}
