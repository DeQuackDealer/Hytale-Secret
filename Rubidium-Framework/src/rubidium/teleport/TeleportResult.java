package rubidium.teleport;

public record TeleportResult(
    boolean success,
    String message,
    Location destination,
    TeleportFailReason failReason
) {
    public static TeleportResult success(Location destination) {
        return new TeleportResult(true, "Teleported successfully", destination, null);
    }
    
    public static TeleportResult success(Location destination, String message) {
        return new TeleportResult(true, message, destination, null);
    }
    
    public static TeleportResult failure(TeleportFailReason reason, String message) {
        return new TeleportResult(false, message, null, reason);
    }
    
    public static TeleportResult onCooldown(long remainingSeconds) {
        return new TeleportResult(false, 
            "Teleport on cooldown. Wait " + remainingSeconds + " seconds.",
            null, TeleportFailReason.COOLDOWN);
    }
    
    public static TeleportResult noPermission() {
        return new TeleportResult(false, "You don't have permission to do that.",
            null, TeleportFailReason.NO_PERMISSION);
    }
    
    public static TeleportResult playerNotFound(String name) {
        return new TeleportResult(false, "Player '" + name + "' not found.",
            null, TeleportFailReason.PLAYER_NOT_FOUND);
    }
    
    public static TeleportResult unsafeLocation() {
        return new TeleportResult(false, "Destination is not safe.",
            null, TeleportFailReason.UNSAFE_LOCATION);
    }
    
    public enum TeleportFailReason {
        COOLDOWN,
        NO_PERMISSION,
        PLAYER_NOT_FOUND,
        UNSAFE_LOCATION,
        WORLD_NOT_FOUND,
        OUT_OF_BOUNDS,
        CANCELLED,
        UNKNOWN
    }
}
