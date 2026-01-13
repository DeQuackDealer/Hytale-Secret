package rubidium.particles;

import rubidium.api.player.Player;

import java.util.*;

public class ParticleBuilder {
    
    private String particleType;
    private double x, y, z;
    private double offsetX, offsetY, offsetZ;
    private float speed;
    private int count;
    private boolean longDistance;
    private Object extraData;
    
    private ParticleBuilder() {
        this.offsetX = 0;
        this.offsetY = 0;
        this.offsetZ = 0;
        this.speed = 0;
        this.count = 1;
        this.longDistance = false;
    }
    
    public static ParticleBuilder create(String particleType) {
        ParticleBuilder builder = new ParticleBuilder();
        builder.particleType = particleType;
        return builder;
    }
    
    public ParticleBuilder at(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }
    
    public ParticleBuilder offset(double x, double y, double z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        return this;
    }
    
    public ParticleBuilder speed(float speed) {
        this.speed = speed;
        return this;
    }
    
    public ParticleBuilder count(int count) {
        this.count = count;
        return this;
    }
    
    public ParticleBuilder longDistance(boolean longDistance) {
        this.longDistance = longDistance;
        return this;
    }
    
    public ParticleBuilder data(Object data) {
        this.extraData = data;
        return this;
    }
    
    public void sendTo(Player player) {
        player.sendPacket(build());
    }
    
    public void sendToAll(Collection<Player> players) {
        ParticlePacket packet = build();
        for (Player player : players) {
            double distance = calculateDistance(player, x, y, z);
            if (longDistance || distance <= 32.0) {
                player.sendPacket(packet);
            }
        }
    }
    
    public void sendToNearby(Collection<Player> players, double radius) {
        ParticlePacket packet = build();
        for (Player player : players) {
            double distance = calculateDistance(player, x, y, z);
            if (distance <= radius) {
                player.sendPacket(packet);
            }
        }
    }
    
    private double calculateDistance(Player player, double x, double y, double z) {
        double dx = player.getLocation().x() - x;
        double dy = player.getLocation().y() - y;
        double dz = player.getLocation().z() - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    public ParticlePacket build() {
        return new ParticlePacket(particleType, x, y, z, offsetX, offsetY, offsetZ, speed, count, longDistance, extraData);
    }
    
    public record ParticlePacket(
        String particleType,
        double x, double y, double z,
        double offsetX, double offsetY, double offsetZ,
        float speed,
        int count,
        boolean longDistance,
        Object extraData
    ) {}
}
