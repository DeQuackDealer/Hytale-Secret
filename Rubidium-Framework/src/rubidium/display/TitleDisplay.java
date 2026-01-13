package rubidium.display;

import rubidium.api.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TitleDisplay {
    
    private final Map<UUID, TitleState> activeTitles;
    
    public TitleDisplay() {
        this.activeTitles = new ConcurrentHashMap<>();
    }
    
    public void showTitle(Player player, String title) {
        showTitle(player, title, null, 500, 3000, 500);
    }
    
    public void showTitle(Player player, String title, String subtitle) {
        showTitle(player, title, subtitle, 500, 3000, 500);
    }
    
    public void showTitle(Player player, String title, String subtitle, 
                          int fadeInMs, int stayMs, int fadeOutMs) {
        TitleState state = new TitleState(title, subtitle, fadeInMs, stayMs, fadeOutMs, System.currentTimeMillis());
        activeTitles.put(player.getUUID(), state);
        player.sendPacket(new TitlePacket(title, subtitle, fadeInMs, stayMs, fadeOutMs));
    }
    
    public void showSubtitle(Player player, String subtitle) {
        TitleState existing = activeTitles.get(player.getUUID());
        if (existing != null) {
            showTitle(player, existing.title(), subtitle, existing.fadeIn(), existing.stay(), existing.fadeOut());
        } else {
            showTitle(player, "", subtitle);
        }
    }
    
    public void clear(Player player) {
        activeTitles.remove(player.getUUID());
        player.sendPacket(new TitleClearPacket());
    }
    
    public void reset(Player player) {
        activeTitles.remove(player.getUUID());
        player.sendPacket(new TitleResetPacket());
    }
    
    public void tick() {
        long now = System.currentTimeMillis();
        activeTitles.entrySet().removeIf(entry -> {
            TitleState state = entry.getValue();
            long totalDuration = state.fadeIn() + state.stay() + state.fadeOut();
            return now - state.startTime() > totalDuration;
        });
    }
    
    public boolean hasActiveTitle(Player player) {
        return activeTitles.containsKey(player.getUUID());
    }
    
    public record TitleState(String title, String subtitle, int fadeIn, int stay, int fadeOut, long startTime) {}
    public record TitlePacket(String title, String subtitle, int fadeIn, int stay, int fadeOut) {}
    public record TitleClearPacket() {}
    public record TitleResetPacket() {}
}
