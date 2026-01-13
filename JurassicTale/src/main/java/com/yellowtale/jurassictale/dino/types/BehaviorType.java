package com.yellowtale.jurassictale.dino.types;

public enum BehaviorType {
    HERD("Herd AI", "Flee, defend young, stampede when threatened"),
    AMBUSH("Ambush AI", "Wait, burst attack, retreat to recover"),
    PACK("Pack AI", "Coordinate with pack, surround targets, flank"),
    TERRITORIAL("Territorial AI", "Patrol territory, roar warning, chase intruders"),
    SCAVENGER("Scavenger AI", "Attracted to corpses and campfires"),
    CAVE_HUNTER("Cave Hunter AI", "Attracted to sound and torch light");
    
    private final String displayName;
    private final String description;
    
    BehaviorType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
