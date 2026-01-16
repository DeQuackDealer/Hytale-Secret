package rubidium.api.entity;

import rubidium.api.registry.ResourceId;
import java.util.*;
import java.util.function.Consumer;

public class EntityDefinition {
    
    private final ResourceId id;
    private final String displayName;
    private final EntityType type;
    private final double maxHealth;
    private final double movementSpeed;
    private final double attackDamage;
    private final double width;
    private final double height;
    private final boolean hostile;
    private final boolean tameable;
    private final Set<String> tags;
    private final List<AITask> aiTasks;
    private final Map<String, Object> attributes;
    private final Consumer<EntitySpawnContext> onSpawn;
    private final Consumer<EntityTickContext> onTick;
    private final Consumer<EntityDamageContext> onDamage;
    private final Consumer<EntityDeathContext> onDeath;
    private final Consumer<EntityInteractContext> onInteract;
    
    private EntityDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.type = builder.type;
        this.maxHealth = builder.maxHealth;
        this.movementSpeed = builder.movementSpeed;
        this.attackDamage = builder.attackDamage;
        this.width = builder.width;
        this.height = builder.height;
        this.hostile = builder.hostile;
        this.tameable = builder.tameable;
        this.tags = Set.copyOf(builder.tags);
        this.aiTasks = List.copyOf(builder.aiTasks);
        this.attributes = Map.copyOf(builder.attributes);
        this.onSpawn = builder.onSpawn;
        this.onTick = builder.onTick;
        this.onDamage = builder.onDamage;
        this.onDeath = builder.onDeath;
        this.onInteract = builder.onInteract;
    }
    
    public ResourceId getId() { return id; }
    public String getDisplayName() { return displayName; }
    public EntityType getType() { return type; }
    public double getMaxHealth() { return maxHealth; }
    public double getMovementSpeed() { return movementSpeed; }
    public double getAttackDamage() { return attackDamage; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public boolean isHostile() { return hostile; }
    public boolean isTameable() { return tameable; }
    public Set<String> getTags() { return tags; }
    public List<AITask> getAiTasks() { return aiTasks; }
    public Map<String, Object> getAttributes() { return attributes; }
    
    public void handleSpawn(EntitySpawnContext ctx) { if (onSpawn != null) onSpawn.accept(ctx); }
    public void handleTick(EntityTickContext ctx) { if (onTick != null) onTick.accept(ctx); }
    public void handleDamage(EntityDamageContext ctx) { if (onDamage != null) onDamage.accept(ctx); }
    public void handleDeath(EntityDeathContext ctx) { if (onDeath != null) onDeath.accept(ctx); }
    public void handleInteract(EntityInteractContext ctx) { if (onInteract != null) onInteract.accept(ctx); }
    
    public static Builder builder(String id) {
        return new Builder(ResourceId.of(id));
    }
    
    public static Builder builder(ResourceId id) {
        return new Builder(id);
    }
    
    public enum EntityType {
        CREATURE, MONSTER, ANIMAL, NPC, PROJECTILE, VEHICLE, AMBIENT
    }
    
    public static class Builder {
        private final ResourceId id;
        private String displayName;
        private EntityType type = EntityType.CREATURE;
        private double maxHealth = 20.0;
        private double movementSpeed = 0.25;
        private double attackDamage = 2.0;
        private double width = 0.6;
        private double height = 1.8;
        private boolean hostile = false;
        private boolean tameable = false;
        private Set<String> tags = new HashSet<>();
        private List<AITask> aiTasks = new ArrayList<>();
        private Map<String, Object> attributes = new HashMap<>();
        private Consumer<EntitySpawnContext> onSpawn;
        private Consumer<EntityTickContext> onTick;
        private Consumer<EntityDamageContext> onDamage;
        private Consumer<EntityDeathContext> onDeath;
        private Consumer<EntityInteractContext> onInteract;
        
        private Builder(ResourceId id) {
            this.id = id;
            this.displayName = id.path();
        }
        
        public Builder displayName(String name) { this.displayName = name; return this; }
        public Builder type(EntityType type) { this.type = type; return this; }
        public Builder maxHealth(double health) { this.maxHealth = health; return this; }
        public Builder movementSpeed(double speed) { this.movementSpeed = speed; return this; }
        public Builder attackDamage(double damage) { this.attackDamage = damage; return this; }
        public Builder size(double width, double height) { this.width = width; this.height = height; return this; }
        public Builder hostile() { this.hostile = true; return this; }
        public Builder tameable() { this.tameable = true; return this; }
        public Builder tag(String tag) { this.tags.add(tag); return this; }
        public Builder ai(AITask task) { this.aiTasks.add(task); return this; }
        public Builder attribute(String key, Object value) { this.attributes.put(key, value); return this; }
        public Builder onSpawn(Consumer<EntitySpawnContext> h) { this.onSpawn = h; return this; }
        public Builder onTick(Consumer<EntityTickContext> h) { this.onTick = h; return this; }
        public Builder onDamage(Consumer<EntityDamageContext> h) { this.onDamage = h; return this; }
        public Builder onDeath(Consumer<EntityDeathContext> h) { this.onDeath = h; return this; }
        public Builder onInteract(Consumer<EntityInteractContext> h) { this.onInteract = h; return this; }
        
        public EntityDefinition build() {
            return new EntityDefinition(this);
        }
    }
    
    public record AITask(String type, int priority, Map<String, Object> parameters) {
        public static AITask wander(int priority) { return new AITask("wander", priority, Map.of()); }
        public static AITask follow(int priority, double range) { return new AITask("follow", priority, Map.of("range", range)); }
        public static AITask attack(int priority, double range) { return new AITask("attack", priority, Map.of("range", range)); }
        public static AITask flee(int priority, double range) { return new AITask("flee", priority, Map.of("range", range)); }
        public static AITask lookAtPlayer(int priority) { return new AITask("look_at_player", priority, Map.of()); }
    }
    
    public record EntitySpawnContext(Object entity, double x, double y, double z, String world) {}
    public record EntityTickContext(Object entity, long tick) {}
    public record EntityDamageContext(Object entity, Object source, double damage, boolean cancelled) {}
    public record EntityDeathContext(Object entity, Object killer, List<Object> drops) {}
    public record EntityInteractContext(Object player, Object entity, String hand) {}
}
