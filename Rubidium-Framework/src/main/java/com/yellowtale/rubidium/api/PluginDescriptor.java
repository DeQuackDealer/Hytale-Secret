package com.yellowtale.rubidium.api;

import java.util.List;
import java.util.Collections;

public class PluginDescriptor {
    
    private final String id;
    private final String name;
    private final String version;
    private final String author;
    private final String description;
    private final String mainClass;
    private final String apiVersion;
    private final List<PluginDependency> dependencies;
    private final List<String> softDependencies;
    private final List<String> loadBefore;
    
    private PluginDescriptor(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.version = builder.version;
        this.author = builder.author;
        this.description = builder.description;
        this.mainClass = builder.mainClass;
        this.apiVersion = builder.apiVersion;
        this.dependencies = Collections.unmodifiableList(builder.dependencies);
        this.softDependencies = Collections.unmodifiableList(builder.softDependencies);
        this.loadBefore = Collections.unmodifiableList(builder.loadBefore);
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getMainClass() {
        return mainClass;
    }
    
    public String getApiVersion() {
        return apiVersion;
    }
    
    public List<PluginDependency> getDependencies() {
        return dependencies;
    }
    
    public List<String> getSoftDependencies() {
        return softDependencies;
    }
    
    public List<String> getLoadBefore() {
        return loadBefore;
    }
    
    public static Builder builder(String id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final String id;
        private String name;
        private String version = "1.0.0";
        private String author = "Unknown";
        private String description = "";
        private String mainClass;
        private String apiVersion = "1.0.0";
        private List<PluginDependency> dependencies = Collections.emptyList();
        private List<String> softDependencies = Collections.emptyList();
        private List<String> loadBefore = Collections.emptyList();
        
        private Builder(String id) {
            this.id = id;
            this.name = id;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        public Builder author(String author) {
            this.author = author;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder mainClass(String mainClass) {
            this.mainClass = mainClass;
            return this;
        }
        
        public Builder apiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }
        
        public Builder dependencies(List<PluginDependency> dependencies) {
            this.dependencies = dependencies;
            return this;
        }
        
        public Builder softDependencies(List<String> softDependencies) {
            this.softDependencies = softDependencies;
            return this;
        }
        
        public Builder loadBefore(List<String> loadBefore) {
            this.loadBefore = loadBefore;
            return this;
        }
        
        public PluginDescriptor build() {
            return new PluginDescriptor(this);
        }
    }
}
