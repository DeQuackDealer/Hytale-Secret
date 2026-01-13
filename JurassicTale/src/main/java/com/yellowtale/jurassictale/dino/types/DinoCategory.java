package com.yellowtale.jurassictale.dino.types;

public enum DinoCategory {
    HERBIVORE("Herbivore", "Herd wildlife - food and material source"),
    PREDATOR("Predator", "Pack and solo predators - core danger"),
    APEX("Apex Predator", "Territory bosses - high rewards"),
    FLYER("Flyer", "Air threats - disrupts camps and convoys"),
    AQUATIC("Aquatic", "Water threats - swamp and river danger"),
    CAVE("Cave Dweller", "Underground threats - mining danger");
    
    private final String displayName;
    private final String description;
    
    DinoCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
