package com.yellowtale.rubidium.api;

import com.yellowtale.rubidium.api.command.CommandManager;
import com.yellowtale.rubidium.api.config.PluginConfig;
import com.yellowtale.rubidium.api.event.EventBus;
import com.yellowtale.rubidium.api.logging.PluginLogger;
import com.yellowtale.rubidium.api.scheduler.TaskScheduler;
import com.yellowtale.rubidium.optimization.OptimizationContext;
import com.yellowtale.rubidium.integration.YellowTaleAPI;

import java.io.File;
import java.io.InputStream;

public abstract class RubidiumPlugin {
    
    private PluginDescriptor descriptor;
    private PluginLogger logger;
    private File dataFolder;
    private PluginConfig config;
    private boolean enabled;
    
    private EventBus eventBus;
    private CommandManager commandManager;
    private TaskScheduler scheduler;
    private OptimizationContext optimization;
    private YellowTaleAPI yellowTale;
    
    public final void initialize(
            PluginDescriptor descriptor,
            File dataFolder,
            EventBus eventBus,
            CommandManager commandManager,
            TaskScheduler scheduler,
            OptimizationContext optimization,
            YellowTaleAPI yellowTale
    ) {
        this.descriptor = descriptor;
        this.dataFolder = dataFolder;
        this.logger = new PluginLogger(descriptor.getId());
        this.config = new PluginConfig(new File(dataFolder, "config.yml"));
        this.eventBus = eventBus;
        this.commandManager = commandManager;
        this.scheduler = scheduler;
        this.optimization = optimization;
        this.yellowTale = yellowTale;
    }
    
    public abstract void onEnable();
    
    public void onDisable() {}
    
    public void onLoad() {}
    
    public void onReload() {
        config.reload();
    }
    
    public final PluginDescriptor getDescriptor() {
        return descriptor;
    }
    
    public final String getId() {
        return descriptor.getId();
    }
    
    public final String getName() {
        return descriptor.getName();
    }
    
    public final String getVersion() {
        return descriptor.getVersion();
    }
    
    public final PluginLogger getLogger() {
        return logger;
    }
    
    public final File getDataFolder() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        return dataFolder;
    }
    
    public final PluginConfig getConfig() {
        return config;
    }
    
    public final void saveDefaultConfig() {
        if (!config.getFile().exists()) {
            InputStream defaultConfig = getResource("config.yml");
            if (defaultConfig != null) {
                config.saveFromStream(defaultConfig);
            }
        }
    }
    
    public final InputStream getResource(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }
    
    public final EventBus getEventBus() {
        return eventBus;
    }
    
    public final CommandManager getCommandManager() {
        return commandManager;
    }
    
    public final TaskScheduler getScheduler() {
        return scheduler;
    }
    
    public final OptimizationContext getOptimization() {
        return optimization;
    }
    
    public final YellowTaleAPI getYellowTale() {
        return yellowTale;
    }
    
    public final boolean isEnabled() {
        return enabled;
    }
    
    final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public final void registerEvents(Object listener) {
        eventBus.registerListener(this, listener);
    }
    
    public final void registerCommand(String name, Object handler) {
        commandManager.registerCommand(this, name, handler);
    }
}
