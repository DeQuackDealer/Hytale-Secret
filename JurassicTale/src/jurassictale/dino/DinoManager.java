package jurassictale.dino;

import rubidium.core.logging.RubidiumLogger;
import jurassictale.JurassicTaleConfig;
import jurassictale.dino.behavior.*;
import jurassictale.territory.TerritoryManager;
import jurassictale.territory.TerritoryType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DinoManager {
    
    private final RubidiumLogger logger;
    private final DinoRegistry registry;
    private final TerritoryManager territoryManager;
    private final JurassicTaleConfig config;
    
    private final Map<UUID, DinoEntity> activeDinos;
    private final Map<String, BehaviorController> behaviorControllers;
    
    private final List<Consumer<DinoEvent>> eventListeners;
    
    private boolean chaosStormActive;
    
    public DinoManager(RubidiumLogger logger, DinoRegistry registry, 
                       TerritoryManager territoryManager, JurassicTaleConfig config) {
        this.logger = logger;
        this.registry = registry;
        this.territoryManager = territoryManager;
        this.config = config;
        this.activeDinos = new ConcurrentHashMap<>();
        this.behaviorControllers = new ConcurrentHashMap<>();
        this.eventListeners = new ArrayList<>();
        this.chaosStormActive = false;
        
        registerBehaviorControllers();
    }
    
    private void registerBehaviorControllers() {
        behaviorControllers.put("HERD", new HerdBehavior());
        behaviorControllers.put("AMBUSH", new AmbushBehavior());
        behaviorControllers.put("PACK", new PackBehavior());
        behaviorControllers.put("TERRITORIAL", new TerritorialBehavior());
        behaviorControllers.put("SCAVENGER", new ScavengerBehavior());
        behaviorControllers.put("CAVE_HUNTER", new CaveHunterBehavior());
    }
    
    public DinoEntity spawn(String dinoId, double x, double y, double z) {
        Optional<DinoDefinition> defOpt = registry.get(dinoId);
        if (defOpt.isEmpty()) {
            logger.warn("Cannot spawn unknown dino type: {}", dinoId);
            return null;
        }
        
        DinoDefinition def = defOpt.get();
        DinoEntity entity = new DinoEntity(UUID.randomUUID(), def, x, y, z);
        
        BehaviorController controller = behaviorControllers.get(def.getBehaviorType().name());
        if (controller != null) {
            entity.setBehaviorController(controller);
        }
        
        activeDinos.put(entity.getId(), entity);
        
        fireEvent(new DinoEvent(DinoEvent.Type.SPAWNED, entity));
        logger.debug("Spawned {} at ({}, {}, {})", def.getName(), x, y, z);
        
        return entity;
    }
    
    public List<DinoEntity> spawnPack(String dinoId, double x, double y, double z) {
        Optional<DinoDefinition> defOpt = registry.get(dinoId);
        if (defOpt.isEmpty() || !defOpt.get().isPack()) {
            DinoEntity single = spawn(dinoId, x, y, z);
            return single != null ? List.of(single) : List.of();
        }
        
        DinoDefinition def = defOpt.get();
        int packSize = def.getMinPackSize() + 
            (int)(Math.random() * (def.getMaxPackSize() - def.getMinPackSize() + 1));
        
        List<DinoEntity> pack = new ArrayList<>();
        UUID packId = UUID.randomUUID();
        
        for (int i = 0; i < packSize; i++) {
            double offsetX = (Math.random() - 0.5) * 10;
            double offsetZ = (Math.random() - 0.5) * 10;
            
            DinoEntity entity = spawn(dinoId, x + offsetX, y, z + offsetZ);
            if (entity != null) {
                entity.setPackId(packId);
                pack.add(entity);
            }
        }
        
        logger.info("Spawned {} pack of {} at ({}, {}, {})", def.getName(), pack.size(), x, y, z);
        return pack;
    }
    
    public void despawn(UUID entityId) {
        DinoEntity entity = activeDinos.remove(entityId);
        if (entity != null) {
            fireEvent(new DinoEvent(DinoEvent.Type.DESPAWNED, entity));
        }
    }
    
    public Optional<DinoEntity> getEntity(UUID entityId) {
        return Optional.ofNullable(activeDinos.get(entityId));
    }
    
    public List<DinoEntity> getEntitiesInRadius(double x, double y, double z, double radius) {
        double radiusSq = radius * radius;
        return activeDinos.values().stream()
            .filter(e -> e.distanceSquared(x, y, z) <= radiusSq)
            .toList();
    }
    
    public List<DinoEntity> getEntitiesByTerritory(TerritoryType territory) {
        return activeDinos.values().stream()
            .filter(e -> e.getCurrentTerritory() == territory)
            .toList();
    }
    
    public void tick() {
        for (DinoEntity entity : activeDinos.values()) {
            tickEntity(entity);
        }
    }
    
    private void tickEntity(DinoEntity entity) {
        BehaviorController controller = entity.getBehaviorController();
        if (controller != null) {
            BehaviorContext context = new BehaviorContext(
                entity,
                territoryManager,
                chaosStormActive,
                config
            );
            controller.update(context);
        }
        
        entity.updateAnimation();
    }
    
    public void setChaosStormActive(boolean active) {
        this.chaosStormActive = active;
        
        double multiplier = active ? config.chaosStorm().dinoAggressionMultiplier() : 1.0;
        for (DinoEntity entity : activeDinos.values()) {
            entity.setAggressionMultiplier(multiplier);
        }
        
        logger.info("Chaos Storm {} - aggression multiplier: {}", 
            active ? "ACTIVE" : "ended", multiplier);
    }
    
    public void onEvent(Consumer<DinoEvent> listener) {
        eventListeners.add(listener);
    }
    
    private void fireEvent(DinoEvent event) {
        for (Consumer<DinoEvent> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                logger.error("Error in dino event listener: {}", e.getMessage());
            }
        }
    }
    
    public void shutdown() {
        activeDinos.clear();
        logger.info("Dino manager shutdown - cleared {} entities", activeDinos.size());
    }
    
    public int getActiveCount() {
        return activeDinos.size();
    }
    
    public record DinoEvent(Type type, DinoEntity entity) {
        public enum Type { SPAWNED, DESPAWNED, KILLED, CAPTURED, ESCAPED }
    }
}
