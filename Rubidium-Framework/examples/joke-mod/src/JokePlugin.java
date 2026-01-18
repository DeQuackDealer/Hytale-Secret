package com.example.jokemod;

import rubidium.api.plugin.RubidiumPlugin;
import rubidium.api.plugin.PluginInfo;
import rubidium.api.command.CommandAPI;
import rubidium.api.chat.ChatAPI;
import rubidium.api.npc.NPCAPI;
import rubidium.api.npc.NPCAPI.NPC;
import rubidium.api.npc.NPCAPI.NPCDefinition;
import rubidium.api.common.Vec3i;

import java.util.UUID;

@PluginInfo(
    id = "joke-mod",
    name = "Joke Mod",
    version = "1.0.0",
    author = "Rubidium Examples",
    description = "Example mod demonstrating ChatAPI and NPC speech"
)
public class JokePlugin extends RubidiumPlugin {
    
    private NPC comedianBot;
    
    @Override
    public void onEnable() {
        getLogger().info("Joke Mod enabled!");
        
        NPCDefinition definition = new NPCDefinition.Builder("comedian_bot")
            .displayName("Comedian Bot")
            .type(NPCAPI.NPCType.HUMANOID)
            .showNameTag(true)
            .invulnerable(true)
            .interactable(false)
            .build();
        
        NPCAPI.registerDefinition(definition);
        
        comedianBot = NPCAPI.spawn("comedian_bot", new Vec3i(0, 64, 0));
        
        registerJokeCommand();
    }
    
    private void registerJokeCommand() {
        CommandAPI.register(CommandAPI.command("joke")
            .description("Tells a joke")
            .permission("joke.use")
            .handler(context -> {
                comedianBot.speak("67");
                
                ChatAPI.tip(context.getPlayer(), "The comedian bot just told a joke!");
                
                return CommandAPI.CommandResult.success();
            })
            .build());
        
        CommandAPI.register(CommandAPI.command("telljoke")
            .description("Bot tells a custom joke")
            .usage("/telljoke <message>")
            .permission("joke.custom")
            .handler(context -> {
                if (context.getArgs().length == 0) {
                    ChatAPI.error(context.getPlayer(), "Usage: /telljoke <message>");
                    return CommandAPI.CommandResult.failure("Missing message argument");
                }
                
                String joke = String.join(" ", context.getArgs());
                comedianBot.speak(joke);
                
                return CommandAPI.CommandResult.success();
            })
            .build());
        
        CommandAPI.register(CommandAPI.command("broadcast")
            .description("Broadcast a message as the bot")
            .permission("joke.broadcast")
            .handler(context -> {
                if (context.getArgs().length == 0) {
                    ChatAPI.error(context.getPlayer(), "Usage: /broadcast <message>");
                    return CommandAPI.CommandResult.failure("Missing message argument");
                }
                
                String message = String.join(" ", context.getArgs());
                ChatAPI.sendAsBot("Comedian", message);
                
                return CommandAPI.CommandResult.success();
            })
            .build());
        
        getLogger().info("Registered /joke, /telljoke, and /broadcast commands");
    }
    
    @Override
    public void onDisable() {
        if (comedianBot != null) {
            NPCAPI.despawn(comedianBot.getId());
        }
        getLogger().info("Joke Mod disabled!");
    }
}
