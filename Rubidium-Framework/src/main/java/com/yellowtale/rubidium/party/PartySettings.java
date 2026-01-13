package com.yellowtale.rubidium.party;

public record PartySettings(
    int maxMembers,
    boolean allowFriendlyFire,
    boolean shareXP,
    float xpShareRadius,
    boolean shareLoot,
    LootDistribution lootDistribution,
    boolean enableVoiceChat,
    boolean enableWaypointSharing,
    boolean allowMemberTeleport,
    boolean showMemberHealth,
    boolean showMemberLocation
) {
    public static PartySettings defaults() {
        return new PartySettings(
            8,
            false,
            true,
            50.0f,
            true,
            LootDistribution.FREE_FOR_ALL,
            true,
            true,
            true,
            true,
            false
        );
    }
    
    public PartySettings withMaxMembers(int maxMembers) {
        return new PartySettings(maxMembers, allowFriendlyFire, shareXP, xpShareRadius, shareLoot, lootDistribution, enableVoiceChat, enableWaypointSharing, allowMemberTeleport, showMemberHealth, showMemberLocation);
    }
    
    public PartySettings withFriendlyFire(boolean allow) {
        return new PartySettings(maxMembers, allow, shareXP, xpShareRadius, shareLoot, lootDistribution, enableVoiceChat, enableWaypointSharing, allowMemberTeleport, showMemberHealth, showMemberLocation);
    }
    
    public PartySettings withShareXP(boolean share) {
        return new PartySettings(maxMembers, allowFriendlyFire, share, xpShareRadius, shareLoot, lootDistribution, enableVoiceChat, enableWaypointSharing, allowMemberTeleport, showMemberHealth, showMemberLocation);
    }
    
    public PartySettings withLootDistribution(LootDistribution distribution) {
        return new PartySettings(maxMembers, allowFriendlyFire, shareXP, xpShareRadius, shareLoot, distribution, enableVoiceChat, enableWaypointSharing, allowMemberTeleport, showMemberHealth, showMemberLocation);
    }
    
    public PartySettings withVoiceChat(boolean enable) {
        return new PartySettings(maxMembers, allowFriendlyFire, shareXP, xpShareRadius, shareLoot, lootDistribution, enable, enableWaypointSharing, allowMemberTeleport, showMemberHealth, showMemberLocation);
    }
    
    public PartySettings withWaypointSharing(boolean enable) {
        return new PartySettings(maxMembers, allowFriendlyFire, shareXP, xpShareRadius, shareLoot, lootDistribution, enableVoiceChat, enable, allowMemberTeleport, showMemberHealth, showMemberLocation);
    }
    
    public PartySettings withMemberTeleport(boolean allow) {
        return new PartySettings(maxMembers, allowFriendlyFire, shareXP, xpShareRadius, shareLoot, lootDistribution, enableVoiceChat, enableWaypointSharing, allow, showMemberHealth, showMemberLocation);
    }
    
    public enum LootDistribution {
        FREE_FOR_ALL,
        ROUND_ROBIN,
        NEED_BEFORE_GREED,
        LEADER_DECIDES,
        RANDOM
    }
}
