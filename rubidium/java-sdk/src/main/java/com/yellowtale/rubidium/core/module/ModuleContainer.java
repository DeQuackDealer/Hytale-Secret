package com.yellowtale.rubidium.core.module;

/**
 * Internal container for a loaded module instance and its metadata.
 */
record ModuleContainer(
    ModuleDescriptor descriptor,
    Module module,
    ModuleContext context,
    ModuleState state
) {
    ModuleContainer withState(ModuleState newState) {
        return new ModuleContainer(descriptor, module, context, newState);
    }
}
