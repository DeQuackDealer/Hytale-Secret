package com.yellowtale.rubidium.core.config;

import java.util.List;
import java.util.Properties;

/**
 * Base interface for typed configurations.
 * 
 * Implementations should:
 * - Have a no-arg constructor for reflection-based instantiation
 * - Provide sensible defaults for all fields
 * - Implement validation logic
 */
public interface Config {
    
    /**
     * Load configuration from properties.
     */
    void load(Properties props);
    
    /**
     * Save configuration to properties.
     */
    void save(Properties props);
    
    /**
     * Validate the configuration.
     * @return List of validation error messages, empty if valid
     */
    List<String> validate();
    
    /**
     * Get the schema version for migration support.
     */
    default int getSchemaVersion() {
        return 1;
    }
    
    /**
     * Migrate from an older schema version.
     * @param oldVersion The version being migrated from
     * @param props The properties to migrate
     * @return Migrated properties
     */
    default Properties migrate(int oldVersion, Properties props) {
        return props;
    }
}
