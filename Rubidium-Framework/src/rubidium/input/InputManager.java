package rubidium.input;

import rubidium.api.player.Player;
import rubidium.core.logging.RubidiumLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class InputManager {
    
    private final RubidiumLogger logger;
    private final Map<UUID, InputSession> activeSessions;
    private final Map<String, InputValidator> validators;
    
    public InputManager(RubidiumLogger logger) {
        this.logger = logger;
        this.activeSessions = new ConcurrentHashMap<>();
        this.validators = new HashMap<>();
        registerDefaultValidators();
    }
    
    private void registerDefaultValidators() {
        validators.put("integer", input -> {
            try { Integer.parseInt(input); return true; }
            catch (NumberFormatException e) { return false; }
        });
        validators.put("double", input -> {
            try { Double.parseDouble(input); return true; }
            catch (NumberFormatException e) { return false; }
        });
        validators.put("positive_integer", input -> {
            try { return Integer.parseInt(input) > 0; }
            catch (NumberFormatException e) { return false; }
        });
        validators.put("alphanumeric", input -> input.matches("^[a-zA-Z0-9]+$"));
        validators.put("username", input -> input.matches("^[a-zA-Z0-9_]{3,16}$"));
        validators.put("non_empty", input -> !input.trim().isEmpty());
    }
    
    public void registerValidator(String name, InputValidator validator) {
        validators.put(name, validator);
    }
    
    public void requestInput(Player player, String prompt, Consumer<String> callback) {
        requestInput(player, prompt, null, callback, null, 60000);
    }
    
    public void requestInput(Player player, String prompt, String validatorName, 
                             Consumer<String> callback, Consumer<Player> timeoutCallback, long timeoutMs) {
        InputValidator validator = validatorName != null ? validators.get(validatorName) : null;
        InputSession session = new InputSession(prompt, validator, callback, timeoutCallback, 
            System.currentTimeMillis() + timeoutMs);
        activeSessions.put(player.getUUID(), session);
        
        player.sendPacket(new InputPromptPacket(prompt));
        logger.debug("Requested input from " + player.getName() + ": " + prompt);
    }
    
    public void requestConfirmation(Player player, String message, 
                                    Consumer<Boolean> callback) {
        requestInput(player, message + " (yes/no)", input -> {
            String lower = input.toLowerCase();
            boolean confirmed = lower.equals("yes") || lower.equals("y") || lower.equals("confirm");
            callback.accept(confirmed);
        });
    }
    
    public void requestNumber(Player player, String prompt, int min, int max, 
                              Consumer<Integer> callback) {
        requestInput(player, prompt + " (" + min + "-" + max + ")", input -> {
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    callback.accept(value);
                } else {
                    player.sendMessage("Number must be between " + min + " and " + max);
                    requestNumber(player, prompt, min, max, callback);
                }
            } catch (NumberFormatException e) {
                player.sendMessage("Please enter a valid number");
                requestNumber(player, prompt, min, max, callback);
            }
        });
    }
    
    public boolean handleInput(Player player, String input) {
        InputSession session = activeSessions.remove(player.getUUID());
        if (session == null) return false;
        
        if (session.validator() != null && !session.validator().validate(input)) {
            player.sendMessage("Invalid input. Please try again.");
            activeSessions.put(player.getUUID(), session);
            return true;
        }
        
        session.callback().accept(input);
        return true;
    }
    
    public void cancelInput(Player player) {
        InputSession session = activeSessions.remove(player.getUUID());
        if (session != null) {
            player.sendMessage("Input cancelled.");
        }
    }
    
    public boolean isAwaitingInput(Player player) {
        return activeSessions.containsKey(player.getUUID());
    }
    
    public void tick() {
        long now = System.currentTimeMillis();
        activeSessions.entrySet().removeIf(entry -> {
            if (now > entry.getValue().expiresAt()) {
                return true;
            }
            return false;
        });
    }
    
    @FunctionalInterface
    public interface InputValidator {
        boolean validate(String input);
    }
    
    public record InputSession(
        String prompt,
        InputValidator validator,
        Consumer<String> callback,
        Consumer<Player> timeoutCallback,
        long expiresAt
    ) {}
    
    public record InputPromptPacket(String prompt) {}
}
