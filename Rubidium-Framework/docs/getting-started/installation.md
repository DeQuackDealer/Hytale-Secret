# Installation

Learn how to install and set up Rubidium for your Hytale plugin development.

This guide walks you through setting up a complete development environment for Hytale plugin development with Rubidium.

## Prerequisites

Before you begin, make sure you have:

- A computer running Windows 10/11, macOS, or Linux
- At least 8GB of RAM
- 10GB of free disk space
- Administrative privileges on your system

## Required Software

### 1. Java Development Kit (JDK)

Rubidium requires **Java 17 or later**. We recommend using Eclipse Temurin (OpenJDK 21).

#### Windows

1. Download OpenJDK 21 from [Adoptium](https://adoptium.net/)
2. Run the installer with default settings
3. Verify installation by opening Command Prompt and running:

```bash
java -version
```

Expected output:
```
openjdk version "21.0.1" 2024-01-16
OpenJDK Runtime Environment Temurin-21.0.1+12 (build 21.0.1+12)
OpenJDK 64-Bit Server VM Temurin-21.0.1+12 (build 21.0.1+12, mixed mode, sharing)
```

#### macOS

Using Homebrew:

```bash
brew install openjdk@21
```

> **Note**: If `java --version` shows "Unable to locate a Java Runtime", add OpenJDK to your PATH:
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

We recommend **IntelliJ IDEA Community Edition** for Hytale modding.

1. Download from [JetBrains website](https://www.jetbrains.com/idea/download/)
2. Install with default settings
3. Launch and complete the initial setup wizard
4. Install the "Gradle" plugin if not already installed

### 3. Gradle

Rubidium uses Gradle for build automation. You have two options:

**Option A: Use Gradle Wrapper (Recommended)**

The plugin template includes a Gradle wrapper. No separate installation needed.

**Option B: Install Gradle Globally**

1. Visit [https://gradle.org/install/](https://gradle.org/install/)
2. Download and install Gradle 8.0 or later
3. Add Gradle to your PATH
4. Verify: `gradle --version`

## Project Setup

### Method 1: Clone Template (Recommended)

```bash
git clone https://github.com/yellow-tale/rubidium-plugin-template.git MyPlugin
cd MyPlugin
```

### Method 2: Manual Setup

1. Create a new directory for your project
2. Initialize with the following structure:

```
MyPlugin/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/myplugin/
│       │       └── MyPlugin.java
│       └── resources/
│           └── rubidium.yml
├── libs/
│   └── HytaleServer.jar
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/
    └── wrapper/
        ├── gradle-wrapper.jar
        └── gradle-wrapper.properties
```

### Configure build.gradle.kts

```kotlin
plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
}

group = "com.example"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

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

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.example.myplugin.MyPlugin"
        )
    }
}
```

### Configure settings.gradle.kts

```kotlin
rootProject.name = "MyPlugin"
```

## Adding HytaleServer.jar

Before you can compile plugins, you need the Hytale server JAR:

1. Download HytaleServer.jar using the [Hytale Downloader](https://support.hytale.com/)
2. Create a `libs` folder in your project root
3. Copy `HytaleServer.jar` into the `libs` folder

## Open in IntelliJ IDEA

1. Open IntelliJ IDEA
2. Click **File > Open** and navigate to your project directory
3. IntelliJ will detect it as a Gradle project
4. Click **Load Gradle Project** when prompted
5. Wait for indexing and dependency download to complete

## Building Your Plugin

### Using Gradle Wrapper (Recommended)

```bash
# Linux/macOS
./gradlew shadowJar

# Windows
gradlew.bat shadowJar
```

### Using Global Gradle

```bash
gradle shadowJar
```

The compiled plugin JAR will be in `build/libs/MyPlugin.jar`.

## Testing Your Plugin

1. Copy your plugin JAR to your Hytale server's `plugins` folder
2. Start the Hytale server
3. Your plugin will load automatically
4. Check the console for `[MyPlugin] Plugin enabled!`

## Rubidium Plus Setup

If you have a Rubidium Plus license:

1. Log in to [rubidium.dev/account](https://rubidium.dev/account)
2. Generate your license key
3. Add your license to `~/.gradle/gradle.properties`:

```properties
rubidiumLicenseKey=YOUR_LICENSE_KEY
```

4. Update your `build.gradle.kts`:

```kotlin
repositories {
    maven {
        url = uri("https://repo.rubidium.dev/plus")
        credentials {
            username = "license"
            password = project.findProperty("rubidiumLicenseKey") as String? ?: ""
        }
    }
}

dependencies {
    compileOnly("com.rubidium:rubidium-plus:1.0.0")
}
```

## Next Steps

Now that your development environment is ready:

1. **[Create Your First Plugin](./first-plugin.md)** - Learn plugin basics
2. **[Command API Guide](../guides/commands.md)** - Add commands
3. **[Event API Guide](../guides/events.md)** - Handle events
4. **[Chat API Guide](../guides/chat.md)** - Send messages

## Troubleshooting

### Gradle sync fails

- Ensure you have Java 17+ installed: `java -version`
- Check your internet connection
- Clear Gradle cache: `./gradlew --refresh-dependencies`
- Delete `.gradle` folder and reimport

### Plugin doesn't load

- Check the server console for error messages
- Verify your `@PluginInfo` annotation is present
- Ensure the JAR is in the correct `plugins` folder
- Check that HytaleServer.jar is in your `libs` folder

### IDE shows red errors

- Wait for indexing to complete (bottom status bar)
- Click **File > Invalidate Caches / Restart**
- Run **Build > Rebuild Project**
- Verify Gradle sync completed successfully

### "Class not found" at runtime

- Ensure you're using `shadowJar` task, not just `jar`
- Verify dependencies are listed as `compileOnly` (not `implementation`)
- Check that your main class path matches your package structure

### HytaleServer.jar issues

- Verify the JAR is in the `libs` folder
- Check that the `compileOnly(files("libs/HytaleServer.jar"))` path is correct
- Try using an absolute path temporarily to diagnose
