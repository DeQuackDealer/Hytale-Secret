package rubidium.api.npc;

import rubidium.api.pathfinding.PathfindingAPI;
import rubidium.api.pathfinding.PathfindingAPI.*;
import rubidium.api.event.EventAPI;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class NPCAPI {
    
    private static final Map<String, NPCDefinition> definitions = new ConcurrentHashMap<>();
    private static final Map<UUID, NPC> npcs = new ConcurrentHashMap<>();
    private static final Map<String, NPCBehavior> behaviors = new ConcurrentHashMap<>();
    
    private NPCAPI() {}
    
    public static NPCDefinition.Builder create(String id) {
        return new NPCDefinition.Builder(id);
    }
    
    public static NPCDefinition register(NPCDefinition definition) {
        definitions.put(definition.id(), definition);
        return definition;
    }
    
    public static NPCDefinition register(NPCDefinition.Builder builder) {
        return register(builder.build());
    }
    
    public static Optional<NPCDefinition> getDefinition(String id) {
        return Optional.ofNullable(definitions.get(id));
    }
    
    public static NPC spawn(String definitionId, Vec3i location) {
        NPCDefinition def = definitions.get(definitionId);
        if (def == null) {
            throw new IllegalArgumentException("Unknown NPC definition: " + definitionId);
        }
        
        NPC npc = new NPC(UUID.randomUUID(), def, location);
        npcs.put(npc.getId(), npc);
        
        EventAPI.fire(new NPCSpawnEvent(npc));
        
        return npc;
    }
    
    public static NPC spawn(NPCDefinition definition, Vec3i location) {
        register(definition);
        return spawn(definition.id(), location);
    }
    
    public static void despawn(UUID npcId) {
        NPC npc = npcs.remove(npcId);
        if (npc != null) {
            EventAPI.fire(new NPCDespawnEvent(npc));
        }
    }
    
    public static void despawn(NPC npc) {
        despawn(npc.getId());
    }
    
    public static Optional<NPC> get(UUID id) {
        return Optional.ofNullable(npcs.get(id));
    }
    
    public static Collection<NPC> all() {
        return npcs.values();
    }
    
    public static Collection<NPC> nearby(Vec3i center, double radius) {
        return npcs.values().stream()
            .filter(npc -> npc.getLocation().distanceTo(center) <= radius)
            .toList();
    }
    
    public static void registerBehavior(String id, NPCBehavior behavior) {
        behaviors.put(id, behavior);
    }
    
    public static Optional<NPCBehavior> getBehavior(String id) {
        return Optional.ofNullable(behaviors.get(id));
    }
    
    public static NPCDefinition villager(String id, String name) {
        return create(id)
            .displayName(name)
            .type(NPCDefinition.NPCType.VILLAGER)
            .behavior("idle")
            .interactable(true)
            .build();
    }
    
    public static NPCDefinition guard(String id, String name) {
        return create(id)
            .displayName(name)
            .type(NPCDefinition.NPCType.GUARD)
            .behavior("guard")
            .hostile(true)
            .build();
    }
    
    public static NPCDefinition merchant(String id, String name) {
        return create(id)
            .displayName(name)
            .type(NPCDefinition.NPCType.MERCHANT)
            .behavior("idle")
            .interactable(true)
            .build();
    }
    
    public static NPCDefinition questGiver(String id, String name) {
        return create(id)
            .displayName(name)
            .type(NPCDefinition.NPCType.QUEST_GIVER)
            .behavior("idle")
            .interactable(true)
            .showNameTag(true)
            .build();
    }
    
    static {
        registerBehavior("idle", new IdleBehavior());
        registerBehavior("wander", new WanderBehavior(10));
        registerBehavior("follow", new FollowBehavior(5.0));
        registerBehavior("guard", new GuardBehavior(15.0));
        registerBehavior("patrol", new PatrolBehavior());
    }
    
    public record NPCDefinition(
        String id,
        String displayName,
        NPCType type,
        String model,
        String skin,
        String defaultBehavior,
        List<DialogNode> dialog,
        Map<String, Object> attributes,
        boolean interactable,
        boolean invulnerable,
        boolean showNameTag,
        boolean hostile,
        double moveSpeed,
        double health
    ) {
        public enum NPCType {
            VILLAGER, MERCHANT, GUARD, QUEST_GIVER, COMPANION, ENEMY, BOSS, CUSTOM
        }
        
        public static class Builder {
            private final String id;
            private String displayName = "NPC";
            private NPCType type = NPCType.CUSTOM;
            private String model = "default";
            private String skin = "default";
            private String defaultBehavior = "idle";
            private List<DialogNode> dialog = new ArrayList<>();
            private Map<String, Object> attributes = new HashMap<>();
            private boolean interactable = true;
            private boolean invulnerable = false;
            private boolean showNameTag = true;
            private boolean hostile = false;
            private double moveSpeed = 0.2;
            private double health = 20.0;
            
            public Builder(String id) { this.id = id; }
            
            public Builder displayName(String name) { this.displayName = name; return this; }
            public Builder type(NPCType type) { this.type = type; return this; }
            public Builder model(String model) { this.model = model; return this; }
            public Builder skin(String skin) { this.skin = skin; return this; }
            public Builder behavior(String behavior) { this.defaultBehavior = behavior; return this; }
            public Builder dialog(DialogNode... nodes) { this.dialog.addAll(Arrays.asList(nodes)); return this; }
            public Builder attribute(String key, Object value) { this.attributes.put(key, value); return this; }
            public Builder interactable(boolean v) { this.interactable = v; return this; }
            public Builder invulnerable(boolean v) { this.invulnerable = v; return this; }
            public Builder showNameTag(boolean v) { this.showNameTag = v; return this; }
            public Builder hostile(boolean v) { this.hostile = v; return this; }
            public Builder moveSpeed(double speed) { this.moveSpeed = speed; return this; }
            public Builder health(double health) { this.health = health; return this; }
            
            public NPCDefinition build() {
                return new NPCDefinition(id, displayName, type, model, skin, defaultBehavior,
                    List.copyOf(dialog), Map.copyOf(attributes), interactable, invulnerable,
                    showNameTag, hostile, moveSpeed, health);
            }
        }
    }
    
    public static class NPC {
        private final UUID id;
        private final NPCDefinition definition;
        private Vec3i location;
        private Vec3i homeLocation;
        private double yaw, pitch;
        private double currentHealth;
        private String currentBehavior;
        private Object target;
        private List<Vec3i> patrolPoints = new ArrayList<>();
        private int patrolIndex = 0;
        private PathResult currentPath;
        private int pathIndex = 0;
        private final Map<String, Object> data = new ConcurrentHashMap<>();
        
        public NPC(UUID id, NPCDefinition definition, Vec3i location) {
            this.id = id;
            this.definition = definition;
            this.location = location;
            this.homeLocation = location;
            this.currentHealth = definition.health();
            this.currentBehavior = definition.defaultBehavior();
        }
        
        public UUID getId() { return id; }
        public NPCDefinition getDefinition() { return definition; }
        public Vec3i getLocation() { return location; }
        public Vec3i getHomeLocation() { return homeLocation; }
        public double getYaw() { return yaw; }
        public double getPitch() { return pitch; }
        public double getHealth() { return currentHealth; }
        public String getCurrentBehavior() { return currentBehavior; }
        public Object getTarget() { return target; }
        
        public void setLocation(Vec3i loc) { this.location = loc; }
        public void setHomeLocation(Vec3i loc) { this.homeLocation = loc; }
        public void setRotation(double yaw, double pitch) { this.yaw = yaw; this.pitch = pitch; }
        public void setHealth(double health) { this.currentHealth = Math.max(0, Math.min(definition.health(), health)); }
        public void setTarget(Object target) { this.target = target; }
        
        public void setBehavior(String behaviorId) {
            this.currentBehavior = behaviorId;
        }
        
        public void setPatrolPoints(List<Vec3i> points) {
            this.patrolPoints = new ArrayList<>(points);
            this.patrolIndex = 0;
        }
        
        public void moveTo(Vec3i target, PathfindingContext context) {
            this.currentPath = PathfindingAPI.findPath(location, target, context);
            this.pathIndex = 0;
        }
        
        public void moveToAsync(Vec3i target, PathfindingContext context, Consumer<PathResult> callback) {
            PathfindingAPI.findPathAsync(location, target, context)
                .thenAccept(result -> {
                    this.currentPath = result;
                    this.pathIndex = 0;
                    if (callback != null) callback.accept(result);
                });
        }
        
        public boolean tick() {
            NPCBehavior behavior = behaviors.get(currentBehavior);
            if (behavior != null) {
                behavior.tick(this);
            }
            
            if (currentPath != null && currentPath.success() && pathIndex < currentPath.path().size()) {
                Vec3i nextPos = currentPath.path().get(pathIndex);
                this.location = nextPos;
                pathIndex++;
                
                if (pathIndex >= currentPath.path().size()) {
                    currentPath = null;
                }
                return true;
            }
            
            return false;
        }
        
        public void lookAt(Vec3i target) {
            double dx = target.x() - location.x();
            double dy = target.y() - location.y();
            double dz = target.z() - location.z();
            double dist = Math.sqrt(dx * dx + dz * dz);
            this.yaw = Math.toDegrees(Math.atan2(-dx, dz));
            this.pitch = Math.toDegrees(Math.atan2(dy, dist));
        }
        
        public void damage(double amount) {
            if (!definition.invulnerable()) {
                setHealth(currentHealth - amount);
            }
        }
        
        public boolean isDead() {
            return currentHealth <= 0;
        }
        
        public void setData(String key, Object value) { data.put(key, value); }
        @SuppressWarnings("unchecked")
        public <T> T getData(String key) { return (T) data.get(key); }
        public <T> T getData(String key, T defaultValue) {
            Object v = data.get(key);
            return v != null ? (T) v : defaultValue;
        }
        
        public void speak(String message) {
            rubidium.api.chat.ChatAPI.sendAsNPC(this, message);
        }
        
        public void say(String message) {
            speak(message);
        }
        
        public Vec3i getNextPatrolPoint() {
            if (patrolPoints.isEmpty()) return homeLocation;
            Vec3i point = patrolPoints.get(patrolIndex);
            patrolIndex = (patrolIndex + 1) % patrolPoints.size();
            return point;
        }
    }
    
    public record DialogNode(
        String id,
        String text,
        List<DialogOption> options
    ) {
        public static DialogNode simple(String id, String text) {
            return new DialogNode(id, text, List.of());
        }
        
        public static DialogNode withOptions(String id, String text, DialogOption... options) {
            return new DialogNode(id, text, List.of(options));
        }
    }
    
    public record DialogOption(
        String text,
        String nextNodeId,
        String action
    ) {
        public static DialogOption of(String text, String nextNodeId) {
            return new DialogOption(text, nextNodeId, null);
        }
        
        public static DialogOption withAction(String text, String action) {
            return new DialogOption(text, null, action);
        }
    }
    
    public interface NPCBehavior {
        void tick(NPC npc);
        default void onStart(NPC npc) {}
        default void onStop(NPC npc) {}
    }
    
    public static class IdleBehavior implements NPCBehavior {
        @Override
        public void tick(NPC npc) {
        }
    }
    
    public static class WanderBehavior implements NPCBehavior {
        private final int radius;
        private final Random random = new Random();
        private int tickCounter = 0;
        
        public WanderBehavior(int radius) {
            this.radius = radius;
        }
        
        @Override
        public void tick(NPC npc) {
            tickCounter++;
            if (tickCounter >= 100 + random.nextInt(100)) {
                tickCounter = 0;
                
                Vec3i home = npc.getHomeLocation();
                int dx = random.nextInt(radius * 2 + 1) - radius;
                int dz = random.nextInt(radius * 2 + 1) - radius;
                Vec3i target = home.add(dx, 0, dz);
                
            }
        }
    }
    
    public static class FollowBehavior implements NPCBehavior {
        private final double maxDistance;
        
        public FollowBehavior(double maxDistance) {
            this.maxDistance = maxDistance;
        }
        
        @Override
        public void tick(NPC npc) {
            Object target = npc.getTarget();
            if (target != null) {
            }
        }
    }
    
    public static class GuardBehavior implements NPCBehavior {
        private final double aggroRange;
        
        public GuardBehavior(double aggroRange) {
            this.aggroRange = aggroRange;
        }
        
        @Override
        public void tick(NPC npc) {
        }
    }
    
    public static class PatrolBehavior implements NPCBehavior {
        private int waitTicks = 0;
        
        @Override
        public void tick(NPC npc) {
            if (waitTicks > 0) {
                waitTicks--;
                return;
            }
            
        }
    }
    
    public static class NPCSpawnEvent extends EventAPI.Event {
        private final NPC npc;
        public NPCSpawnEvent(NPC npc) { this.npc = npc; }
        public NPC getNpc() { return npc; }
    }
    
    public static class NPCDespawnEvent extends EventAPI.Event {
        private final NPC npc;
        public NPCDespawnEvent(NPC npc) { this.npc = npc; }
        public NPC getNpc() { return npc; }
    }
    
    public static class NPCInteractEvent extends EventAPI.CancellableEvent {
        private final NPC npc;
        private final Object player;
        public NPCInteractEvent(NPC npc, Object player) { this.npc = npc; this.player = player; }
        public NPC getNpc() { return npc; }
        public Object getPlayer() { return player; }
    }
}
