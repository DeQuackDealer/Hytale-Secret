package com.yellowtale.rubidium.api.anticheat;

import com.yellowtale.rubidium.api.player.Player;
import java.util.List;
import java.util.UUID;

public interface AnticheatService {
    boolean isEnabled();
    void setEnabled(boolean enabled);
    
    void processMovement(Player player, MovementSnapshot snapshot);
    void processCombat(Player player, CombatSnapshot snapshot);
    
    List<Finding> getRecentFindings(int count);
    List<Finding> getPlayerFindings(UUID playerId, int count);
    int getPlayerViolationCount(UUID playerId);
    
    boolean shouldKickPlayer(UUID playerId);
    void clearPlayerData(UUID playerId);
    
    void reloadConfig();
}
