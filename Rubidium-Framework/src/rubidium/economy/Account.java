package rubidium.economy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Account {
    private final UUID id;
    private final UUID playerId;
    private final AccountType type;
    private final Map<String, Long> balances;
    private final long createdAt;
    private long lastTransaction;
    private boolean frozen;
    private AccountFlags flags;
    
    public Account(UUID id, UUID playerId, AccountType type) {
        this.id = id;
        this.playerId = playerId;
        this.type = type;
        this.balances = new ConcurrentHashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.lastTransaction = 0;
        this.frozen = false;
        this.flags = new AccountFlags(false, false, false, true);
    }
    
    public UUID getId() { return id; }
    public UUID getPlayerId() { return playerId; }
    public AccountType getType() { return type; }
    public long getCreatedAt() { return createdAt; }
    public long getLastTransaction() { return lastTransaction; }
    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }
    public AccountFlags getFlags() { return flags; }
    public void setFlags(AccountFlags flags) { this.flags = flags; }
    
    public long getBalance(String currency) {
        return balances.getOrDefault(currency, 0L);
    }
    
    public Map<String, Long> getAllBalances() {
        return new ConcurrentHashMap<>(balances);
    }
    
    public synchronized boolean withdraw(String currency, long amount) {
        long balance = balances.getOrDefault(currency, 0L);
        if (balance < amount && !flags.allowNegative()) {
            return false;
        }
        balances.put(currency, balance - amount);
        lastTransaction = System.currentTimeMillis();
        return true;
    }
    
    public synchronized void deposit(String currency, long amount) {
        long balance = balances.getOrDefault(currency, 0L);
        balances.put(currency, balance + amount);
        lastTransaction = System.currentTimeMillis();
    }
    
    public synchronized void setBalance(String currency, long amount) {
        balances.put(currency, amount);
        lastTransaction = System.currentTimeMillis();
    }
    
    public boolean hasBalance(String currency, long amount) {
        return getBalance(currency) >= amount;
    }
    
    public enum AccountType {
        PLAYER,
        SHOP,
        GUILD,
        SERVER,
        ESCROW
    }
    
    public record AccountFlags(
        boolean allowNegative,
        boolean autoInterest,
        boolean requirePin,
        boolean notifyTransactions
    ) {}
}
