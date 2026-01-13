package com.yellowtale.rubidium.waypoints;

public record NavigationData(
    Waypoint waypoint,
    double distance,
    double horizontalDistance,
    double verticalDistance,
    double bearing,
    Direction compassDirection,
    String formattedDistance,
    String eta,
    boolean inRange,
    boolean sameWorld
) {
    public enum Direction {
        N("North"),
        NE("Northeast"),
        E("East"),
        SE("Southeast"),
        S("South"),
        SW("Southwest"),
        W("West"),
        NW("Northwest");
        
        private final String fullName;
        
        Direction(String fullName) {
            this.fullName = fullName;
        }
        
        public String getFullName() {
            return fullName;
        }
    }
    
    public static NavigationData calculate(WaypointManager.Location from, Waypoint waypoint) {
        if (!from.world().equals(waypoint.getWorld())) {
            return new NavigationData(
                waypoint, -1, -1, -1, 0, null, 
                "Different World", null, false, false
            );
        }
        
        double dx = waypoint.getX() - from.x();
        double dy = waypoint.getY() - from.y();
        double dz = waypoint.getZ() - from.z();
        
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        double bearing = Math.toDegrees(Math.atan2(-dx, dz));
        if (bearing < 0) bearing += 360;
        
        Direction compass = getCompassDirection(bearing);
        String formatted = formatDistance(distance);
        
        return new NavigationData(
            waypoint, distance, horizontalDist, dy, bearing, compass,
            formatted, null, distance <= 1000, true
        );
    }
    
    private static Direction getCompassDirection(double bearing) {
        bearing = (bearing + 22.5) % 360;
        int index = (int) (bearing / 45);
        return Direction.values()[index % 8];
    }
    
    private static String formatDistance(double distance) {
        if (distance < 0) return "???";
        if (distance < 10) return String.format("%.1f", distance);
        if (distance < 1000) return String.format("%.0f", distance);
        if (distance < 10000) return String.format("%.1fk", distance / 1000);
        return String.format("%.0fk", distance / 1000);
    }
    
    public String getDirectionArrow() {
        if (compassDirection == null) return "?";
        return switch (compassDirection) {
            case N -> "↑";
            case NE -> "↗";
            case E -> "→";
            case SE -> "↘";
            case S -> "↓";
            case SW -> "↙";
            case W -> "←";
            case NW -> "↖";
        };
    }
    
    public String getFormattedNavigation() {
        if (!sameWorld) return "Different World";
        return String.format("%s %s (%s)", 
            getDirectionArrow(), 
            formattedDistance, 
            compassDirection != null ? compassDirection.name() : "?"
        );
    }
    
    public double getVerticalDirection() {
        if (verticalDistance > 5) return 1;
        if (verticalDistance < -5) return -1;
        return 0;
    }
    
    public String getVerticalIndicator() {
        double dir = getVerticalDirection();
        if (dir > 0) return "▲ " + String.format("%.0f", verticalDistance);
        if (dir < 0) return "▼ " + String.format("%.0f", Math.abs(verticalDistance));
        return "●";
    }
}
