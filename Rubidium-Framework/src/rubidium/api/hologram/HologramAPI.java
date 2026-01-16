package rubidium.api.hologram;

import rubidium.api.pathfinding.PathfindingAPI.Vec3i;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class HologramAPI {
    
    private static final Map<UUID, Hologram> holograms = new ConcurrentHashMap<>();
    private static final Map<String, UUID> namedHolograms = new ConcurrentHashMap<>();
    
    private HologramAPI() {}
    
    public static Hologram.Builder create() {
        return new Hologram.Builder();
    }
    
    public static Hologram.Builder create(String name) {
        return new Hologram.Builder().name(name);
    }
    
    public static Hologram spawn(Hologram hologram) {
        holograms.put(hologram.getId(), hologram);
        if (hologram.getName() != null) {
            namedHolograms.put(hologram.getName(), hologram.getId());
        }
        return hologram;
    }
    
    public static Hologram spawn(Hologram.Builder builder) {
        return spawn(builder.build());
    }
    
    public static Hologram text(String name, Vec3i location, String... lines) {
        return spawn(create(name)
            .location(location)
            .lines(lines)
            .build());
    }
    
    public static Hologram text(Vec3i location, String... lines) {
        return spawn(create()
            .location(location)
            .lines(lines)
            .build());
    }
    
    public static Hologram item(String name, Vec3i location, String itemId) {
        return spawn(create(name)
            .location(location)
            .displayItem(itemId)
            .build());
    }
    
    public static Optional<Hologram> get(UUID id) {
        return Optional.ofNullable(holograms.get(id));
    }
    
    public static Optional<Hologram> get(String name) {
        UUID id = namedHolograms.get(name);
        return id != null ? get(id) : Optional.empty();
    }
    
    public static void remove(UUID id) {
        Hologram hologram = holograms.remove(id);
        if (hologram != null && hologram.getName() != null) {
            namedHolograms.remove(hologram.getName());
        }
    }
    
    public static void remove(String name) {
        UUID id = namedHolograms.remove(name);
        if (id != null) {
            holograms.remove(id);
        }
    }
    
    public static void remove(Hologram hologram) {
        remove(hologram.getId());
    }
    
    public static Collection<Hologram> all() {
        return holograms.values();
    }
    
    public static Collection<Hologram> nearby(Vec3i center, double radius) {
        return holograms.values().stream()
            .filter(h -> h.getLocation().distanceTo(center) <= radius)
            .toList();
    }
    
    public static void clearAll() {
        holograms.clear();
        namedHolograms.clear();
    }
    
    public static class Hologram {
        private final UUID id;
        private final String name;
        private Vec3i location;
        private final List<HologramLine> lines = new ArrayList<>();
        private String displayItem;
        private boolean visible = true;
        private double lineSpacing = 0.25;
        private Set<UUID> visibleTo = null;
        private boolean billboarding = true;
        
        private Hologram(Builder builder) {
            this.id = UUID.randomUUID();
            this.name = builder.name;
            this.location = builder.location;
            this.lines.addAll(builder.lines);
            this.displayItem = builder.displayItem;
            this.lineSpacing = builder.lineSpacing;
            this.billboarding = builder.billboarding;
            if (builder.visibleTo != null) {
                this.visibleTo = new HashSet<>(builder.visibleTo);
            }
        }
        
        public UUID getId() { return id; }
        public String getName() { return name; }
        public Vec3i getLocation() { return location; }
        public List<HologramLine> getLines() { return Collections.unmodifiableList(lines); }
        public String getDisplayItem() { return displayItem; }
        public boolean isVisible() { return visible; }
        public double getLineSpacing() { return lineSpacing; }
        public boolean isBillboarding() { return billboarding; }
        
        public void setLocation(Vec3i location) { this.location = location; }
        public void setVisible(boolean visible) { this.visible = visible; }
        public void setDisplayItem(String itemId) { this.displayItem = itemId; }
        public void setLineSpacing(double spacing) { this.lineSpacing = spacing; }
        public void setBillboarding(boolean enabled) { this.billboarding = enabled; }
        
        public void setLine(int index, String text) {
            if (index >= 0 && index < lines.size()) {
                lines.set(index, new HologramLine(text, lines.get(index).clickAction()));
            }
        }
        
        public void addLine(String text) {
            lines.add(new HologramLine(text, null));
        }
        
        public void addLine(String text, String clickAction) {
            lines.add(new HologramLine(text, clickAction));
        }
        
        public void insertLine(int index, String text) {
            if (index >= 0 && index <= lines.size()) {
                lines.add(index, new HologramLine(text, null));
            }
        }
        
        public void removeLine(int index) {
            if (index >= 0 && index < lines.size()) {
                lines.remove(index);
            }
        }
        
        public void clearLines() {
            lines.clear();
        }
        
        public boolean isVisibleTo(UUID playerId) {
            return visibleTo == null || visibleTo.contains(playerId);
        }
        
        public void showTo(UUID playerId) {
            if (visibleTo == null) {
                visibleTo = new HashSet<>();
            }
            visibleTo.add(playerId);
        }
        
        public void hideFrom(UUID playerId) {
            if (visibleTo != null) {
                visibleTo.remove(playerId);
            }
        }
        
        public void showToAll() {
            visibleTo = null;
        }
        
        public void teleport(Vec3i newLocation) {
            this.location = newLocation;
        }
        
        public static class Builder {
            private String name;
            private Vec3i location = Vec3i.ZERO;
            private List<HologramLine> lines = new ArrayList<>();
            private String displayItem;
            private double lineSpacing = 0.25;
            private boolean billboarding = true;
            private Set<UUID> visibleTo;
            
            public Builder name(String name) { this.name = name; return this; }
            public Builder location(Vec3i loc) { this.location = loc; return this; }
            public Builder location(int x, int y, int z) { this.location = Vec3i.of(x, y, z); return this; }
            public Builder line(String text) { this.lines.add(new HologramLine(text, null)); return this; }
            public Builder line(String text, String clickAction) { this.lines.add(new HologramLine(text, clickAction)); return this; }
            public Builder lines(String... texts) { 
                for (String t : texts) lines.add(new HologramLine(t, null)); 
                return this; 
            }
            public Builder displayItem(String itemId) { this.displayItem = itemId; return this; }
            public Builder lineSpacing(double spacing) { this.lineSpacing = spacing; return this; }
            public Builder billboarding(boolean enabled) { this.billboarding = enabled; return this; }
            public Builder visibleTo(UUID... players) { 
                this.visibleTo = new HashSet<>(Arrays.asList(players)); 
                return this; 
            }
            
            public Hologram build() {
                return new Hologram(this);
            }
        }
    }
    
    public record HologramLine(String text, String clickAction) {
        public HologramLine(String text) {
            this(text, null);
        }
        
        public boolean hasClickAction() {
            return clickAction != null && !clickAction.isEmpty();
        }
    }
}
