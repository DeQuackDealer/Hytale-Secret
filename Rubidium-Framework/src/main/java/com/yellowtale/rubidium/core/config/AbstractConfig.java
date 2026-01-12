package com.yellowtale.rubidium.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;

/**
 * Base class providing utility methods for configuration implementations.
 */
public abstract class AbstractConfig implements Config {
    
    protected String getString(Properties props, String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
    
    protected int getInt(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    protected long getLong(Properties props, String key, long defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    protected double getDouble(Properties props, String key, double defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    protected boolean getBoolean(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }
    
    protected List<String> getStringList(Properties props, String key, List<String> defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) return defaultValue;
        return List.of(value.split(","));
    }
    
    protected void setString(Properties props, String key, String value) {
        if (value != null) {
            props.setProperty(key, value);
        }
    }
    
    protected void setInt(Properties props, String key, int value) {
        props.setProperty(key, String.valueOf(value));
    }
    
    protected void setLong(Properties props, String key, long value) {
        props.setProperty(key, String.valueOf(value));
    }
    
    protected void setDouble(Properties props, String key, double value) {
        props.setProperty(key, String.valueOf(value));
    }
    
    protected void setBoolean(Properties props, String key, boolean value) {
        props.setProperty(key, String.valueOf(value));
    }
    
    protected void setStringList(Properties props, String key, List<String> value) {
        if (value != null) {
            props.setProperty(key, String.join(",", value));
        }
    }
    
    /**
     * Helper for building validation errors.
     */
    protected static class ValidationBuilder {
        private final List<String> errors = new ArrayList<>();
        
        public ValidationBuilder require(String fieldName, Object value) {
            if (value == null) {
                errors.add(fieldName + " is required");
            }
            return this;
        }
        
        public ValidationBuilder requireNotBlank(String fieldName, String value) {
            if (value == null || value.isBlank()) {
                errors.add(fieldName + " cannot be blank");
            }
            return this;
        }
        
        public ValidationBuilder requireRange(String fieldName, int value, int min, int max) {
            if (value < min || value > max) {
                errors.add(fieldName + " must be between " + min + " and " + max);
            }
            return this;
        }
        
        public ValidationBuilder requireRange(String fieldName, long value, long min, long max) {
            if (value < min || value > max) {
                errors.add(fieldName + " must be between " + min + " and " + max);
            }
            return this;
        }
        
        public ValidationBuilder requirePositive(String fieldName, int value) {
            if (value <= 0) {
                errors.add(fieldName + " must be positive");
            }
            return this;
        }
        
        public ValidationBuilder requireNonNegative(String fieldName, int value) {
            if (value < 0) {
                errors.add(fieldName + " cannot be negative");
            }
            return this;
        }
        
        public <T> ValidationBuilder check(String message, T value, Predicate<T> predicate) {
            if (!predicate.test(value)) {
                errors.add(message);
            }
            return this;
        }
        
        public List<String> build() {
            return errors;
        }
    }
}
