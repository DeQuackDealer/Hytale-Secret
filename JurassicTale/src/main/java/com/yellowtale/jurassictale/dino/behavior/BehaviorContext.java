package com.yellowtale.jurassictale.dino.behavior;

import com.yellowtale.jurassictale.JurassicTaleConfig;
import com.yellowtale.jurassictale.ai.perception.PerceptionData;
import com.yellowtale.jurassictale.dino.DinoEntity;
import com.yellowtale.jurassictale.territory.TerritoryManager;

public record BehaviorContext(
    DinoEntity entity,
    TerritoryManager territoryManager,
    boolean chaosStormActive,
    JurassicTaleConfig config,
    PerceptionData perception
) {
    public BehaviorContext(DinoEntity entity, TerritoryManager territoryManager, 
                           boolean chaosStormActive, JurassicTaleConfig config) {
        this(entity, territoryManager, chaosStormActive, config, null);
    }
}
