package rubidium.optimization;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntityCulling {
    
    private final Map<UUID, CullingState> entityStates = new ConcurrentHashMap<>();
    
    private double cullingDistance = 128.0;
    private double updateDistance = 32.0;
    private int maxVisibleEntities = 200;
    private boolean frustumCullingEnabled = true;
    
    public EntityCulling() {}
    
    public void setCullingDistance(double distance) {
        this.cullingDistance = distance;
    }
    
    public void setUpdateDistance(double distance) {
        this.updateDistance = distance;
    }
    
    public void setMaxVisibleEntities(int max) {
        this.maxVisibleEntities = max;
    }
    
    public void setFrustumCullingEnabled(boolean enabled) {
        this.frustumCullingEnabled = enabled;
    }
    
    public boolean shouldRenderEntity(UUID entityId, double ex, double ey, double ez,
                                      double px, double py, double pz,
                                      double lookX, double lookY, double lookZ) {
        double dx = ex - px;
        double dy = ey - py;
        double dz = ez - pz;
        double distSq = dx * dx + dy * dy + dz * dz;
        
        if (distSq > cullingDistance * cullingDistance) {
            return false;
        }
        
        if (frustumCullingEnabled) {
            double dot = dx * lookX + dy * lookY + dz * lookZ;
            if (dot < 0 && distSq > 100) {
                return false;
            }
        }
        
        return true;
    }
    
    public boolean shouldUpdateEntity(UUID entityId, double ex, double ey, double ez,
                                      double px, double py, double pz) {
        double dx = ex - px;
        double dy = ey - py;
        double dz = ez - pz;
        double distSq = dx * dx + dy * dy + dz * dz;
        
        if (distSq > cullingDistance * cullingDistance) {
            return false;
        }
        
        CullingState state = entityStates.computeIfAbsent(entityId, k -> new CullingState());
        
        int updateInterval;
        if (distSq < updateDistance * updateDistance) {
            updateInterval = 1;
        } else if (distSq < (cullingDistance / 2) * (cullingDistance / 2)) {
            updateInterval = 2;
        } else {
            updateInterval = 4;
        }
        
        state.tickCounter++;
        if (state.tickCounter >= updateInterval) {
            state.tickCounter = 0;
            return true;
        }
        
        return false;
    }
    
    public List<UUID> prioritizeEntities(List<EntityInfo> entities, double px, double py, double pz) {
        entities.sort((a, b) -> {
            double distA = distanceSquared(a.x(), a.y(), a.z(), px, py, pz);
            double distB = distanceSquared(b.x(), b.y(), b.z(), px, py, pz);
            
            if (a.priority() != b.priority()) {
                return Integer.compare(b.priority(), a.priority());
            }
            
            return Double.compare(distA, distB);
        });
        
        List<UUID> result = new ArrayList<>();
        int count = 0;
        for (EntityInfo entity : entities) {
            if (count >= maxVisibleEntities) break;
            result.add(entity.id());
            count++;
        }
        
        return result;
    }
    
    private double distanceSquared(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }
    
    public void clearEntity(UUID entityId) {
        entityStates.remove(entityId);
    }
    
    public void clear() {
        entityStates.clear();
    }
    
    public record EntityInfo(UUID id, double x, double y, double z, int priority) {}
    
    private static class CullingState {
        int tickCounter = 0;
    }
}
