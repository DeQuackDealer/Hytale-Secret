# JurassicTale

**A Hytale-based fantasy adventure survival MMO where players build and defend compounds in a dinosaur-filled Exclusion Zone.**

> Internal Codename: Project Extinction

## Overview

JurassicTale is a persistent multiplayer open-world experience built on Hytale using the Rubidium Framework. Players begin at a safe research compound hub and venture into the Exclusion Zone—an expansive wilderness filled with dinosaurs, ruins, rival outposts, and unpredictable dangers.

## Core Features

### Dinosaur System
- **16 dinosaur species** across 6 categories (Herbivores, Predators, Apex, Flyers, Aquatic, Cave)
- **6 behavior AI templates**: Herd, Pack, Ambush, Territorial, Scavenger, Cave Hunter
- **Capture system** with tranquilizers, nets, bolas, and transport classes
- **Temperament system**: Passive, Skittish, Aggressive, Territorial, Apex

### Territory System
- **6 zone types**: Grasslands, Jungle, Swamp, Highlands, Caves, Storm Zone
- Distance-based danger scaling
- Territory-specific dinosaur spawns

### Compound System
- **4-tier power cores**: Field Generator → Industrial → Nuclear → Fusion
- **Wireless power grid** with zones and devices
- **NPC hiring** with payroll and sabotage risk
- **Defense infrastructure**: Fences, turrets, sensors, cameras

### Weapons & Items
- **Firearms**: Pistols, SMGs, Rifles, Shotguns
- **Tranquilizers**: Tranq guns, darts, nets, bolas
- **Melee**: Knives, machete, spear, stun baton
- **Tactical**: Grenades, flashbangs, smoke, flares
- **Raid tools**: Lockpicks, fence cutters, breach charges

### Chaos Storm Events
- Scheduled PvPvE windows with warnings
- Increased dinosaur aggression multiplier
- Optional PvP during events
- High-risk, high-reward gameplay

## Project Structure

```
JurassicTale/
├── src/main/java/com/yellowtale/jurassictale/
│   ├── JurassicTalePlugin.java      # Main plugin entry point
│   ├── JurassicTaleConfig.java      # Configuration
│   ├── dino/
│   │   ├── DinoRegistry.java        # Dino type registry
│   │   ├── DinoManager.java         # Entity management
│   │   ├── DinoDefinition.java      # Dino type definitions
│   │   ├── DinoEntity.java          # Runtime dino entity
│   │   ├── types/                   # Enums (Category, Behavior, etc.)
│   │   └── behavior/                # AI controllers
│   ├── territory/
│   │   ├── TerritoryType.java       # Zone definitions
│   │   └── TerritoryManager.java    # Zone management
│   ├── compound/
│   │   ├── Compound.java            # Compound entity
│   │   ├── CompoundManager.java     # Compound management
│   │   └── power/                   # Power grid system
│   ├── items/
│   │   ├── ItemRegistry.java        # Item definitions
│   │   ├── ItemDefinition.java      # Item properties
│   │   └── ItemCategory.java        # Item categories
│   ├── capture/
│   │   └── CaptureManager.java      # Capture system
│   └── events/
│       └── ChaosStormManager.java   # Chaos storm events
└── README.md
```

## Design Pillars

1. **Adventure-First** - Exciting, magical, cinematic—not grindy
2. **Compounds Are the Spine** - Identity, progression, and strategy
3. **Simple to Learn, Deep to Master** - Quick building, optimized gameplay
4. **Danger is Optional but Meaningful** - PvP during Chaos Windows only
5. **Anti-Monopoly by Design** - NPC payroll and sabotage scaling
6. **World Integrity Matters** - Building limitations preserve aesthetics

## MVP Dino Roster (16 species)

### Herbivores
- Dryosaurus (starter prey)
- Parasaurolophus (herd mid-tier)
- Stegosaurus (defensive)
- Triceratops (tank)
- Brachiosaurus (legendary giant)

### Predators
- Dilophosaurus (starter predator)
- Velociraptor (pack)
- Allosaurus (territorial)
- Carnotaurus (ambush)
- Tyrannosaurus Rex (apex)

### Flyers
- Pteranodon (common)
- Quetzalcoatlus (legendary)

### Aquatic
- Spinosaurus (swamp apex)
- Deinosuchus (ambush croc)

### Cave
- Troodon (night predator)
- Utahraptor (heavy pack)

## Integration with Rubidium

This plugin uses the Rubidium Framework for:
- Model and animation management
- Asset registry
- Event system
- Logging
- Plugin lifecycle

```java
@PluginInfo(
    id = "jurassictale",
    name = "JurassicTale",
    version = "0.1.0"
)
public class JurassicTalePlugin implements RubidiumPlugin {
    @Override
    public void onEnable(RubidiumLogger logger, Path dataDir) {
        // Initialize managers and content
    }
}
```

## Status

**Work in Progress** - This is the scaffold/framework implementation. Features will be expanded as Hytale's modding API becomes available. The Rubidium APIs will be mapped to the actual Hytale server JAR for full functionality.

---

*Built with the Rubidium Framework for Yellow Tale*
