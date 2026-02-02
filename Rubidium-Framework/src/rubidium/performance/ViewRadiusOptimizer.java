package rubidium.performance;

public class ViewRadiusOptimizer {
    private final TpsMonitor tpsMonitor;
    private final int minViewRadius;
    private final int maxViewRadius;
    private volatile int currentViewRadius;
    
    private final double increaseThreshold;
    private final double decreaseThreshold;
    private final long adjustmentCooldownMs;
    private volatile long lastAdjustmentTime;
    
    public ViewRadiusOptimizer(TpsMonitor tpsMonitor) {
        this(tpsMonitor, 4, 16, 10);
    }
    
    public ViewRadiusOptimizer(TpsMonitor tpsMonitor, int minViewRadius, int maxViewRadius, int initialRadius) {
        this.tpsMonitor = tpsMonitor;
        this.minViewRadius = minViewRadius;
        this.maxViewRadius = maxViewRadius;
        this.currentViewRadius = initialRadius;
        this.increaseThreshold = 19.5;
        this.decreaseThreshold = 17.0;
        this.adjustmentCooldownMs = 5000;
        this.lastAdjustmentTime = 0;
    }
    
    public int getOptimalViewRadius() {
        long now = System.currentTimeMillis();
        if (now - lastAdjustmentTime < adjustmentCooldownMs) {
            return currentViewRadius;
        }
        
        double tps = tpsMonitor.getCurrentTps();
        
        if (tps >= increaseThreshold && currentViewRadius < maxViewRadius) {
            currentViewRadius++;
            lastAdjustmentTime = now;
        } else if (tps < decreaseThreshold && currentViewRadius > minViewRadius) {
            currentViewRadius--;
            lastAdjustmentTime = now;
        }
        
        return currentViewRadius;
    }
    
    public int getCurrentViewRadius() {
        return currentViewRadius;
    }
    
    public void setViewRadius(int radius) {
        this.currentViewRadius = Math.max(minViewRadius, Math.min(maxViewRadius, radius));
    }
    
    public ViewRadiusInfo getInfo() {
        return new ViewRadiusInfo(
            currentViewRadius,
            minViewRadius,
            maxViewRadius,
            tpsMonitor.getCurrentTps(),
            tpsMonitor.getPerformanceLevel()
        );
    }
    
    public record ViewRadiusInfo(
        int currentRadius,
        int minRadius,
        int maxRadius,
        double currentTps,
        TpsMonitor.PerformanceLevel performanceLevel
    ) {}
}
