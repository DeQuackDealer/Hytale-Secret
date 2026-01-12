package com.yellowtale.rubidium.core.module;

/**
 * Public read-only information about a module.
 */
public record ModuleInfo(
    String id,
    String displayName,
    String version,
    String description,
    ModuleState state
) {}
