package com.yellowtale.rubidium.core.module;

import com.yellowtale.rubidium.core.RubidiumCore;
import com.yellowtale.rubidium.core.config.ConfigManager;
import com.yellowtale.rubidium.core.lifecycle.LifecycleManager;
import com.yellowtale.rubidium.core.logging.LogManager;
import com.yellowtale.rubidium.core.logging.RubidiumLogger;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * Manages the discovery, loading, and lifecycle of Rubidium modules.
 * 
 * Features:
 * - Runtime loading/unloading of modules
 * - Dependency resolution with topological sorting
 * - Isolated classloaders per module
 * - Safe enable/disable with rollback on failure
 */
public final class ModuleManager {
    
    private final Path modulesDirectory;
    private final LifecycleManager lifecycleManager;
    private final ConfigManager configManager;
    private final LogManager logManager;
    private final RubidiumLogger logger;
    
    private final Map<String, ModuleContainer> modules = new ConcurrentHashMap<>();
    private final Map<String, URLClassLoader> classLoaders = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public ModuleManager(
        Path modulesDirectory,
        LifecycleManager lifecycleManager,
        ConfigManager configManager,
        LogManager logManager
    ) {
        this.modulesDirectory = modulesDirectory;
        this.lifecycleManager = lifecycleManager;
        this.configManager = configManager;
        this.logManager = logManager;
        this.logger = logManager.getLogger("ModuleManager");
    }
    
    /**
     * Discover and load all modules from the modules directory.
     */
    public void discoverAndLoadModules() {
        logger.info("Discovering modules in {}", modulesDirectory);
        
        try {
            Files.createDirectories(modulesDirectory);
        } catch (IOException e) {
            logger.error("Failed to create modules directory", e);
            return;
        }
        
        List<Path> jarFiles;
        try (var stream = Files.list(modulesDirectory)) {
            jarFiles = stream
                .filter(p -> p.toString().endsWith(".jar"))
                .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to list modules directory", e);
            return;
        }
        
        logger.info("Found {} module JAR files", jarFiles.size());
        
        List<ModuleDescriptor> descriptors = new ArrayList<>();
        for (Path jarPath : jarFiles) {
            try {
                ModuleDescriptor descriptor = parseModuleDescriptor(jarPath);
                if (descriptor != null) {
                    descriptors.add(descriptor);
                }
            } catch (Exception e) {
                logger.error("Failed to parse module: {}", jarPath.getFileName(), e);
            }
        }
        
        List<ModuleDescriptor> sorted = resolveDependencies(descriptors);
        
        for (ModuleDescriptor descriptor : sorted) {
            loadModule(descriptor);
        }
        
        for (String moduleId : getLoadedModuleIds()) {
            enableModule(moduleId);
        }
        
        logger.info("Loaded and enabled {} modules", modules.size());
    }
    
    private ModuleDescriptor parseModuleDescriptor(Path jarPath) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                logger.warn("No manifest in {}", jarPath.getFileName());
                return null;
            }
            
            var attrs = manifest.getMainAttributes();
            String moduleClass = attrs.getValue("Rubidium-Module-Class");
            if (moduleClass == null) {
                logger.warn("No Rubidium-Module-Class in {}", jarPath.getFileName());
                return null;
            }
            
            String id = attrs.getValue("Rubidium-Module-Id");
            String version = attrs.getValue("Rubidium-Module-Version");
            String dependencies = attrs.getValue("Rubidium-Dependencies");
            String softDependencies = attrs.getValue("Rubidium-Soft-Dependencies");
            
            return new ModuleDescriptor(
                jarPath,
                id != null ? id : moduleClass.substring(moduleClass.lastIndexOf('.') + 1).toLowerCase(),
                version != null ? version : "1.0.0",
                moduleClass,
                parseDependencyList(dependencies),
                parseDependencyList(softDependencies)
            );
        }
    }
    
    private Set<String> parseDependencyList(String deps) {
        if (deps == null || deps.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(deps.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }
    
    private List<ModuleDescriptor> resolveDependencies(List<ModuleDescriptor> descriptors) {
        Map<String, ModuleDescriptor> byId = new HashMap<>();
        for (ModuleDescriptor desc : descriptors) {
            byId.put(desc.id(), desc);
        }
        
        List<ModuleDescriptor> sorted = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        for (ModuleDescriptor desc : descriptors) {
            if (!visited.contains(desc.id())) {
                if (!topologicalSort(desc, byId, visited, visiting, sorted)) {
                    logger.error("Circular dependency detected involving {}", desc.id());
                }
            }
        }
        
        return sorted;
    }
    
    private boolean topologicalSort(
        ModuleDescriptor desc,
        Map<String, ModuleDescriptor> byId,
        Set<String> visited,
        Set<String> visiting,
        List<ModuleDescriptor> result
    ) {
        if (visiting.contains(desc.id())) {
            return false;
        }
        if (visited.contains(desc.id())) {
            return true;
        }
        
        visiting.add(desc.id());
        
        for (String depId : desc.hardDependencies()) {
            ModuleDescriptor dep = byId.get(depId);
            if (dep != null) {
                if (!topologicalSort(dep, byId, visited, visiting, result)) {
                    return false;
                }
            } else {
                logger.warn("Module {} requires missing dependency: {}", desc.id(), depId);
            }
        }
        
        for (String depId : desc.softDependencies()) {
            ModuleDescriptor dep = byId.get(depId);
            if (dep != null) {
                topologicalSort(dep, byId, visited, visiting, result);
            }
        }
        
        visiting.remove(desc.id());
        visited.add(desc.id());
        result.add(desc);
        return true;
    }
    
    /**
     * Load a module from its descriptor.
     */
    public boolean loadModule(ModuleDescriptor descriptor) {
        lock.writeLock().lock();
        try {
            if (modules.containsKey(descriptor.id())) {
                logger.warn("Module {} is already loaded", descriptor.id());
                return false;
            }
            
            logger.info("Loading module: {} v{}", descriptor.id(), descriptor.version());
            
            for (String depId : descriptor.hardDependencies()) {
                if (!modules.containsKey(depId)) {
                    logger.error("Cannot load {}: missing dependency {}", descriptor.id(), depId);
                    return false;
                }
            }
            
            URL[] urls = new URL[] { descriptor.jarPath().toUri().toURL() };
            URLClassLoader classLoader = new URLClassLoader(
                urls,
                getClass().getClassLoader()
            );
            
            Class<?> moduleClass = classLoader.loadClass(descriptor.moduleClass());
            if (!Module.class.isAssignableFrom(moduleClass)) {
                logger.error("Class {} does not implement Module", descriptor.moduleClass());
                classLoader.close();
                return false;
            }
            
            Module module = (Module) moduleClass.getDeclaredConstructor().newInstance();
            
            Path moduleDataDir = modulesDirectory.resolve(descriptor.id());
            Files.createDirectories(moduleDataDir);
            
            RubidiumLogger moduleLogger = logManager.getLogger("Module:" + descriptor.id());
            ModuleContext context = new ModuleContext(
                descriptor.id(),
                moduleDataDir,
                moduleLogger,
                configManager,
                RubidiumCore.getInstance().getScheduler(),
                RubidiumCore.getInstance().getMetricsRegistry(),
                this
            );
            
            module.onLoad(context);
            
            ModuleContainer container = new ModuleContainer(
                descriptor,
                module,
                context,
                ModuleState.LOADED
            );
            
            modules.put(descriptor.id(), container);
            classLoaders.put(descriptor.id(), classLoader);
            
            logger.info("Module {} loaded successfully", descriptor.id());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to load module {}", descriptor.id(), e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Enable a loaded module.
     */
    public boolean enableModule(String moduleId) {
        lock.writeLock().lock();
        try {
            ModuleContainer container = modules.get(moduleId);
            if (container == null) {
                logger.error("Cannot enable unknown module: {}", moduleId);
                return false;
            }
            
            if (!container.state().canEnable()) {
                logger.warn("Module {} cannot be enabled from state {}", moduleId, container.state());
                return false;
            }
            
            logger.info("Enabling module: {}", moduleId);
            container = container.withState(ModuleState.ENABLING);
            modules.put(moduleId, container);
            
            try {
                container.module().onEnable();
                container = container.withState(ModuleState.ENABLED);
                modules.put(moduleId, container);
                logger.info("Module {} enabled", moduleId);
                return true;
            } catch (Exception e) {
                logger.error("Failed to enable module {}", moduleId, e);
                container = container.withState(ModuleState.FAILED);
                modules.put(moduleId, container);
                return false;
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Disable an enabled module.
     */
    public boolean disableModule(String moduleId) {
        lock.writeLock().lock();
        try {
            ModuleContainer container = modules.get(moduleId);
            if (container == null) {
                return false;
            }
            
            if (!container.state().canDisable()) {
                logger.warn("Module {} cannot be disabled from state {}", moduleId, container.state());
                return false;
            }
            
            logger.info("Disabling module: {}", moduleId);
            container = container.withState(ModuleState.DISABLING);
            modules.put(moduleId, container);
            
            try {
                container.module().onDisable();
                container = container.withState(ModuleState.DISABLED);
                modules.put(moduleId, container);
                logger.info("Module {} disabled", moduleId);
                return true;
            } catch (Exception e) {
                logger.error("Error disabling module {}", moduleId, e);
                container = container.withState(ModuleState.DISABLED);
                modules.put(moduleId, container);
                return true;
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Unload a module completely, removing it from memory.
     */
    public boolean unloadModule(String moduleId) {
        lock.writeLock().lock();
        try {
            ModuleContainer container = modules.get(moduleId);
            if (container == null) {
                return false;
            }
            
            logger.info("Unloading module: {}", moduleId);
            container = container.withState(ModuleState.UNLOADING);
            modules.put(moduleId, container);
            
            if (container.state().isActive() || container.state() == ModuleState.UNLOADING) {
                try {
                    container.module().onDisable();
                    logger.debug("Module {} disabled during unload", moduleId);
                } catch (Exception e) {
                    logger.error("Error disabling module {} during unload", moduleId, e);
                }
            }
            
            modules.remove(moduleId);
            
            URLClassLoader classLoader = classLoaders.remove(moduleId);
            if (classLoader != null) {
                try {
                    classLoader.close();
                    logger.debug("Classloader closed for {}", moduleId);
                } catch (IOException e) {
                    logger.warn("Error closing classloader for {}", moduleId, e);
                }
            }
            
            System.gc();
            
            logger.info("Module {} unloaded", moduleId);
            return true;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Reload a module's configuration.
     */
    public boolean reloadModule(String moduleId) {
        lock.readLock().lock();
        try {
            ModuleContainer container = modules.get(moduleId);
            if (container == null) {
                return false;
            }
            
            if (!container.module().supportsReload()) {
                logger.warn("Module {} does not support reload", moduleId);
                return false;
            }
            
            logger.info("Reloading module: {}", moduleId);
            container.module().onReload();
            logger.info("Module {} reloaded", moduleId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to reload module {}", moduleId, e);
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Reload all modules.
     */
    public void reloadAllModules() {
        for (String moduleId : getEnabledModuleIds()) {
            reloadModule(moduleId);
        }
    }
    
    /**
     * Unload all modules in reverse dependency order.
     */
    public void unloadAllModules() {
        List<String> moduleIds = new ArrayList<>(modules.keySet());
        Collections.reverse(moduleIds);
        
        for (String moduleId : moduleIds) {
            unloadModule(moduleId);
        }
    }
    
    public boolean isModuleLoaded(String moduleId) {
        return modules.containsKey(moduleId);
    }
    
    public boolean isModuleEnabled(String moduleId) {
        ModuleContainer container = modules.get(moduleId);
        return container != null && container.state().isActive();
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(String moduleId, Class<T> type) {
        ModuleContainer container = modules.get(moduleId);
        if (container != null && type.isInstance(container.module())) {
            return (T) container.module();
        }
        return null;
    }
    
    public Set<String> getLoadedModuleIds() {
        return Set.copyOf(modules.keySet());
    }
    
    public Set<String> getEnabledModuleIds() {
        return modules.entrySet().stream()
            .filter(e -> e.getValue().state().isActive())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }
    
    public ModuleState getModuleState(String moduleId) {
        ModuleContainer container = modules.get(moduleId);
        return container != null ? container.state() : null;
    }
    
    public Collection<ModuleInfo> getModuleInfos() {
        return modules.values().stream()
            .map(c -> new ModuleInfo(
                c.descriptor().id(),
                c.module().getDisplayName(),
                c.module().getVersion(),
                c.module().getDescription(),
                c.state()
            ))
            .collect(Collectors.toList());
    }
}
