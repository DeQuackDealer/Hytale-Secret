package rubidium.core.feature;

import java.time.Instant;
import java.util.*;

/**
 * Health status of a feature with detailed diagnostics.
 */
public record FeatureHealth(
    Status status,
    String message,
    Instant lastCheck,
    Map<String, Object> metrics,
    List<HealthIssue> issues
) {
    
    public enum Status {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        DISABLED,
        UNKNOWN
    }
    
    public record HealthIssue(
        Severity severity,
        String code,
        String description,
        Instant occurredAt
    ) {
        public enum Severity {
            INFO, WARNING, ERROR, CRITICAL
        }
    }
    
    public static FeatureHealth healthy() {
        return new FeatureHealth(
            Status.HEALTHY, 
            "Operating normally", 
            Instant.now(), 
            Map.of(), 
            List.of()
        );
    }
    
    public static FeatureHealth healthy(String message) {
        return new FeatureHealth(Status.HEALTHY, message, Instant.now(), Map.of(), List.of());
    }
    
    public static FeatureHealth degraded(String message, List<HealthIssue> issues) {
        return new FeatureHealth(Status.DEGRADED, message, Instant.now(), Map.of(), issues);
    }
    
    public static FeatureHealth unhealthy(String message, Throwable cause) {
        return new FeatureHealth(
            Status.UNHEALTHY,
            message,
            Instant.now(),
            Map.of("exception", cause.getClass().getName()),
            List.of(new HealthIssue(
                HealthIssue.Severity.CRITICAL,
                "EXCEPTION",
                cause.getMessage(),
                Instant.now()
            ))
        );
    }
    
    public static FeatureHealth disabled(String reason) {
        return new FeatureHealth(Status.DISABLED, reason, Instant.now(), Map.of(), List.of());
    }
    
    public boolean isOperational() {
        return status == Status.HEALTHY || status == Status.DEGRADED;
    }
    
    public FeatureHealth withMetric(String key, Object value) {
        Map<String, Object> newMetrics = new HashMap<>(metrics);
        newMetrics.put(key, value);
        return new FeatureHealth(status, message, lastCheck, newMetrics, issues);
    }
}
