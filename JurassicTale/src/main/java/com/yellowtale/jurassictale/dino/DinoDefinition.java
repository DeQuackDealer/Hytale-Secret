package com.yellowtale.jurassictale.dino;

import com.yellowtale.jurassictale.dino.types.*;
import com.yellowtale.jurassictale.territory.TerritoryType;

import java.util.ArrayList;
import java.util.List;

public class DinoDefinition {
    
    private final String id;
    private final String name;
    private final DinoCategory category;
    private final BehaviorType behaviorType;
    private final Temperament temperament;
    private final CaptureDifficulty captureDifficulty;
    private final TransportClass transportClass;
    
    private final int baseHealth;
    private final int baseDamage;
    private final float baseSpeed;
    
    private final int minPackSize;
    private final int maxPackSize;
    
    private final List<TerritoryType> spawnTerritories;
    
    private final boolean legendary;
    private final boolean canBreakBlocks;
    private final boolean nightPredator;
    
    private final String modelId;
    private final List<String> animationIds;
    
    private DinoDefinition(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.category = builder.category;
        this.behaviorType = builder.behaviorType;
        this.temperament = builder.temperament;
        this.captureDifficulty = builder.captureDifficulty;
        this.transportClass = builder.transportClass;
        this.baseHealth = builder.baseHealth;
        this.baseDamage = builder.baseDamage;
        this.baseSpeed = builder.baseSpeed;
        this.minPackSize = builder.minPackSize;
        this.maxPackSize = builder.maxPackSize;
        this.spawnTerritories = List.copyOf(builder.spawnTerritories);
        this.legendary = builder.legendary;
        this.canBreakBlocks = builder.canBreakBlocks;
        this.nightPredator = builder.nightPredator;
        this.modelId = builder.modelId;
        this.animationIds = List.copyOf(builder.animationIds);
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public DinoCategory getCategory() { return category; }
    public BehaviorType getBehaviorType() { return behaviorType; }
    public Temperament getTemperament() { return temperament; }
    public CaptureDifficulty getCaptureDifficulty() { return captureDifficulty; }
    public TransportClass getTransportClass() { return transportClass; }
    public int getBaseHealth() { return baseHealth; }
    public int getBaseDamage() { return baseDamage; }
    public float getBaseSpeed() { return baseSpeed; }
    public int getMinPackSize() { return minPackSize; }
    public int getMaxPackSize() { return maxPackSize; }
    public List<TerritoryType> getSpawnTerritories() { return spawnTerritories; }
    public boolean isLegendary() { return legendary; }
    public boolean canBreakBlocks() { return canBreakBlocks; }
    public boolean isNightPredator() { return nightPredator; }
    public String getModelId() { return modelId; }
    public List<String> getAnimationIds() { return animationIds; }
    
    public boolean isPack() {
        return behaviorType == BehaviorType.PACK && maxPackSize > 1;
    }
    
    public static Builder builder(String id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final String id;
        private String name;
        private DinoCategory category = DinoCategory.HERBIVORE;
        private BehaviorType behaviorType = BehaviorType.HERD;
        private Temperament temperament = Temperament.PASSIVE;
        private CaptureDifficulty captureDifficulty = CaptureDifficulty.MEDIUM;
        private TransportClass transportClass = TransportClass.MEDIUM;
        private int baseHealth = 100;
        private int baseDamage = 10;
        private float baseSpeed = 1.0f;
        private int minPackSize = 1;
        private int maxPackSize = 1;
        private List<TerritoryType> spawnTerritories = new ArrayList<>();
        private boolean legendary = false;
        private boolean canBreakBlocks = false;
        private boolean nightPredator = false;
        private String modelId;
        private List<String> animationIds = new ArrayList<>();
        
        private Builder(String id) {
            this.id = id;
            this.name = id;
            this.modelId = "dino_" + id;
        }
        
        public Builder name(String name) { this.name = name; return this; }
        public Builder category(DinoCategory category) { this.category = category; return this; }
        public Builder behaviorType(BehaviorType type) { this.behaviorType = type; return this; }
        public Builder temperament(Temperament temperament) { this.temperament = temperament; return this; }
        public Builder captureDifficulty(CaptureDifficulty difficulty) { this.captureDifficulty = difficulty; return this; }
        public Builder transportClass(TransportClass transportClass) { this.transportClass = transportClass; return this; }
        public Builder health(int health) { this.baseHealth = health; return this; }
        public Builder damage(int damage) { this.baseDamage = damage; return this; }
        public Builder speed(float speed) { this.baseSpeed = speed; return this; }
        public Builder packSize(int min, int max) { this.minPackSize = min; this.maxPackSize = max; return this; }
        public Builder addSpawnTerritory(TerritoryType territory) { this.spawnTerritories.add(territory); return this; }
        public Builder legendary(boolean legendary) { this.legendary = legendary; return this; }
        public Builder canBreakBlocks(boolean canBreak) { this.canBreakBlocks = canBreak; return this; }
        public Builder nightPredator(boolean nightPredator) { this.nightPredator = nightPredator; return this; }
        public Builder modelId(String modelId) { this.modelId = modelId; return this; }
        public Builder addAnimation(String animationId) { this.animationIds.add(animationId); return this; }
        
        public DinoDefinition build() {
            return new DinoDefinition(this);
        }
    }
}
