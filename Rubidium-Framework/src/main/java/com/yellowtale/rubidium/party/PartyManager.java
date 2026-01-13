package com.yellowtale.rubidium.party;

import com.yellowtale.rubidium.core.logging.RubidiumLogger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PartyManager {
    
    private final RubidiumLogger logger;
    
    private final Map<UUID, Party> parties;
    private final Map<UUID, UUID> playerPartyMap;
    private final Map<UUID, PartyInvite> pendingInvites;
    
    private final List<Consumer<Party>> createListeners;
    private final List<Consumer<Party>> disbandListeners;
    private final List<BiConsumer<Party, UUID>> joinListeners;
    private final List<BiConsumer<Party, UUID>> leaveListeners;
    
    public static final int MAX_PARTY_SIZE = 8;
    public static final int MAX_PARTIES = 1000;
    public static final Duration INVITE_EXPIRY = Duration.ofMinutes(5);
    
    public PartyManager(RubidiumLogger logger) {
        this.logger = logger;
        this.parties = new ConcurrentHashMap<>();
        this.playerPartyMap = new ConcurrentHashMap<>();
        this.pendingInvites = new ConcurrentHashMap<>();
        this.createListeners = new ArrayList<>();
        this.disbandListeners = new ArrayList<>();
        this.joinListeners = new ArrayList<>();
        this.leaveListeners = new ArrayList<>();
    }
    
    public Party createParty(UUID leader, String name) {
        if (isInParty(leader)) {
            throw new IllegalStateException("Player is already in a party");
        }
        
        if (parties.size() >= MAX_PARTIES) {
            throw new IllegalStateException("Maximum parties reached");
        }
        
        Party party = new Party(UUID.randomUUID(), name != null ? name : "Party", leader);
        parties.put(party.getId(), party);
        playerPartyMap.put(leader, party.getId());
        
        for (Consumer<Party> listener : createListeners) {
            listener.accept(party);
        }
        
        logger.debug("Party '{}' created by {}", name, leader);
        return party;
    }
    
    public void disbandParty(UUID partyId) {
        Party party = parties.remove(partyId);
        if (party == null) return;
        
        for (UUID member : party.getMembers()) {
            playerPartyMap.remove(member);
            notifyLeave(party, member);
        }
        
        pendingInvites.values().removeIf(invite -> invite.getPartyId().equals(partyId));
        
        for (Consumer<Party> listener : disbandListeners) {
            listener.accept(party);
        }
        
        logger.debug("Party '{}' disbanded", party.getName());
    }
    
    public void transferLeadership(UUID partyId, UUID newLeader) {
        Party party = parties.get(partyId);
        if (party == null) return;
        
        if (!party.getMembers().contains(newLeader)) {
            throw new IllegalArgumentException("New leader is not a party member");
        }
        
        party.setLeader(newLeader);
        logger.debug("Party leadership transferred to {}", newLeader);
    }
    
    public void addMember(UUID partyId, UUID playerId) {
        if (isInParty(playerId)) {
            throw new IllegalStateException("Player is already in a party");
        }
        
        Party party = parties.get(partyId);
        if (party == null) {
            throw new IllegalArgumentException("Party not found");
        }
        
        if (party.getMembers().size() >= MAX_PARTY_SIZE) {
            throw new IllegalStateException("Party is full");
        }
        
        if (party.isBanned(playerId)) {
            throw new IllegalStateException("Player is banned from this party");
        }
        
        party.addMember(playerId);
        playerPartyMap.put(playerId, partyId);
        
        notifyJoin(party, playerId);
        
        logger.debug("Player {} joined party '{}'", playerId, party.getName());
    }
    
    public void removeMember(UUID partyId, UUID playerId) {
        Party party = parties.get(partyId);
        if (party == null) return;
        
        if (!party.getMembers().contains(playerId)) return;
        
        party.removeMember(playerId);
        playerPartyMap.remove(playerId);
        
        notifyLeave(party, playerId);
        
        if (playerId.equals(party.getLeader())) {
            if (party.getMembers().isEmpty()) {
                disbandParty(partyId);
            } else {
                UUID newLeader = party.getMembers().iterator().next();
                party.setLeader(newLeader);
            }
        }
        
        if (party.getMembers().isEmpty()) {
            disbandParty(partyId);
        }
        
        logger.debug("Player {} left party '{}'", playerId, party.getName());
    }
    
    public void kickMember(UUID partyId, UUID playerId, String reason) {
        removeMember(partyId, playerId);
    }
    
    public void banMember(UUID partyId, UUID playerId) {
        Party party = parties.get(partyId);
        if (party == null) return;
        
        removeMember(partyId, playerId);
        party.ban(playerId);
    }
    
    public void unbanMember(UUID partyId, UUID playerId) {
        Party party = parties.get(partyId);
        if (party != null) {
            party.unban(playerId);
        }
    }
    
    public PartyInvite invitePlayer(UUID partyId, UUID inviter, UUID invitee) {
        if (isInParty(invitee)) {
            throw new IllegalStateException("Player is already in a party");
        }
        
        Party party = parties.get(partyId);
        if (party == null) {
            throw new IllegalArgumentException("Party not found");
        }
        
        if (!party.getMembers().contains(inviter)) {
            throw new IllegalStateException("Inviter is not in the party");
        }
        
        if (party.isBanned(invitee)) {
            throw new IllegalStateException("Player is banned from this party");
        }
        
        PartyInvite existing = pendingInvites.values().stream()
            .filter(i -> i.getPartyId().equals(partyId) && i.getInvitee().equals(invitee))
            .findFirst()
            .orElse(null);
        
        if (existing != null) {
            throw new IllegalStateException("Player already has a pending invite");
        }
        
        PartyInvite invite = new PartyInvite(
            UUID.randomUUID(),
            partyId,
            inviter,
            invitee,
            System.currentTimeMillis(),
            System.currentTimeMillis() + INVITE_EXPIRY.toMillis()
        );
        
        pendingInvites.put(invite.getId(), invite);
        
        return invite;
    }
    
    public void acceptInvite(UUID inviteId) {
        PartyInvite invite = pendingInvites.remove(inviteId);
        if (invite == null) {
            throw new IllegalArgumentException("Invite not found or expired");
        }
        
        if (invite.isExpired()) {
            throw new IllegalStateException("Invite has expired");
        }
        
        addMember(invite.getPartyId(), invite.getInvitee());
    }
    
    public void declineInvite(UUID inviteId) {
        pendingInvites.remove(inviteId);
    }
    
    public void cancelInvite(UUID inviteId) {
        pendingInvites.remove(inviteId);
    }
    
    public List<PartyInvite> getPendingInvites(UUID playerId) {
        return pendingInvites.values().stream()
            .filter(i -> i.getInvitee().equals(playerId) && !i.isExpired())
            .toList();
    }
    
    public Optional<Party> getParty(UUID partyId) {
        return Optional.ofNullable(parties.get(partyId));
    }
    
    public Optional<Party> getPlayerParty(UUID playerId) {
        UUID partyId = playerPartyMap.get(playerId);
        return partyId != null ? Optional.ofNullable(parties.get(partyId)) : Optional.empty();
    }
    
    public boolean isInParty(UUID playerId) {
        return playerPartyMap.containsKey(playerId);
    }
    
    public boolean isPartyLeader(UUID playerId) {
        return getPlayerParty(playerId)
            .map(party -> party.getLeader().equals(playerId))
            .orElse(false);
    }
    
    public boolean areInSameParty(UUID player1, UUID player2) {
        UUID party1 = playerPartyMap.get(player1);
        UUID party2 = playerPartyMap.get(player2);
        return party1 != null && party1.equals(party2);
    }
    
    public List<Party> getOpenParties() {
        return parties.values().stream()
            .filter(p -> p.getPrivacy() == Party.PartyPrivacy.OPEN)
            .toList();
    }
    
    public void cleanupExpiredInvites() {
        pendingInvites.values().removeIf(PartyInvite::isExpired);
    }
    
    public void onPartyCreated(Consumer<Party> callback) {
        createListeners.add(callback);
    }
    
    public void onPartyDisbanded(Consumer<Party> callback) {
        disbandListeners.add(callback);
    }
    
    public void onMemberJoined(BiConsumer<Party, UUID> callback) {
        joinListeners.add(callback);
    }
    
    public void onMemberLeft(BiConsumer<Party, UUID> callback) {
        leaveListeners.add(callback);
    }
    
    private void notifyJoin(Party party, UUID member) {
        for (BiConsumer<Party, UUID> listener : joinListeners) {
            listener.accept(party, member);
        }
    }
    
    private void notifyLeave(Party party, UUID member) {
        for (BiConsumer<Party, UUID> listener : leaveListeners) {
            listener.accept(party, member);
        }
    }
    
    public int getPartyCount() {
        return parties.size();
    }
    
    public int getTotalPlayersInParties() {
        return playerPartyMap.size();
    }
}
