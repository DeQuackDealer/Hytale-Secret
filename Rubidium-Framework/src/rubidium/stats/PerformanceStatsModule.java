package rubidium.stats;

import rubidium.api.RubidiumModule;
import rubidium.api.player.Player;
import rubidium.hud.HUDRegistry;
import rubidium.settings.PlayerSettings;
import rubidium.settings.SettingsRegistry;
import rubidium.ui.components.*;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceStatsModule implements RubidiumModule {
    
    private static PerformanceStatsModule instance;
    
    private final PerformanceMetrics metrics = new PerformanceMetrics();
    private boolean enabled = true;
    
    @Override
    public String getId() { return "rubidium-performance-stats"; }
    
    @Override
    public String getName() { return "Performance Statistics"; }
    
    @Override
    public String getVersion() { return "1.0.0"; }
    
    @Override
    public void onEnable() {
        instance = this;
        
        HUDRegistry.get().registerWidget(new StatsHUDWidget());
        
        log("Performance statistics module enabled");
    }
    
    @Override
    public void onDisable() {
        instance = null;
    }
    
    public static PerformanceStatsModule getInstance() {
        return instance;
    }
    
    public PerformanceMetrics getMetrics() {
        return metrics;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public static class PerformanceMetrics {
        private int fps = 0;
        private int dps = 0;
        private long ramUsed = 0;
        private long ramMax = 0;
        private float cpuUsage = 0;
        private int tps = 20;
        private long lastUpdate = 0;
        
        private final AtomicLong frameCount = new AtomicLong(0);
        private final AtomicLong damageCount = new AtomicLong(0);
        private long lastFpsUpdate = System.currentTimeMillis();
        private long lastDpsUpdate = System.currentTimeMillis();
        
        public void tick() {
            long now = System.currentTimeMillis();
            
            if (now - lastFpsUpdate >= 1000) {
                fps = (int) frameCount.getAndSet(0);
                lastFpsUpdate = now;
            }
            
            if (now - lastDpsUpdate >= 1000) {
                dps = (int) damageCount.getAndSet(0);
                lastDpsUpdate = now;
            }
            
            Runtime runtime = Runtime.getRuntime();
            ramUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            ramMax = runtime.maxMemory() / (1024 * 1024);
            
            lastUpdate = now;
        }
        
        public void recordFrame() {
            frameCount.incrementAndGet();
        }
        
        public void recordDamage(float amount) {
            damageCount.addAndGet((long) amount);
        }
        
        public int getFps() { return fps; }
        public int getDps() { return dps; }
        public long getRamUsed() { return ramUsed; }
        public long getRamMax() { return ramMax; }
        public float getRamPercent() { return ramMax > 0 ? (float) ramUsed / ramMax * 100 : 0; }
        public float getCpuUsage() { return cpuUsage; }
        public int getTps() { return tps; }
        
        public void setTps(int tps) { this.tps = tps; }
        public void setCpuUsage(float cpu) { this.cpuUsage = cpu; }
    }
    
    private class StatsHUDWidget extends HUDRegistry.HUDWidget {
        
        public StatsHUDWidget() {
            super("statistics", "Performance Stats", "FPS, DPS, RAM usage display", false, true, true);
        }
        
        @Override
        public boolean isVisible(UUID playerId) {
            if (!enabled) return false;
            rubidium.settings.ServerSettings serverSettings = SettingsRegistry.get().getServerSettings();
            if (!serverSettings.isStatisticsAllowed()) return false;
            
            PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
            return settings.isStatisticsEnabled();
        }
        
        @Override
        public void render(UUID playerId, HUDRegistry.RenderContext ctx, PlayerSettings.HUDPosition position) {
            int x = ctx.resolveX(position);
            int y = ctx.resolveY(position);
            
            UIContainer statsPanel = new UIContainer("stats_hud")
                .setPosition(x, y)
                .setSize(position.getWidth(), position.getHeight())
                .setBackground(0x1E1E23AA);
            
            statsPanel.addChild(new UIText("stats_title")
                .setText("Performance")
                .setFontSize(12)
                .setColor(0x8A2BE2)
                .setPosition(10, 5));
            
            int fpsColor = metrics.getFps() >= 60 ? 0x32CD32 : (metrics.getFps() >= 30 ? 0xFFD700 : 0xFF4500);
            statsPanel.addChild(new UIText("fps_value")
                .setText("FPS: " + metrics.getFps())
                .setFontSize(14)
                .setColor(fpsColor)
                .setPosition(10, 25));
            
            statsPanel.addChild(new UIText("dps_value")
                .setText("DPS: " + metrics.getDps())
                .setFontSize(14)
                .setColor(0xF0F0F5)
                .setPosition(10, 42));
            
            int ramColor = metrics.getRamPercent() < 70 ? 0x32CD32 : (metrics.getRamPercent() < 90 ? 0xFFD700 : 0xFF4500);
            statsPanel.addChild(new UIText("ram_value")
                .setText("RAM: " + metrics.getRamUsed() + "/" + metrics.getRamMax() + " MB")
                .setFontSize(12)
                .setColor(ramColor)
                .setPosition(10, 59));
            
            statsPanel.addChild(new UIText("tps_value")
                .setText("TPS: " + metrics.getTps())
                .setFontSize(12)
                .setColor(metrics.getTps() >= 18 ? 0x32CD32 : 0xFF4500)
                .setPosition(120, 25));
        }
        
        @Override
        public int getDefaultWidth() { return 200; }
        
        @Override
        public int getDefaultHeight() { return 80; }
    }
    
}
