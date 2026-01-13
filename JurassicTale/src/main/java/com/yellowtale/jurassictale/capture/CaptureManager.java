package com.yellowtale.jurassictale.capture;

import com.yellowtale.rubidium.core.logging.RubidiumLogger;
import com.yellowtale.jurassictale.dino.DinoManager;
import com.yellowtale.jurassictale.dino.DinoEntity;
import com.yellowtale.jurassictale.dino.types.CaptureDifficulty;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CaptureManager {
    
    private final RubidiumLogger logger;
    private final DinoManager dinoManager;
    
    private final Map<UUID, CaptureSession> activeSessions;
    private final Map<UUID, CapturedDino> capturedDinos;
    
    public CaptureManager(RubidiumLogger logger, DinoManager dinoManager) {
        this.logger = logger;
        this.dinoManager = dinoManager;
        this.activeSessions = new ConcurrentHashMap<>();
        this.capturedDinos = new ConcurrentHashMap<>();
    }
    
    public void applyTranqDamage(UUID dinoId, int torpor, UUID shooterId) {
        Optional<DinoEntity> entityOpt = dinoManager.getEntity(dinoId);
        if (entityOpt.isEmpty()) return;
        
        DinoEntity entity = entityOpt.get();
        CaptureDifficulty difficulty = entity.getDefinition().getCaptureDifficulty();
        
        int effectiveTorpor = (int) (torpor * difficulty.getTranqEffectiveness());
        entity.addTorpor(effectiveTorpor);
        
        if (entity.isTransquilized()) {
            startCaptureSession(entity, shooterId);
        }
    }
    
    public void applyNet(UUID dinoId, int duration, UUID throwerId) {
        Optional<DinoEntity> entityOpt = dinoManager.getEntity(dinoId);
        if (entityOpt.isEmpty()) return;
        
        DinoEntity entity = entityOpt.get();
        
        if (entity.getDefinition().getTransportClass().getTier() <= 2) {
            entity.setState(DinoEntity.DinoState.CAPTURED);
            
            CaptureSession session = new CaptureSession(
                UUID.randomUUID(),
                dinoId,
                throwerId,
                CaptureMethod.NET,
                System.currentTimeMillis()
            );
            session.setNetExpiry(System.currentTimeMillis() + (duration * 50L));
            activeSessions.put(session.getId(), session);
            
            logger.debug("Net applied to {} by {}", entity.getDefinition().getName(), throwerId);
        }
    }
    
    public void applyBola(UUID dinoId, int duration, UUID throwerId) {
        Optional<DinoEntity> entityOpt = dinoManager.getEntity(dinoId);
        if (entityOpt.isEmpty()) return;
        
        DinoEntity entity = entityOpt.get();
        
        if (entity.getDefinition().getTransportClass().getTier() <= 1) {
            entity.setState(DinoEntity.DinoState.CAPTURED);
            
            CaptureSession session = new CaptureSession(
                UUID.randomUUID(),
                dinoId,
                throwerId,
                CaptureMethod.BOLA,
                System.currentTimeMillis()
            );
            session.setBolaExpiry(System.currentTimeMillis() + (duration * 50L));
            activeSessions.put(session.getId(), session);
        }
    }
    
    private void startCaptureSession(DinoEntity entity, UUID captorId) {
        CaptureSession session = new CaptureSession(
            UUID.randomUUID(),
            entity.getId(),
            captorId,
            CaptureMethod.TRANQ,
            System.currentTimeMillis()
        );
        activeSessions.put(session.getId(), session);
        
        logger.info("{} has been tranquilized by {}", 
            entity.getDefinition().getName(), captorId);
    }
    
    public boolean attemptCapture(UUID sessionId) {
        CaptureSession session = activeSessions.get(sessionId);
        if (session == null) return false;
        
        Optional<DinoEntity> entityOpt = dinoManager.getEntity(session.getDinoId());
        if (entityOpt.isEmpty()) {
            activeSessions.remove(sessionId);
            return false;
        }
        
        DinoEntity entity = entityOpt.get();
        double successRate = entity.getDefinition().getCaptureDifficulty().getCaptureSuccessRate();
        
        if (Math.random() < successRate) {
            CapturedDino captured = new CapturedDino(
                UUID.randomUUID(),
                entity.getDefinition().getId(),
                session.getCaptorId(),
                System.currentTimeMillis()
            );
            capturedDinos.put(captured.getId(), captured);
            
            entity.setCaptured(true);
            dinoManager.despawn(entity.getId());
            activeSessions.remove(sessionId);
            
            logger.info("{} successfully captured!", entity.getDefinition().getName());
            return true;
        }
        
        return false;
    }
    
    public void tick() {
        long now = System.currentTimeMillis();
        
        Iterator<Map.Entry<UUID, CaptureSession>> it = activeSessions.entrySet().iterator();
        while (it.hasNext()) {
            CaptureSession session = it.next().getValue();
            
            if (session.isExpired(now)) {
                Optional<DinoEntity> entityOpt = dinoManager.getEntity(session.getDinoId());
                if (entityOpt.isPresent()) {
                    DinoEntity entity = entityOpt.get();
                    entity.setState(DinoEntity.DinoState.IDLE);
                    entity.reduceTorpor(entity.getTorpor());
                }
                it.remove();
            }
        }
    }
    
    public List<CapturedDino> getPlayerCapturedDinos(UUID playerId) {
        return capturedDinos.values().stream()
            .filter(c -> c.getOwnerId().equals(playerId))
            .toList();
    }
    
    public int getActiveCaptureSessionCount() {
        return activeSessions.size();
    }
    
    public enum CaptureMethod {
        TRANQ, NET, BOLA, TRAP
    }
    
    public static class CaptureSession {
        private final UUID id;
        private final UUID dinoId;
        private final UUID captorId;
        private final CaptureMethod method;
        private final long startTime;
        
        private long netExpiry = Long.MAX_VALUE;
        private long bolaExpiry = Long.MAX_VALUE;
        
        public CaptureSession(UUID id, UUID dinoId, UUID captorId, CaptureMethod method, long startTime) {
            this.id = id;
            this.dinoId = dinoId;
            this.captorId = captorId;
            this.method = method;
            this.startTime = startTime;
        }
        
        public UUID getId() { return id; }
        public UUID getDinoId() { return dinoId; }
        public UUID getCaptorId() { return captorId; }
        public CaptureMethod getMethod() { return method; }
        public long getStartTime() { return startTime; }
        
        public void setNetExpiry(long expiry) { this.netExpiry = expiry; }
        public void setBolaExpiry(long expiry) { this.bolaExpiry = expiry; }
        
        public boolean isExpired(long now) {
            return now > netExpiry || now > bolaExpiry;
        }
    }
    
    public record CapturedDino(UUID id, String definitionId, UUID ownerId, long capturedAt) {}
}
