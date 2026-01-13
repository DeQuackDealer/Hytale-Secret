package jurassictale.compound.power;

import java.util.UUID;

public class PowerDevice {
    
    private final UUID id;
    private final PowerDeviceType type;
    
    private final double x, y, z;
    
    private boolean powered;
    private boolean enabled;
    
    public PowerDevice(UUID id, PowerDeviceType type, double x, double y, double z) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.powered = false;
        this.enabled = true;
    }
    
    public UUID getId() { return id; }
    public PowerDeviceType getType() { return type; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    
    public long getPowerDraw() {
        return enabled ? type.getPowerDraw() : 0;
    }
    
    public boolean isPowered() { return powered && enabled; }
    public void setPowered(boolean powered) { this.powered = powered; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isOperational() {
        return powered && enabled;
    }
    
    public enum PowerDeviceType {
        ELECTRIC_FENCE("Electric Fence", 10, DeviceCategory.PERIMETER),
        REINFORCED_FENCE("Reinforced Electric Fence", 25, DeviceCategory.PERIMETER),
        CONTAINMENT_FENCE("Containment-Grade Fence", 50, DeviceCategory.PERIMETER),
        
        STATIC_CAMERA("Static Camera", 5, DeviceCategory.SURVEILLANCE),
        ROTATING_CAMERA("Rotating Camera", 10, DeviceCategory.SURVEILLANCE),
        NIGHT_VISION_CAMERA("Night Vision Camera", 15, DeviceCategory.SURVEILLANCE),
        THERMAL_CAMERA("Thermal Camera", 25, DeviceCategory.SURVEILLANCE),
        
        MOTION_SENSOR("Motion Sensor", 5, DeviceCategory.DETECTION),
        SOUND_SENSOR("Sound Sensor", 8, DeviceCategory.DETECTION),
        FENCE_BREACH_SENSOR("Fence Breach Sensor", 10, DeviceCategory.DETECTION),
        THERMAL_SENSOR("Thermal Sensor", 20, DeviceCategory.DETECTION),
        
        ALARM_SIREN("Alarm Siren", 15, DeviceCategory.ALERT),
        WARNING_BEACON("Warning Beacon", 10, DeviceCategory.ALERT),
        
        POWERED_DOOR_T1("Powered Door Tier I", 10, DeviceCategory.ACCESS),
        POWERED_DOOR_T2("Powered Door Tier II", 20, DeviceCategory.ACCESS),
        POWERED_DOOR_T3("Powered Door Tier III", 35, DeviceCategory.ACCESS),
        POWERED_DOOR_T4("Powered Door Tier IV", 50, DeviceCategory.ACCESS),
        KEYPAD_LOCK("Keypad Lock", 5, DeviceCategory.ACCESS),
        KEYCARD_READER("Keycard Reader", 8, DeviceCategory.ACCESS),
        BIOMETRIC_SCANNER("Biometric Scanner", 15, DeviceCategory.ACCESS),
        
        SECURITY_LIGHT("Security Light", 5, DeviceCategory.LIGHTING),
        FLOOD_LIGHT("Flood Light", 15, DeviceCategory.LIGHTING),
        SPOTLIGHT("Spotlight", 20, DeviceCategory.LIGHTING),
        
        BALLISTIC_TURRET("Ballistic Turret", 50, DeviceCategory.DEFENSE),
        TRANQ_TURRET("Tranq Turret", 40, DeviceCategory.DEFENSE),
        STUN_TURRET("Stun Turret", 35, DeviceCategory.DEFENSE),
        SONIC_REPELLER("Sonic Repeller Tower", 60, DeviceCategory.DEFENSE),
        
        REPAIR_STATION("Repair Station", 25, DeviceCategory.UTILITY),
        AMMO_LOCKER("Ammo Locker", 5, DeviceCategory.UTILITY),
        STORAGE_LOCKER("Storage Locker", 5, DeviceCategory.UTILITY);
        
        private final String displayName;
        private final long powerDraw;
        private final DeviceCategory category;
        
        PowerDeviceType(String displayName, long powerDraw, DeviceCategory category) {
            this.displayName = displayName;
            this.powerDraw = powerDraw;
            this.category = category;
        }
        
        public String getDisplayName() { return displayName; }
        public long getPowerDraw() { return powerDraw; }
        public DeviceCategory getCategory() { return category; }
        
        public enum DeviceCategory {
            PERIMETER, SURVEILLANCE, DETECTION, ALERT, ACCESS, LIGHTING, DEFENSE, UTILITY
        }
    }
}
