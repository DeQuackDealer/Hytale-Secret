# Joke Mod - Rubidium Example Plugin

A simple example plugin demonstrating Rubidium's ChatAPI and NPC system.

## Features

- `/joke` - Makes the comedian bot say "67"
- `/telljoke <message>` - Makes the bot say a custom message
- `/broadcast <message>` - Broadcasts a message as a bot

## Building

Copy the source into your mod project and compile against Rubidium API.

## Usage

```java
// Using ChatAPI to send messages
ChatAPI.sendAsBot("BotName", "Hello world!");
ChatAPI.broadcast("Message to all players");
ChatAPI.whisper(fromPlayer, toPlayer, "Private message");

// Using NPC speech
NPC myNpc = NPCAPI.spawn("npc_id", location);
myNpc.speak("Hello, I am an NPC!");

// Or directly with ChatAPI
ChatAPI.sendAsNPC(myNpc, "Another way to make NPCs talk");
```

## Permissions

- `joke.use` - Use the /joke command
- `joke.custom` - Use the /telljoke command
- `joke.broadcast` - Use the /broadcast command
