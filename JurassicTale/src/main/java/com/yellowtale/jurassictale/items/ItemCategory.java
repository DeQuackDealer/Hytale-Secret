package com.yellowtale.jurassictale.items;

public enum ItemCategory {
    FIREARM("Firearms", "Ranged weapons using bullets"),
    TRANQ("Tranquilizers", "Non-lethal capture weapons"),
    MELEE("Melee", "Close combat weapons"),
    THROWABLE("Throwables", "Grenades and tactical items"),
    RAID_TOOL("Raid Tools", "Breach and infiltration equipment"),
    MEDICAL("Medical", "Healing and status items"),
    CONSUMABLE("Consumables", "Food and water"),
    AMMO("Ammunition", "Weapon ammunition"),
    MATERIAL("Materials", "Crafting and building materials"),
    EQUIPMENT("Equipment", "Wearable gear");
    
    private final String displayName;
    private final String description;
    
    ItemCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
