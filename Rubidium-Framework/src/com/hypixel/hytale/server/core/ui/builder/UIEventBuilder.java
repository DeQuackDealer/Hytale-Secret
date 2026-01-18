package com.hypixel.hytale.server.core.ui.builder;

import com.hypixel.hytale.codec.builder.BuilderCodec;

public class UIEventBuilder {
    
    public <T> UIEventBuilder on(String event, BuilderCodec<T> codec) {
        return this;
    }
    
    public UIEventBuilder on(String event) {
        return this;
    }
}
