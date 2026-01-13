# Command Reference

> **Document Purpose**: Complete reference for all Rubidium commands.

## Command Format

```
/command <required> [optional] [choice1|choice2] <repeating...>
```

| Symbol | Meaning |
|--------|---------|
| `<arg>` | Required argument |
| `[arg]` | Optional argument |
| `[a\|b]` | Choice between options |
| `<args...>` | One or more arguments |
| `[args...]` | Zero or more arguments |

---

## Core Commands

### /rubidium

```
/rubidium                       - Show Rubidium info and version
/rubidium help                  - Show help
/rubidium reload                - Reload all configurations
/rubidium reload <module>       - Reload specific module
/rubidium status                - Show system status
/rubidium modules               - List loaded modules
/rubidium debug [on|off]        - Toggle debug mode
```

**Permission**: `rubidium.admin`

---

## QoL Commands

### /qol

```
/qol                            - Show QoL features status
/qol list                       - List all features
/qol enable <feature>           - Enable a feature
/qol disable <feature>          - Disable a feature
/qol toggle <feature>           - Toggle a feature
/qol info <feature>             - Show feature details
/qol reload                     - Reload QoL configuration
```

**Permission**: `qol.manage`

### /afk

```
/afk                            - Toggle AFK status
/afk <message>                  - Set AFK with message
```

**Permission**: `qol.afk`

### /maintenance

```
/maintenance                    - Show maintenance status
/maintenance on [reason]        - Enable maintenance mode
/maintenance off                - Disable maintenance mode
/maintenance add <player>       - Add to bypass list
/maintenance remove <player>    - Remove from bypass list
/maintenance list               - Show bypass list
```

**Permission**: `qol.maintenance`

---

## Staff Commands

### /vanish

```
/vanish                         - Toggle invisibility
/vanish on                      - Enable vanish
/vanish off                     - Disable vanish
/vanish list                    - List vanished players
```

**Permission**: `qol.staff.vanish`

### /godmode

```
/godmode                        - Toggle invincibility
/godmode on                     - Enable god mode
/godmode off                    - Disable god mode
```

**Permission**: `qol.staff.godmode`

### /freeze

```
/freeze <player>                - Toggle freeze on player
/freeze <player> on             - Freeze player
/freeze <player> off            - Unfreeze player
/freeze list                    - List frozen players
```

**Permission**: `qol.staff.freeze`

### /staffmode

```
/staffmode                      - Show active staff modes
```

**Permission**: `qol.staff.use`

---

## Replay Commands

### /replay

```
/replay record <player> [reason] - Start recording a player
/replay stop <player>           - Stop recording a player
/replay list [player]           - List recordings
/replay info <session-id>       - Show recording details
/replay review <session-id>     - Load recording for review
/replay purge <player>          - Delete player's recordings
/replay status                  - Show system status
/replay config [key] [value]    - View/modify config
```

**Permission**: `replay.use`, `replay.admin`

---

## Voice Chat Commands

### /voice

```
/voice                          - Show voice status
/voice toggle                   - Toggle voice chat
/voice mute                     - Mute microphone
/voice unmute                   - Unmute microphone
/voice deafen                   - Deafen yourself
/voice undeafen                 - Undeafen yourself
/voice volume <0-200>           - Set output volume
/voice inputvolume <0-200>      - Set input volume
/voice ptt                      - Toggle push-to-talk
/voice quality <low|medium|high|ultra> - Set quality
```

**Permission**: `voice.use`

### /voice channel

```
/voice channel list             - List channels
/voice channel join <name>      - Join channel
/voice channel leave            - Leave channel
/voice channel create <name>    - Create channel
/voice channel delete <name>    - Delete channel
/voice channel invite <player>  - Invite to channel
/voice channel kick <player>    - Kick from channel
/voice channel password <pass>  - Set password
```

**Permission**: `voice.channel`

### /voice admin

```
/voice admin mute <player>      - Server mute player
/voice admin unmute <player>    - Server unmute player
/voice admin priority <player>  - Toggle priority speaker
/voice admin record <player>    - Start recording
/voice admin stoprecord <player> - Stop recording
/voice admin channels           - List all channels
/voice admin stats              - Show statistics
```

**Permission**: `voice.admin`

---

## Waypoint Commands

### /waypoint (alias: /wp)

```
/waypoint create <name>         - Create at current location
/waypoint delete <name>         - Delete waypoint
/waypoint list [category]       - List waypoints
/waypoint info <name>           - Show details
/waypoint edit <name> <field> <value> - Edit waypoint
/waypoint goto <name>           - Set as navigation target
/waypoint stop                  - Clear navigation
```

**Permission**: `waypoint.use`

### /waypoint share

```
/waypoint share <name> <player> - Share with player
/waypoint unshare <name> <player> - Remove share
/waypoint visibility <name> <level> - Set visibility
/waypoint shared                - List shared waypoints
```

**Permission**: `waypoint.share`

### /waypoint category

```
/waypoint category create <name> <color> - Create category
/waypoint category delete <name> - Delete category
/waypoint category list         - List categories
/waypoint category set <wp> <cat> - Set category
```

**Permission**: `waypoint.category`

### /waypoint admin

```
/waypoint admin create <name> <x> <y> <z> [world] - Create server waypoint
/waypoint admin delete <name>   - Delete server waypoint
/waypoint admin list            - List server waypoints
/waypoint admin player <player> - View player's waypoints
/waypoint admin clear <player>  - Clear player's waypoints
/waypoint admin import <file>   - Import waypoints
/waypoint admin export <file>   - Export waypoints
```

**Permission**: `waypoint.admin`

---

## Party Commands

### /party (alias: /p)

```
/party create [name]            - Create party
/party disband                  - Disband party
/party leave                    - Leave party
/party info                     - Show party info
/party list                     - List members
```

**Permission**: `party.use`

### /party invite

```
/party invite <player>          - Invite player
/party accept [player]          - Accept invite
/party decline [player]         - Decline invite
/party cancel <player>          - Cancel invite
```

**Permission**: `party.invite`

### /party manage

```
/party kick <player> [reason]   - Kick member
/party ban <player>             - Ban player
/party unban <player>           - Unban player
/party promote <player>         - Promote to mod
/party demote <player>          - Demote from mod
/party leader <player>          - Transfer leadership
```

**Permission**: `party.manage`

### /party settings

```
/party settings                 - Show settings
/party settings maxmembers <n>  - Set max members
/party settings friendlyfire <on|off> - Toggle FF
/party settings sharexp <on|off> - Toggle XP sharing
/party settings loot <mode>     - Set loot mode
/party settings voice <on|off>  - Toggle voice
/party settings waypoints <on|off> - Toggle waypoints
/party settings teleport <on|off> - Toggle teleport
/party settings privacy <level> - Set privacy
```

**Permission**: `party.settings`

### /party chat

```
/party chat <message>           - Send party message
/p <message>                    - Shorthand
/party ping [message]           - Ping location
/party announce <message>       - Broadcast (mod+)
```

**Permission**: `party.chat`

### /party teleport

```
/party tp <member>              - Request teleport
/party tphere <member>          - Request member here
/party accept                   - Accept teleport
/party decline                  - Decline teleport
/party summon                   - Summon all (leader)
```

**Permission**: `party.teleport`

---

## Economy Commands

### /balance (alias: /bal)

```
/balance [player]               - Show balance
/balance all                    - Show all currencies
/balance top [currency]         - Show richest players
```

**Permission**: `economy.balance`

### /pay (alias: /transfer)

```
/pay <player> <amount> [currency] - Send money
```

**Permission**: `economy.pay`

### /shop

```
/shop                           - Open nearest shop
/shop browse                    - Browse all shops
/shop search <query>            - Search listings
/shop history                   - Purchase history
```

**Permission**: `economy.shop.use`

### /shop manage

```
/shop create <name>             - Create shop
/shop delete <name>             - Delete shop
/shop list                      - List your shops
/shop open <name>               - Open shop
/shop close <name>              - Close shop
/shop add <item> <buy> <sell> [stock] - Add listing
/shop remove <item>             - Remove listing
/shop stock <item> <amount>     - Restock
/shop price <item> <buy> <sell> - Update prices
/shop info <name>               - Show info
/shop settings <name> <key> <value> - Configure
```

**Permission**: `economy.shop.create`

### /eco (admin)

```
/eco give <player> <amount> [currency] - Give money
/eco take <player> <amount> [currency] - Take money
/eco set <player> <amount> [currency] - Set balance
/eco reset <player>             - Reset balances
/eco freeze <player>            - Freeze account
/eco unfreeze <player>          - Unfreeze account
/eco history <player>           - Transaction history
/eco rollback <tx-id>           - Rollback transaction
```

**Permission**: `economy.admin`

---

## Teleportation Commands

### /tpa

```
/tpa <player>                   - Request teleport to
/tpahere <player>               - Request teleport here
/tpaccept [player]              - Accept request
/tpdeny [player]                - Deny request
/tpacancel                      - Cancel request
/tpatoggle                      - Toggle receiving requests
```

**Permission**: `teleport.tpa`

### /home

```
/home [name]                    - Teleport to home
/sethome [name]                 - Set home
/delhome <name>                 - Delete home
/homes                          - List homes
/home rename <old> <new>        - Rename home
/home icon <name> <icon>        - Set icon
/home default <name>            - Set default
```

**Permission**: `teleport.home`

### /warp

```
/warp <name>                    - Teleport to warp
/warps                          - List warps
/warp info <name>               - Show info
```

**Permission**: `teleport.warp`

### /warp admin

```
/setwarp <name>                 - Create warp
/delwarp <name>                 - Delete warp
/warp enable <name>             - Enable warp
/warp disable <name>            - Disable warp
/warp permission <name> <perm>  - Set permission
/warp cost <name> <amount>      - Set cost
/warp category <name> <cat>     - Set category
```

**Permission**: `teleport.warp.create`

### /spawn

```
/spawn [name]                   - Teleport to spawn
/setspawn [name]                - Set spawn
/spawns                         - List spawns
```

**Permission**: `teleport.spawn`

### /back

```
/back                           - Return to previous location
```

**Permission**: `teleport.back`

### /tp (admin)

```
/tp <player>                    - Teleport to player
/tphere <player>                - Teleport player here
/tppos <x> <y> <z> [world]      - Teleport to coords
/top                            - Teleport to highest block
/tpoffline <player>             - Set offline spawn
```

**Permission**: `teleport.tp`

---

## Permission Commands

### /perm

```
/perm check <player> <permission> - Check permission
/perm info <player>             - Show permission info
/perm list <player>             - List permissions
/perm set <player> <perm> <true|false> - Set permission
/perm unset <player> <perm>     - Remove permission
/perm temp <player> <perm> <true|false> <duration> - Temporary
```

**Permission**: `permission.admin`

### /role

```
/role list                      - List roles
/role info <role>               - Show role info
/role create <name> [priority]  - Create role
/role delete <name>             - Delete role
/role setperm <role> <perm> <true|false> - Set permission
/role unsetperm <role> <perm>   - Remove permission
/role setparent <role> <parent> - Add parent
/role removeparent <role> <parent> - Remove parent
/role setprefix <role> <prefix> - Set prefix
/role setsuffix <role> <suffix> - Set suffix
/role setcolor <role> <color>   - Set color
/role setpriority <role> <priority> - Set priority
```

**Permission**: `permission.role.manage`

### /role player

```
/role add <player> <role>       - Add role
/role remove <player> <role>    - Remove role
/role setprimary <player> <role> - Set primary
/role player <player>           - List player's roles
```

**Permission**: `permission.role.assign`

### /prefix & /suffix

```
/prefix set <player> <prefix>   - Set prefix
/prefix clear <player>          - Clear prefix
/suffix set <player> <suffix>   - Set suffix
/suffix clear <player>          - Clear suffix
```

**Permission**: `permission.prefix`, `permission.suffix`

---

## Chat Commands

### /channel (alias: /ch)

```
/channel list                   - List channels
/channel join <name>            - Join channel
/channel leave [name]           - Leave channel
/channel focus <name>           - Set default channel
/channel create <name>          - Create channel
/channel delete <name>          - Delete channel
/channel invite <player>        - Invite to channel
/channel kick <player>          - Kick from channel
/channel mute <player>          - Mute in channel
/channel unmute <player>        - Unmute in channel
```

**Permission**: `chat.channel`

### /msg (alias: /tell, /whisper, /w)

```
/msg <player> <message>         - Private message
/r <message>                    - Reply to last message
```

**Permission**: `chat.msg`

### /ignore

```
/ignore <player>                - Ignore player
/ignore list                    - List ignored
/unignore <player>              - Unignore player
```

**Permission**: `chat.ignore`

### /broadcast

```
/broadcast <message>            - Server-wide broadcast
/bc <message>                   - Shorthand
```

**Permission**: `chat.broadcast`

---

## Marketplace Commands

### /marketplace (alias: /market)

```
/marketplace search <query>     - Search plugins
/marketplace browse [category]  - Browse plugins
/marketplace info <plugin>      - Plugin details
/marketplace install <plugin>   - Install plugin
/marketplace update <plugin>    - Update plugin
/marketplace uninstall <plugin> - Uninstall plugin
/marketplace list               - List installed
/marketplace updates            - Check for updates
```

**Permission**: `marketplace.use`, `marketplace.admin`

---

## Tab Completion

All commands support intelligent tab completion:

```java
// Example: /waypoint create <name>
// Tab at <name> position shows:
// - Suggested names based on location
// - Recent waypoint names
// - Common naming patterns

// Example: /party invite <player>
// Tab at <player> position shows:
// - Online players
// - Friends list
// - Recent party members
// - Excludes current party members
```
