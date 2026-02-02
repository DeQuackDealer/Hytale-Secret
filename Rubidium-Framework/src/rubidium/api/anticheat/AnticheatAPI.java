package rubidium.api.anticheat;

import rubidium.anticheat.AnticheatServiceImpl;
import rubidium.api.player.Player;

import java.util.List;
import java.util.UUID;

public final class AnticheatAPI {
    
    private static AnticheatService service;
    
    private AnticheatAPI() {}
    
    public static void initialize(AnticheatService anticheatService) {
        service = anticheatService;
    }
    
    public static AnticheatService getService() {
        if (service == null) {
            service = AnticheatServiceImpl.getInstance();
        }
        return service;
    }
    
    public static boolean isEnabled() {
        return getService().isEnabled();
    }
    
    public static void setEnabled(boolean enabled) {
        getService().setEnabled(enabled);
    }
    
    public static void processMovement(Player player, MovementSnapshot snapshot) {
        getService().processMovement(player, snapshot);
    }
    
    public static void processCombat(Player player, CombatSnapshot snapshot) {
        getService().processCombat(player, snapshot);
    }
    
    public static List<Finding> getRecentFindings(int count) {
        return getService().getRecentFindings(count);
    }
    
    public static List<Finding> getPlayerFindings(UUID playerId, int count) {
        return getService().getPlayerFindings(playerId, count);
    }
    
    public static int getPlayerViolationCount(UUID playerId) {
        return getService().getPlayerViolationCount(playerId);
    }
    
    public static boolean shouldKickPlayer(UUID playerId) {
        return getService().shouldKickPlayer(playerId);
    }
    
    public static void clearPlayerData(UUID playerId) {
        getService().clearPlayerData(playerId);
    }
    
    public static void reloadConfig() {
        getService().reloadConfig();
    }
    
    public static MovementSnapshot.Builder createMovementSnapshot(double x, double y, double z) {
        return new MovementSnapshot.Builder(x, y, z);
    }
    
    public static CombatSnapshot.Builder createCombatSnapshot(boolean isAttack) {
        return new CombatSnapshot.Builder(isAttack);
    }
}
