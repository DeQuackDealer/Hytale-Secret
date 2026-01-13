# Party System

> **Document Purpose**: Complete reference for Rubidium's party and group coordination system.

## Overview

Rubidium's party system enables player coordination with:
- **Parties**: Temporary groups for playing together
- **Teams**: Persistent competitive teams
- **Guilds**: Long-term organizations (optional module)
- **Integrated Features**: Voice chat, waypoints, teleportation, and shared XP

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      PartyManager                                │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Party Lifecycle                                           │   │
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │   │
│  │ │ Creation │ │ Invites  │ │ Members  │ │ Dissolution│    │   │
│  │ │ Manager  │ │ Manager  │ │ Manager  │ │ Handler   │     │   │
│  │ └──────────┘ └──────────┘ └──────────┘ └──────────┘     │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Integrations                                              │   │
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │   │
│  │ │ Voice    │ │ Waypoint │ │ Teleport │ │ Chat     │     │   │
│  │ │ Channel  │ │ Sharing  │ │ Requests │ │ Channel  │     │   │
│  │ └──────────┘ └──────────┘ └──────────┘ └──────────┘     │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Core Classes

### Party

```java
package com.yellowtale.rubidium.party;

public class Party {
    // Identity
    private UUID id;
    private String name;
    private long createdAt;
    
    // Members
    private UUID leader;
    private Set<UUID> members;
    private Set<UUID> moderators;
    private Map<UUID, PartyRole> roles;
    
    // Invites
    private Map<UUID, PartyInvite> pendingInvites;
    private Set<UUID> bannedPlayers;
    
    // Settings
    private PartySettings settings;
    private PartyPrivacy privacy;
    
    // Integrations
    private VoiceChannel voiceChannel;
    private ChatChannel chatChannel;
    private Set<UUID> sharedWaypoints;
    
    // Statistics
    private Map<UUID, PartyMemberStats> memberStats;
    
    public enum PartyRole {
        LEADER(100),
        MODERATOR(50),
        MEMBER(10),
        GUEST(1);
        
        private final int priority;
    }
    
    public enum PartyPrivacy {
        OPEN,           // Anyone can join
        INVITE_ONLY,    // Requires invite
        FRIENDS_ONLY,   // Only friends can request
        PRIVATE         // Invite only, hidden from lists
    }
}
```

### PartySettings

```java
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
    public enum LootDistribution {
        FREE_FOR_ALL,       // First come first serve
        ROUND_ROBIN,        // Alternate between members
        NEED_BEFORE_GREED,  // Roll system
        LEADER_DECIDES,     // Leader assigns loot
        RANDOM              // Random distribution
    }
    
    public static PartySettings defaults() {
        return new PartySettings(
            8, false, true, 50.0f, true,
            LootDistribution.FREE_FOR_ALL,
            true, true, true, true, false
        );
    }
}
```

### PartyManager

```java
public class PartyManager {
    
    private Map<UUID, Party> parties;
    private Map<UUID, UUID> playerPartyMap;
    
    // Limits
    public static final int MAX_PARTY_SIZE = 8;
    public static final int MAX_PARTIES = 1000;
    public static final Duration INVITE_EXPIRY = Duration.ofMinutes(5);
    
    // Party Lifecycle
    public Party createParty(UUID leader, String name);
    public void disbandParty(UUID partyId);
    public void transferLeadership(UUID partyId, UUID newLeader);
    
    // Member Management
    public void addMember(UUID partyId, UUID playerId);
    public void removeMember(UUID partyId, UUID playerId);
    public void kickMember(UUID partyId, UUID playerId, String reason);
    public void banMember(UUID partyId, UUID playerId);
    public void unbanMember(UUID partyId, UUID playerId);
    
    // Invites
    public PartyInvite invitePlayer(UUID partyId, UUID inviter, UUID invitee);
    public void acceptInvite(UUID inviteId);
    public void declineInvite(UUID inviteId);
    public void cancelInvite(UUID inviteId);
    
    // Queries
    public Optional<Party> getParty(UUID partyId);
    public Optional<Party> getPlayerParty(UUID playerId);
    public List<Party> getOpenParties();
    public List<UUID> getNearbyPartyMembers(UUID playerId, double radius);
    
    // Events (callback registration)
    public void onPartyCreated(Consumer<Party> callback);
    public void onPartyDisbanded(Consumer<Party> callback);
    public void onMemberJoined(BiConsumer<Party, UUID> callback);
    public void onMemberLeft(BiConsumer<Party, UUID> callback);
}
```

## Party Features

### XP Sharing

```java
public class PartyXPManager {
    
    public void distributeXP(Party party, UUID earner, int amount, Location location) {
        PartySettings settings = party.getSettings();
        if (!settings.shareXP()) {
            grantXP(earner, amount);
            return;
        }
        
        float radius = settings.xpShareRadius();
        List<UUID> nearbyMembers = party.getMembers().stream()
            .filter(id -> isWithinRadius(id, location, radius))
            .toList();
        
        if (nearbyMembers.isEmpty()) {
            grantXP(earner, amount);
            return;
        }
        
        // Bonus XP for party play
        float partyBonus = calculatePartyBonus(nearbyMembers.size());
        int totalXP = (int) (amount * partyBonus);
        int xpPerMember = totalXP / nearbyMembers.size();
        
        for (UUID member : nearbyMembers) {
            grantXP(member, xpPerMember);
        }
    }
    
    private float calculatePartyBonus(int memberCount) {
        return 1.0f + (0.05f * (memberCount - 1)); // 5% bonus per extra member
    }
}
```

### Loot Distribution

```java
public class PartyLootManager {
    
    public void distributeLoot(Party party, ItemStack item, Location location) {
        switch (party.getSettings().lootDistribution()) {
            case FREE_FOR_ALL -> dropItem(item, location);
            case ROUND_ROBIN -> giveToNextMember(party, item);
            case NEED_BEFORE_GREED -> startRoll(party, item);
            case LEADER_DECIDES -> notifyLeader(party, item);
            case RANDOM -> giveToRandomMember(party, item);
        }
    }
    
    private void startRoll(Party party, ItemStack item) {
        LootRoll roll = new LootRoll(party, item, Duration.ofSeconds(30));
        roll.onComplete((winner) -> {
            if (winner != null) {
                giveItemTo(winner, item);
                notifyParty(party, winner.getName() + " won " + item.getName());
            } else {
                dropItem(item, party.getLeaderLocation());
            }
        });
        activeLootRolls.put(roll.getId(), roll);
        
        for (UUID member : party.getMembers()) {
            sendRollPrompt(member, roll);
        }
    }
}
```

### Party Teleportation

```java
public class PartyTeleportManager {
    
    public TeleportRequest requestTeleport(UUID requester, UUID target) {
        Party party = partyManager.getPlayerParty(requester).orElse(null);
        if (party == null || !party.getSettings().allowMemberTeleport()) {
            return TeleportRequest.denied("Not in a party or teleport disabled");
        }
        
        if (!party.getMembers().contains(target)) {
            return TeleportRequest.denied("Target not in your party");
        }
        
        TeleportRequest request = new TeleportRequest(
            requester, target, 
            TeleportType.PARTY,
            Duration.ofSeconds(30)
        );
        
        pendingRequests.put(request.getId(), request);
        notifyTarget(target, requester.getName() + " wants to teleport to you");
        
        return request;
    }
    
    public void summonParty(UUID leader) {
        Party party = partyManager.getPlayerParty(leader).orElse(null);
        if (party == null || !party.getLeader().equals(leader)) {
            return;
        }
        
        Location leaderLoc = getPlayerLocation(leader);
        for (UUID member : party.getMembers()) {
            if (!member.equals(leader)) {
                teleportPlayer(member, leaderLoc);
            }
        }
    }
}
```

## Commands

### Basic Commands

```
/party create [name]            - Create a new party
/party disband                  - Disband your party (leader only)
/party leave                    - Leave your current party
/party info                     - Show party information
/party list                     - List party members
```

### Shorthand Alias

```
/p create [name]                - Create party
/p leave                        - Leave party
/p i                            - Party info
```

### Invite Commands

```
/party invite <player>          - Invite player to party
/party accept [player]          - Accept party invite
/party decline [player]         - Decline party invite
/party cancel <player>          - Cancel sent invite
```

### Member Management

```
/party kick <player> [reason]   - Kick member from party
/party ban <player>             - Ban player from party
/party unban <player>           - Unban player
/party promote <player>         - Promote to moderator
/party demote <player>          - Demote from moderator
/party leader <player>          - Transfer leadership
```

### Settings Commands

```
/party settings                 - Show current settings
/party settings maxmembers <n>  - Set max members
/party settings friendlyfire <on|off> - Toggle friendly fire
/party settings sharexp <on|off> - Toggle XP sharing
/party settings loot <mode>     - Set loot distribution
/party settings voice <on|off>  - Toggle voice channel
/party settings waypoints <on|off> - Toggle waypoint sharing
/party settings teleport <on|off> - Toggle member teleport
/party settings privacy <level> - Set party privacy
```

### Communication

```
/party chat <message>           - Send party chat message
/p <message>                    - Shorthand for party chat
/party ping [message]           - Ping your location
/party announce <message>       - Broadcast to party (mod+)
```

### Teleportation

```
/party tp <member>              - Request teleport to member
/party tphere <member>          - Request member teleport to you
/party accept                   - Accept teleport request
/party decline                  - Decline teleport request
/party summon                   - Summon all members (leader)
```

## Events

### Party Events

```java
public class PartyCreatedEvent extends Event {
    private Party party;
    private UUID creator;
}

public class PartyDisbandedEvent extends Event {
    private Party party;
    private DisbandReason reason;
    
    public enum DisbandReason {
        LEADER_DISBANDED,
        ALL_MEMBERS_LEFT,
        INACTIVITY,
        ADMIN_ACTION
    }
}

public class PartyMemberJoinedEvent extends Event {
    private Party party;
    private UUID member;
    private JoinMethod method;
    
    public enum JoinMethod {
        INVITE_ACCEPTED,
        OPEN_JOIN,
        FRIEND_REQUEST
    }
}

public class PartyMemberLeftEvent extends Event {
    private Party party;
    private UUID member;
    private LeaveReason reason;
    
    public enum LeaveReason {
        VOLUNTARY,
        KICKED,
        BANNED,
        DISCONNECTED,
        PARTY_DISBANDED
    }
}

public class PartyInviteSentEvent extends Event implements Cancellable {
    private Party party;
    private UUID inviter;
    private UUID invitee;
    private boolean cancelled;
}
```

## Integration Examples

### Voice Chat Auto-Join

```java
partyManager.onPartyCreated((party) -> {
    VoiceChannel channel = voiceChat.createChannel(
        "party-" + party.getId(),
        ChannelType.PARTY
    );
    channel.setMaxMembers(party.getSettings().maxMembers());
    party.setVoiceChannel(channel);
    
    voiceChat.joinChannel(party.getLeader(), channel);
});

partyManager.onMemberJoined((party, member) -> {
    if (party.getSettings().enableVoiceChat()) {
        voiceChat.joinChannel(member, party.getVoiceChannel());
    }
});

partyManager.onMemberLeft((party, member) -> {
    voiceChat.leaveChannel(member, party.getVoiceChannel());
});

partyManager.onPartyDisbanded((party) -> {
    voiceChat.deleteChannel(party.getVoiceChannel());
});
```

### Waypoint Sharing

```java
waypointManager.onWaypointCreated((waypoint) -> {
    if (waypoint.getVisibility() == WaypointVisibility.PARTY) {
        Party party = partyManager.getPlayerParty(waypoint.getOwner()).orElse(null);
        if (party != null && party.getSettings().enableWaypointSharing()) {
            for (UUID member : party.getMembers()) {
                if (!member.equals(waypoint.getOwner())) {
                    waypointManager.shareWaypoint(waypoint.getId(), member);
                }
            }
        }
    }
});
```

### Combat Tracking

```java
combatManager.onDamage((attacker, victim, damage) -> {
    Party attackerParty = partyManager.getPlayerParty(attacker).orElse(null);
    Party victimParty = partyManager.getPlayerParty(victim).orElse(null);
    
    if (attackerParty != null && attackerParty.equals(victimParty)) {
        if (!attackerParty.getSettings().allowFriendlyFire()) {
            return DamageResult.CANCELLED;
        }
    }
    
    return DamageResult.ALLOWED;
});
```

## Party HUD

### Display Elements

```java
public class PartyHUD {
    
    public void render(Party party, UUID viewer) {
        int yOffset = 10;
        
        // Party name
        renderText(party.getName(), 10, yOffset, 0xFFFFFF);
        yOffset += 15;
        
        // Members
        for (UUID member : party.getMembers()) {
            PartyMemberStats stats = party.getMemberStats(member);
            int color = member.equals(party.getLeader()) ? 0xFFD700 : 0xFFFFFF;
            
            String display = formatMemberDisplay(member, stats, party.getSettings());
            renderText(display, 10, yOffset, color);
            
            if (party.getSettings().showMemberHealth()) {
                renderHealthBar(stats.getHealth(), stats.getMaxHealth(), 150, yOffset);
            }
            
            yOffset += 12;
        }
    }
    
    private String formatMemberDisplay(UUID member, PartyMemberStats stats, PartySettings settings) {
        StringBuilder sb = new StringBuilder();
        sb.append(stats.getName());
        
        if (settings.showMemberLocation()) {
            sb.append(" [").append((int) stats.getDistance()).append("m]");
        }
        
        return sb.toString();
    }
}
```

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `party.create` | Create parties | All |
| `party.invite` | Invite players | All |
| `party.kick` | Kick members | Moderator |
| `party.ban` | Ban players | Leader |
| `party.settings` | Modify settings | Leader |
| `party.summon` | Summon members | Leader |
| `party.admin` | Admin controls | Staff |
