package com.yellowtale.rubidium.party;

import java.util.UUID;

public class PartyInvite {
    private final UUID id;
    private final UUID partyId;
    private final UUID inviter;
    private final UUID invitee;
    private final long createdAt;
    private final long expiresAt;
    
    public PartyInvite(UUID id, UUID partyId, UUID inviter, UUID invitee, long createdAt, long expiresAt) {
        this.id = id;
        this.partyId = partyId;
        this.inviter = inviter;
        this.invitee = invitee;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
    
    public UUID getId() { return id; }
    public UUID getPartyId() { return partyId; }
    public UUID getInviter() { return inviter; }
    public UUID getInvitee() { return invitee; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
    
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
    
    public long getRemainingTime() {
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }
}
