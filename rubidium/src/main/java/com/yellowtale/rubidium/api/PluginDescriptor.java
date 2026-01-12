package com.yellowtale.rubidium.api;

import com.google.gson.Gson;

import java.nio.file.Path;
import java.util.List;

public class PluginDescriptor {
    private static final Gson gson = new Gson();
    
    private String name;
    private String version;
    private String main;
    private String description;
    private List<String> authors;
    private List<String> dependencies;
    private List<String> softDependencies;
    private transient Path jarPath;
    
    public static PluginDescriptor fromJson(String json) {
        return gson.fromJson(json, PluginDescriptor.class);
    }
    
    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getMainClass() {
        return main;
    }
    
    public String getDescription() {
        return description != null ? description : "";
    }
    
    public String[] getAuthors() {
        return authors != null ? authors.toArray(new String[0]) : new String[0];
    }
    
    public String[] getDependencies() {
        return dependencies != null ? dependencies.toArray(new String[0]) : new String[0];
    }
    
    public String[] getSoftDependencies() {
        return softDependencies != null ? softDependencies.toArray(new String[0]) : new String[0];
    }
    
    public Path getJarPath() {
        return jarPath;
    }
    
    public void setJarPath(Path jarPath) {
        this.jarPath = jarPath;
    }
}
