package com.yellowtale.rubidium.waypoints;

public record WaypointConfig(
    int maxWaypointsPerPlayer,
    int maxSharedWaypoints,
    int maxServerWaypoints,
    int maxCategoriesPerPlayer,
    double minDistanceBetweenWaypoints,
    boolean enableBeams,
    double maxBeamDistance,
    boolean enableCompass,
    boolean enableHUD,
    HUDDisplayMode hudDisplayMode,
    boolean allowNetherWaypoints,
    boolean allowEndWaypoints,
    boolean trackVisitStatistics,
    int navigationUpdateInterval
) {
    public static WaypointConfig defaults() {
        return new WaypointConfig(
            100,
            50,
            1000,
            20,
            1.0,
            true,
            256.0,
            true,
            true,
            HUDDisplayMode.FULL,
            true,
            true,
            true,
            5
        );
    }
    
    public enum HUDDisplayMode {
        OFF,
        MINIMAL,
        COMPACT,
        FULL,
        DETAILED
    }
}
