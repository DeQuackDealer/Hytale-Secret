package com.yellowtale.rubidium.core.config;

import java.util.List;
import java.util.Properties;

/**
 * Main configuration for the Rubidium runtime.
 * Demonstrates proper Config implementation.
 */
public class RubidiumConfig extends AbstractConfig {
    
    private int tickBudgetMs = 45;
    private int defaultModuleBudgetMs = 10;
    private boolean metricsEnabled = true;
    private int metricsExportIntervalSeconds = 60;
    private LogLevel logLevel = LogLevel.INFO;
    private int networkBatchSize = 64;
    private int networkFlushIntervalMs = 10;
    private long maxBandwidthBytesPerSecond = -1;
    
    public RubidiumConfig() {
    }
    
    @Override
    public void load(Properties props) {
        tickBudgetMs = getInt(props, "performance.tick_budget_ms", 45);
        defaultModuleBudgetMs = getInt(props, "performance.default_module_budget_ms", 10);
        metricsEnabled = getBoolean(props, "metrics.enabled", true);
        metricsExportIntervalSeconds = getInt(props, "metrics.export_interval_seconds", 60);
        logLevel = LogLevel.valueOf(getString(props, "logging.level", "INFO").toUpperCase());
        networkBatchSize = getInt(props, "network.batch_size", 64);
        networkFlushIntervalMs = getInt(props, "network.flush_interval_ms", 10);
        maxBandwidthBytesPerSecond = getLong(props, "network.max_bandwidth_bytes_per_second", -1);
    }
    
    @Override
    public void save(Properties props) {
        setInt(props, "performance.tick_budget_ms", tickBudgetMs);
        setInt(props, "performance.default_module_budget_ms", defaultModuleBudgetMs);
        setBoolean(props, "metrics.enabled", metricsEnabled);
        setInt(props, "metrics.export_interval_seconds", metricsExportIntervalSeconds);
        setString(props, "logging.level", logLevel.name());
        setInt(props, "network.batch_size", networkBatchSize);
        setInt(props, "network.flush_interval_ms", networkFlushIntervalMs);
        setLong(props, "network.max_bandwidth_bytes_per_second", maxBandwidthBytesPerSecond);
    }
    
    @Override
    public List<String> validate() {
        return new ValidationBuilder()
            .requireRange("tick_budget_ms", tickBudgetMs, 1, 100)
            .requireRange("default_module_budget_ms", defaultModuleBudgetMs, 1, 50)
            .requireRange("metrics_export_interval_seconds", metricsExportIntervalSeconds, 1, 3600)
            .requirePositive("network_batch_size", networkBatchSize)
            .requirePositive("network_flush_interval_ms", networkFlushIntervalMs)
            .build();
    }
    
    public int getTickBudgetMs() {
        return tickBudgetMs;
    }
    
    public void setTickBudgetMs(int tickBudgetMs) {
        this.tickBudgetMs = tickBudgetMs;
    }
    
    public int getDefaultModuleBudgetMs() {
        return defaultModuleBudgetMs;
    }
    
    public void setDefaultModuleBudgetMs(int defaultModuleBudgetMs) {
        this.defaultModuleBudgetMs = defaultModuleBudgetMs;
    }
    
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }
    
    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }
    
    public int getMetricsExportIntervalSeconds() {
        return metricsExportIntervalSeconds;
    }
    
    public void setMetricsExportIntervalSeconds(int metricsExportIntervalSeconds) {
        this.metricsExportIntervalSeconds = metricsExportIntervalSeconds;
    }
    
    public LogLevel getLogLevel() {
        return logLevel;
    }
    
    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }
    
    public int getNetworkBatchSize() {
        return networkBatchSize;
    }
    
    public void setNetworkBatchSize(int networkBatchSize) {
        this.networkBatchSize = networkBatchSize;
    }
    
    public int getNetworkFlushIntervalMs() {
        return networkFlushIntervalMs;
    }
    
    public void setNetworkFlushIntervalMs(int networkFlushIntervalMs) {
        this.networkFlushIntervalMs = networkFlushIntervalMs;
    }
    
    public long getMaxBandwidthBytesPerSecond() {
        return maxBandwidthBytesPerSecond;
    }
    
    public void setMaxBandwidthBytesPerSecond(long maxBandwidthBytesPerSecond) {
        this.maxBandwidthBytesPerSecond = maxBandwidthBytesPerSecond;
    }
    
    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR, OFF
    }
}
