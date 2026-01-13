package jurassictale.dino.types;

public enum TransportClass {
    SMALL(1, "Carry by hand or small cage"),
    MEDIUM(2, "Requires cage or cart"),
    LARGE(3, "Requires large transport vehicle"),
    MASSIVE(4, "Requires specialized equipment");
    
    private final int tier;
    private final String requirements;
    
    TransportClass(int tier, String requirements) {
        this.tier = tier;
        this.requirements = requirements;
    }
    
    public int getTier() { return tier; }
    public String getRequirements() { return requirements; }
}
