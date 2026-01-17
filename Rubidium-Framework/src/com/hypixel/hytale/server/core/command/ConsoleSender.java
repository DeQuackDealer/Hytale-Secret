package com.hypixel.hytale.server.core.command;

import java.util.UUID;

/**
 * Stub for Hytale's ConsoleSender.
 * At runtime, the real ConsoleSender from HytaleServer.jar will be used.
 */
public class ConsoleSender implements CommandSender {
    
    public static final ConsoleSender INSTANCE = new ConsoleSender();
    private static final UUID CONSOLE_UUID = new UUID(0, 0);
    
    private ConsoleSender() {}
    
    @Override
    public void sendMessage(String message) {
        System.out.println("[Console] " + message);
    }
    
    @Override
    public boolean hasPermission(String permission) {
        return true;
    }
    
    @Override
    public String getName() {
        return "Console";
    }
    
    @Override
    public boolean isPlayer() {
        return false;
    }
    
    @Override
    public boolean isConsole() {
        return true;
    }
    
    @Override
    public UUID getUniqueId() {
        return CONSOLE_UUID;
    }
}
