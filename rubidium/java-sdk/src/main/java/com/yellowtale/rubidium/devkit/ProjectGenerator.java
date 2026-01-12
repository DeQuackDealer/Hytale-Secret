package com.yellowtale.rubidium.devkit;

import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.HashMap;

public class ProjectGenerator {
    
    public static void generateJavaPlugin(Path outputDir, PluginTemplate template) throws IOException {
        Files.createDirectories(outputDir);
        
        String packagePath = template.packageName.replace('.', '/');
        Path srcDir = outputDir.resolve("src/main/java/" + packagePath);
        Path resourcesDir = outputDir.resolve("src/main/resources");
        Files.createDirectories(srcDir);
        Files.createDirectories(resourcesDir);
        
        generateMainClass(srcDir, template);
        generatePluginToml(resourcesDir, template);
        generateBuildGradle(outputDir, template);
        generateGitignore(outputDir);
    }
    
    private static void generateMainClass(Path srcDir, PluginTemplate template) throws IOException {
        String className = toPascalCase(template.pluginId) + "Plugin";
        String content = """
            package %s;
            
            import com.yellowtale.rubidium.api.*;
            import com.yellowtale.rubidium.annotations.*;
            import com.yellowtale.rubidium.api.event.*;
            import com.yellowtale.rubidium.api.event.player.*;
            import com.yellowtale.rubidium.api.player.*;
            
            @Plugin(
                id = "%s",
                name = "%s",
                version = "%s",
                author = "%s",
                description = "%s"
            )
            public class %s extends RubidiumPlugin {
                
                @Override
                public void onEnable() {
                    saveDefaultConfig();
                    getLogger().info("%s has been enabled!");
                    
                    registerEvents(this);
                }
                
                @Override
                public void onDisable() {
                    getLogger().info("%s has been disabled!");
                }
                
                @EventHandler(priority = EventPriority.NORMAL)
                public void onPlayerJoin(PlayerJoinEvent event) {
                    Player player = event.getPlayer();
                    player.sendMessage("Welcome to the server, " + player.getName() + "!");
                }
                
                @Command(
                    name = "%s",
                    description = "Main command for %s",
                    permission = "%s.use"
                )
                public void mainCommand(CommandSender sender, String[] args) {
                    sender.sendMessage("%s v%s by %s");
                }
            }
            """.formatted(
                template.packageName,
                template.pluginId,
                template.pluginName,
                template.version,
                template.author,
                template.description,
                className,
                template.pluginName,
                template.pluginName,
                template.pluginId,
                template.pluginName,
                template.pluginId,
                template.pluginName,
                template.version,
                template.author
            );
        
        Files.writeString(srcDir.resolve(className + ".java"), content);
    }
    
    private static void generatePluginToml(Path resourcesDir, PluginTemplate template) throws IOException {
        String content = """
            [plugin]
            id = "%s"
            name = "%s"
            version = "%s"
            author = "%s"
            description = "%s"
            api_version = "1.0.0"
            main = "%s.%sPlugin"
            
            [[dependencies]]
            # Add dependencies here
            # id = "other-plugin"
            # version = ">=1.0.0"
            # required = true
            """.formatted(
                template.pluginId,
                template.pluginName,
                template.version,
                template.author,
                template.description,
                template.packageName,
                toPascalCase(template.pluginId)
            );
        
        Files.writeString(resourcesDir.resolve("plugin.toml"), content);
    }
    
    private static void generateBuildGradle(Path outputDir, PluginTemplate template) throws IOException {
        String content = """
            plugins {
                java
                id("com.yellowtale.rubidium-plugin") version "1.0.0"
            }
            
            group = "%s"
            version = "%s"
            
            java {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            
            repositories {
                mavenCentral()
                maven { url = uri("https://repo.yellowtale.com/maven") }
            }
            
            dependencies {
                compileOnly("com.yellowtale:rubidium-api:1.0.0")
            }
            
            rubidium {
                pluginId = "%s"
                pluginName = "%s"
                author = "%s"
            }
            """.formatted(
                template.packageName,
                template.version,
                template.pluginId,
                template.pluginName,
                template.author
            );
        
        Files.writeString(outputDir.resolve("build.gradle.kts"), content);
    }
    
    private static void generateGitignore(Path outputDir) throws IOException {
        String content = """
            # Gradle
            .gradle/
            build/
            
            # IDE
            .idea/
            *.iml
            .vscode/
            
            # OS
            .DS_Store
            Thumbs.db
            
            # Plugin
            run/
            """;
        
        Files.writeString(outputDir.resolve(".gitignore"), content);
    }
    
    private static String toPascalCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : input.toCharArray()) {
            if (c == '-' || c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    public static class PluginTemplate {
        public String pluginId;
        public String pluginName;
        public String packageName;
        public String version = "1.0.0";
        public String author;
        public String description = "";
        
        public PluginTemplate(String pluginId, String pluginName, String packageName, String author) {
            this.pluginId = pluginId;
            this.pluginName = pluginName;
            this.packageName = packageName;
            this.author = author;
        }
        
        public PluginTemplate version(String version) {
            this.version = version;
            return this;
        }
        
        public PluginTemplate description(String description) {
            this.description = description;
            return this;
        }
    }
}
