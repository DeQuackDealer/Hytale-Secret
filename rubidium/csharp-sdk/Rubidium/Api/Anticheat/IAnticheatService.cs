using System;
using System.Collections.Generic;
using Rubidium.Api.Player;

namespace Rubidium.Api.Anticheat
{
    public interface IAnticheatService
    {
        bool IsEnabled { get; set; }
        
        void ProcessMovement(IPlayer player, MovementSnapshot snapshot);
        void ProcessCombat(IPlayer player, CombatSnapshot snapshot);
        
        IReadOnlyList<Finding> GetRecentFindings(int count);
        IReadOnlyList<Finding> GetPlayerFindings(Guid playerId, int count);
        int GetPlayerViolationCount(Guid playerId);
        
        bool ShouldKickPlayer(Guid playerId);
        void ClearPlayerData(Guid playerId);
        
        void ReloadConfig();
    }
}
