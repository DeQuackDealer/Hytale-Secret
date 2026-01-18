package com.hypixel.hytale.server.core.entity.entities.player.hud;

import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.HashSet;
import java.util.Set;

public class HudManager {
    
    private final Set<HudComponent> visibleHudComponents = new HashSet<>();
    private CustomUIHud customHud;
    
    public HudManager() {
    }
    
    public CustomUIHud getCustomHud() {
        return customHud;
    }
    
    public Set<HudComponent> getVisibleHudComponents() {
        return visibleHudComponents;
    }
    
    public void setVisibleHudComponents(PlayerRef playerRef, HudComponent... components) {
        visibleHudComponents.clear();
        for (HudComponent c : components) {
            visibleHudComponents.add(c);
        }
    }
    
    public void setVisibleHudComponents(PlayerRef playerRef, Set<HudComponent> components) {
        visibleHudComponents.clear();
        visibleHudComponents.addAll(components);
    }
    
    public void showHudComponents(PlayerRef playerRef, HudComponent... components) {
        for (HudComponent c : components) {
            visibleHudComponents.add(c);
        }
    }
    
    public void showHudComponents(PlayerRef playerRef, Set<HudComponent> components) {
        visibleHudComponents.addAll(components);
    }
    
    public void hideHudComponents(PlayerRef playerRef, HudComponent... components) {
        for (HudComponent c : components) {
            visibleHudComponents.remove(c);
        }
    }
    
    public void setCustomHud(PlayerRef playerRef, CustomUIHud hud) {
        this.customHud = hud;
    }
    
    public void resetHud(PlayerRef playerRef) {
        customHud = null;
    }
    
    public void resetUserInterface(PlayerRef playerRef) {
        customHud = null;
    }
}
