package rubidium.api.bossbar;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BossBarAPI {
    
    private static final Map<String, BossBar> bossBars = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> playerBossBars = new ConcurrentHashMap<>();
    
    private BossBarAPI() {}
    
    public static BossBar.Builder create(String id) {
        return new BossBar.Builder(id);
    }
    
    public static BossBar register(BossBar bossBar) {
        bossBars.put(bossBar.getId(), bossBar);
        return bossBar;
    }
    
    public static BossBar register(BossBar.Builder builder) {
        return register(builder.build());
    }
    
    public static Optional<BossBar> get(String id) {
        return Optional.ofNullable(bossBars.get(id));
    }
    
    public static void remove(String id) {
        BossBar bar = bossBars.remove(id);
        if (bar != null) {
            for (UUID viewer : bar.getViewers()) {
                Set<String> bars = playerBossBars.get(viewer);
                if (bars != null) bars.remove(id);
            }
        }
    }
    
    public static Collection<BossBar> all() {
        return bossBars.values();
    }
    
    public static void showTo(String barId, UUID playerId) {
        BossBar bar = bossBars.get(barId);
        if (bar != null) {
            bar.addViewer(playerId);
            playerBossBars.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(barId);
        }
    }
    
    public static void hideFrom(String barId, UUID playerId) {
        BossBar bar = bossBars.get(barId);
        if (bar != null) {
            bar.removeViewer(playerId);
            Set<String> bars = playerBossBars.get(playerId);
            if (bars != null) bars.remove(barId);
        }
    }
    
    public static void showToAll(String barId, Collection<UUID> players) {
        for (UUID player : players) {
            showTo(barId, player);
        }
    }
    
    public static Set<String> getPlayerBossBars(UUID playerId) {
        return playerBossBars.getOrDefault(playerId, Set.of());
    }
    
    public static BossBar simple(String id, String title, BarColor color) {
        return register(create(id).title(title).color(color).build());
    }
    
    public static BossBar progress(String id, String title, BarColor color, float progress) {
        return register(create(id).title(title).color(color).progress(progress).build());
    }
    
    public static class BossBar {
        private final String id;
        private String title;
        private float progress;
        private BarColor color;
        private BarStyle style;
        private boolean visible;
        private boolean darkenSky;
        private boolean playBossMusic;
        private boolean createFog;
        private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
        
        private BossBar(Builder builder) {
            this.id = builder.id;
            this.title = builder.title;
            this.progress = builder.progress;
            this.color = builder.color;
            this.style = builder.style;
            this.visible = builder.visible;
            this.darkenSky = builder.darkenSky;
            this.playBossMusic = builder.playBossMusic;
            this.createFog = builder.createFog;
        }
        
        public String getId() { return id; }
        public String getTitle() { return title; }
        public float getProgress() { return progress; }
        public BarColor getColor() { return color; }
        public BarStyle getStyle() { return style; }
        public boolean isVisible() { return visible; }
        public boolean isDarkenSky() { return darkenSky; }
        public boolean isPlayBossMusic() { return playBossMusic; }
        public boolean isCreateFog() { return createFog; }
        public Set<UUID> getViewers() { return Collections.unmodifiableSet(viewers); }
        
        public void setTitle(String title) { this.title = title; }
        public void setProgress(float progress) { this.progress = Math.max(0, Math.min(1, progress)); }
        public void setColor(BarColor color) { this.color = color; }
        public void setStyle(BarStyle style) { this.style = style; }
        public void setVisible(boolean visible) { this.visible = visible; }
        public void setDarkenSky(boolean darken) { this.darkenSky = darken; }
        public void setPlayBossMusic(boolean play) { this.playBossMusic = play; }
        public void setCreateFog(boolean fog) { this.createFog = fog; }
        
        public void addViewer(UUID playerId) { viewers.add(playerId); }
        public void removeViewer(UUID playerId) { viewers.remove(playerId); }
        public void clearViewers() { viewers.clear(); }
        public boolean hasViewer(UUID playerId) { return viewers.contains(playerId); }
        
        public void incrementProgress(float amount) {
            setProgress(progress + amount);
        }
        
        public void decrementProgress(float amount) {
            setProgress(progress - amount);
        }
        
        public static class Builder {
            private final String id;
            private String title = "";
            private float progress = 1.0f;
            private BarColor color = BarColor.PINK;
            private BarStyle style = BarStyle.SOLID;
            private boolean visible = true;
            private boolean darkenSky = false;
            private boolean playBossMusic = false;
            private boolean createFog = false;
            
            public Builder(String id) { this.id = id; }
            
            public Builder title(String title) { this.title = title; return this; }
            public Builder progress(float progress) { this.progress = Math.max(0, Math.min(1, progress)); return this; }
            public Builder color(BarColor color) { this.color = color; return this; }
            public Builder style(BarStyle style) { this.style = style; return this; }
            public Builder visible(boolean visible) { this.visible = visible; return this; }
            public Builder darkenSky(boolean darken) { this.darkenSky = darken; return this; }
            public Builder playBossMusic(boolean play) { this.playBossMusic = play; return this; }
            public Builder createFog(boolean fog) { this.createFog = fog; return this; }
            
            public BossBar build() {
                return new BossBar(this);
            }
        }
    }
    
    public enum BarColor {
        PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
    }
    
    public enum BarStyle {
        SOLID,
        SEGMENTED_6,
        SEGMENTED_10,
        SEGMENTED_12,
        SEGMENTED_20
    }
}
