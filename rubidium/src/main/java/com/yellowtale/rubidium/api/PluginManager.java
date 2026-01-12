package com.yellowtale.rubidium.api;

import com.yellowtale.rubidium.RubidiumPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginManager {
    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);
    
    private final RubidiumPlugin rubidium;
    private final Map<String, Plugin> plugins = new ConcurrentHashMap<>();
    private final Map<String, PluginDescriptor> descriptors = new ConcurrentHashMap<>();
    private final Map<String, PluginState> states = new ConcurrentHashMap<>();
    
    public PluginManager(RubidiumPlugin rubidium) {
        this.rubidium = rubidium;
    }
    
    public void discoverPlugins(Path pluginsDir) {
        if (!Files.exists(pluginsDir)) {
            return;
        }
        
        try {
            Files.list(pluginsDir)
                .filter(p -> p.toString().endsWith(".jar"))
                .forEach(this::discoverPlugin);
        } catch (Exception e) {
            logger.error("Failed to discover plugins", e);
        }
    }
    
    private void discoverPlugin(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry("rubidium-plugin.json");
            if (entry == null) {
                entry = jar.getJarEntry("plugin.json");
            }
            
            if (entry != null) {
                try (InputStream is = jar.getInputStream(entry)) {
                    String json = new String(is.readAllBytes());
                    PluginDescriptor descriptor = PluginDescriptor.fromJson(json);
                    descriptor.setJarPath(jarPath);
                    descriptors.put(descriptor.getName(), descriptor);
                    states.put(descriptor.getName(), PluginState.DISCOVERED);
                    logger.info("Discovered plugin: {} v{}", descriptor.getName(), descriptor.getVersion());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to read plugin: {}", jarPath, e);
        }
    }
    
    public void loadAll() {
        List<String> sorted = resolveDependencies();
        
        for (String name : sorted) {
            loadPlugin(name);
        }
    }
    
    public void loadPlugin(String name) {
        PluginDescriptor descriptor = descriptors.get(name);
        if (descriptor == null) {
            logger.warn("Plugin not found: {}", name);
            return;
        }
        
        if (states.get(name) != PluginState.DISCOVERED) {
            return;
        }
        
        try {
            URLClassLoader loader = new URLClassLoader(
                new URL[]{descriptor.getJarPath().toUri().toURL()},
                getClass().getClassLoader()
            );
            
            Class<?> mainClass = loader.loadClass(descriptor.getMainClass());
            Plugin plugin = (Plugin) mainClass.getDeclaredConstructor().newInstance();
            
            plugins.put(name, plugin);
            states.put(name, PluginState.LOADED);
            
            plugin.onLoad();
            logger.info("Loaded plugin: {}", name);
            
        } catch (Exception e) {
            states.put(name, PluginState.FAILED);
            logger.error("Failed to load plugin: {}", name, e);
        }
    }
    
    public void enableAll() {
        for (String name : plugins.keySet()) {
            enablePlugin(name);
        }
    }
    
    public void enablePlugin(String name) {
        Plugin plugin = plugins.get(name);
        if (plugin == null) return;
        
        if (states.get(name) != PluginState.LOADED) return;
        
        try {
            plugin.onEnable();
            states.put(name, PluginState.ENABLED);
            logger.info("Enabled plugin: {}", name);
        } catch (Exception e) {
            states.put(name, PluginState.FAILED);
            logger.error("Failed to enable plugin: {}", name, e);
        }
    }
    
    public void disableAll() {
        List<String> reversed = new ArrayList<>(plugins.keySet());
        Collections.reverse(reversed);
        
        for (String name : reversed) {
            disablePlugin(name);
        }
    }
    
    public void disablePlugin(String name) {
        Plugin plugin = plugins.get(name);
        if (plugin == null) return;
        
        if (states.get(name) != PluginState.ENABLED) return;
        
        try {
            plugin.onDisable();
            states.put(name, PluginState.DISABLED);
            logger.info("Disabled plugin: {}", name);
        } catch (Exception e) {
            logger.error("Error disabling plugin: {}", name, e);
        }
    }
    
    public void unloadPlugin(String name) {
        disablePlugin(name);
        plugins.remove(name);
        states.put(name, PluginState.UNLOADED);
    }
    
    private List<String> resolveDependencies() {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        for (String name : descriptors.keySet()) {
            topologicalSort(name, result, visited, visiting);
        }
        
        return result;
    }
    
    private void topologicalSort(String name, List<String> result, Set<String> visited, Set<String> visiting) {
        if (visited.contains(name)) return;
        if (visiting.contains(name)) {
            logger.warn("Circular dependency detected for: {}", name);
            return;
        }
        
        visiting.add(name);
        
        PluginDescriptor desc = descriptors.get(name);
        if (desc != null) {
            for (String dep : desc.getDependencies()) {
                topologicalSort(dep, result, visited, visiting);
            }
        }
        
        visiting.remove(name);
        visited.add(name);
        result.add(name);
    }
    
    public Plugin getPlugin(String name) {
        return plugins.get(name);
    }
    
    public Collection<Plugin> getLoadedPlugins() {
        return Collections.unmodifiableCollection(plugins.values());
    }
    
    public PluginState getState(String name) {
        return states.getOrDefault(name, PluginState.UNKNOWN);
    }
    
    public enum PluginState {
        UNKNOWN,
        DISCOVERED,
        LOADED,
        ENABLED,
        DISABLED,
        UNLOADED,
        FAILED
    }
}
