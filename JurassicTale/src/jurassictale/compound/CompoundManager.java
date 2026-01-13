package jurassictale.compound;

import rubidium.core.logging.RubidiumLogger;
import jurassictale.compound.power.PowerGridManager;
import jurassictale.compound.power.PowerCore;
import jurassictale.compound.power.PowerGrid;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CompoundManager {
    
    private final RubidiumLogger logger;
    private final PowerGridManager powerGridManager;
    private final Path dataDir;
    
    private final Map<UUID, Compound> compounds;
    private final Map<UUID, List<UUID>> playerCompounds;
    
    public CompoundManager(RubidiumLogger logger, PowerGridManager powerGridManager, Path dataDir) {
        this.logger = logger;
        this.powerGridManager = powerGridManager;
        this.dataDir = dataDir;
        this.compounds = new ConcurrentHashMap<>();
        this.playerCompounds = new ConcurrentHashMap<>();
    }
    
    public Compound createCompound(UUID ownerId, String name, PowerCore.PowerCoreTier tier,
                                   double x, double y, double z) {
        UUID compoundId = UUID.randomUUID();
        
        PowerCore core = new PowerCore(UUID.randomUUID(), tier, x, y, z);
        PowerGrid grid = powerGridManager.createGrid(compoundId, core);
        
        Compound compound = new Compound(compoundId, ownerId, name, core, grid);
        compounds.put(compoundId, compound);
        
        playerCompounds.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(compoundId);
        
        logger.info("Created compound '{}' for player {} at ({}, {}, {})", 
            name, ownerId, x, y, z);
        
        return compound;
    }
    
    public void removeCompound(UUID compoundId) {
        Compound compound = compounds.remove(compoundId);
        if (compound != null) {
            powerGridManager.removeGrid(compound.getPowerGrid().getId());
            
            List<UUID> owned = playerCompounds.get(compound.getOwnerId());
            if (owned != null) {
                owned.remove(compoundId);
            }
            
            logger.info("Removed compound '{}'", compound.getName());
        }
    }
    
    public Optional<Compound> getCompound(UUID compoundId) {
        return Optional.ofNullable(compounds.get(compoundId));
    }
    
    public List<Compound> getPlayerCompounds(UUID playerId) {
        List<UUID> ids = playerCompounds.get(playerId);
        if (ids == null) return List.of();
        
        return ids.stream()
            .map(compounds::get)
            .filter(Objects::nonNull)
            .toList();
    }
    
    public Optional<Compound> getCompoundAt(double x, double y, double z) {
        return compounds.values().stream()
            .filter(c -> c.getPowerCore().isInRange(x, y, z))
            .findFirst();
    }
    
    public boolean isInAnyCompound(double x, double y, double z) {
        return getCompoundAt(x, y, z).isPresent();
    }
    
    public void tick() {
        for (Compound compound : compounds.values()) {
            compound.tick();
        }
    }
    
    public void saveAll() {
        logger.info("Saving {} compounds...", compounds.size());
    }
    
    public int getTotalCompounds() {
        return compounds.size();
    }
}
