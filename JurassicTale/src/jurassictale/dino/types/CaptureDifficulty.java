package jurassictale.dino.types;

public enum CaptureDifficulty {
    EASY(1.0, 0.8, "Simple to tranq and transport"),
    MEDIUM(0.7, 0.6, "Requires preparation"),
    HARD(0.5, 0.4, "Team effort recommended"),
    EXTREME(0.3, 0.2, "Legendary capture challenge");
    
    private final double tranqEffectiveness;
    private final double captureSuccessRate;
    private final String description;
    
    CaptureDifficulty(double tranqEffectiveness, double captureSuccessRate, String description) {
        this.tranqEffectiveness = tranqEffectiveness;
        this.captureSuccessRate = captureSuccessRate;
        this.description = description;
    }
    
    public double getTranqEffectiveness() { return tranqEffectiveness; }
    public double getCaptureSuccessRate() { return captureSuccessRate; }
    public String getDescription() { return description; }
}
