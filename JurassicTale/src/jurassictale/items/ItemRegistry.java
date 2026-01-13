package jurassictale.items;

import rubidium.core.logging.RubidiumLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemRegistry {
    
    private final RubidiumLogger logger;
    private final Map<String, ItemDefinition> items;
    
    public ItemRegistry(RubidiumLogger logger) {
        this.logger = logger;
        this.items = new ConcurrentHashMap<>();
    }
    
    public void register(ItemDefinition item) {
        items.put(item.getId(), item);
        logger.debug("Registered item: {}", item.getId());
    }
    
    public Optional<ItemDefinition> get(String id) {
        return Optional.ofNullable(items.get(id));
    }
    
    public List<ItemDefinition> getByCategory(ItemCategory category) {
        return items.values().stream()
            .filter(i -> i.getCategory() == category)
            .toList();
    }
    
    public int getRegisteredCount() {
        return items.size();
    }
    
    public void registerDefaults() {
        registerFirearms();
        registerTranqWeapons();
        registerMeleeWeapons();
        registerThrowables();
        registerRaidTools();
        registerSurvivalItems();
    }
    
    private void registerFirearms() {
        register(ItemDefinition.weapon("pistol_9mm", "9mm Sidearm", ItemCategory.FIREARM)
            .damage(15).fireRate(4).magazineSize(12).reloadTime(1.5f).build());
        register(ItemDefinition.weapon("pistol_heavy", "Heavy Pistol", ItemCategory.FIREARM)
            .damage(35).fireRate(2).magazineSize(7).reloadTime(2.0f).build());
        register(ItemDefinition.weapon("smg_compact", "Compact SMG", ItemCategory.FIREARM)
            .damage(12).fireRate(12).magazineSize(25).reloadTime(2.0f).build());
        register(ItemDefinition.weapon("smg_tactical", "Tactical SMG", ItemCategory.FIREARM)
            .damage(14).fireRate(10).magazineSize(30).reloadTime(2.2f).build());
        register(ItemDefinition.weapon("rifle_assault", "Assault Rifle", ItemCategory.FIREARM)
            .damage(25).fireRate(8).magazineSize(30).reloadTime(2.5f).build());
        register(ItemDefinition.weapon("rifle_dmr", "DMR", ItemCategory.FIREARM)
            .damage(45).fireRate(3).magazineSize(10).reloadTime(2.5f).build());
        register(ItemDefinition.weapon("rifle_bolt", "Bolt-Action Rifle", ItemCategory.FIREARM)
            .damage(80).fireRate(1).magazineSize(5).reloadTime(3.0f).build());
        register(ItemDefinition.weapon("shotgun_pump", "Pump Shotgun", ItemCategory.FIREARM)
            .damage(60).fireRate(1.5f).magazineSize(6).reloadTime(3.5f).build());
        register(ItemDefinition.weapon("shotgun_combat", "Combat Shotgun", ItemCategory.FIREARM)
            .damage(50).fireRate(3).magazineSize(8).reloadTime(3.0f).build());
        register(ItemDefinition.weapon("flare_gun", "Flare Gun", ItemCategory.FIREARM)
            .damage(5).fireRate(0.5f).magazineSize(1).reloadTime(2.0f).build());
    }
    
    private void registerTranqWeapons() {
        register(ItemDefinition.weapon("tranq_pistol", "Tranq Pistol", ItemCategory.TRANQ)
            .torpor(30).fireRate(2).magazineSize(6).reloadTime(2.0f).build());
        register(ItemDefinition.weapon("tranq_rifle", "Tranq Rifle", ItemCategory.TRANQ)
            .torpor(60).fireRate(1).magazineSize(5).reloadTime(2.5f).build());
        register(ItemDefinition.weapon("dart_gun", "Dart Gun", ItemCategory.TRANQ)
            .torpor(80).fireRate(0.8f).magazineSize(3).reloadTime(3.0f).build());
        register(ItemDefinition.weapon("net_launcher", "Net Launcher", ItemCategory.TRANQ)
            .netDuration(100).fireRate(0.5f).magazineSize(1).reloadTime(4.0f).build());
        register(ItemDefinition.item("bola", "Bola", ItemCategory.TRANQ)
            .stackable(5).build());
        register(ItemDefinition.item("tranq_darts", "Tranquilizer Darts", ItemCategory.AMMO)
            .stackable(50).build());
    }
    
    private void registerMeleeWeapons() {
        register(ItemDefinition.weapon("knife_survival", "Survival Knife", ItemCategory.MELEE)
            .damage(15).attackSpeed(1.5f).build());
        register(ItemDefinition.weapon("knife_hunting", "Hunting Knife", ItemCategory.MELEE)
            .damage(20).attackSpeed(1.4f).build());
        register(ItemDefinition.weapon("knife_combat", "Combat Knife", ItemCategory.MELEE)
            .damage(25).attackSpeed(1.6f).build());
        register(ItemDefinition.weapon("machete", "Machete", ItemCategory.MELEE)
            .damage(30).attackSpeed(1.2f).build());
        register(ItemDefinition.weapon("baton_stun", "Stun Baton", ItemCategory.MELEE)
            .damage(10).torpor(20).attackSpeed(1.3f).build());
        register(ItemDefinition.weapon("spear", "Spear", ItemCategory.MELEE)
            .damage(35).attackSpeed(1.0f).build());
    }
    
    private void registerThrowables() {
        register(ItemDefinition.item("grenade_frag", "Frag Grenade", ItemCategory.THROWABLE)
            .stackable(3).build());
        register(ItemDefinition.item("grenade_smoke", "Smoke Grenade", ItemCategory.THROWABLE)
            .stackable(5).build());
        register(ItemDefinition.item("flashbang", "Flashbang", ItemCategory.THROWABLE)
            .stackable(5).build());
        register(ItemDefinition.item("noise_maker", "Noise Maker", ItemCategory.THROWABLE)
            .stackable(10).build());
        register(ItemDefinition.item("flare", "Flare", ItemCategory.THROWABLE)
            .stackable(10).build());
    }
    
    private void registerRaidTools() {
        register(ItemDefinition.item("lockpick_kit", "Lockpick Kit", ItemCategory.RAID_TOOL)
            .durability(10).build());
        register(ItemDefinition.item("fence_cutter", "Fence Cutter", ItemCategory.RAID_TOOL)
            .durability(20).build());
        register(ItemDefinition.item("hacking_tool", "Hacking Tool", ItemCategory.RAID_TOOL)
            .durability(5).build());
        register(ItemDefinition.item("breach_charge", "Breach Charge", ItemCategory.RAID_TOOL)
            .stackable(2).build());
        register(ItemDefinition.item("door_jammer", "Door Jammer", ItemCategory.RAID_TOOL)
            .durability(3).build());
    }
    
    private void registerSurvivalItems() {
        register(ItemDefinition.item("bandage", "Bandage", ItemCategory.MEDICAL)
            .healing(20).stackable(10).build());
        register(ItemDefinition.item("medkit", "Medkit", ItemCategory.MEDICAL)
            .healing(75).stackable(3).build());
        register(ItemDefinition.item("painkillers", "Painkillers", ItemCategory.MEDICAL)
            .stackable(5).build());
        register(ItemDefinition.item("antidote", "Antidote", ItemCategory.MEDICAL)
            .stackable(5).build());
        register(ItemDefinition.item("water_bottle", "Water Bottle", ItemCategory.CONSUMABLE)
            .stackable(5).build());
        register(ItemDefinition.item("rations", "Rations", ItemCategory.CONSUMABLE)
            .stackable(10).build());
    }
}
