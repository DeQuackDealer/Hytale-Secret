package rubidium.flight;

import rubidium.hytale.api.player.Player;
import rubidium.core.RubidiumLogger;
import rubidium.commands.Command;
import rubidium.commands.CommandContext;
import rubidium.commands.CommandResult;

import java.util.*;
import java.util.concurrent.*;

public final class FlightSpeedManager {
    
    public record FlightConfig(
        float defaultSpeed,
        float minSpeed,
        float maxSpeed,
        float creativeMaxSpeed,
        float spectatorMaxSpeed,
        boolean persistSpeeds,
        boolean notifyOnChange
    ) {
        public static FlightConfig defaults() {
            return new FlightConfig(
                1.0f,
                0.1f,
                10.0f,
                5.0f,
                10.0f,
                true,
                true
            );
        }
    }
    
    public record PlayerFlightState(
        UUID playerId,
        float currentSpeed,
        float walkSpeed,
        boolean isFlying,
        boolean canFly,
        FlightMode mode
    ) {}
    
    public enum FlightMode {
        CREATIVE,
        SPECTATOR,
        CUSTOM,
        DISABLED
    }
    
    public record SpeedPreset(
        String name,
        float speed,
        String permission
    ) {}
    
    private final RubidiumLogger logger;
    private final Map<UUID, PlayerFlightState> playerStates = new ConcurrentHashMap<>();
    private final Map<UUID, Float> persistedSpeeds = new ConcurrentHashMap<>();
    private final Map<String, SpeedPreset> presets = new ConcurrentHashMap<>();
    
    private java.util.function.BiConsumer<Player, Float> flightSpeedApplier;
    private java.util.function.BiConsumer<Player, Float> walkSpeedApplier;
    private java.util.function.BiConsumer<Player, Boolean> flyModeApplier;
    
    private volatile FlightConfig config = FlightConfig.defaults();
    
    public FlightSpeedManager(RubidiumLogger logger) {
        this.logger = logger;
        initializeDefaultPresets();
    }
    
    private void initializeDefaultPresets() {
        presets.put("default", new SpeedPreset("Default", 1.0f, null));
        presets.put("slow", new SpeedPreset("Slow", 0.5f, "rubidium.flight.preset.slow"));
        presets.put("normal", new SpeedPreset("Normal", 1.0f, "rubidium.flight.preset.normal"));
        presets.put("fast", new SpeedPreset("Fast", 2.0f, "rubidium.flight.preset.fast"));
        presets.put("veryfast", new SpeedPreset("Very Fast", 3.0f, "rubidium.flight.preset.veryfast"));
        presets.put("sonic", new SpeedPreset("Sonic", 5.0f, "rubidium.flight.preset.sonic"));
        presets.put("max", new SpeedPreset("Maximum", 10.0f, "rubidium.flight.preset.max"));
    }
    
    public void setFlightSpeedApplier(java.util.function.BiConsumer<Player, Float> applier) {
        this.flightSpeedApplier = applier;
    }
    
    public void setWalkSpeedApplier(java.util.function.BiConsumer<Player, Float> applier) {
        this.walkSpeedApplier = applier;
    }
    
    public void setFlyModeApplier(java.util.function.BiConsumer<Player, Boolean> applier) {
        this.flyModeApplier = applier;
    }
    
    public void setConfig(FlightConfig config) {
        this.config = config;
    }
    
    public FlightConfig getConfig() {
        return config;
    }
    
    public boolean setFlightSpeed(Player player, float speed) {
        var maxAllowed = getMaxSpeedForPlayer(player);
        
        if (speed < config.minSpeed()) {
            speed = config.minSpeed();
        } else if (speed > maxAllowed) {
            speed = maxAllowed;
        }
        
        var state = getOrCreateState(player);
        var newState = new PlayerFlightState(
            player.getUniqueId(),
            speed,
            state.walkSpeed(),
            state.isFlying(),
            state.canFly(),
            state.mode()
        );
        
        playerStates.put(player.getUniqueId(), newState);
        
        if (config.persistSpeeds()) {
            persistedSpeeds.put(player.getUniqueId(), speed);
        }
        
        if (flightSpeedApplier != null) {
            flightSpeedApplier.accept(player, speed);
        }
        
        if (config.notifyOnChange()) {
            player.sendMessage("&aFlight speed set to &f" + String.format("%.1f", speed) + "x");
        }
        
        logger.debug("Set flight speed for " + player.getName() + " to " + speed);
        return true;
    }
    
    public boolean setWalkSpeed(Player player, float speed) {
        if (speed < 0.1f) speed = 0.1f;
        if (speed > 1.0f) speed = 1.0f;
        
        var state = getOrCreateState(player);
        var newState = new PlayerFlightState(
            player.getUniqueId(),
            state.currentSpeed(),
            speed,
            state.isFlying(),
            state.canFly(),
            state.mode()
        );
        
        playerStates.put(player.getUniqueId(), newState);
        
        if (walkSpeedApplier != null) {
            walkSpeedApplier.accept(player, speed);
        }
        
        if (config.notifyOnChange()) {
            player.sendMessage("&aWalk speed set to &f" + String.format("%.1f", speed * 10) + "x");
        }
        
        return true;
    }
    
    public boolean applyPreset(Player player, String presetName) {
        var preset = presets.get(presetName.toLowerCase());
        if (preset == null) {
            player.sendMessage("&cUnknown speed preset: " + presetName);
            return false;
        }
        
        return setFlightSpeed(player, preset.speed());
    }
    
    public void enableFlight(Player player, FlightMode mode) {
        var state = getOrCreateState(player);
        var newState = new PlayerFlightState(
            player.getUniqueId(),
            state.currentSpeed(),
            state.walkSpeed(),
            true,
            true,
            mode
        );
        
        playerStates.put(player.getUniqueId(), newState);
        
        if (flyModeApplier != null) {
            flyModeApplier.accept(player, true);
        }
        
        var savedSpeed = persistedSpeeds.get(player.getUniqueId());
        if (savedSpeed != null) {
            setFlightSpeed(player, savedSpeed);
        }
    }
    
    public void disableFlight(Player player) {
        var state = getOrCreateState(player);
        var newState = new PlayerFlightState(
            player.getUniqueId(),
            config.defaultSpeed(),
            state.walkSpeed(),
            false,
            false,
            FlightMode.DISABLED
        );
        
        playerStates.put(player.getUniqueId(), newState);
        
        if (flyModeApplier != null) {
            flyModeApplier.accept(player, false);
        }
    }
    
    public float getFlightSpeed(Player player) {
        var state = playerStates.get(player.getUniqueId());
        return state != null ? state.currentSpeed() : config.defaultSpeed();
    }
    
    public Optional<PlayerFlightState> getState(UUID playerId) {
        return Optional.ofNullable(playerStates.get(playerId));
    }
    
    public Collection<SpeedPreset> getPresets() {
        return Collections.unmodifiableCollection(presets.values());
    }
    
    public void registerPreset(String key, SpeedPreset preset) {
        presets.put(key.toLowerCase(), preset);
    }
    
    private float getMaxSpeedForPlayer(Player player) {
        return config.creativeMaxSpeed();
    }
    
    private PlayerFlightState getOrCreateState(Player player) {
        return playerStates.computeIfAbsent(player.getUniqueId(), id ->
            new PlayerFlightState(
                id,
                config.defaultSpeed(),
                0.2f,
                false,
                false,
                FlightMode.DISABLED
            )
        );
    }
    
    public void handlePlayerJoin(Player player) {
        var savedSpeed = persistedSpeeds.get(player.getUniqueId());
        if (savedSpeed != null && flightSpeedApplier != null) {
            flightSpeedApplier.accept(player, savedSpeed);
        }
    }
    
    public void handlePlayerQuit(UUID playerId) {
        if (!config.persistSpeeds()) {
            playerStates.remove(playerId);
        }
    }
    
    @Command(
        name = "flyspeed",
        aliases = {"fs", "flightspeed"},
        description = "Set your flight speed",
        usage = "/flyspeed <speed|preset> [player]",
        permission = "rubidium.flight.speed"
    )
    public CommandResult flySpeedCommand(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player player)) {
            ctx.sender().sendMessage("&cThis command can only be used by players.");
            return CommandResult.failure("Not a player");
        }
        
        if (ctx.args().length == 0) {
            showFlightSpeedHelp(player);
            return CommandResult.success();
        }
        
        var input = ctx.args()[0].toLowerCase();
        
        if (presets.containsKey(input)) {
            applyPreset(player, input);
            return CommandResult.success();
        }
        
        try {
            var speed = Float.parseFloat(input);
            setFlightSpeed(player, speed);
        } catch (NumberFormatException e) {
            player.sendMessage("&cInvalid speed. Use a number or preset name.");
            showFlightSpeedHelp(player);
            return CommandResult.failure("Invalid speed");
        }
        
        return CommandResult.success();
    }
    
    private void showFlightSpeedHelp(Player player) {
        var currentSpeed = getFlightSpeed(player);
        player.sendMessage("&6=== Flight Speed ===");
        player.sendMessage("&7Current speed: &f" + String.format("%.1f", currentSpeed) + "x");
        player.sendMessage("");
        player.sendMessage("&7Usage: /flyspeed <speed|preset>");
        player.sendMessage("");
        player.sendMessage("&7Available presets:");
        
        var presetList = new StringBuilder();
        for (var preset : presets.values()) {
            presetList.append("&e").append(preset.name().toLowerCase())
                      .append(" &7(").append(preset.speed()).append("x), ");
        }
        player.sendMessage(presetList.toString());
    }
    
    @Command(
        name = "walkspeed",
        aliases = {"ws"},
        description = "Set your walking speed",
        usage = "/walkspeed <speed>",
        permission = "rubidium.flight.walkspeed"
    )
    public CommandResult walkSpeedCommand(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player player)) {
            ctx.sender().sendMessage("&cThis command can only be used by players.");
            return CommandResult.failure("Not a player");
        }
        
        if (ctx.args().length == 0) {
            var state = getOrCreateState(player);
            player.sendMessage("&7Current walk speed: &f" + String.format("%.1f", state.walkSpeed() * 10) + "x");
            player.sendMessage("&7Usage: /walkspeed <0.1-1.0>");
            return CommandResult.success();
        }
        
        try {
            var speed = Float.parseFloat(ctx.args()[0]);
            setWalkSpeed(player, speed);
        } catch (NumberFormatException e) {
            player.sendMessage("&cInvalid speed. Use a number between 0.1 and 1.0.");
            return CommandResult.failure("Invalid speed");
        }
        
        return CommandResult.success();
    }
    
    @Command(
        name = "fly",
        description = "Toggle flight mode",
        usage = "/fly [player]",
        permission = "rubidium.flight.toggle"
    )
    public CommandResult flyCommand(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player player)) {
            ctx.sender().sendMessage("&cThis command can only be used by players.");
            return CommandResult.failure("Not a player");
        }
        
        var state = getOrCreateState(player);
        
        if (state.isFlying()) {
            disableFlight(player);
            player.sendMessage("&cFlight disabled.");
        } else {
            enableFlight(player, FlightMode.CREATIVE);
            player.sendMessage("&aFlight enabled.");
        }
        
        return CommandResult.success();
    }
}
