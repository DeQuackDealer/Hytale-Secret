package com.yellowtale.rubidium.core.module;

import java.nio.file.Path;
import java.util.Set;

/**
 * Describes a module's metadata as parsed from its JAR manifest.
 */
public record ModuleDescriptor(
    Path jarPath,
    String id,
    String version,
    String moduleClass,
    Set<String> hardDependencies,
    Set<String> softDependencies
) {
    public ModuleDescriptor {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Module ID cannot be null or blank");
        }
        if (!id.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Module ID must be lowercase alphanumeric with underscores: " + id);
        }
        hardDependencies = hardDependencies != null ? Set.copyOf(hardDependencies) : Set.of();
        softDependencies = softDependencies != null ? Set.copyOf(softDependencies) : Set.of();
    }
}
