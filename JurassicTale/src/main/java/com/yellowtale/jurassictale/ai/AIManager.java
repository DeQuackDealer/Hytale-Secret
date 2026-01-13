package com.yellowtale.jurassictale.ai;

import com.yellowtale.rubidium.core.logging.RubidiumLogger;
import com.yellowtale.jurassictale.ai.perception.PerceptionData;
import com.yellowtale.jurassictale.ai.perception.PerceptionSystem;
import com.yellowtale.jurassictale.ai.perception.PerceptionSystem.PerceptionConfig;
import com.yellowtale.jurassictale.ai.pool.AIObjectPools;
import com.yellowtale.jurassictale.dino.DinoEntity;
import com.yellowtale.jurassictale.dino.behavior.BehaviorContext;
import com.yellowtale.jurassictale.territory.TerritoryManager;
import com.yellowtale.jurassictale.JurassicTaleConfig;

import java.util.*;
import java.util.concurrent.*;

public class AIManager {
    
    private final RubidiumLogger logger;
    private final PerceptionSystem perceptionSystem;
    private final AIObjectPools pools;
    private final Map<UUID, PerceptionData> cachedPerceptions;
    private final long perceptionCacheTime;
    private final Map<UUID, Long> lastPerceptionUpdate;
    
    private TerritoryManager territoryManager;
    private JurassicTaleConfig config;
    private boolean chaosStormActive;
    
    public AIManager(RubidiumLogger logger) {
        this.logger = logger;
        this.perceptionSystem = new PerceptionSystem(PerceptionConfig.defaults());
        this.pools = new AIObjectPools();
        this.cachedPerceptions = new ConcurrentHashMap<>();
        this.lastPerceptionUpdate = new ConcurrentHashMap<>();
        this.perceptionCacheTime = 100;
        this.chaosStormActive = false;
    }
    
    public void setTerritoryManager(TerritoryManager manager) {
        this.territoryManager = manager;
    }
    
    public void setConfig(JurassicTaleConfig config) {
        this.config = config;
    }
    
    public void setChaosStormActive(boolean active) {
        this.chaosStormActive = active;
    }
    
    public void registerDino(DinoEntity dino) {
        perceptionSystem.updateDino(dino);
    }
    
    public void unregisterDino(DinoEntity dino) {
        perceptionSystem.removeDino(dino);
        cachedPerceptions.remove(dino.getId());
        lastPerceptionUpdate.remove(dino.getId());
    }
    
    public void updateDinoPosition(DinoEntity dino) {
        perceptionSystem.updateDino(dino);
    }
    
    public void tickDino(DinoEntity dino) {
        if (dino.isDead() || dino.isTransquilized()) return;
        
        PerceptionData perception = getOrUpdatePerception(dino);
        
        BehaviorContext context = new BehaviorContext(
            dino, territoryManager, chaosStormActive, config, perception
        );
        
        if (dino.getBehaviorController() != null) {
            dino.getBehaviorController().update(context);
        }
        
        updateDinoPosition(dino);
    }
    
    public void tickAll(Collection<DinoEntity> dinos) {
        for (DinoEntity dino : dinos) {
            tickDino(dino);
        }
    }
    
    private PerceptionData getOrUpdatePerception(DinoEntity dino) {
        long now = System.currentTimeMillis();
        Long lastUpdate = lastPerceptionUpdate.get(dino.getId());
        
        if (lastUpdate == null || now - lastUpdate > perceptionCacheTime) {
            PerceptionData oldData = cachedPerceptions.get(dino.getId());
            if (oldData != null) {
                pools.releasePerception(oldData);
            }
            
            PerceptionData newData = perceptionSystem.perceive(dino);
            cachedPerceptions.put(dino.getId(), newData);
            lastPerceptionUpdate.put(dino.getId(), now);
            return newData;
        }
        
        return cachedPerceptions.getOrDefault(dino.getId(), new PerceptionData());
    }
    
    public AIObjectPools.PoolStats getPoolStats() {
        return pools.getStats();
    }
    
    public int getTrackedDinoCount() {
        return cachedPerceptions.size();
    }
    
    public void shutdown() {
        cachedPerceptions.clear();
        lastPerceptionUpdate.clear();
        logger.info("AIManager shutdown complete");
    }
}
