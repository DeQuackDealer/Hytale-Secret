package jurassictale.ai.perception;

import jurassictale.ai.steering.Vec2;

import java.util.*;

public class PerceptionData {
    
    private final List<PerceivedEntity> threats;
    private final List<PerceivedEntity> allies;
    private final List<PerceivedEntity> prey;
    private final List<PerceivedEntity> neutrals;
    private PerceivedEntity currentTarget;
    
    public PerceptionData() {
        this.threats = new ArrayList<>();
        this.allies = new ArrayList<>();
        this.prey = new ArrayList<>();
        this.neutrals = new ArrayList<>();
    }
    
    public void addThreat(PerceivedEntity entity) {
        threats.add(entity);
    }
    
    public void addAlly(PerceivedEntity entity) {
        allies.add(entity);
    }
    
    public void addPrey(PerceivedEntity entity) {
        prey.add(entity);
    }
    
    public void addNeutral(PerceivedEntity entity) {
        neutrals.add(entity);
    }
    
    public void setTarget(PerceivedEntity target) {
        this.currentTarget = target;
    }
    
    public boolean hasThreats() {
        return !threats.isEmpty();
    }
    
    public boolean hasTarget() {
        return currentTarget != null;
    }
    
    public boolean hasAllies() {
        return !allies.isEmpty();
    }
    
    public boolean hasPrey() {
        return !prey.isEmpty();
    }
    
    public int getAllyCount() {
        return allies.size();
    }
    
    public int getThreatCount() {
        return threats.size();
    }
    
    public Optional<Vec2> getTargetPosition() {
        return currentTarget != null ? Optional.of(currentTarget.position()) : Optional.empty();
    }
    
    public Optional<Vec2> getTargetVelocity() {
        return currentTarget != null ? Optional.of(currentTarget.velocity()) : Optional.empty();
    }
    
    public Optional<Vec2> getNearestThreatPosition() {
        return threats.stream()
            .min(Comparator.comparingDouble(PerceivedEntity::distance))
            .map(PerceivedEntity::position);
    }
    
    public Optional<Vec2> getNearestThreatVelocity() {
        return threats.stream()
            .min(Comparator.comparingDouble(PerceivedEntity::distance))
            .map(PerceivedEntity::velocity);
    }
    
    public double getNearestThreatDistance() {
        return threats.stream()
            .mapToDouble(PerceivedEntity::distance)
            .min()
            .orElse(Double.MAX_VALUE);
    }
    
    public Optional<PerceivedEntity> getNearestPrey() {
        return prey.stream()
            .min(Comparator.comparingDouble(PerceivedEntity::distance));
    }
    
    public List<Vec2> getNearbyAllyPositions() {
        return allies.stream()
            .map(PerceivedEntity::position)
            .toList();
    }
    
    public List<Vec2> getNearbyAllyVelocities() {
        return allies.stream()
            .map(PerceivedEntity::velocity)
            .toList();
    }
    
    public List<PerceivedEntity> getThreats() {
        return Collections.unmodifiableList(threats);
    }
    
    public List<PerceivedEntity> getAllies() {
        return Collections.unmodifiableList(allies);
    }
    
    public List<PerceivedEntity> getPrey() {
        return Collections.unmodifiableList(prey);
    }
    
    public void clear() {
        threats.clear();
        allies.clear();
        prey.clear();
        neutrals.clear();
        currentTarget = null;
    }
    
    public record PerceivedEntity(
        UUID id,
        Vec2 position,
        Vec2 velocity,
        double distance,
        PerceptionType type,
        float threatLevel,
        long lastSeenTime
    ) {}
    
    public enum PerceptionType {
        SIGHT,
        SOUND,
        SCENT
    }
}
