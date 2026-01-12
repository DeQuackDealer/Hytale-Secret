package com.yellowtale.rubidium.qol.features;

import com.yellowtale.rubidium.core.logging.RubidiumLogger;
import com.yellowtale.rubidium.qol.QoLFeature;

import java.util.*;
import java.util.function.Supplier;

public class MotdFeature extends QoLFeature {
    
    public record MotdConfig(
        List<String> motdLines,
        String serverName,
        boolean showPlayerCount,
        boolean showOnlineTime,
        boolean randomizeMotd,
        List<String> tabHeader,
        List<String> tabFooter,
        int refreshIntervalSeconds
    ) {
        public static MotdConfig defaults() {
            return new MotdConfig(
                List.of(
                    "&6&l{server_name}",
                    "&7A Hytale server powered by &bRubidium"
                ),
                "Hytale Server",
                true,
                true,
                false,
                List.of(
                    "&b&l{server_name}",
                    "&7Online: &a{online}&7/&a{max}"
                ),
                List.of(
                    "&7TPS: {tps} &8| &7Uptime: {uptime}",
                    "&ePowered by Rubidium"
                ),
                30
            );
        }
    }
    
    private MotdConfig config;
    private final Random random = new Random();
    private long serverStartTime;
    
    private Supplier<Integer> onlinePlayersSupplier;
    private Supplier<Integer> maxPlayersSupplier;
    private Supplier<Double> tpsSupplier;
    
    public MotdFeature(RubidiumLogger logger) {
        super("motd", "MOTD & Tab List", 
            "Customizable server MOTD and tab list header/footer",
            logger);
        this.config = MotdConfig.defaults();
        this.serverStartTime = System.currentTimeMillis();
    }
    
    public void setConfig(MotdConfig config) {
        this.config = config;
    }
    
    public MotdConfig getConfig() {
        return config;
    }
    
    public void setOnlinePlayersSupplier(Supplier<Integer> supplier) {
        this.onlinePlayersSupplier = supplier;
    }
    
    public void setMaxPlayersSupplier(Supplier<Integer> supplier) {
        this.maxPlayersSupplier = supplier;
    }
    
    public void setTpsSupplier(Supplier<Double> supplier) {
        this.tpsSupplier = supplier;
    }
    
    @Override
    protected void onEnable() {
        serverStartTime = System.currentTimeMillis();
        logger.debug("MOTD feature enabled");
    }
    
    @Override
    protected void onDisable() {
    }
    
    public List<String> getMotd() {
        if (!enabled) {
            return List.of(config.serverName());
        }
        
        List<String> lines;
        if (config.randomizeMotd() && config.motdLines().size() > 2) {
            List<String> shuffled = new ArrayList<>(config.motdLines());
            Collections.shuffle(shuffled, random);
            lines = shuffled.subList(0, Math.min(2, shuffled.size()));
        } else {
            lines = config.motdLines();
        }
        
        return lines.stream()
            .map(this::replacePlaceholders)
            .toList();
    }
    
    public String getMotdString() {
        return String.join("\n", getMotd());
    }
    
    public List<String> getTabHeader() {
        if (!enabled) return List.of();
        
        return config.tabHeader().stream()
            .map(this::replacePlaceholders)
            .toList();
    }
    
    public String getTabHeaderString() {
        return String.join("\n", getTabHeader());
    }
    
    public List<String> getTabFooter() {
        if (!enabled) return List.of();
        
        return config.tabFooter().stream()
            .map(this::replacePlaceholders)
            .toList();
    }
    
    public String getTabFooterString() {
        return String.join("\n", getTabFooter());
    }
    
    private String replacePlaceholders(String text) {
        String result = text
            .replace("{server_name}", config.serverName());
        
        if (config.showPlayerCount()) {
            int online = onlinePlayersSupplier != null ? onlinePlayersSupplier.get() : 0;
            int max = maxPlayersSupplier != null ? maxPlayersSupplier.get() : 100;
            result = result
                .replace("{online}", String.valueOf(online))
                .replace("{max}", String.valueOf(max));
        }
        
        if (config.showOnlineTime()) {
            result = result.replace("{uptime}", formatUptime());
        }
        
        if (tpsSupplier != null) {
            double tps = tpsSupplier.get();
            String tpsColor = tps >= 19 ? "&a" : (tps >= 15 ? "&e" : "&c");
            result = result.replace("{tps}", tpsColor + String.format("%.1f", tps));
        } else {
            result = result.replace("{tps}", "&a20.0");
        }
        
        return result;
    }
    
    private String formatUptime() {
        long uptimeMs = System.currentTimeMillis() - serverStartTime;
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh", days, hours % 24);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else {
            return String.format("%dm", minutes);
        }
    }
    
    public long getUptimeMillis() {
        return System.currentTimeMillis() - serverStartTime;
    }
}
