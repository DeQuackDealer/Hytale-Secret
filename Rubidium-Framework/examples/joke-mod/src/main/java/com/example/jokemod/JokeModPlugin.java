package com.example.jokemod;

import rubidium.api.RubidiumPlugin;
import rubidium.api.chat.ChatAPI;
import rubidium.command.RubidiumCommandAPI;

public class JokeModPlugin extends RubidiumPlugin {
    
    @Override
    public void onEnable() {
        getLogger().info("JokeMod enabled!");
        
        RubidiumCommandAPI.command("joke")
            .description("Tell a joke - a bot will say 67")
            .executor((ctx, args) -> {
                ChatAPI.sendAsBot("JokeBot", "67");
                ctx.success("Joke delivered!");
                return true;
            })
            .register();
        
        getLogger().info("Registered /joke command");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("JokeMod disabled!");
    }
}
