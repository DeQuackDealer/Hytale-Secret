# Installation

Learn how to install and set up Rubidium for your Hytale plugin development.

This guide will walk you through setting up a complete development environment for Hytale plugin development with Rubidium.

## Prerequisites

Before you begin, make sure you have:

- A computer running Windows 10/11, macOS, or Linux
- At least 8GB of RAM
- 10GB of free disk space
- Administrative privileges on your system

## Required Software

### 1. Java Development Kit (JDK)

Rubidium requires Java 17 or later. We recommend using Eclipse Temurin.

#### Windows

1. Download OpenJDK 21 from [Adoptium](https://adoptium.net/)
2. Run the installer with default settings
3. Verify installation by opening Command Prompt and running:

```bash
java -version
```

#### macOS

```bash
# Using Homebrew
brew install openjdk@21
```

> **Info**: If `java --version` shows "Unable to locate a Java Runtime", add OpenJDK to your PATH:
> ```bash
> echo 'export PATH="$(brew --prefix)/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
> source ~/.zshrc
> ```

#### Linux (Ubuntu/Debian)

```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

### 2. Integrated Development Environment (IDE)

We recommend IntelliJ IDEA Community Edition for Hytale modding.

1. Download from [JetBrains website](https://www.jetbrains.com/idea/download/)
2. Install with default settings
3. Launch and complete the initial setup wizard

### 3. Gradle

Rubidium uses Gradle for build automation.

1. Visit the official website: [https://gradle.org/install/](https://gradle.org/install/)
2. Download and install Gradle 8.0 or later
3. Add Gradle to your PATH

Alternatively, use the Gradle wrapper included in the project template.

## Setting Up Your Project

### 1. Clone the Plugin Template

```bash
# Clone the Rubidium plugin template
git clone https://github.com/yellow-tale/rubidium-plugin-template.git MyPlugin
cd MyPlugin
```

### 2. Open in IntelliJ IDEA

1. Open IntelliJ IDEA
2. Click "Open" and navigate to your `MyPlugin` directory
3. IntelliJ will automatically detect it as a Gradle project
4. Wait for the project to index and dependencies to download

### 3. Configure Dependencies

Edit your `build.gradle.kts` to include Rubidium:

```kotlin
plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.rubidium.dev/releases") }
}

dependencies {
    // Rubidium API (Free Edition)
    compileOnly("com.rubidium:rubidium:1.0.0")
    
    // Or Rubidium Plus (Premium Edition)
    // compileOnly("com.rubidium:rubidium-plus:1.0.0")
    
    // HytaleServer API
    compileOnly(files("libs/HytaleServer.jar"))
}

tasks.shadowJar {
    archiveBaseName.set("MyPlugin")
    archiveClassifier.set("")
}
```

### 4. Add HytaleServer.jar

Before you can compile plugins, you need the Hytale server JAR:

1. Download HytaleServer.jar using the [Hytale Downloader](https://support.hytale.com/)
2. Create a `libs` folder in your project
3. Copy `HytaleServer.jar` into the `libs` folder

## Building Your Plugin

```bash
# Build your plugin
./gradlew shadowJar

# Output will be in build/libs/MyPlugin.jar
```

## Testing Your Plugin

1. Copy your plugin JAR to your Hytale server's `plugins` folder
2. Start the server
3. Your plugin will load automatically

## Next Steps

Now that you have your development environment set up:

1. [Create Your First Plugin](./first-plugin.md) - Learn plugin basics
2. [Command API Guide](../guides/commands.md) - Add commands
3. [Event API Guide](../guides/events.md) - Handle events
4. [Chat API Guide](../guides/chat.md) - Send messages

## Troubleshooting

### Gradle sync fails

- Ensure you have Java 17+ installed
- Check your internet connection
- Try `./gradlew --refresh-dependencies`

### Plugin doesn't load

- Check the server console for errors
- Verify your `@PluginInfo` annotation
- Ensure the JAR is in the correct folder

### IDE shows red errors

- Wait for indexing to complete
- Try "File > Invalidate Caches / Restart"
- Rebuild the project
