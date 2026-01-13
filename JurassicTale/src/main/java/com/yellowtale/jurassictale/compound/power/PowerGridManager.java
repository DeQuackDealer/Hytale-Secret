package com.yellowtale.jurassictale.compound.power;

import com.yellowtale.rubidium.core.logging.RubidiumLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PowerGridManager {
    
    private final RubidiumLogger logger;
    private final Map<UUID, PowerGrid> grids;
    private final Map<UUID, PowerDevice> devices;
    
    public PowerGridManager(RubidiumLogger logger) {
        this.logger = logger;
        this.grids = new ConcurrentHashMap<>();
        this.devices = new ConcurrentHashMap<>();
    }
    
    public PowerGrid createGrid(UUID compoundId, PowerCore core) {
        PowerGrid grid = new PowerGrid(UUID.randomUUID(), compoundId, core);
        grids.put(grid.getId(), grid);
        logger.debug("Created power grid {} for compound {}", grid.getId(), compoundId);
        return grid;
    }
    
    public void removeGrid(UUID gridId) {
        PowerGrid grid = grids.remove(gridId);
        if (grid != null) {
            logger.debug("Removed power grid {}", gridId);
        }
    }
    
    public Optional<PowerGrid> getGrid(UUID gridId) {
        return Optional.ofNullable(grids.get(gridId));
    }
    
    public Optional<PowerGrid> getGridForCompound(UUID compoundId) {
        return grids.values().stream()
            .filter(g -> g.getCompoundId().equals(compoundId))
            .findFirst();
    }
    
    public void registerDevice(PowerDevice device, UUID gridId) {
        devices.put(device.getId(), device);
        
        PowerGrid grid = grids.get(gridId);
        if (grid != null) {
            grid.addDevice(device);
        }
    }
    
    public void unregisterDevice(UUID deviceId) {
        PowerDevice device = devices.remove(deviceId);
        if (device != null) {
            for (PowerGrid grid : grids.values()) {
                grid.removeDevice(deviceId);
            }
        }
    }
    
    public void tick() {
        for (PowerGrid grid : grids.values()) {
            grid.tick();
        }
    }
    
    public int getTotalGrids() {
        return grids.size();
    }
    
    public long getTotalPowerGenerated() {
        return grids.values().stream()
            .mapToLong(PowerGrid::getPowerGeneration)
            .sum();
    }
    
    public long getTotalPowerConsumed() {
        return grids.values().stream()
            .mapToLong(PowerGrid::getPowerConsumption)
            .sum();
    }
}
