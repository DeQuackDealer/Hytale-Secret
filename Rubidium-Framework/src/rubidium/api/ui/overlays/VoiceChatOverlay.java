package rubidium.api.ui.overlays;

import rubidium.api.ui.RubidiumOverlayPage;
import rubidium.api.ui.RubidiumUI;
import rubidium.features.voicechat.VoiceChatManager;
import rubidium.features.voicechat.VoiceChatState;
import com.hypixel.hytale.server.api.player.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceChatOverlay extends RubidiumOverlayPage<Void> {
    
    private static final String PAGE_ID = "rubidium_voicechat";
    private static final String UI_PATH = "Common/UI/Custom/Pages/RubidiumVoiceChat.ui";
    
    private final VoiceChatManager voiceChatManager;
    private final Set<UUID> speakingPlayers = ConcurrentHashMap.newKeySet();
    private boolean pttActive = false;
    private boolean muted = false;
    private float volume = 1.0f;
    
    public VoiceChatOverlay(VoiceChatManager voiceChatManager) {
        this.voiceChatManager = voiceChatManager;
    }
    
    @Override
    public String getPageId() {
        return PAGE_ID;
    }
    
    @Override
    public String getUIPath() {
        return UI_PATH;
    }
    
    @Override
    protected void registerEvents(UIEventBuilder events) {
        events.onClick("#PTTButton", this::togglePTT);
        events.onClick("#MuteButton", this::toggleMute);
        events.onSliderChange("#VolumeSlider", this::setVolume);
    }
    
    @Override
    protected void buildUI(UICommandBuilder builder) {
        builder.set("#PTTIndicator.visible", pttActive);
        builder.set("#MuteIcon.visible", muted);
        builder.set("#VolumeSlider.value", volume);
        builder.set("#SpeakingList.visible", !speakingPlayers.isEmpty());
    }
    
    public void togglePTT() {
        pttActive = !pttActive;
        state.set("#PTTIndicator.visible", pttActive);
        state.set("#PTTButton.active", pttActive);
        
        if (voiceChatManager != null && player != null) {
            voiceChatManager.setSpeaking(player.getUUID(), pttActive);
        }
        rebuild();
    }
    
    public void toggleMute() {
        muted = !muted;
        state.set("#MuteIcon.visible", muted);
        state.set("#MuteButton.active", muted);
        
        if (voiceChatManager != null && player != null) {
            voiceChatManager.setMuted(player.getUUID(), muted);
        }
        rebuild();
    }
    
    public void setVolume(float vol) {
        this.volume = Math.max(0f, Math.min(1f, vol));
        state.set("#VolumeSlider.value", volume);
        state.set("#VolumeLabel.text", String.format("%.0f%%", volume * 100));
        
        if (voiceChatManager != null && player != null) {
            voiceChatManager.setVolume(player.getUUID(), volume);
        }
        rebuild();
    }
    
    public void onPlayerStartSpeaking(UUID playerId, String playerName) {
        speakingPlayers.add(playerId);
        state.set("#Speaker_" + playerId.toString().substring(0, 8) + ".visible", true);
        state.set("#Speaker_" + playerId.toString().substring(0, 8) + ".text", playerName);
        state.set("#SpeakingList.visible", true);
        rebuild();
    }
    
    public void onPlayerStopSpeaking(UUID playerId) {
        speakingPlayers.remove(playerId);
        state.set("#Speaker_" + playerId.toString().substring(0, 8) + ".visible", false);
        state.set("#SpeakingList.visible", !speakingPlayers.isEmpty());
        rebuild();
    }
    
    public boolean isPTTActive() {
        return pttActive;
    }
    
    public boolean isMuted() {
        return muted;
    }
    
    public float getVolume() {
        return volume;
    }
    
    public Set<UUID> getSpeakingPlayers() {
        return Set.copyOf(speakingPlayers);
    }
    
    public static void open(Player player, VoiceChatManager voiceChatManager) {
        VoiceChatOverlay overlay = new VoiceChatOverlay(voiceChatManager);
        RubidiumUI.openOverlay(player, overlay);
    }
}
