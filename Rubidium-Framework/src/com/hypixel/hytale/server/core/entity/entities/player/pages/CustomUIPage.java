package com.hypixel.hytale.server.core.entity.entities.player.pages;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public abstract class CustomUIPage {
    
    protected final PlayerRef playerRef;
    protected CustomPageLifetime lifetime;
    
    public CustomUIPage(PlayerRef playerRef, CustomPageLifetime lifetime) {
        this.playerRef = playerRef;
        this.lifetime = lifetime;
    }
    
    public void setLifetime(CustomPageLifetime lifetime) {
        this.lifetime = lifetime;
    }
    
    public CustomPageLifetime getLifetime() {
        return lifetime;
    }
    
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, String data) {
    }
    
    public abstract void build(Ref<EntityStore> ref, UICommandBuilder ui, UIEventBuilder events, Store<EntityStore> store);
    
    protected void rebuild() {
    }
    
    protected void sendUpdate() {
    }
    
    protected void sendUpdate(UICommandBuilder builder) {
    }
    
    protected void sendUpdate(UICommandBuilder builder, boolean reset) {
    }
    
    protected void close() {
    }
    
    public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
    }
}
