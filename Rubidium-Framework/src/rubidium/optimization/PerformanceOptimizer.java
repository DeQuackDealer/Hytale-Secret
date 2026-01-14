package rubidium.optimization;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.Logger;

public class PerformanceOptimizer {
    
    private static final Logger logger = Logger.getLogger("Rubidium-Optimizer");
    
    private static PerformanceOptimizer instance;
    
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final RuntimeMXBean runtimeBean;
    private final List<GarbageCollectorMXBean> gcBeans;
    
    private final ScheduledExecutorService monitorExecutor;
    private final Map<String, PerformanceMetric> metrics = new ConcurrentHashMap<>();
    private final List<OptimizationRule> rules = new ArrayList<>();
    
    private volatile boolean running = false;
    private volatile OptimizationLevel currentLevel = OptimizationLevel.BALANCED;
    
    private final AtomicLong totalTickTime = new AtomicLong(0);
    private final AtomicLong tickCount = new AtomicLong(0);
    private final AtomicLong gcPauseTime = new AtomicLong(0);
    
    public PerformanceOptimizer() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        
        this.monitorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Rubidium-PerfMonitor");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
        
        initializeRules();
    }
    
    public static PerformanceOptimizer getInstance() {
        if (instance == null) {
            instance = new PerformanceOptimizer();
        }
        return instance;
    }
    
    private void initializeRules() {
        rules.add(new OptimizationRule("high-memory", 
            () -> getHeapUsagePercent() > 85,
            this::onHighMemory,
            "Heap usage exceeds 85%"));
        
        rules.add(new OptimizationRule("low-memory",
            () -> getHeapUsagePercent() < 50,
            this::onLowMemory,
            "Heap usage below 50%"));
        
        rules.add(new OptimizationRule("slow-ticks",
            () -> getAverageTickTime() > 45,
            this::onSlowTicks,
            "Average tick time exceeds 45ms"));
        
        rules.add(new OptimizationRule("gc-pressure",
            () -> getGCPausePercent() > 10,
            this::onGCPressure,
            "GC pause time exceeds 10%"));
    }
    
    public void start() {
        if (running) return;
        running = true;
        
        monitorExecutor.scheduleAtFixedRate(this::monitorTick, 1, 1, TimeUnit.SECONDS);
        monitorExecutor.scheduleAtFixedRate(this::evaluateRules, 5, 5, TimeUnit.SECONDS);
        
        logger.info("Performance optimizer started with level: " + currentLevel);
    }
    
    public void stop() {
        running = false;
        monitorExecutor.shutdown();
    }
    
    private void monitorTick() {
        if (!running) return;
        
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        
        recordMetric("heap.used", heap.getUsed());
        recordMetric("heap.max", heap.getMax());
        recordMetric("heap.percent", (double) heap.getUsed() / heap.getMax() * 100);
        recordMetric("nonheap.used", nonHeap.getUsed());
        recordMetric("threads.count", threadBean.getThreadCount());
        recordMetric("threads.peak", threadBean.getPeakThreadCount());
        
        long totalGcTime = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            totalGcTime += gc.getCollectionTime();
        }
        gcPauseTime.set(totalGcTime);
        recordMetric("gc.time.total", totalGcTime);
        
        if (tickCount.get() > 0) {
            recordMetric("tick.avg", (double) totalTickTime.get() / tickCount.get());
        }
    }
    
    private void evaluateRules() {
        if (!running) return;
        
        for (OptimizationRule rule : rules) {
            try {
                if (rule.condition().getAsBoolean()) {
                    logger.fine("Optimization rule triggered: " + rule.name());
                    rule.action().run();
                }
            } catch (Exception e) {
                logger.warning("Error evaluating rule " + rule.name() + ": " + e.getMessage());
            }
        }
    }
    
    private void onHighMemory() {
        logger.info("High memory detected - suggesting GC");
        System.gc();
        
        if (currentLevel != OptimizationLevel.AGGRESSIVE) {
            currentLevel = OptimizationLevel.AGGRESSIVE;
            logger.info("Switching to AGGRESSIVE optimization level");
        }
    }
    
    private void onLowMemory() {
        if (currentLevel == OptimizationLevel.AGGRESSIVE) {
            currentLevel = OptimizationLevel.BALANCED;
            logger.info("Memory normalized - switching to BALANCED optimization level");
        }
    }
    
    private void onSlowTicks() {
        logger.warning("Slow tick performance detected - avg: " + getAverageTickTime() + "ms");
    }
    
    private void onGCPressure() {
        logger.warning("High GC pressure detected - " + getGCPausePercent() + "% pause time");
    }
    
    public void recordTickTime(long nanos) {
        totalTickTime.addAndGet(nanos / 1_000_000);
        tickCount.incrementAndGet();
    }
    
    public void recordMetric(String name, double value) {
        metrics.compute(name, (k, v) -> {
            if (v == null) {
                return new PerformanceMetric(name, value);
            }
            v.record(value);
            return v;
        });
    }
    
    public double getHeapUsagePercent() {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        return (double) heap.getUsed() / heap.getMax() * 100;
    }
    
    public double getAverageTickTime() {
        long count = tickCount.get();
        return count > 0 ? (double) totalTickTime.get() / count : 0;
    }
    
    public double getGCPausePercent() {
        long uptime = runtimeBean.getUptime();
        if (uptime == 0) return 0;
        return (double) gcPauseTime.get() / uptime * 100;
    }
    
    public OptimizationLevel getCurrentLevel() {
        return currentLevel;
    }
    
    public void setOptimizationLevel(OptimizationLevel level) {
        this.currentLevel = level;
        logger.info("Optimization level set to: " + level);
    }
    
    public Map<String, PerformanceMetric> getMetrics() {
        return Collections.unmodifiableMap(metrics);
    }
    
    public PerformanceSummary getSummary() {
        return new PerformanceSummary(
            getHeapUsagePercent(),
            getAverageTickTime(),
            getGCPausePercent(),
            threadBean.getThreadCount(),
            currentLevel,
            runtimeBean.getUptime()
        );
    }
    
    public enum OptimizationLevel {
        MINIMAL,
        BALANCED,
        AGGRESSIVE,
        MAXIMUM
    }
    
    public record PerformanceSummary(
        double heapUsagePercent,
        double avgTickMs,
        double gcPausePercent,
        int threadCount,
        OptimizationLevel level,
        long uptimeMs
    ) {}
    
    public static class PerformanceMetric {
        private final String name;
        private double current;
        private double min;
        private double max;
        private double sum;
        private long count;
        
        public PerformanceMetric(String name, double initial) {
            this.name = name;
            this.current = initial;
            this.min = initial;
            this.max = initial;
            this.sum = initial;
            this.count = 1;
        }
        
        public synchronized void record(double value) {
            current = value;
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
            count++;
        }
        
        public String getName() { return name; }
        public double getCurrent() { return current; }
        public double getMin() { return min; }
        public double getMax() { return max; }
        public double getAverage() { return count > 0 ? sum / count : 0; }
        public long getCount() { return count; }
    }
    
    private record OptimizationRule(
        String name,
        java.util.function.BooleanSupplier condition,
        Runnable action,
        String description
    ) {}
}
