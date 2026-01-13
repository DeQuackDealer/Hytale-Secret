package com.yellowtale.jurassictale.items;

public class ItemDefinition {
    
    private final String id;
    private final String name;
    private final ItemCategory category;
    
    private final int damage;
    private final int torpor;
    private final float fireRate;
    private final float attackSpeed;
    private final int magazineSize;
    private final float reloadTime;
    private final int netDuration;
    
    private final int healing;
    private final int durability;
    private final int maxStack;
    
    private final String modelId;
    
    private ItemDefinition(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.category = builder.category;
        this.damage = builder.damage;
        this.torpor = builder.torpor;
        this.fireRate = builder.fireRate;
        this.attackSpeed = builder.attackSpeed;
        this.magazineSize = builder.magazineSize;
        this.reloadTime = builder.reloadTime;
        this.netDuration = builder.netDuration;
        this.healing = builder.healing;
        this.durability = builder.durability;
        this.maxStack = builder.maxStack;
        this.modelId = builder.modelId;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public ItemCategory getCategory() { return category; }
    public int getDamage() { return damage; }
    public int getTorpor() { return torpor; }
    public float getFireRate() { return fireRate; }
    public float getAttackSpeed() { return attackSpeed; }
    public int getMagazineSize() { return magazineSize; }
    public float getReloadTime() { return reloadTime; }
    public int getNetDuration() { return netDuration; }
    public int getHealing() { return healing; }
    public int getDurability() { return durability; }
    public int getMaxStack() { return maxStack; }
    public String getModelId() { return modelId; }
    
    public boolean isWeapon() {
        return category == ItemCategory.FIREARM || 
               category == ItemCategory.TRANQ || 
               category == ItemCategory.MELEE;
    }
    
    public boolean isStackable() {
        return maxStack > 1;
    }
    
    public static Builder weapon(String id, String name, ItemCategory category) {
        return new Builder(id, name, category);
    }
    
    public static Builder item(String id, String name, ItemCategory category) {
        return new Builder(id, name, category);
    }
    
    public static class Builder {
        private final String id;
        private final String name;
        private final ItemCategory category;
        
        private int damage = 0;
        private int torpor = 0;
        private float fireRate = 1.0f;
        private float attackSpeed = 1.0f;
        private int magazineSize = 1;
        private float reloadTime = 1.0f;
        private int netDuration = 0;
        private int healing = 0;
        private int durability = -1;
        private int maxStack = 1;
        private String modelId;
        
        private Builder(String id, String name, ItemCategory category) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.modelId = "item_" + id;
        }
        
        public Builder damage(int damage) { this.damage = damage; return this; }
        public Builder torpor(int torpor) { this.torpor = torpor; return this; }
        public Builder fireRate(float rate) { this.fireRate = rate; return this; }
        public Builder attackSpeed(float speed) { this.attackSpeed = speed; return this; }
        public Builder magazineSize(int size) { this.magazineSize = size; return this; }
        public Builder reloadTime(float time) { this.reloadTime = time; return this; }
        public Builder netDuration(int duration) { this.netDuration = duration; return this; }
        public Builder healing(int healing) { this.healing = healing; return this; }
        public Builder durability(int durability) { this.durability = durability; return this; }
        public Builder stackable(int maxStack) { this.maxStack = maxStack; return this; }
        public Builder modelId(String modelId) { this.modelId = modelId; return this; }
        
        public ItemDefinition build() {
            return new ItemDefinition(this);
        }
    }
}
