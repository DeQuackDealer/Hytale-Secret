package jurassictale.dino.types;

public enum Temperament {
    PASSIVE(0, "Will not attack unless provoked"),
    SKITTISH(1, "Flees from players, hard to approach"),
    AGGRESSIVE(2, "Attacks players on sight"),
    TERRITORIAL(3, "Attacks players entering territory"),
    APEX(4, "Extreme aggression, ignores weak structures");
    
    private final int aggressionLevel;
    private final String description;
    
    Temperament(int aggressionLevel, String description) {
        this.aggressionLevel = aggressionLevel;
        this.description = description;
    }
    
    public int getAggressionLevel() { return aggressionLevel; }
    public String getDescription() { return description; }
}
