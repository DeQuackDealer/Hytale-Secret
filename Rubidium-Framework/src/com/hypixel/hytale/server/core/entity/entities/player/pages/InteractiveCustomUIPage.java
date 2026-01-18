package com.hypixel.hytale.server.core.entity.entities.player.pages;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public abstract class InteractiveCustomUIPage<T> extends CustomUIPage {
    
    protected final BuilderCodec<T> eventDataCodec;
    
    public InteractiveCustomUIPage(PlayerRef playerRef, CustomPageLifetime lifetime, BuilderCodec<T> eventDataCodec) {
        super(playerRef, lifetime);
        this.eventDataCodec = eventDataCodec;
    }
    
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, T data) {
    }
    
    protected void sendUpdate(UICommandBuilder ui, UIEventBuilder events, boolean reset) {
    }
    
    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, String data) {
        if (eventDataCodec != null && data != null) {
            T decoded = eventDataCodec.decode(data);
            handleDataEvent(ref, store, decoded);
        }
    }
}
