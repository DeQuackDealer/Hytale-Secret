package rubidium.essentials;

import rubidium.hytale.api.world.Location;

/**
 * Server warp point.
 */
public record Warp(String name, Location location, String permission) {}
