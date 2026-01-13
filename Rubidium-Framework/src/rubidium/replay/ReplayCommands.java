package rubidium.replay;

import rubidium.api.player.Player;
import rubidium.core.logging.RubidiumLogger;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class ReplayCommands {
    
    private final ModeratorReplayFeature replayFeature;
    private final RubidiumLogger logger;
    private final Function<String, Optional<Player>> playerLookup;
    private final BiConsumer<Player, String> messageSender;
    
    public ReplayCommands(
            ModeratorReplayFeature replayFeature,
            RubidiumLogger logger,
            Function<String, Optional<Player>> playerLookup,
            BiConsumer<Player, String> messageSender) {
        this.replayFeature = replayFeature;
        this.logger = logger;
        this.playerLookup = playerLookup;
        this.messageSender = messageSender;
    }
    
    public String handleCommand(Player sender, String[] args) {
        if (args.length == 0) {
            return getUsage();
        }
        
        String subcommand = args[0].toLowerCase();
        
        return switch (subcommand) {
            case "record" -> handleRecord(sender, args);
            case "stop" -> handleStop(sender, args);
            case "list" -> handleList(sender, args);
            case "info" -> handleInfo(sender, args);
            case "review" -> handleReview(sender, args);
            case "purge" -> handlePurge(sender, args);
            case "status" -> handleStatus(sender);
            case "config" -> handleConfig(sender, args);
            default -> "Unknown subcommand: " + subcommand + "\n" + getUsage();
        };
    }
    
    private String getUsage() {
        return """
            Replay Commands:
              /replay record <player> [reason]  - Start recording a player
              /replay stop <player>             - Stop recording a player
              /replay list [player]             - List replay sessions
              /replay info <session-id>         - Show session details
              /replay review <session-id>       - Review a replay session
              /replay purge <player>            - Delete all replays for player
              /replay status                    - Show system status
              /replay config <key> [value]      - View/set configuration
            """;
    }
    
    private String handleRecord(Player sender, String[] args) {
        if (args.length < 2) {
            return "Usage: /replay record <player> [reason]";
        }
        
        Optional<Player> target = playerLookup.apply(args[1]);
        if (target.isEmpty()) {
            return "Player not found: " + args[1];
        }
        
        Player player = target.get();
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) 
            : "Manual recording by " + sender.getName();
        
        if (replayFeature.getActiveSession(player.getUuid()) != null) {
            return "Already recording " + player.getName();
        }
        
        replayFeature.startRecording(
            player.getUuid(), 
            player.getName(), 
            ReplaySession.TriggerType.MANUAL, 
            reason
        );
        
        return "Started recording " + player.getName();
    }
    
    private String handleStop(Player sender, String[] args) {
        if (args.length < 2) {
            return "Usage: /replay stop <player>";
        }
        
        Optional<Player> target = playerLookup.apply(args[1]);
        if (target.isEmpty()) {
            return "Player not found: " + args[1];
        }
        
        Player player = target.get();
        
        if (replayFeature.getActiveSession(player.getUuid()) == null) {
            return "Not recording " + player.getName();
        }
        
        replayFeature.stopRecording(player.getUuid());
        
        return "Stopped recording " + player.getName();
    }
    
    private String handleList(Player sender, String[] args) {
        Set<UUID> recording = replayFeature.getRecordingPlayers();
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== Active Recordings ===\n");
        
        if (recording.isEmpty()) {
            sb.append("No active recordings.\n");
        } else {
            for (UUID playerId : recording) {
                ReplaySession session = replayFeature.getActiveSession(playerId);
                if (session != null) {
                    Duration elapsed = Duration.between(session.getStartTime(), Instant.now());
                    sb.append(String.format(" - %s (%s): %s, %d frames\n",
                        session.getPlayerName(),
                        session.getTriggerType(),
                        formatDuration(elapsed),
                        session.getTotalFrames()
                    ));
                }
            }
        }
        
        if (args.length > 1) {
            Optional<Player> target = playerLookup.apply(args[1]);
            if (target.isPresent()) {
                sb.append("\n=== Saved Sessions for ").append(target.get().getName()).append(" ===\n");
                List<ReplaySession.SessionMetadata> sessions = listSavedSessions(target.get().getUuid());
                if (sessions.isEmpty()) {
                    sb.append("No saved sessions.\n");
                } else {
                    for (ReplaySession.SessionMetadata meta : sessions) {
                        sb.append(String.format(" - %s: %s, %d frames, %s\n",
                            meta.sessionId().toString().substring(0, 8),
                            meta.triggerType(),
                            meta.totalFrames(),
                            formatBytes(meta.fileSizeBytes())
                        ));
                    }
                }
            }
        }
        
        return sb.toString();
    }
    
    private String handleInfo(Player sender, String[] args) {
        if (args.length < 2) {
            return "Usage: /replay info <session-id>";
        }
        
        UUID sessionId;
        try {
            sessionId = parseSessionId(args[1]);
        } catch (IllegalArgumentException e) {
            return "Invalid session ID: " + args[1];
        }
        
        Path sessionDir = findSessionDir(sessionId);
        if (sessionDir == null) {
            return "Session not found: " + args[1];
        }
        
        try {
            ReplaySession session = ReplaySession.load(sessionDir);
            ReplaySession.SessionMetadata meta = session.getMetadata();
            
            return String.format("""
                === Replay Session ===
                ID: %s
                Player: %s
                Trigger: %s
                Reason: %s
                Started: %s
                Ended: %s
                Duration: %s
                Frames: %d
                Segments: %d
                Size: %s
                """,
                meta.sessionId(),
                meta.playerName(),
                meta.triggerType(),
                meta.triggerReason(),
                meta.startTime(),
                meta.endTime(),
                meta.endTime() != null ? formatDuration(Duration.between(meta.startTime(), meta.endTime())) : "In progress",
                meta.totalFrames(),
                meta.segmentCount(),
                formatBytes(meta.fileSizeBytes())
            );
        } catch (IOException e) {
            return "Failed to load session: " + e.getMessage();
        }
    }
    
    private String handleReview(Player sender, String[] args) {
        if (args.length < 2) {
            return "Usage: /replay review <session-id>";
        }
        
        UUID sessionId;
        try {
            sessionId = parseSessionId(args[1]);
        } catch (IllegalArgumentException e) {
            return "Invalid session ID: " + args[1];
        }
        
        Path sessionDir = findSessionDir(sessionId);
        if (sessionDir == null) {
            return "Session not found: " + args[1];
        }
        
        try {
            ReplaySession session = ReplaySession.load(sessionDir);
            
            return String.format("""
                Loaded replay session for %s
                Total frames: %d
                Use /replay play to start playback
                Use /replay seek <time> to jump to a specific time
                Use /replay speed <0.5|1|2> to change playback speed
                """,
                session.getPlayerName(),
                session.getTotalFrames()
            );
        } catch (IOException e) {
            return "Failed to load session: " + e.getMessage();
        }
    }
    
    private String handlePurge(Player sender, String[] args) {
        if (args.length < 2) {
            return "Usage: /replay purge <player>";
        }
        
        Optional<Player> target = playerLookup.apply(args[1]);
        UUID playerId;
        String playerName;
        
        if (target.isPresent()) {
            playerId = target.get().getUuid();
            playerName = target.get().getName();
        } else {
            try {
                playerId = UUID.fromString(args[1]);
                playerName = args[1];
            } catch (IllegalArgumentException e) {
                return "Player not found: " + args[1];
            }
        }
        
        replayFeature.purgePlayerData(playerId);
        
        return "Purged all replay data for " + playerName;
    }
    
    private String handleStatus(Player sender) {
        ModeratorReplayFeature.ReplayConfig config = replayFeature.getConfig();
        
        return String.format("""
            === Replay System Status ===
            Enabled: %s
            Target FPS: %d
            Capture Radius: %d blocks
            Buffer Duration: %d minutes
            
            Active Buffers: %d
            Active Sessions: %d
            Total Frames Captured: %d
            Total Sessions Created: %d
            Dropped Frames (Low TPS): %d
            
            Frame Pool Hit Rate: %.1f%%
            Storage Dir: %s
            """,
            replayFeature.isEnabled() ? "Yes" : "No",
            config.targetFps(),
            config.captureRadius(),
            config.bufferDuration().toMinutes(),
            replayFeature.getActiveBufferCount(),
            replayFeature.getActiveSessionCount(),
            replayFeature.getTotalFramesCaptured(),
            replayFeature.getTotalSessionsCreated(),
            replayFeature.getDroppedFramesDueToTps(),
            replayFeature.getFramePoolHitRate() * 100,
            config.storageDir()
        );
    }
    
    private String handleConfig(Player sender, String[] args) {
        if (args.length < 2) {
            ModeratorReplayFeature.ReplayConfig config = replayFeature.getConfig();
            return String.format("""
                === Replay Configuration ===
                targetFps: %d
                captureRadius: %d
                bufferDuration: %d minutes
                segmentDuration: %d seconds
                continuousMode: %s
                recordOnSuspicion: %s
                recordOnCombat: %s
                recordOnReport: %s
                minTpsThreshold: %.1f
                
                Use /replay config <key> <value> to change settings
                """,
                config.targetFps(),
                config.captureRadius(),
                config.bufferDuration().toMinutes(),
                config.segmentDuration().toSeconds(),
                config.continuousMode(),
                config.recordOnSuspicion(),
                config.recordOnCombat(),
                config.recordOnReport(),
                config.minTpsThreshold()
            );
        }
        
        String key = args[1].toLowerCase();
        if (args.length < 3) {
            return "Usage: /replay config " + key + " <value>";
        }
        
        String value = args[2];
        ModeratorReplayFeature.ReplayConfig current = replayFeature.getConfig();
        
        try {
            ModeratorReplayFeature.ReplayConfig newConfig = switch (key) {
                case "targetfps" -> new ModeratorReplayFeature.ReplayConfig(
                    Integer.parseInt(value), current.captureRadius(), current.bufferDuration(),
                    current.segmentDuration(), current.continuousMode(), current.recordOnSuspicion(),
                    current.recordOnCombat(), current.recordOnReport(), current.storageDir(),
                    current.maxStorageBytes(), current.maxStoragePerPlayerBytes(),
                    current.retentionPeriod(), current.compressionWorkers(), current.minTpsThreshold()
                );
                case "captureradius" -> new ModeratorReplayFeature.ReplayConfig(
                    current.targetFps(), Integer.parseInt(value), current.bufferDuration(),
                    current.segmentDuration(), current.continuousMode(), current.recordOnSuspicion(),
                    current.recordOnCombat(), current.recordOnReport(), current.storageDir(),
                    current.maxStorageBytes(), current.maxStoragePerPlayerBytes(),
                    current.retentionPeriod(), current.compressionWorkers(), current.minTpsThreshold()
                );
                case "continuousmode" -> new ModeratorReplayFeature.ReplayConfig(
                    current.targetFps(), current.captureRadius(), current.bufferDuration(),
                    current.segmentDuration(), Boolean.parseBoolean(value), current.recordOnSuspicion(),
                    current.recordOnCombat(), current.recordOnReport(), current.storageDir(),
                    current.maxStorageBytes(), current.maxStoragePerPlayerBytes(),
                    current.retentionPeriod(), current.compressionWorkers(), current.minTpsThreshold()
                );
                case "recordonsuspicion" -> new ModeratorReplayFeature.ReplayConfig(
                    current.targetFps(), current.captureRadius(), current.bufferDuration(),
                    current.segmentDuration(), current.continuousMode(), Boolean.parseBoolean(value),
                    current.recordOnCombat(), current.recordOnReport(), current.storageDir(),
                    current.maxStorageBytes(), current.maxStoragePerPlayerBytes(),
                    current.retentionPeriod(), current.compressionWorkers(), current.minTpsThreshold()
                );
                case "mintpsthreshold" -> new ModeratorReplayFeature.ReplayConfig(
                    current.targetFps(), current.captureRadius(), current.bufferDuration(),
                    current.segmentDuration(), current.continuousMode(), current.recordOnSuspicion(),
                    current.recordOnCombat(), current.recordOnReport(), current.storageDir(),
                    current.maxStorageBytes(), current.maxStoragePerPlayerBytes(),
                    current.retentionPeriod(), current.compressionWorkers(), Double.parseDouble(value)
                );
                default -> throw new IllegalArgumentException("Unknown config key: " + key);
            };
            
            replayFeature.setConfig(newConfig);
            return "Updated " + key + " to " + value;
            
        } catch (NumberFormatException e) {
            return "Invalid value for " + key + ": " + value;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
    
    private UUID parseSessionId(String input) {
        if (input.length() == 8) {
            Path storageDir = replayFeature.getConfig().storageDir();
            try (Stream<Path> players = Files.list(storageDir)) {
                Optional<UUID> found = players
                    .filter(Files::isDirectory)
                    .flatMap(p -> {
                        try {
                            return Files.list(p);
                        } catch (IOException e) {
                            return Stream.empty();
                        }
                    })
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.startsWith(input))
                    .map(UUID::fromString)
                    .findFirst();
                
                if (found.isPresent()) {
                    return found.get();
                }
            } catch (IOException e) {
                // Fall through to full UUID parse
            }
        }
        
        return UUID.fromString(input);
    }
    
    private Path findSessionDir(UUID sessionId) {
        Path storageDir = replayFeature.getConfig().storageDir();
        
        try (Stream<Path> players = Files.list(storageDir)) {
            return players
                .filter(Files::isDirectory)
                .flatMap(p -> {
                    try {
                        return Files.list(p);
                    } catch (IOException e) {
                        return Stream.empty();
                    }
                })
                .filter(p -> p.getFileName().toString().equals(sessionId.toString()))
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }
    
    private List<ReplaySession.SessionMetadata> listSavedSessions(UUID playerId) {
        Path playerDir = replayFeature.getConfig().storageDir().resolve(playerId.toString());
        if (!Files.exists(playerDir)) {
            return Collections.emptyList();
        }
        
        List<ReplaySession.SessionMetadata> sessions = new ArrayList<>();
        try (Stream<Path> sessionDirs = Files.list(playerDir)) {
            sessionDirs.filter(Files::isDirectory).forEach(dir -> {
                try {
                    ReplaySession session = ReplaySession.load(dir);
                    sessions.add(session.getMetadata());
                } catch (IOException e) {
                    // Skip invalid sessions
                }
            });
        } catch (IOException e) {
            logger.error("Failed to list sessions", e);
        }
        
        return sessions;
    }
    
    private String formatDuration(Duration duration) {
        long seconds = duration.toSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
