package rubidium.map;

import java.util.*;

/**
 * Zone definition for map visualization.
 */
public class Zone {
    
    private final String id;
    private String name;
    private ZoneType type;
    private String world;
    private final List<ZoneVertex> vertices;
    private double minY, maxY;
    private String fillColor;
    private String borderColor;
    private float opacity;
    private boolean visible;
    private final Map<String, Object> properties;
    
    public Zone(String id, String name, ZoneType type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.vertices = new ArrayList<>();
        this.minY = 0;
        this.maxY = 256;
        this.fillColor = "#3388ff";
        this.borderColor = "#2266cc";
        this.opacity = 0.3f;
        this.visible = true;
        this.properties = new HashMap<>();
    }
    
    public String getId() { return id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public ZoneType getType() { return type; }
    public void setType(ZoneType type) { this.type = type; }
    
    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }
    
    public List<ZoneVertex> getVertices() { return Collections.unmodifiableList(vertices); }
    
    public void addVertex(double x, double z) {
        vertices.add(new ZoneVertex(x, z));
    }
    
    public void clearVertices() {
        vertices.clear();
    }
    
    public void setYBounds(double minY, double maxY) {
        this.minY = minY;
        this.maxY = maxY;
    }
    
    public double getMinY() { return minY; }
    public double getMaxY() { return maxY; }
    
    public String getFillColor() { return fillColor; }
    public void setFillColor(String color) { this.fillColor = color; }
    
    public String getBorderColor() { return borderColor; }
    public void setBorderColor(String color) { this.borderColor = color; }
    
    public float getOpacity() { return opacity; }
    public void setOpacity(float opacity) { this.opacity = Math.max(0f, Math.min(1f, opacity)); }
    
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }
    
    public boolean contains(double x, double y, double z) {
        if (y < minY || y > maxY) return false;
        return containsPoint(x, z);
    }
    
    public boolean containsPoint(double x, double z) {
        if (vertices.size() < 3) return false;
        
        int count = 0;
        int n = vertices.size();
        
        for (int i = 0; i < n; i++) {
            ZoneVertex v1 = vertices.get(i);
            ZoneVertex v2 = vertices.get((i + 1) % n);
            
            if ((v1.z() <= z && z < v2.z()) || (v2.z() <= z && z < v1.z())) {
                double xIntersect = v1.x() + (z - v1.z()) / (v2.z() - v1.z()) * (v2.x() - v1.x());
                if (x < xIntersect) {
                    count++;
                }
            }
        }
        
        return count % 2 == 1;
    }
    
    public double getArea() {
        if (vertices.size() < 3) return 0;
        
        double area = 0;
        int n = vertices.size();
        
        for (int i = 0; i < n; i++) {
            ZoneVertex v1 = vertices.get(i);
            ZoneVertex v2 = vertices.get((i + 1) % n);
            area += v1.x() * v2.z() - v2.x() * v1.z();
        }
        
        return Math.abs(area) / 2.0;
    }
    
    public record ZoneVertex(double x, double z) {}
    
    public enum ZoneType {
        SAFE,
        PVP,
        DUNGEON,
        TOWN,
        WILDERNESS,
        RESTRICTED,
        BOSS,
        EVENT,
        CUSTOM
    }
}
