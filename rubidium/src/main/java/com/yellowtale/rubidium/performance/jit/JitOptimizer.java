package com.yellowtale.rubidium.performance.jit;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public final class JitOptimizer {
    private static final Logger LOGGER = Logger.getLogger(JitOptimizer.class.getName());
    private static final JitOptimizer INSTANCE = new JitOptimizer();
    
    private final Map<String, HotPath> hotPaths = new ConcurrentHashMap<>();
    private final AtomicLong totalInvocations = new AtomicLong();
    private final long startTime = System.nanoTime();
    
    private volatile boolean c2Enabled = true;
    private volatile boolean tieredCompilation = true;
    private volatile int compileThreshold = 10000;
    private volatile boolean inliningEnabled = true;
    
    private JitOptimizer() {
        detectJvmCapabilities();
    }
    
    public static JitOptimizer getInstance() {
        return INSTANCE;
    }
    
    private void detectJvmCapabilities() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> args = runtimeMxBean.getInputArguments();
        
        for (String arg : args) {
            if (arg.contains("-XX:-TieredCompilation")) {
                tieredCompilation = false;
            }
            if (arg.contains("-XX:-UseC2")) {
                c2Enabled = false;
            }
            if (arg.startsWith("-XX:CompileThreshold=")) {
                try {
                    compileThreshold = Integer.parseInt(arg.split("=")[1]);
                } catch (NumberFormatException e) {
                }
            }
            if (arg.contains("-XX:-Inline")) {
                inliningEnabled = false;
            }
        }
        
        String vmName = System.getProperty("java.vm.name", "");
        String vmVersion = System.getProperty("java.vm.version", "");
        
        LOGGER.info("[RPAL] JIT Optimizer initialized (Lithium-style)");
        LOGGER.info("[RPAL]   VM: " + vmName + " " + vmVersion);
        LOGGER.info("[RPAL]   Tiered compilation: " + tieredCompilation);
        LOGGER.info("[RPAL]   C2 compiler: " + c2Enabled);
        LOGGER.info("[RPAL]   Compile threshold: " + compileThreshold);
        LOGGER.info("[RPAL]   Inlining: " + inliningEnabled);
    }
    
    public void recordHotPath(String pathId) {
        totalInvocations.incrementAndGet();
        hotPaths.computeIfAbsent(pathId, HotPath::new).record();
    }
    
    public void recordHotPath(String pathId, long executionNanos) {
        totalInvocations.incrementAndGet();
        hotPaths.computeIfAbsent(pathId, HotPath::new).record(executionNanos);
    }
    
    public void warmUp(Runnable hotCode, String pathId, int iterations) {
        LOGGER.fine("[RPAL] Warming up hot path: " + pathId + " (" + iterations + " iterations)");
        
        HotPath path = hotPaths.computeIfAbsent(pathId, HotPath::new);
        
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            try {
                hotCode.run();
            } catch (Exception e) {
                LOGGER.warning("[RPAL] Warm-up iteration failed: " + e.getMessage());
                break;
            }
            long elapsed = System.nanoTime() - start;
            path.record(elapsed);
            totalInvocations.incrementAndGet();
        }
        
        LOGGER.fine("[RPAL] Warm-up complete for: " + pathId);
    }
    
    public boolean isHot(String pathId) {
        HotPath path = hotPaths.get(pathId);
        return path != null && path.getInvocationCount() >= compileThreshold;
    }
    
    public List<String> getRecommendedJvmFlags() {
        List<String> flags = new ArrayList<>();
        flags.add("-XX:+UseG1GC");
        flags.add("-XX:MaxGCPauseMillis=50");
        flags.add("-XX:+TieredCompilation");
        flags.add("-XX:TieredStopAtLevel=4");
        flags.add("-XX:+UseStringDeduplication");
        flags.add("-XX:+OptimizeStringConcat");
        flags.add("-XX:+AggressiveOpts");
        flags.add("-XX:+UseFastAccessorMethods");
        flags.add("-XX:MaxInlineSize=100");
        flags.add("-XX:InlineSmallCode=2000");
        flags.add("-XX:+UseCompressedOops");
        flags.add("-XX:+UseCompressedClassPointers");
        flags.add("-XX:+AlwaysPreTouch");
        return flags;
    }
    
    public List<String> getLowLatencyJvmFlags() {
        List<String> flags = new ArrayList<>();
        flags.add("-XX:+UseZGC");
        flags.add("-XX:+ZGenerational");
        flags.add("-XX:+TieredCompilation");
        flags.add("-XX:TieredStopAtLevel=4");
        flags.add("-XX:+UseTransparentHugePages");
        flags.add("-XX:+AlwaysPreTouch");
        flags.add("-XX:+DisableExplicitGC");
        flags.add("-XX:ReservedCodeCacheSize=256m");
        return flags;
    }
    
    public List<String> getHighThroughputJvmFlags() {
        List<String> flags = new ArrayList<>();
        flags.add("-XX:+UseParallelGC");
        flags.add("-XX:ParallelGCThreads=4");
        flags.add("-XX:+TieredCompilation");
        flags.add("-XX:+AggressiveHeap");
        flags.add("-XX:+UseNUMA");
        return flags;
    }
    
    public JitStats getStats() {
        Map<String, HotPath.Stats> pathStats = new HashMap<>();
        for (Map.Entry<String, HotPath> entry : hotPaths.entrySet()) {
            pathStats.put(entry.getKey(), entry.getValue().getStats());
        }
        
        long uptime = System.nanoTime() - startTime;
        
        return new JitStats(
            totalInvocations.get(),
            pathStats,
            tieredCompilation,
            c2Enabled,
            compileThreshold,
            inliningEnabled,
            uptime
        );
    }
    
    public void reset() {
        hotPaths.clear();
        totalInvocations.set(0);
        LOGGER.info("[RPAL] JIT optimizer stats reset");
    }
    
    private static final class HotPath {
        private final String id;
        private final AtomicLong invocationCount = new AtomicLong();
        private final AtomicLong totalTimeNanos = new AtomicLong();
        private volatile long minTimeNanos = Long.MAX_VALUE;
        private volatile long maxTimeNanos = 0;
        private final long createdAt = System.nanoTime();
        
        HotPath(String id) {
            this.id = id;
        }
        
        void record() {
            invocationCount.incrementAndGet();
        }
        
        void record(long executionNanos) {
            invocationCount.incrementAndGet();
            totalTimeNanos.addAndGet(executionNanos);
            
            if (executionNanos < minTimeNanos) minTimeNanos = executionNanos;
            if (executionNanos > maxTimeNanos) maxTimeNanos = executionNanos;
        }
        
        long getInvocationCount() {
            return invocationCount.get();
        }
        
        Stats getStats() {
            long count = invocationCount.get();
            long total = totalTimeNanos.get();
            double avg = count > 0 ? (double) total / count : 0;
            
            return new Stats(
                id,
                count,
                total,
                avg,
                minTimeNanos == Long.MAX_VALUE ? 0 : minTimeNanos,
                maxTimeNanos,
                System.nanoTime() - createdAt
            );
        }
        
        record Stats(
            String id,
            long invocationCount,
            long totalTimeNanos,
            double avgTimeNanos,
            long minTimeNanos,
            long maxTimeNanos,
            long ageNanos
        ) {}
    }
    
    public record JitStats(
        long totalInvocations,
        Map<String, HotPath.Stats> hotPaths,
        boolean tieredCompilation,
        boolean c2Enabled,
        int compileThreshold,
        boolean inliningEnabled,
        long uptimeNanos
    ) {
        public int hotPathCount() {
            return hotPaths.size();
        }
        
        public long compiledPaths() {
            return hotPaths.values().stream()
                .filter(s -> s.invocationCount() >= compileThreshold)
                .count();
        }
    }
}
