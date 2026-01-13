package jurassictale;

import java.nio.file.Path;

public record JurassicTaleConfig(
    DinoConfig dinos,
    TerritoryConfig territories,
    CompoundConfig compounds,
    ChaosStormConfig chaosStorm,
    CaptureConfig capture
) {
    
    public static JurassicTaleConfig load(Path configPath) {
        return defaults();
    }
    
    public static JurassicTaleConfig defaults() {
        return new JurassicTaleConfig(
            DinoConfig.defaults(),
            TerritoryConfig.defaults(),
            CompoundConfig.defaults(),
            ChaosStormConfig.defaults(),
            CaptureConfig.defaults()
        );
    }
    
    public record DinoConfig(
        int maxDinosPerTerritory,
        int spawnIntervalTicks,
        double packSpawnChance,
        double apexSpawnChance,
        boolean enableNightBehavior,
        int despawnDistanceBlocks
    ) {
        public static DinoConfig defaults() {
            return new DinoConfig(50, 200, 0.3, 0.05, true, 256);
        }
    }
    
    public record TerritoryConfig(
        int territoryChunkSize,
        boolean enableZoneBorders,
        boolean showTerritoryWarnings
    ) {
        public static TerritoryConfig defaults() {
            return new TerritoryConfig(16, true, true);
        }
    }
    
    public record CompoundConfig(
        int maxCompoundsPerPlayer,
        int powerCoreRangeBlocks,
        double sabotageBaseChance,
        int npcPayrollIntervalMinutes
    ) {
        public static CompoundConfig defaults() {
            return new CompoundConfig(3, 64, 0.02, 60);
        }
    }
    
    public record ChaosStormConfig(
        int stormDurationMinutes,
        int cooldownMinutes,
        double dinoAggressionMultiplier,
        boolean enablePvP,
        int warningMinutes
    ) {
        public static ChaosStormConfig defaults() {
            return new ChaosStormConfig(30, 120, 2.0, true, 5);
        }
    }
    
    public record CaptureConfig(
        double baseTranqEffectiveness,
        int netDurationTicks,
        int bolaDurationTicks,
        double escapeChancePerSecond
    ) {
        public static CaptureConfig defaults() {
            return new CaptureConfig(1.0, 100, 60, 0.05);
        }
    }
}
