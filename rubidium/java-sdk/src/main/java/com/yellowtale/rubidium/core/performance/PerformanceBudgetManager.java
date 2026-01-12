package com.yellowtale.rubidium.core.performance;

import com.yellowtale.rubidium.core.metrics.MetricsRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages performance budgets and tracks execution time per module.
 * 
 * Features:
 * - Per-module time tracking
 * - Soft budget enforcement
 * - Automatic task deferral when over budget
 * - Performance reporting
 */
public final class PerformanceBudgetManager {
    
    private static final long DEFAULT_TICK_BUDGET_MS = 45;
    private static final long DEFAULT_MODULE_BUDGET_MS = 10;
    
    private final MetricsRegistry metricsRegistry;
    private final Map<String, ModuleBudget> moduleBudgets = new ConcurrentHashMap<>();
    
    private volatile long tickBudgetNanos = DEFAULT_TICK_BUDGET_MS * 1_000_000;
    private volatile long defaultModuleBudgetNanos = DEFAULT_MODULE_BUDGET_MS * 1_000_000;
    
    private final AtomicLong totalTickOverruns = new AtomicLong(0);
    private final AtomicLong lastOverrunTick = new AtomicLong(-1);
    
    public PerformanceBudgetManager(MetricsRegistry metricsRegistry) {
        this.metricsRegistry = metricsRegistry;
    }
    
    /**
     * Initialize the performance manager.
     */
    public void initialize() {
        // Load configuration if needed
    }
    
    /**
     * Shutdown the performance manager.
     */
    public void shutdown() {
        moduleBudgets.clear();
    }
    
    /**
     * Get the tick budget in nanoseconds.
     */
    public long getTickBudgetNanos() {
        return tickBudgetNanos;
    }
    
    /**
     * Set the tick budget in milliseconds.
     */
    public void setTickBudgetMs(long ms) {
        this.tickBudgetNanos = ms * 1_000_000;
    }
    
    /**
     * Register a module with a custom budget.
     */
    public void registerModule(String moduleId, long budgetNanos) {
        moduleBudgets.put(moduleId, new ModuleBudget(moduleId, budgetNanos));
    }
    
    /**
     * Register a module with the default budget.
     */
    public void registerModule(String moduleId) {
        registerModule(moduleId, defaultModuleBudgetNanos);
    }
    
    /**
     * Unregister a module.
     */
    public void unregisterModule(String moduleId) {
        moduleBudgets.remove(moduleId);
    }
    
    /**
     * Start timing an operation for a module.
     * 
     * @return A timing context to be closed when the operation completes
     */
    public TimingContext startTiming(String moduleId) {
        return new TimingContext(moduleId, System.nanoTime());
    }
    
    /**
     * Record execution time for a module.
     */
    public void recordExecutionTime(String moduleId, long nanos) {
        ModuleBudget budget = moduleBudgets.get(moduleId);
        if (budget != null) {
            budget.addExecutionTime(nanos);
        }
        metricsRegistry.recordModuleExecution(moduleId, nanos);
    }
    
    /**
     * Check if a module is within its budget for this tick.
     */
    public boolean isWithinBudget(String moduleId) {
        ModuleBudget budget = moduleBudgets.get(moduleId);
        return budget == null || budget.isWithinBudget();
    }
    
    /**
     * Get remaining budget for a module in nanoseconds.
     */
    public long getRemainingBudget(String moduleId) {
        ModuleBudget budget = moduleBudgets.get(moduleId);
        return budget != null ? budget.getRemainingBudget() : defaultModuleBudgetNanos;
    }
    
    /**
     * Report a tick overrun.
     */
    public void reportTickOverrun(long tickNumber, long actualNanos) {
        totalTickOverruns.incrementAndGet();
        lastOverrunTick.set(tickNumber);
        metricsRegistry.recordTickOverrun(tickNumber, actualNanos);
    }
    
    /**
     * Reset all module budgets for a new tick.
     */
    public void resetTickBudgets() {
        for (ModuleBudget budget : moduleBudgets.values()) {
            budget.reset();
        }
    }
    
    /**
     * Get performance statistics.
     */
    public PerformanceStats getStats() {
        return new PerformanceStats(
            totalTickOverruns.get(),
            lastOverrunTick.get(),
            tickBudgetNanos,
            moduleBudgets.size()
        );
    }
    
    /**
     * Get detailed statistics for a module.
     */
    public ModuleStats getModuleStats(String moduleId) {
        ModuleBudget budget = moduleBudgets.get(moduleId);
        if (budget == null) {
            return null;
        }
        return new ModuleStats(
            moduleId,
            budget.budgetNanos,
            budget.totalExecutionTime.get(),
            budget.tickExecutionTime.get(),
            budget.overBudgetCount.get()
        );
    }
    
    public class TimingContext implements AutoCloseable {
        private final String moduleId;
        private final long startNanos;
        
        TimingContext(String moduleId, long startNanos) {
            this.moduleId = moduleId;
            this.startNanos = startNanos;
        }
        
        @Override
        public void close() {
            long elapsed = System.nanoTime() - startNanos;
            recordExecutionTime(moduleId, elapsed);
        }
        
        public long elapsed() {
            return System.nanoTime() - startNanos;
        }
    }
    
    private static class ModuleBudget {
        final String moduleId;
        final long budgetNanos;
        final AtomicLong tickExecutionTime = new AtomicLong(0);
        final AtomicLong totalExecutionTime = new AtomicLong(0);
        final AtomicLong overBudgetCount = new AtomicLong(0);
        
        ModuleBudget(String moduleId, long budgetNanos) {
            this.moduleId = moduleId;
            this.budgetNanos = budgetNanos;
        }
        
        void addExecutionTime(long nanos) {
            tickExecutionTime.addAndGet(nanos);
            totalExecutionTime.addAndGet(nanos);
            if (tickExecutionTime.get() > budgetNanos) {
                overBudgetCount.incrementAndGet();
            }
        }
        
        boolean isWithinBudget() {
            return tickExecutionTime.get() < budgetNanos;
        }
        
        long getRemainingBudget() {
            return Math.max(0, budgetNanos - tickExecutionTime.get());
        }
        
        void reset() {
            tickExecutionTime.set(0);
        }
    }
    
    public record PerformanceStats(
        long totalTickOverruns,
        long lastOverrunTick,
        long tickBudgetNanos,
        int trackedModules
    ) {}
    
    public record ModuleStats(
        String moduleId,
        long budgetNanos,
        long totalExecutionNanos,
        long tickExecutionNanos,
        long overBudgetCount
    ) {}
}
