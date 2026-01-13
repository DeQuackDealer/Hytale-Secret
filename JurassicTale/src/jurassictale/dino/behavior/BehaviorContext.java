package jurassictale.dino.behavior;

import jurassictale.JurassicTaleConfig;
import jurassictale.ai.perception.PerceptionData;
import jurassictale.dino.DinoEntity;
import jurassictale.territory.TerritoryManager;

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
