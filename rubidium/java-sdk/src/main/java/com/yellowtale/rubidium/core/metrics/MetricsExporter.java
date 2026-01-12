package com.yellowtale.rubidium.core.metrics;

/**
 * Interface for exporting metrics to external systems.
 */
@FunctionalInterface
public interface MetricsExporter {
    
    /**
     * Export metrics data.
     * 
     * @param export The metrics export containing all collected data
     */
    void export(MetricsRegistry.MetricsExport export);
}
