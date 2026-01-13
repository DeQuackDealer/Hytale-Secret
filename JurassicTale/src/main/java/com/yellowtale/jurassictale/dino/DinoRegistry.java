package com.yellowtale.jurassictale.dino;

import com.yellowtale.rubidium.core.logging.RubidiumLogger;
import com.yellowtale.jurassictale.dino.types.*;
import com.yellowtale.jurassictale.territory.TerritoryType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DinoRegistry {
    
    private final RubidiumLogger logger;
    private final Map<String, DinoDefinition> definitions;
    
    public DinoRegistry(RubidiumLogger logger) {
        this.logger = logger;
        this.definitions = new ConcurrentHashMap<>();
    }
    
    public void register(DinoDefinition definition) {
        definitions.put(definition.getId(), definition);
        logger.debug("Registered dino type: {}", definition.getId());
    }
    
    public Optional<DinoDefinition> get(String id) {
        return Optional.ofNullable(definitions.get(id));
    }
    
    public List<DinoDefinition> getByCategory(DinoCategory category) {
        return definitions.values().stream()
            .filter(d -> d.getCategory() == category)
            .toList();
    }
    
    public List<DinoDefinition> getByTerritory(TerritoryType territory) {
        return definitions.values().stream()
            .filter(d -> d.getSpawnTerritories().contains(territory))
            .toList();
    }
    
    public int getRegisteredCount() {
        return definitions.size();
    }
    
    public void registerDefaults() {
        registerHerbivores();
        registerPredators();
        registerFlyers();
        registerAquatic();
        registerCave();
    }
    
    private void registerHerbivores() {
        register(DinoDefinition.builder("dryosaurus")
            .name("Dryosaurus")
            .category(DinoCategory.HERBIVORE)
            .behaviorType(BehaviorType.HERD)
            .temperament(Temperament.SKITTISH)
            .captureDifficulty(CaptureDifficulty.EASY)
            .transportClass(TransportClass.SMALL)
            .health(40).damage(5).speed(1.4f)
            .addSpawnTerritory(TerritoryType.GRASSLANDS)
            .modelId("dino_dryosaurus")
            .build());
        
        register(DinoDefinition.builder("parasaurolophus")
            .name("Parasaurolophus")
            .category(DinoCategory.HERBIVORE)
            .behaviorType(BehaviorType.HERD)
            .temperament(Temperament.PASSIVE)
            .captureDifficulty(CaptureDifficulty.MEDIUM)
            .transportClass(TransportClass.MEDIUM)
            .health(80).damage(15).speed(1.1f)
            .addSpawnTerritory(TerritoryType.GRASSLANDS)
            .addSpawnTerritory(TerritoryType.JUNGLE)
            .modelId("dino_parasaurolophus")
            .build());
        
        register(DinoDefinition.builder("stegosaurus")
            .name("Stegosaurus")
            .category(DinoCategory.HERBIVORE)
            .behaviorType(BehaviorType.HERD)
            .temperament(Temperament.PASSIVE)
            .captureDifficulty(CaptureDifficulty.MEDIUM)
            .transportClass(TransportClass.LARGE)
            .health(150).damage(35).speed(0.8f)
            .addSpawnTerritory(TerritoryType.JUNGLE)
            .modelId("dino_stegosaurus")
            .build());
        
        register(DinoDefinition.builder("triceratops")
            .name("Triceratops")
            .category(DinoCategory.HERBIVORE)
            .behaviorType(BehaviorType.HERD)
            .temperament(Temperament.AGGRESSIVE)
            .captureDifficulty(CaptureDifficulty.HARD)
            .transportClass(TransportClass.LARGE)
            .health(200).damage(50).speed(0.9f)
            .addSpawnTerritory(TerritoryType.GRASSLANDS)
            .addSpawnTerritory(TerritoryType.HIGHLANDS)
            .modelId("dino_triceratops")
            .build());
        
        register(DinoDefinition.builder("brachiosaurus")
            .name("Brachiosaurus")
            .category(DinoCategory.HERBIVORE)
            .behaviorType(BehaviorType.HERD)
            .temperament(Temperament.PASSIVE)
            .captureDifficulty(CaptureDifficulty.EXTREME)
            .transportClass(TransportClass.MASSIVE)
            .health(400).damage(80).speed(0.6f)
            .addSpawnTerritory(TerritoryType.JUNGLE)
            .legendary(true)
            .modelId("dino_brachiosaurus")
            .build());
    }
    
    private void registerPredators() {
        register(DinoDefinition.builder("dilophosaurus")
            .name("Dilophosaurus")
            .category(DinoCategory.PREDATOR)
            .behaviorType(BehaviorType.AMBUSH)
            .temperament(Temperament.AGGRESSIVE)
            .captureDifficulty(CaptureDifficulty.MEDIUM)
            .transportClass(TransportClass.SMALL)
            .health(60).damage(25).speed(1.2f)
            .addSpawnTerritory(TerritoryType.JUNGLE)
            .modelId("dino_dilophosaurus")
            .build());
        
        register(DinoDefinition.builder("velociraptor")
            .name("Velociraptor")
            .category(DinoCategory.PREDATOR)
            .behaviorType(BehaviorType.PACK)
            .temperament(Temperament.AGGRESSIVE)
            .captureDifficulty(CaptureDifficulty.HARD)
            .transportClass(TransportClass.SMALL)
            .health(50).damage(30).speed(1.5f)
            .packSize(3, 6)
            .addSpawnTerritory(TerritoryType.JUNGLE)
            .modelId("dino_velociraptor")
            .build());
        
        register(DinoDefinition.builder("allosaurus")
            .name("Allosaurus")
            .category(DinoCategory.PREDATOR)
            .behaviorType(BehaviorType.TERRITORIAL)
            .temperament(Temperament.TERRITORIAL)
            .captureDifficulty(CaptureDifficulty.HARD)
            .transportClass(TransportClass.LARGE)
            .health(180).damage(60).speed(1.0f)
            .addSpawnTerritory(TerritoryType.HIGHLANDS)
            .modelId("dino_allosaurus")
            .build());
        
        register(DinoDefinition.builder("carnotaurus")
            .name("Carnotaurus")
            .category(DinoCategory.PREDATOR)
            .behaviorType(BehaviorType.AMBUSH)
            .temperament(Temperament.AGGRESSIVE)
            .captureDifficulty(CaptureDifficulty.HARD)
            .transportClass(TransportClass.LARGE)
            .health(160).damage(55).speed(1.3f)
            .addSpawnTerritory(TerritoryType.JUNGLE)
            .addSpawnTerritory(TerritoryType.GRASSLANDS)
            .modelId("dino_carnotaurus")
            .build());
        
        register(DinoDefinition.builder("tyrannosaurus")
            .name("Tyrannosaurus Rex")
            .category(DinoCategory.APEX)
            .behaviorType(BehaviorType.TERRITORIAL)
            .temperament(Temperament.APEX)
            .captureDifficulty(CaptureDifficulty.EXTREME)
            .transportClass(TransportClass.MASSIVE)
            .health(500).damage(120).speed(0.9f)
            .addSpawnTerritory(TerritoryType.STORM_ZONE)
            .legendary(true)
            .canBreakBlocks(true)
            .modelId("dino_tyrannosaurus")
            .build());
    }
    
    private void registerFlyers() {
        register(DinoDefinition.builder("pteranodon")
            .name("Pteranodon")
            .category(DinoCategory.FLYER)
            .behaviorType(BehaviorType.SCAVENGER)
            .temperament(Temperament.AGGRESSIVE)
            .captureDifficulty(CaptureDifficulty.MEDIUM)
            .transportClass(TransportClass.MEDIUM)
            .health(70).damage(20).speed(1.6f)
            .addSpawnTerritory(TerritoryType.HIGHLANDS)
            .modelId("dino_pteranodon")
            .build());
        
        register(DinoDefinition.builder("quetzalcoatlus")
            .name("Quetzalcoatlus")
            .category(DinoCategory.FLYER)
            .behaviorType(BehaviorType.TERRITORIAL)
            .temperament(Temperament.APEX)
            .captureDifficulty(CaptureDifficulty.EXTREME)
            .transportClass(TransportClass.LARGE)
            .health(200).damage(70).speed(1.4f)
            .addSpawnTerritory(TerritoryType.HIGHLANDS)
            .addSpawnTerritory(TerritoryType.STORM_ZONE)
            .legendary(true)
            .modelId("dino_quetzalcoatlus")
            .build());
    }
    
    private void registerAquatic() {
        register(DinoDefinition.builder("spinosaurus")
            .name("Spinosaurus")
            .category(DinoCategory.AQUATIC)
            .behaviorType(BehaviorType.TERRITORIAL)
            .temperament(Temperament.APEX)
            .captureDifficulty(CaptureDifficulty.EXTREME)
            .transportClass(TransportClass.MASSIVE)
            .health(450).damage(100).speed(1.0f)
            .addSpawnTerritory(TerritoryType.SWAMP)
            .legendary(true)
            .modelId("dino_spinosaurus")
            .build());
        
        register(DinoDefinition.builder("deinosuchus")
            .name("Deinosuchus")
            .category(DinoCategory.AQUATIC)
            .behaviorType(BehaviorType.AMBUSH)
            .temperament(Temperament.AGGRESSIVE)
            .captureDifficulty(CaptureDifficulty.HARD)
            .transportClass(TransportClass.LARGE)
            .health(180).damage(80).speed(0.7f)
            .addSpawnTerritory(TerritoryType.SWAMP)
            .modelId("dino_deinosuchus")
            .build());
    }
    
    private void registerCave() {
        register(DinoDefinition.builder("troodon")
            .name("Troodon")
            .category(DinoCategory.CAVE)
            .behaviorType(BehaviorType.PACK)
            .temperament(Temperament.AGGRESSIVE)
            .captureDifficulty(CaptureDifficulty.MEDIUM)
            .transportClass(TransportClass.SMALL)
            .health(40).damage(25).speed(1.3f)
            .packSize(4, 8)
            .addSpawnTerritory(TerritoryType.CAVES)
            .nightPredator(true)
            .modelId("dino_troodon")
            .build());
        
        register(DinoDefinition.builder("utahraptor")
            .name("Utahraptor")
            .category(DinoCategory.CAVE)
            .behaviorType(BehaviorType.PACK)
            .temperament(Temperament.AGGRESSIVE)
            .captureDifficulty(CaptureDifficulty.HARD)
            .transportClass(TransportClass.MEDIUM)
            .health(100).damage(45).speed(1.2f)
            .packSize(2, 4)
            .addSpawnTerritory(TerritoryType.CAVES)
            .modelId("dino_utahraptor")
            .build());
    }
}
