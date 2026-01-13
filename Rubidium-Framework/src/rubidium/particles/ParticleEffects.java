package rubidium.particles;

import rubidium.api.player.Player;

import java.util.*;

public class ParticleEffects {
    
    public static void circle(Collection<Player> players, String particle, double centerX, double centerY, double centerZ, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double x = centerX + radius * Math.cos(angle);
            double z = centerZ + radius * Math.sin(angle);
            
            ParticleBuilder.create(particle)
                .at(x, centerY, z)
                .count(1)
                .sendToAll(players);
        }
    }
    
    public static void sphere(Collection<Player> players, String particle, double centerX, double centerY, double centerZ, double radius, int density) {
        for (int i = 0; i < density; i++) {
            double theta = Math.random() * 2 * Math.PI;
            double phi = Math.acos(2 * Math.random() - 1);
            
            double x = centerX + radius * Math.sin(phi) * Math.cos(theta);
            double y = centerY + radius * Math.cos(phi);
            double z = centerZ + radius * Math.sin(phi) * Math.sin(theta);
            
            ParticleBuilder.create(particle)
                .at(x, y, z)
                .count(1)
                .sendToAll(players);
        }
    }
    
    public static void line(Collection<Player> players, String particle, double x1, double y1, double z1, double x2, double y2, double z2, double spacing) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int points = (int) (length / spacing);
        
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            double x = x1 + dx * t;
            double y = y1 + dy * t;
            double z = z1 + dz * t;
            
            ParticleBuilder.create(particle)
                .at(x, y, z)
                .count(1)
                .sendToAll(players);
        }
    }
    
    public static void helix(Collection<Player> players, String particle, double centerX, double centerY, double centerZ, double radius, double height, int turns, int pointsPerTurn) {
        int totalPoints = turns * pointsPerTurn;
        for (int i = 0; i < totalPoints; i++) {
            double angle = (2 * Math.PI * i) / pointsPerTurn;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + (height * i / totalPoints);
            double z = centerZ + radius * Math.sin(angle);
            
            ParticleBuilder.create(particle)
                .at(x, y, z)
                .count(1)
                .sendToAll(players);
        }
    }
    
    public static void explosion(Collection<Player> players, String particle, double x, double y, double z, int count, float speed) {
        ParticleBuilder.create(particle)
            .at(x, y, z)
            .offset(0.5, 0.5, 0.5)
            .speed(speed)
            .count(count)
            .sendToAll(players);
    }
    
    public static void ring(Collection<Player> players, String particle, double centerX, double centerY, double centerZ, double radius, double yaw, int points) {
        double yawRad = Math.toRadians(yaw);
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double localX = radius * Math.cos(angle);
            double localZ = radius * Math.sin(angle);
            
            double x = centerX + localX * Math.cos(yawRad) - localZ * Math.sin(yawRad);
            double z = centerZ + localX * Math.sin(yawRad) + localZ * Math.cos(yawRad);
            
            ParticleBuilder.create(particle)
                .at(x, centerY, z)
                .count(1)
                .sendToAll(players);
        }
    }
    
    public static void trail(Player player, String particle, int count, float spread) {
        ParticleBuilder.create(particle)
            .at(player.getLocation().x(), player.getLocation().y(), player.getLocation().z())
            .offset(spread, spread, spread)
            .count(count)
            .sendTo(player);
    }
    
    public static void aura(Collection<Player> viewers, Player target, String particle, double radius, int count) {
        double x = target.getLocation().x();
        double y = target.getLocation().y() + 1.0;
        double z = target.getLocation().z();
        
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double r = Math.random() * radius;
            double px = x + r * Math.cos(angle);
            double pz = z + r * Math.sin(angle);
            double py = y + Math.random() * 0.5;
            
            ParticleBuilder.create(particle)
                .at(px, py, pz)
                .count(1)
                .sendToAll(viewers);
        }
    }
}
