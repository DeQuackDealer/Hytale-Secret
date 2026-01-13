package com.yellowtale.jurassictale.compound.power;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PowerGrid {
    
    private final UUID id;
    private final UUID compoundId;
    private final PowerCore primaryCore;
    
    private final Map<UUID, PowerDevice> devices;
    private final List<PowerCore> backupCores;
    
    private long powerGeneration;
    private long powerConsumption;
    private long powerStored;
    private long maxPowerStorage;
    
    private boolean online;
    private boolean overloaded;
    
    public PowerGrid(UUID id, UUID compoundId, PowerCore primaryCore) {
        this.id = id;
        this.compoundId = compoundId;
        this.primaryCore = primaryCore;
        this.devices = new ConcurrentHashMap<>();
        this.backupCores = new ArrayList<>();
        this.powerGeneration = primaryCore.getPowerOutput();
        this.powerConsumption = 0;
        this.powerStored = 0;
        this.maxPowerStorage = primaryCore.getPowerStorage();
        this.online = true;
        this.overloaded = false;
    }
    
    public UUID getId() { return id; }
    public UUID getCompoundId() { return compoundId; }
    public PowerCore getPrimaryCore() { return primaryCore; }
    
    public long getPowerGeneration() { return powerGeneration; }
    public long getPowerConsumption() { return powerConsumption; }
    public long getPowerStored() { return powerStored; }
    public long getMaxPowerStorage() { return maxPowerStorage; }
    public long getAvailablePower() { return powerGeneration + powerStored - powerConsumption; }
    
    public boolean isOnline() { return online; }
    public boolean isOverloaded() { return overloaded; }
    
    public void addDevice(PowerDevice device) {
        devices.put(device.getId(), device);
        recalculatePower();
    }
    
    public void removeDevice(UUID deviceId) {
        devices.remove(deviceId);
        recalculatePower();
    }
    
    public void addBackupCore(PowerCore core) {
        backupCores.add(core);
        recalculatePower();
    }
    
    public void tick() {
        if (!online) return;
        
        long surplus = powerGeneration - powerConsumption;
        
        if (surplus > 0) {
            powerStored = Math.min(maxPowerStorage, powerStored + surplus);
            overloaded = false;
        } else if (surplus < 0) {
            long deficit = -surplus;
            if (powerStored >= deficit) {
                powerStored -= deficit;
                overloaded = false;
            } else {
                powerStored = 0;
                overloaded = true;
            }
        }
        
        for (PowerDevice device : devices.values()) {
            device.setPowered(!overloaded);
        }
    }
    
    private void recalculatePower() {
        powerGeneration = primaryCore.getPowerOutput();
        for (PowerCore backup : backupCores) {
            powerGeneration += backup.getPowerOutput();
        }
        
        maxPowerStorage = primaryCore.getPowerStorage();
        for (PowerCore backup : backupCores) {
            maxPowerStorage += backup.getPowerStorage();
        }
        
        powerConsumption = devices.values().stream()
            .mapToLong(PowerDevice::getPowerDraw)
            .sum();
    }
    
    public void setOnline(boolean online) {
        this.online = online;
        if (!online) {
            for (PowerDevice device : devices.values()) {
                device.setPowered(false);
            }
        }
    }
    
    public Collection<PowerDevice> getDevices() {
        return Collections.unmodifiableCollection(devices.values());
    }
}
