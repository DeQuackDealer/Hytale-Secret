package jurassictale.ai.perception;

import jurassictale.ai.spatial.SpatialEntity;
import jurassictale.ai.spatial.SpatialHashGrid;
import jurassictale.ai.steering.Vec2;
import jurassictale.ai.perception.PerceptionData.PerceivedEntity;
import jurassictale.ai.perception.PerceptionData.PerceptionType;
import jurassictale.dino.DinoEntity;
import jurassictale.dino.types.DinoCategory;

import java.util.*;

public class PerceptionSystem {
    
    private final SpatialHashGrid<DinoEntity> dinoGrid;
    private final SpatialHashGrid<PlayerEntity> playerGrid;
    private final PerceptionConfig config;
    
    public PerceptionSystem(PerceptionConfig config) {
        this.config = config;
        this.dinoGrid = new SpatialHashGrid<>(config.cellSize());
        this.playerGrid = new SpatialHashGrid<>(config.cellSize());
    }
    
    public void updateDino(DinoEntity dino) {
        dinoGrid.update(dino);
    }
    
    public void removeDino(DinoEntity dino) {
        dinoGrid.remove(dino);
    }
    
    public void updatePlayer(PlayerEntity player) {
        playerGrid.update(player);
    }
    
    public void removePlayer(PlayerEntity player) {
        playerGrid.remove(player);
    }
    
    public PerceptionData perceive(DinoEntity perceiver) {
        PerceptionData data = new PerceptionData();
        long now = System.currentTimeMillis();
        
        Vec2 pos = Vec2.of(perceiver.getX(), perceiver.getZ());
        double facing = perceiver.getYaw();
        
        float sightRange = config.baseSightRange() * perceiver.getDefinition().getSpeed();
        float hearingRange = config.baseHearingRange();
        float scentRange = config.baseScentRange();
        
        perceiveDinos(perceiver, data, pos, facing, sightRange, hearingRange, scentRange, now);
        perceivePlayers(perceiver, data, pos, facing, sightRange, hearingRange, scentRange, now);
        
        selectTarget(perceiver, data);
        
        return data;
    }
    
    private void perceiveDinos(DinoEntity perceiver, PerceptionData data, Vec2 pos, 
                                double facing, float sightRange, float hearingRange, 
                                float scentRange, long now) {
        float maxRange = Math.max(sightRange, Math.max(hearingRange, scentRange));
        List<DinoEntity> nearby = dinoGrid.queryRadius(pos.x(), pos.z(), maxRange, 
            d -> !d.getId().equals(perceiver.getId()));
        
        for (DinoEntity other : nearby) {
            Vec2 otherPos = Vec2.of(other.getX(), other.getZ());
            double distance = pos.distanceTo(otherPos);
            
            PerceptionType type = determinePerceptionType(pos, facing, otherPos, distance, 
                sightRange, hearingRange, scentRange);
            if (type == null) continue;
            
            Vec2 velocity = Vec2.of(other.getVelocityX(), other.getVelocityZ());
            float threat = calculateThreatLevel(perceiver, other);
            
            PerceivedEntity perceived = new PerceivedEntity(
                other.getId(), otherPos, velocity, distance, type, threat, now
            );
            
            classifyDino(perceiver, other, perceived, data);
        }
    }
    
    private void perceivePlayers(DinoEntity perceiver, PerceptionData data, Vec2 pos,
                                  double facing, float sightRange, float hearingRange,
                                  float scentRange, long now) {
        float maxRange = Math.max(sightRange, Math.max(hearingRange, scentRange));
        List<PlayerEntity> nearby = playerGrid.queryRadius(pos.x(), pos.z(), maxRange);
        
        for (PlayerEntity player : nearby) {
            Vec2 playerPos = Vec2.of(player.getX(), player.getZ());
            double distance = pos.distanceTo(playerPos);
            
            PerceptionType type = determinePerceptionType(pos, facing, playerPos, distance,
                sightRange, hearingRange, scentRange);
            if (type == null) continue;
            
            Vec2 velocity = Vec2.of(player.velocityX(), player.velocityZ());
            float threat = calculatePlayerThreat(perceiver, player);
            
            PerceivedEntity perceived = new PerceivedEntity(
                player.getId(), playerPos, velocity, distance, type, threat, now
            );
            
            classifyPlayer(perceiver, player, perceived, data);
        }
    }
    
    private PerceptionType determinePerceptionType(Vec2 pos, double facing, Vec2 targetPos,
                                                    double distance, float sightRange,
                                                    float hearingRange, float scentRange) {
        if (distance <= sightRange && isInFieldOfView(pos, facing, targetPos, config.fovDegrees())) {
            return PerceptionType.SIGHT;
        }
        if (distance <= hearingRange) {
            return PerceptionType.SOUND;
        }
        if (distance <= scentRange) {
            return PerceptionType.SCENT;
        }
        return null;
    }
    
    private boolean isInFieldOfView(Vec2 pos, double facing, Vec2 target, float fovDegrees) {
        Vec2 toTarget = target.subtract(pos);
        if (toTarget.isZero()) return true;
        
        double angleToTarget = Math.atan2(toTarget.z(), toTarget.x());
        double angleDiff = Math.abs(normalizeAngle(angleToTarget - facing));
        double halfFov = Math.toRadians(fovDegrees / 2.0);
        
        return angleDiff <= halfFov;
    }
    
    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }
    
    private float calculateThreatLevel(DinoEntity perceiver, DinoEntity other) {
        DinoCategory myCategory = perceiver.getDefinition().getCategory();
        DinoCategory otherCategory = other.getDefinition().getCategory();
        
        if (myCategory == DinoCategory.HERBIVORE) {
            if (otherCategory == DinoCategory.PREDATOR || otherCategory == DinoCategory.APEX) {
                float sizeDiff = other.getDefinition().getHealth() / (float) perceiver.getDefinition().getHealth();
                return Math.min(1.0f, 0.5f + sizeDiff * 0.3f);
            }
        }
        
        if (myCategory == DinoCategory.PREDATOR && otherCategory == DinoCategory.APEX) {
            return 0.8f;
        }
        
        return 0.1f;
    }
    
    private float calculatePlayerThreat(DinoEntity perceiver, PlayerEntity player) {
        float baseThreat = 0.3f;
        if (player.isArmed()) baseThreat += 0.3f;
        if (player.isSprinting()) baseThreat += 0.1f;
        return Math.min(1.0f, baseThreat);
    }
    
    private void classifyDino(DinoEntity perceiver, DinoEntity other, 
                               PerceivedEntity perceived, PerceptionData data) {
        String mySpecies = perceiver.getDefinition().getId();
        String otherSpecies = other.getDefinition().getId();
        DinoCategory myCategory = perceiver.getDefinition().getCategory();
        DinoCategory otherCategory = other.getDefinition().getCategory();
        
        if (mySpecies.equals(otherSpecies)) {
            data.addAlly(perceived);
            return;
        }
        
        if (myCategory == DinoCategory.HERBIVORE) {
            if (otherCategory == DinoCategory.PREDATOR || otherCategory == DinoCategory.APEX) {
                data.addThreat(perceived);
            } else {
                data.addNeutral(perceived);
            }
        } else if (myCategory == DinoCategory.PREDATOR || myCategory == DinoCategory.APEX) {
            if (otherCategory == DinoCategory.HERBIVORE) {
                data.addPrey(perceived);
            } else if (otherCategory == DinoCategory.APEX && myCategory != DinoCategory.APEX) {
                data.addThreat(perceived);
            } else {
                data.addNeutral(perceived);
            }
        }
    }
    
    private void classifyPlayer(DinoEntity perceiver, PlayerEntity player,
                                 PerceivedEntity perceived, PerceptionData data) {
        DinoCategory category = perceiver.getDefinition().getCategory();
        
        switch (perceiver.getDefinition().getTemperament()) {
            case AGGRESSIVE, APEX -> data.addThreat(perceived);
            case TERRITORIAL -> {
                if (perceived.distance() < 20) {
                    data.addThreat(perceived);
                } else {
                    data.addNeutral(perceived);
                }
            }
            case PASSIVE, SKITTISH -> data.addThreat(perceived);
        }
    }
    
    private void selectTarget(DinoEntity perceiver, PerceptionData data) {
        DinoCategory category = perceiver.getDefinition().getCategory();
        
        if (category == DinoCategory.PREDATOR || category == DinoCategory.APEX) {
            data.getNearestPrey().ifPresent(data::setTarget);
        }
        
        if (!data.hasTarget() && data.hasThreats()) {
            data.getThreats().stream()
                .min(Comparator.comparingDouble(PerceivedEntity::distance))
                .ifPresent(data::setTarget);
        }
    }
    
    public record PerceptionConfig(
        float cellSize,
        float baseSightRange,
        float baseHearingRange,
        float baseScentRange,
        float fovDegrees
    ) {
        public static PerceptionConfig defaults() {
            return new PerceptionConfig(16.0f, 40.0f, 25.0f, 15.0f, 120.0f);
        }
    }
    
    public record PlayerEntity(
        UUID id,
        double x, double y, double z,
        double velocityX, double velocityZ,
        boolean isArmed,
        boolean isSprinting
    ) implements SpatialEntity {
        @Override
        public UUID getId() { return id; }
        @Override
        public double getX() { return x; }
        @Override
        public double getY() { return y; }
        @Override
        public double getZ() { return z; }
    }
}
