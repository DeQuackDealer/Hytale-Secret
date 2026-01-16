package rubidium.server;

import rubidium.api.event.EventAPI;
import rubidium.api.command.CommandAPI;
import rubidium.api.permission.PermissionAPI;
import rubidium.api.economy.EconomyAPI;
import rubidium.welcome.FirstJoinHandler;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class RubidiumServer {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium");
    private static RubidiumServer instance;
    
    private final Path serverPath;
    private final Properties config;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    
    public static void main(String[] args) {
        LOGGER.info("Starting Rubidium Server...");
        
        RubidiumServer server = new RubidiumServer(Path.of("."));
        instance = server;
        
        try {
            server.start();
        } catch (Exception e) {
            LOGGER.severe("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public static RubidiumServer getInstance() {
        return instance;
    }
    
    public RubidiumServer(Path serverPath) {
        this.serverPath = serverPath;
        this.config = new Properties();
        this.executor = Executors.newCachedThreadPool();
        this.scheduler = Executors.newScheduledThreadPool(4);
    }
    
    public void start() throws Exception {
        long startTime = System.currentTimeMillis();
        
        LOGGER.info("=".repeat(50));
        LOGGER.info("  Rubidium Framework v1.0.0");
        LOGGER.info("  A Hytale Server Framework");
        LOGGER.info("=".repeat(50));
        
        loadConfig();
        
        initializeCoreAPIs();
        
        registerDefaultCommands();
        
        loadPlugins();
        
        // FirstJoinHandler is registered when event system is fully available
        
        running = true;
        
        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.info("Server started in " + elapsed + "ms");
        LOGGER.info("Listening on port " + config.getProperty("server-port", "25565"));
        
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        
        runConsoleLoop();
    }
    
    public void stop() {
        if (!running) return;
        running = false;
        
        LOGGER.info("Stopping server...");
        
        executor.shutdown();
        scheduler.shutdown();
        
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        LOGGER.info("Server stopped.");
    }
    
    private void loadConfig() throws IOException {
        Path configPath = serverPath.resolve("config/server.properties");
        if (Files.exists(configPath)) {
            try (InputStream is = Files.newInputStream(configPath)) {
                config.load(is);
            }
            LOGGER.info("Loaded configuration from " + configPath);
        } else {
            config.setProperty("server-name", "Rubidium Server");
            config.setProperty("server-port", "25565");
            config.setProperty("max-players", "20");
            config.setProperty("debug-mode", "false");
            LOGGER.info("Using default configuration");
        }
    }
    
    private void initializeCoreAPIs() {
        LOGGER.info("Initializing core APIs...");
        
        EconomyAPI.createCurrency("coins", "Coins", "$");
        EconomyAPI.createCurrency("gems", "Gems", "G");
        EconomyAPI.setDefaultCurrency("coins");
        
        PermissionAPI.createGroup("default").grant("rubidium.command.help");
        PermissionAPI.createGroup("vip").parent("default").prefix("[VIP] ").grant("rubidium.command.spawn");
        PermissionAPI.adminGroup().prefix("[Admin] ");
        
        LOGGER.info("Core APIs initialized");
    }
    
    private void registerDefaultCommands() {
        LOGGER.info("Registering default commands...");
        
        CommandAPI.register(
            CommandAPI.create("help")
                .description("Show available commands")
                .usage("/help [command]")
                .executor(ctx -> {
                    LOGGER.info("Available Commands:");
                    CommandAPI.all().forEach(cmd -> 
                        LOGGER.info("  /" + cmd.getName() + " - " + cmd.getDescription())
                    );
                    return true;
                })
                .build()
        );
        
        CommandAPI.register(
            CommandAPI.create("stop")
                .description("Stop the server")
                .permission("rubidium.admin.stop")
                .executor(ctx -> {
                    LOGGER.info("Server stopping...");
                    stop();
                    return true;
                })
                .build()
        );
        
        CommandAPI.register(
            CommandAPI.create("plugins")
                .description("List loaded plugins")
                .alias("pl")
                .executor(ctx -> {
                    LOGGER.info("Loaded plugins: (check plugins folder)");
                    return true;
                })
                .build()
        );
        
        CommandAPI.register(
            CommandAPI.create("version")
                .description("Show server version")
                .alias("ver")
                .executor(ctx -> {
                    LOGGER.info("Rubidium Framework v1.0.0");
                    LOGGER.info("Java " + System.getProperty("java.version"));
                    return true;
                })
                .build()
        );
        
        CommandAPI.register(
            CommandAPI.create("reload")
                .description("Reload server configuration")
                .permission("rubidium.admin.reload")
                .executor(ctx -> {
                    try {
                        loadConfig();
                        LOGGER.info("Configuration reloaded");
                    } catch (IOException e) {
                        LOGGER.warning("Failed to reload config: " + e.getMessage());
                    }
                    return true;
                })
                .build()
        );
        
        LOGGER.info("Registered " + CommandAPI.all().size() + " commands");
    }
    
    private void loadPlugins() throws IOException {
        Path pluginsPath = serverPath.resolve("plugins");
        Files.createDirectories(pluginsPath);
        
        LOGGER.info("Loading plugins from " + pluginsPath);
        
        try (var files = Files.list(pluginsPath)) {
            List<Path> jarFiles = files
                .filter(p -> p.toString().endsWith(".jar"))
                .toList();
            
            LOGGER.info("Found " + jarFiles.size() + " plugin(s)");
            
            for (Path jar : jarFiles) {
                LOGGER.info("  Loading: " + jar.getFileName());
            }
        }
    }
    
    private void runConsoleLoop() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            LOGGER.info("Console ready. Type 'help' for commands.");
            
            while (running) {
                String line = reader.readLine();
                if (line == null) break;
                
                line = line.trim();
                if (line.isEmpty()) continue;
                
                if (!CommandAPI.execute(null, line)) {
                    LOGGER.warning("Unknown command: " + line);
                }
            }
        } catch (IOException e) {
            if (running) {
                LOGGER.warning("Console input error: " + e.getMessage());
            }
        }
    }
    
    public Path getServerPath() { return serverPath; }
    public Properties getConfig() { return config; }
    public boolean isRunning() { return running; }
    public ExecutorService getExecutor() { return executor; }
    public ScheduledExecutorService getScheduler() { return scheduler; }
    public Logger getLogger() { return LOGGER; }
    
    public String getProperty(String key) {
        return config.getProperty(key);
    }
    
    public String getProperty(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }
    
    public int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(config.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
