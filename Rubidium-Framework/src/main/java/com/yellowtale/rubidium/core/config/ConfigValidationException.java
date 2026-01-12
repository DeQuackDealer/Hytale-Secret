package com.yellowtale.rubidium.core.config;

import java.util.List;

/**
 * Exception thrown when configuration validation fails.
 */
public class ConfigValidationException extends ConfigException {
    
    private final String configId;
    private final List<String> errors;
    
    public ConfigValidationException(String configId, List<String> errors) {
        super("Configuration validation failed for '" + configId + "': " + String.join(", ", errors));
        this.configId = configId;
        this.errors = List.copyOf(errors);
    }
    
    public String getConfigId() {
        return configId;
    }
    
    public List<String> getErrors() {
        return errors;
    }
}
