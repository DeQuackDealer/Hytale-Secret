package rubidium.voice;

import java.util.*;
import java.util.function.BiConsumer;

public class VoiceAPI {
    
    private static VoiceChatManager manager;
    
    public static void initialize(VoiceChatManager voiceChatManager) {
        manager = voiceChatManager;
    }
    
    public static boolean isAvailable() {
        return manager != null && manager.isEnabled();
    }
    
    public static void setPlayerMuted(UUID playerId, boolean muted) {
        if (manager != null) {
            manager.setMuted(playerId, muted);
        }
    }
    
    public static void setPlayerDeafened(UUID playerId, boolean deafened) {
        if (manager != null) {
            manager.setDeafened(playerId, deafened);
        }
    }
    
    public static void setWhisperMode(UUID playerId, boolean whisper) {
        if (manager != null) {
            manager.setWhisperMode(playerId, whisper);
        }
    }
    
    public static void setActivationMode(UUID playerId, VoiceConfig.ActivationMode mode) {
        if (manager != null) {
            manager.setActivationMode(playerId, mode);
        }
    }
    
    public static void setPushToTalk(UUID playerId, boolean active) {
        if (manager != null) {
            manager.setPushToTalkActive(playerId, active);
        }
    }
    
    public static void setPlayerVolume(UUID playerId, UUID targetId, float volume) {
        if (manager != null) {
            manager.setPlayerVolume(playerId, targetId, volume);
        }
    }
    
    public static VoiceGroup createGroup(UUID ownerId, String name) {
        if (manager != null) {
            return manager.createGroup(ownerId, name);
        }
        return null;
    }
    
    public static boolean joinGroup(UUID playerId, String groupId, String password) {
        if (manager != null) {
            return manager.joinGroup(playerId, groupId, password);
        }
        return false;
    }
    
    public static void leaveGroup(UUID playerId, String groupId) {
        if (manager != null) {
            manager.leaveGroup(playerId, groupId);
        }
    }
    
    public static Optional<VoiceState> getVoiceState(UUID playerId) {
        if (manager != null) {
            return manager.getVoiceState(playerId);
        }
        return Optional.empty();
    }
    
    public static List<VoiceGroup> getPlayerGroups(UUID playerId) {
        if (manager != null) {
            return manager.getPlayerGroups(playerId);
        }
        return Collections.emptyList();
    }
    
    public static List<VoiceGroup> getPublicGroups() {
        if (manager != null) {
            return manager.getPublicGroups();
        }
        return Collections.emptyList();
    }
    
    public static void onStateChange(BiConsumer<UUID, VoiceState> listener) {
        if (manager != null) {
            manager.onStateChange(listener);
        }
    }
    
    public static void onGroupJoin(BiConsumer<UUID, VoiceGroup> listener) {
        if (manager != null) {
            manager.onGroupJoin(listener);
        }
    }
    
    public static void onGroupLeave(BiConsumer<UUID, VoiceGroup> listener) {
        if (manager != null) {
            manager.onGroupLeave(listener);
        }
    }
    
    public static VoiceMetrics getMetrics() {
        if (manager != null) {
            return manager.getMetrics();
        }
        return null;
    }
}
