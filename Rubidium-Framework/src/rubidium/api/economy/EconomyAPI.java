package rubidium.api.economy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class EconomyAPI {
    
    private static final Map<String, Currency> currencies = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Double>> balances = new ConcurrentHashMap<>();
    private static final List<Transaction> transactionHistory = Collections.synchronizedList(new ArrayList<>());
    private static String defaultCurrency = "coins";
    
    private EconomyAPI() {}
    
    public static void createCurrency(String id, String name, String symbol) {
        currencies.put(id, new Currency(id, name, symbol, 2, 0.0, Double.MAX_VALUE));
    }
    
    public static void createCurrency(String id, String name, String symbol, int decimals, double minBalance, double maxBalance) {
        currencies.put(id, new Currency(id, name, symbol, decimals, minBalance, maxBalance));
    }
    
    public static Optional<Currency> getCurrency(String id) {
        return Optional.ofNullable(currencies.get(id));
    }
    
    public static Collection<Currency> allCurrencies() {
        return currencies.values();
    }
    
    public static void setDefaultCurrency(String currencyId) {
        defaultCurrency = currencyId;
    }
    
    public static String getDefaultCurrency() {
        return defaultCurrency;
    }
    
    public static double getBalance(UUID playerId) {
        return getBalance(playerId, defaultCurrency);
    }
    
    public static double getBalance(UUID playerId, String currencyId) {
        return balances.getOrDefault(playerId, Map.of()).getOrDefault(currencyId, 0.0);
    }
    
    public static void setBalance(UUID playerId, double amount) {
        setBalance(playerId, defaultCurrency, amount);
    }
    
    public static void setBalance(UUID playerId, String currencyId, double amount) {
        Currency currency = currencies.get(currencyId);
        if (currency != null) {
            amount = Math.max(currency.minBalance(), Math.min(currency.maxBalance(), amount));
        }
        balances.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(currencyId, amount);
    }
    
    public static boolean deposit(UUID playerId, double amount) {
        return deposit(playerId, defaultCurrency, amount);
    }
    
    public static boolean deposit(UUID playerId, String currencyId, double amount) {
        if (amount <= 0) return false;
        
        double current = getBalance(playerId, currencyId);
        double newBalance = current + amount;
        
        Currency currency = currencies.get(currencyId);
        if (currency != null && newBalance > currency.maxBalance()) {
            return false;
        }
        
        setBalance(playerId, currencyId, newBalance);
        recordTransaction(null, playerId, currencyId, amount, TransactionType.DEPOSIT, "Deposit");
        return true;
    }
    
    public static boolean withdraw(UUID playerId, double amount) {
        return withdraw(playerId, defaultCurrency, amount);
    }
    
    public static boolean withdraw(UUID playerId, String currencyId, double amount) {
        if (amount <= 0) return false;
        
        double current = getBalance(playerId, currencyId);
        if (current < amount) return false;
        
        double newBalance = current - amount;
        Currency currency = currencies.get(currencyId);
        if (currency != null && newBalance < currency.minBalance()) {
            return false;
        }
        
        setBalance(playerId, currencyId, newBalance);
        recordTransaction(playerId, null, currencyId, amount, TransactionType.WITHDRAW, "Withdraw");
        return true;
    }
    
    public static boolean transfer(UUID from, UUID to, double amount) {
        return transfer(from, to, defaultCurrency, amount);
    }
    
    public static boolean transfer(UUID from, UUID to, String currencyId, double amount) {
        if (amount <= 0) return false;
        if (!withdraw(from, currencyId, amount)) return false;
        
        if (!deposit(to, currencyId, amount)) {
            deposit(from, currencyId, amount);
            return false;
        }
        
        recordTransaction(from, to, currencyId, amount, TransactionType.TRANSFER, "Transfer");
        return true;
    }
    
    public static boolean has(UUID playerId, double amount) {
        return has(playerId, defaultCurrency, amount);
    }
    
    public static boolean has(UUID playerId, String currencyId, double amount) {
        return getBalance(playerId, currencyId) >= amount;
    }
    
    public static String format(double amount) {
        return format(defaultCurrency, amount);
    }
    
    public static String format(String currencyId, double amount) {
        Currency currency = currencies.get(currencyId);
        if (currency == null) {
            return String.format("%.2f", amount);
        }
        return String.format("%s%." + currency.decimals() + "f", currency.symbol(), amount);
    }
    
    public static List<Transaction> getHistory(UUID playerId) {
        return transactionHistory.stream()
            .filter(t -> playerId.equals(t.from()) || playerId.equals(t.to()))
            .toList();
    }
    
    public static List<Transaction> getRecentHistory(UUID playerId, int limit) {
        return getHistory(playerId).stream()
            .sorted((a, b) -> Long.compare(b.timestamp(), a.timestamp()))
            .limit(limit)
            .toList();
    }
    
    private static void recordTransaction(UUID from, UUID to, String currency, double amount, TransactionType type, String description) {
        transactionHistory.add(new Transaction(
            UUID.randomUUID(),
            from,
            to,
            currency,
            amount,
            type,
            description,
            System.currentTimeMillis()
        ));
        
        if (transactionHistory.size() > 10000) {
            transactionHistory.subList(0, 1000).clear();
        }
    }
    
    public static BankAccount createBank(String name, UUID owner) {
        return new BankAccount(UUID.randomUUID(), name, owner);
    }
    
    public record Currency(
        String id,
        String name,
        String symbol,
        int decimals,
        double minBalance,
        double maxBalance
    ) {}
    
    public record Transaction(
        UUID id,
        UUID from,
        UUID to,
        String currency,
        double amount,
        TransactionType type,
        String description,
        long timestamp
    ) {}
    
    public enum TransactionType {
        DEPOSIT, WITHDRAW, TRANSFER, PURCHASE, SALE, REWARD, PENALTY
    }
    
    public static class BankAccount {
        private final UUID id;
        private final String name;
        private final UUID owner;
        private final Set<UUID> members = ConcurrentHashMap.newKeySet();
        private final Map<String, Double> balances = new ConcurrentHashMap<>();
        
        public BankAccount(UUID id, String name, UUID owner) {
            this.id = id;
            this.name = name;
            this.owner = owner;
            members.add(owner);
        }
        
        public UUID getId() { return id; }
        public String getName() { return name; }
        public UUID getOwner() { return owner; }
        public Set<UUID> getMembers() { return Set.copyOf(members); }
        
        public double getBalance(String currencyId) {
            return balances.getOrDefault(currencyId, 0.0);
        }
        
        public void deposit(String currencyId, double amount) {
            balances.merge(currencyId, amount, Double::sum);
        }
        
        public boolean withdraw(String currencyId, double amount) {
            double current = getBalance(currencyId);
            if (current < amount) return false;
            balances.put(currencyId, current - amount);
            return true;
        }
        
        public void addMember(UUID playerId) {
            members.add(playerId);
        }
        
        public void removeMember(UUID playerId) {
            if (!playerId.equals(owner)) {
                members.remove(playerId);
            }
        }
        
        public boolean isMember(UUID playerId) {
            return members.contains(playerId);
        }
    }
}
