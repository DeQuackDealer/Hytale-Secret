package rubidium.features.minimap;

import java.util.UUID;

public class Waypoint {
    
    private final String id;
    private final String name;
    private double x;
    private double y;
    private double z;
    private String color;
    private String icon;
    private boolean visible;
    
    public Waypoint(String name, double x, double y, double z) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = "#FFFFFF";
        this.icon = "default";
        this.visible = true;
    }
    
    public Waypoint(String id, String name, double x, double y, double z) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = "#FFFFFF";
        this.icon = "default";
        this.visible = true;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public double getX() {
        return x;
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public double getY() {
        return y;
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    public double getZ() {
        return z;
    }
    
    public void setZ(double z) {
        this.z = z;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public double distanceTo(double x, double y, double z) {
        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    public double distanceTo2D(double x, double z) {
        double dx = this.x - x;
        double dz = this.z - z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
