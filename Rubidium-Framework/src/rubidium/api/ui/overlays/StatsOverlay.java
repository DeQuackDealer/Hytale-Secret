package rubidium.api.ui.overlays;

import rubidium.api.ui.RubidiumOverlayPage;
import rubidium.api.ui.RubidiumUI;
import com.hypixel.hytale.server.api.player.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

public class StatsOverlay extends RubidiumOverlayPage<Void> {
    
    private static final String PAGE_ID = "rubidium_stats";
    private static final String UI_PATH = "Common/UI/Custom/Pages/RubidiumStats.ui";
    
    private int fps = 0;
    private int ping = 0;
    private long memoryUsed = 0;
    private long memoryMax = 0;
    private int tps = 20;
    private int entityCount = 0;
    private int chunkCount = 0;
    
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
        events.onClick("#CloseStatsBtn", this::close);
        events.onClick("#ResetStatsBtn", this::resetStats);
    }
    
    @Override
    protected void buildUI(UICommandBuilder builder) {
        builder.set("#FPSValue.text", String.valueOf(fps));
        builder.set("#PingValue.text", ping + "ms");
        builder.set("#MemoryValue.text", formatMemory(memoryUsed) + " / " + formatMemory(memoryMax));
        builder.set("#TPSValue.text", String.format("%.1f", (float) tps));
        builder.set("#EntityValue.text", String.valueOf(entityCount));
        builder.set("#ChunkValue.text", String.valueOf(chunkCount));
        
        updateFPSColor(builder);
        updatePingColor(builder);
        updateTPSColor(builder);
    }
    
    public void updateStats(int fps, int ping, long memoryUsed, long memoryMax) {
        this.fps = fps;
        this.ping = ping;
        this.memoryUsed = memoryUsed;
        this.memoryMax = memoryMax;
        
        state.set("#FPSValue.text", String.valueOf(fps));
        state.set("#PingValue.text", ping + "ms");
        state.set("#MemoryValue.text", formatMemory(memoryUsed) + " / " + formatMemory(memoryMax));
        
        rebuild();
    }
    
    public void updateServerStats(int tps, int entityCount, int chunkCount) {
        this.tps = tps;
        this.entityCount = entityCount;
        this.chunkCount = chunkCount;
        
        state.set("#TPSValue.text", String.format("%.1f", (float) tps));
        state.set("#EntityValue.text", String.valueOf(entityCount));
        state.set("#ChunkValue.text", String.valueOf(chunkCount));
        
        rebuild();
    }
    
    private void updateFPSColor(UICommandBuilder builder) {
        String color;
        if (fps >= 60) {
            color = "#00FF00";
        } else if (fps >= 30) {
            color = "#FFFF00";
        } else {
            color = "#FF0000";
        }
        builder.set("#FPSValue.color", color);
    }
    
    private void updatePingColor(UICommandBuilder builder) {
        String color;
        if (ping <= 50) {
            color = "#00FF00";
        } else if (ping <= 150) {
            color = "#FFFF00";
        } else {
            color = "#FF0000";
        }
        builder.set("#PingValue.color", color);
    }
    
    private void updateTPSColor(UICommandBuilder builder) {
        String color;
        if (tps >= 18) {
            color = "#00FF00";
        } else if (tps >= 15) {
            color = "#FFFF00";
        } else {
            color = "#FF0000";
        }
        builder.set("#TPSValue.color", color);
    }
    
    private String formatMemory(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private void resetStats() {
        fps = 0;
        ping = 0;
        memoryUsed = 0;
        tps = 20;
        entityCount = 0;
        chunkCount = 0;
        rebuild();
    }
    
    public int getFPS() {
        return fps;
    }
    
    public int getPing() {
        return ping;
    }
    
    public int getTPS() {
        return tps;
    }
    
    public static void open(Player player) {
        StatsOverlay overlay = new StatsOverlay();
        RubidiumUI.openOverlay(player, overlay);
    }
}
