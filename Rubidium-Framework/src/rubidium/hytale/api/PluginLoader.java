package rubidium.hytale.api;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.logging.Logger;

/**
 * Plugin loader that discovers and loads JavaPlugin implementations.
 * Used by the server to initialize plugins from JAR files.
 */
public class PluginLoader {
    
    private static final Logger logger = Logger.getLogger("Rubidium-PluginLoader");
    
    private final Path pluginsDirectory;
    private final List<JavaPlugin> loadedPlugins;
    private final Map<String, JavaPlugin> pluginsByName;
    
    public PluginLoader(Path pluginsDirectory) {
        this.pluginsDirectory = pluginsDirectory;
        this.loadedPlugins = new ArrayList<>();
        this.pluginsByName = new HashMap<>();
    }
    
    public void loadPlugins() throws IOException {
        if (!Files.exists(pluginsDirectory)) {
            Files.createDirectories(pluginsDirectory);
            return;
        }
        
        try (var stream = Files.list(pluginsDirectory)) {
            stream.filter(p -> p.toString().endsWith(".jar"))
                  .forEach(this::loadPlugin);
        }
    }
    
    private void loadPlugin(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            PluginMetadata meta = readPluginDescriptor(jar);
            if (meta == null) {
                logger.warning("No plugin descriptor found in: " + jarPath.getFileName());
                return;
            }
            
            ClassLoader loader = new PluginClassLoader(jarPath, getClass().getClassLoader());
            Class<?> mainClass = loader.loadClass(meta.mainClass());
            
            if (!JavaPlugin.class.isAssignableFrom(mainClass)) {
                logger.warning("Main class does not extend JavaPlugin: " + meta.mainClass());
                return;
            }
            
            JavaPlugin plugin = (JavaPlugin) mainClass.getDeclaredConstructor().newInstance();
            plugin.initializeMetadata(meta);
            
            loadedPlugins.add(plugin);
            pluginsByName.put(meta.name(), plugin);
            
            logger.info("Loaded plugin: " + meta.name() + " v" + meta.version());
            
        } catch (Exception e) {
            logger.severe("Failed to load plugin: " + jarPath.getFileName() + " - " + e.getMessage());
        }
    }
    
    private PluginMetadata readPluginDescriptor(JarFile jar) throws IOException {
        JarEntry jsonEntry = jar.getJarEntry("plugin.json");
        if (jsonEntry != null) {
            return parsePluginJson(jar.getInputStream(jsonEntry));
        }
        
        JarEntry ymlEntry = jar.getJarEntry("plugin.yml");
        if (ymlEntry != null) {
            return parsePluginYml(jar.getInputStream(ymlEntry));
        }
        
        return null;
    }
    
    private PluginMetadata parsePluginJson(InputStream in) throws IOException {
        String json = new String(in.readAllBytes());
        String name = extractJsonString(json, "name");
        String version = extractJsonString(json, "version");
        String mainClass = extractJsonString(json, "main");
        String description = extractJsonString(json, "description");
        
        return new PluginMetadata(
            name != null ? name : "Unknown",
            version != null ? version : "1.0.0",
            mainClass != null ? mainClass : "",
            description != null ? description : ""
        );
    }
    
    private PluginMetadata parsePluginYml(InputStream in) throws IOException {
        String yml = new String(in.readAllBytes());
        String name = extractYmlValue(yml, "name");
        String version = extractYmlValue(yml, "version");
        String mainClass = extractYmlValue(yml, "main");
        String description = extractYmlValue(yml, "description");
        
        return new PluginMetadata(
            name != null ? name : "Unknown",
            version != null ? version : "1.0.0",
            mainClass != null ? mainClass : "",
            description != null ? description : ""
        );
    }
    
    private String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? m.group(1) : null;
    }
    
    private String extractYmlValue(String yml, String key) {
        String pattern = "^" + key + ":\\s*(.+)$";
        for (String line : yml.split("\n")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(line.trim());
            if (m.find()) {
                return m.group(1).trim().replaceAll("^[\"']|[\"']$", "");
            }
        }
        return null;
    }
    
    public void enablePlugins() {
        for (JavaPlugin plugin : loadedPlugins) {
            try {
                plugin.onLoad();
            } catch (Exception e) {
                logger.severe("Error loading " + plugin.getName() + ": " + e.getMessage());
            }
        }
        
        for (JavaPlugin plugin : loadedPlugins) {
            try {
                plugin.onEnable();
                plugin.setEnabled(true);
                logger.info("Enabled: " + plugin.getName());
            } catch (Exception e) {
                logger.severe("Error enabling " + plugin.getName() + ": " + e.getMessage());
            }
        }
    }
    
    public void disablePlugins() {
        for (int i = loadedPlugins.size() - 1; i >= 0; i--) {
            JavaPlugin plugin = loadedPlugins.get(i);
            try {
                plugin.onDisable();
                plugin.setEnabled(false);
                logger.info("Disabled: " + plugin.getName());
            } catch (Exception e) {
                logger.severe("Error disabling " + plugin.getName() + ": " + e.getMessage());
            }
        }
    }
    
    public Optional<JavaPlugin> getPlugin(String name) {
        return Optional.ofNullable(pluginsByName.get(name));
    }
    
    public List<JavaPlugin> getLoadedPlugins() {
        return Collections.unmodifiableList(loadedPlugins);
    }
    
    private static class PluginClassLoader extends ClassLoader {
        private final Path jarPath;
        
        PluginClassLoader(Path jarPath, ClassLoader parent) {
            super(parent);
            this.jarPath = jarPath;
        }
        
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try (JarFile jar = new JarFile(jarPath.toFile())) {
                String path = name.replace('.', '/') + ".class";
                JarEntry entry = jar.getJarEntry(path);
                if (entry != null) {
                    byte[] bytes = jar.getInputStream(entry).readAllBytes();
                    return defineClass(name, bytes, 0, bytes.length);
                }
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
            throw new ClassNotFoundException(name);
        }
    }
}
