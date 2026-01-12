# Rubidium DevKit Core

DevKit Core provides IDE-agnostic development tools for Rubidium plugin development.

## Features

### Project Templates
- Java plugin templates with Gradle setup
- C# plugin templates with .NET project files
- Pre-configured build files and manifests

### Code Generators
- Main plugin class scaffolding
- Event listener templates
- Command handler templates
- Configuration file templates

### Schema Validation
- plugin.toml manifest validation
- Configuration file schema validation
- Dependency version checking

### Asset Pipeline
- Resource file bundling
- Localization file compilation
- Asset optimization

### Debug Bridge
- Remote debugging support
- Hot reload integration
- Log streaming

## IDE Integration

### IntelliJ IDEA / Rider
Use the Hytale DevKit plugin for full IDE integration:
- Project creation wizard
- Run configurations
- Debug configurations
- Code inspections
- Quick fixes

### Visual Studio Code
Use the Rubidium extension for:
- Syntax highlighting for plugin.toml
- Code snippets for common patterns
- Build task integration
- Debug launch configurations

## CLI Usage

```bash
# Create a new Java plugin
rubidium new java my-plugin --package com.example.myplugin

# Create a new C# plugin  
rubidium new csharp my-plugin --namespace MyCompany.MyPlugin

# Build a plugin
rubidium build

# Deploy to local server
rubidium deploy --server ./run

# Hot reload a plugin
rubidium reload my-plugin
```

## API

The DevKit Core provides APIs for IDE plugins to use:

```java
// Java
ProjectGenerator.generateJavaPlugin(
    outputDir,
    new PluginTemplate("my-plugin", "My Plugin", "com.example.myplugin", "Author")
);
```

```csharp
// C#
await ProjectGenerator.GenerateCSharpPluginAsync(
    outputDir,
    new PluginTemplate("my-plugin", "My Plugin", "MyCompany.MyPlugin", "Author")
);
```
