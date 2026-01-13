package rubidium.performance;

public record PerformanceConfig(
    int targetTps,
    int emptyServerTps,
    int defaultViewRadius,
    int minimumViewRadius,
    int maximumViewRadius,
    int viewRadiusStep,
    double memoryPressureThreshold,
    double memoryRecoveryThreshold,
    long gcTimeThreshold,
    long minGcInterval,
    int chunkUnloadThreshold,
    boolean enableTpsLimiting,
    boolean enableDynamicViewRadius,
    boolean enableSmartGc
) {
    
    public static PerformanceConfig defaults() {
        return new PerformanceConfig(
            20,
            5,
            10,
            4,
            16,
            1,
            0.85,
            0.70,
            500,
            30000,
            100,
            true,
            true,
            true
        );
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int targetTps = 20;
        private int emptyServerTps = 5;
        private int defaultViewRadius = 10;
        private int minimumViewRadius = 4;
        private int maximumViewRadius = 16;
        private int viewRadiusStep = 1;
        private double memoryPressureThreshold = 0.85;
        private double memoryRecoveryThreshold = 0.70;
        private long gcTimeThreshold = 500;
        private long minGcInterval = 30000;
        private int chunkUnloadThreshold = 100;
        private boolean enableTpsLimiting = true;
        private boolean enableDynamicViewRadius = true;
        private boolean enableSmartGc = true;
        
        public Builder targetTps(int tps) {
            this.targetTps = tps;
            return this;
        }
        
        public Builder emptyServerTps(int tps) {
            this.emptyServerTps = tps;
            return this;
        }
        
        public Builder defaultViewRadius(int radius) {
            this.defaultViewRadius = radius;
            return this;
        }
        
        public Builder minimumViewRadius(int radius) {
            this.minimumViewRadius = radius;
            return this;
        }
        
        public Builder maximumViewRadius(int radius) {
            this.maximumViewRadius = radius;
            return this;
        }
        
        public Builder viewRadiusStep(int step) {
            this.viewRadiusStep = step;
            return this;
        }
        
        public Builder memoryPressureThreshold(double threshold) {
            this.memoryPressureThreshold = threshold;
            return this;
        }
        
        public Builder memoryRecoveryThreshold(double threshold) {
            this.memoryRecoveryThreshold = threshold;
            return this;
        }
        
        public Builder gcTimeThreshold(long ms) {
            this.gcTimeThreshold = ms;
            return this;
        }
        
        public Builder minGcInterval(long ms) {
            this.minGcInterval = ms;
            return this;
        }
        
        public Builder chunkUnloadThreshold(int chunks) {
            this.chunkUnloadThreshold = chunks;
            return this;
        }
        
        public Builder enableTpsLimiting(boolean enable) {
            this.enableTpsLimiting = enable;
            return this;
        }
        
        public Builder enableDynamicViewRadius(boolean enable) {
            this.enableDynamicViewRadius = enable;
            return this;
        }
        
        public Builder enableSmartGc(boolean enable) {
            this.enableSmartGc = enable;
            return this;
        }
        
        public PerformanceConfig build() {
            return new PerformanceConfig(
                targetTps,
                emptyServerTps,
                defaultViewRadius,
                minimumViewRadius,
                maximumViewRadius,
                viewRadiusStep,
                memoryPressureThreshold,
                memoryRecoveryThreshold,
                gcTimeThreshold,
                minGcInterval,
                chunkUnloadThreshold,
                enableTpsLimiting,
                enableDynamicViewRadius,
                enableSmartGc
            );
        }
    }
}
