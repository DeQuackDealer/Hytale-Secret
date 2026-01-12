package com.yellowtale.rubidium.core.metrics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Central registry for performance metrics and profiling data.
 * 
 * Features:
 * - Tick timing collection
 * - Memory usage sampling
 * - Per-module execution tracking
 * - Exportable metrics interface
 * - Rolling statistics
 */
public final class MetricsRegistry {
    
    private static final int TICK_HISTORY_SIZE = 1200;
    private static final int MEMORY_SAMPLE_INTERVAL_TICKS = 20;
    
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, Gauge> gauges = new ConcurrentHashMap<>();
    private final Map<String, Histogram> histograms = new ConcurrentHashMap<>();
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    
    private final long[] tickDurations = new long[TICK_HISTORY_SIZE];
    private final AtomicLong tickIndex = new AtomicLong(0);
    private final AtomicLong totalTicks = new AtomicLong(0);
    
    private final List<TickOverrun> tickOverruns = Collections.synchronizedList(new ArrayList<>());
    private volatile long lastMemorySample = 0;
    private volatile MemorySnapshot lastMemorySnapshot;
    
    private final List<MetricsExporter> exporters = new ArrayList<>();
    
    /**
     * Initialize the metrics registry.
     */
    public void initialize() {
        sampleMemory();
    }
    
    /**
     * Shutdown the metrics registry.
     */
    public void shutdown() {
        counters.clear();
        gauges.clear();
        histograms.clear();
        timers.clear();
        exporters.clear();
    }
    
    /**
     * Get or create a counter metric.
     */
    public Counter counter(String name) {
        return counters.computeIfAbsent(name, Counter::new);
    }
    
    /**
     * Get or create a gauge metric.
     */
    public Gauge gauge(String name) {
        return gauges.computeIfAbsent(name, Gauge::new);
    }
    
    /**
     * Get or create a histogram metric.
     */
    public Histogram histogram(String name) {
        return histograms.computeIfAbsent(name, Histogram::new);
    }
    
    /**
     * Get or create a timer metric.
     */
    public Timer timer(String name) {
        return timers.computeIfAbsent(name, Timer::new);
    }
    
    /**
     * Record a tick duration.
     */
    public void recordTickDuration(long nanos) {
        int index = (int) (tickIndex.getAndIncrement() % TICK_HISTORY_SIZE);
        tickDurations[index] = nanos;
        totalTicks.incrementAndGet();
        
        histogram("rubidium.tick.duration").record(nanos / 1_000_000.0);
        
        if (totalTicks.get() % MEMORY_SAMPLE_INTERVAL_TICKS == 0) {
            sampleMemory();
        }
    }
    
    /**
     * Record a tick overrun.
     */
    public void recordTickOverrun(long tickNumber, long actualNanos) {
        tickOverruns.add(new TickOverrun(tickNumber, actualNanos, System.currentTimeMillis()));
        counter("rubidium.tick.overruns").increment();
        
        while (tickOverruns.size() > 100) {
            tickOverruns.remove(0);
        }
    }
    
    /**
     * Record task execution time.
     */
    public void recordTaskExecution(String owner, long nanos) {
        timer("rubidium.task." + owner).record(nanos);
    }
    
    /**
     * Record module execution time.
     */
    public void recordModuleExecution(String moduleId, long nanos) {
        timer("rubidium.module." + moduleId).record(nanos);
    }
    
    /**
     * Sample current memory usage.
     */
    public void sampleMemory() {
        Runtime runtime = Runtime.getRuntime();
        lastMemorySnapshot = new MemorySnapshot(
            runtime.totalMemory(),
            runtime.freeMemory(),
            runtime.maxMemory(),
            System.currentTimeMillis()
        );
        lastMemorySample = System.currentTimeMillis();
        
        gauge("rubidium.memory.used").set(lastMemorySnapshot.usedMemory());
        gauge("rubidium.memory.free").set(lastMemorySnapshot.freeMemory);
        gauge("rubidium.memory.max").set(lastMemorySnapshot.maxMemory);
    }
    
    /**
     * Get tick duration statistics.
     */
    public TickStats getTickStats() {
        long count = Math.min(totalTicks.get(), TICK_HISTORY_SIZE);
        if (count == 0) {
            return new TickStats(0, 0, 0, 0, 0);
        }
        
        long sum = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        
        for (int i = 0; i < count; i++) {
            long duration = tickDurations[i];
            sum += duration;
            min = Math.min(min, duration);
            max = Math.max(max, duration);
        }
        
        double avg = (double) sum / count;
        
        long[] sorted = Arrays.copyOf(tickDurations, (int) count);
        Arrays.sort(sorted);
        long p99 = sorted[(int) (count * 0.99)];
        
        return new TickStats(avg, min, max, p99, totalTicks.get());
    }
    
    /**
     * Get the latest memory snapshot.
     */
    public MemorySnapshot getMemorySnapshot() {
        return lastMemorySnapshot;
    }
    
    /**
     * Get recent tick overruns.
     */
    public List<TickOverrun> getRecentOverruns() {
        return List.copyOf(tickOverruns);
    }
    
    /**
     * Export all metrics.
     */
    public MetricsExport export() {
        Map<String, Object> data = new LinkedHashMap<>();
        
        counters.forEach((name, counter) -> data.put(name, counter.get()));
        gauges.forEach((name, gauge) -> data.put(name, gauge.get()));
        histograms.forEach((name, histogram) -> data.put(name, histogram.getStats()));
        timers.forEach((name, timer) -> data.put(name, timer.getStats()));
        
        return new MetricsExport(data, System.currentTimeMillis());
    }
    
    /**
     * Add a metrics exporter.
     */
    public void addExporter(MetricsExporter exporter) {
        exporters.add(exporter);
    }
    
    /**
     * Export metrics to all registered exporters.
     */
    public void exportToAll() {
        MetricsExport export = export();
        for (MetricsExporter exporter : exporters) {
            try {
                exporter.export(export);
            } catch (Exception e) {
                // Log error
            }
        }
    }
    
    public static class Counter {
        private final String name;
        private final LongAdder value = new LongAdder();
        
        Counter(String name) {
            this.name = name;
        }
        
        public void increment() {
            value.increment();
        }
        
        public void add(long delta) {
            value.add(delta);
        }
        
        public long get() {
            return value.sum();
        }
        
        public String getName() {
            return name;
        }
    }
    
    public static class Gauge {
        private final String name;
        private volatile double value = 0;
        
        Gauge(String name) {
            this.name = name;
        }
        
        public void set(double value) {
            this.value = value;
        }
        
        public double get() {
            return value;
        }
        
        public String getName() {
            return name;
        }
    }
    
    public static class Histogram {
        private final String name;
        private final LongAdder count = new LongAdder();
        private volatile double sum = 0;
        private volatile double min = Double.MAX_VALUE;
        private volatile double max = Double.MIN_VALUE;
        
        Histogram(String name) {
            this.name = name;
        }
        
        public synchronized void record(double value) {
            count.increment();
            sum += value;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        
        public HistogramStats getStats() {
            long c = count.sum();
            return new HistogramStats(c, c > 0 ? sum / c : 0, min, max);
        }
        
        public String getName() {
            return name;
        }
    }
    
    public static class Timer {
        private final String name;
        private final LongAdder count = new LongAdder();
        private final LongAdder totalNanos = new LongAdder();
        private volatile long minNanos = Long.MAX_VALUE;
        private volatile long maxNanos = Long.MIN_VALUE;
        
        Timer(String name) {
            this.name = name;
        }
        
        public synchronized void record(long nanos) {
            count.increment();
            totalNanos.add(nanos);
            minNanos = Math.min(minNanos, nanos);
            maxNanos = Math.max(maxNanos, nanos);
        }
        
        public TimerStats getStats() {
            long c = count.sum();
            long total = totalNanos.sum();
            return new TimerStats(c, c > 0 ? (double) total / c : 0, minNanos, maxNanos);
        }
        
        public String getName() {
            return name;
        }
    }
    
    public record TickStats(double avgNanos, long minNanos, long maxNanos, long p99Nanos, long totalTicks) {}
    public record MemorySnapshot(long totalMemory, long freeMemory, long maxMemory, long timestamp) {
        public long usedMemory() { return totalMemory - freeMemory; }
    }
    public record TickOverrun(long tickNumber, long actualNanos, long timestamp) {}
    public record HistogramStats(long count, double avg, double min, double max) {}
    public record TimerStats(long count, double avgNanos, long minNanos, long maxNanos) {}
    public record MetricsExport(Map<String, Object> data, long timestamp) {}
}
