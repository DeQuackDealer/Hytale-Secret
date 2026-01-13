# Economy System

> **Document Purpose**: Complete reference for Rubidium's economy, transaction, and shop system.

## Overview

Rubidium's economy system provides:
- **Multi-Currency**: Support for multiple currency types
- **Secure Transactions**: ACID-compliant transaction processing
- **Shops**: Player and server shops with configurable pricing
- **Escrow**: Secure trading between players
- **Logging**: Complete transaction audit trail
- **API**: Hooks for external economy integrations

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      EconomyManager                              │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Core Components                                           │   │
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │   │
│  │ │ Currency │ │ Account  │ │ Trans-   │ │ Audit    │     │   │
│  │ │ Registry │ │ Manager  │ │ actions  │ │ Logger   │     │   │
│  │ └──────────┘ └──────────┘ └──────────┘ └──────────┘     │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Features                                                  │   │
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │   │
│  │ │ Shops    │ │ Escrow   │ │ Auction  │ │ Banking  │     │   │
│  │ │ System   │ │ Service  │ │ House    │ │ Interest │     │   │
│  │ └──────────┘ └──────────┘ └──────────┘ └──────────┘     │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Core Classes

### Currency

```java
package com.yellowtale.rubidium.economy;

public class Currency {
    private String id;
    private String name;
    private String symbol;
    private String format;
    private int decimalPlaces;
    private boolean primary;
    private boolean tradeable;
    private boolean convertible;
    private Map<String, Double> exchangeRates;
    
    // Built-in currencies
    public static final Currency GOLD = new Currency(
        "gold", "Gold", "G", "{symbol}{amount}", 0, true, true, true
    );
    public static final Currency GEMS = new Currency(
        "gems", "Gems", "💎", "{amount} {symbol}", 0, false, false, true
    );
    public static final Currency TOKENS = new Currency(
        "tokens", "Tokens", "T", "{amount}{symbol}", 0, false, true, false
    );
    
    public String format(long amount) {
        String formatted = decimalPlaces > 0 
            ? String.format("%." + decimalPlaces + "f", amount / Math.pow(10, decimalPlaces))
            : String.valueOf(amount);
        return format.replace("{amount}", formatted).replace("{symbol}", symbol);
    }
}
```

### Account

```java
public class Account {
    private UUID id;
    private UUID playerId;
    private AccountType type;
    private Map<String, Long> balances;
    private long createdAt;
    private long lastTransaction;
    private boolean frozen;
    private AccountFlags flags;
    
    public enum AccountType {
        PLAYER,         // Standard player account
        SHOP,           // Shop account
        GUILD,          // Guild treasury
        SERVER,         // Server account
        ESCROW          // Escrow holding account
    }
    
    public record AccountFlags(
        boolean allowNegative,
        boolean autoInterest,
        boolean requirePin,
        boolean notifyTransactions
    ) {}
    
    // Thread-safe balance operations
    public synchronized boolean withdraw(String currency, long amount) {
        long balance = balances.getOrDefault(currency, 0L);
        if (balance < amount && !flags.allowNegative()) {
            return false;
        }
        balances.put(currency, balance - amount);
        return true;
    }
    
    public synchronized void deposit(String currency, long amount) {
        long balance = balances.getOrDefault(currency, 0L);
        balances.put(currency, balance + amount);
    }
}
```

### Transaction

```java
public class Transaction {
    private UUID id;
    private TransactionType type;
    private TransactionStatus status;
    
    private UUID sourceAccount;
    private UUID targetAccount;
    private String currency;
    private long amount;
    
    private String description;
    private Map<String, String> metadata;
    
    private long createdAt;
    private long completedAt;
    private String failureReason;
    
    public enum TransactionType {
        TRANSFER,       // Player to player
        PURCHASE,       // Shop purchase
        SALE,           // Shop sale
        DEPOSIT,        // Bank deposit
        WITHDRAWAL,     // Bank withdrawal
        REWARD,         // Quest/achievement reward
        PENALTY,        // Fine/penalty
        ESCROW_HOLD,    // Escrow hold
        ESCROW_RELEASE, // Escrow release
        ADMIN           // Admin modification
    }
    
    public enum TransactionStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REVERSED,
        EXPIRED
    }
}
```

### EconomyManager

```java
public class EconomyManager {
    
    private CurrencyRegistry currencies;
    private AccountManager accounts;
    private TransactionProcessor processor;
    private AuditLogger auditLogger;
    
    // Currency
    public void registerCurrency(Currency currency);
    public Optional<Currency> getCurrency(String id);
    public Currency getPrimaryCurrency();
    
    // Account Management
    public Account getAccount(UUID playerId);
    public Account getOrCreateAccount(UUID playerId);
    public void freezeAccount(UUID accountId, String reason);
    public void unfreezeAccount(UUID accountId);
    
    // Balance Operations
    public long getBalance(UUID playerId, String currency);
    public boolean hasBalance(UUID playerId, String currency, long amount);
    public void setBalance(UUID playerId, String currency, long amount);
    
    // Transactions
    public Transaction transfer(UUID from, UUID to, String currency, long amount, String description);
    public Transaction deposit(UUID playerId, String currency, long amount, String reason);
    public Transaction withdraw(UUID playerId, String currency, long amount, String reason);
    
    // Conversion
    public long convert(String fromCurrency, String toCurrency, long amount);
    
    // History
    public List<Transaction> getTransactionHistory(UUID playerId, int limit);
    public List<Transaction> getTransactionHistory(UUID playerId, Duration period);
}
```

## Shop System

### Shop

```java
public class Shop {
    private UUID id;
    private String name;
    private UUID owner;
    private ShopType type;
    private Location location;
    private Account account;
    
    private List<ShopListing> listings;
    private ShopSettings settings;
    private ShopStatistics statistics;
    
    public enum ShopType {
        PLAYER,         // Player-owned shop
        SERVER,         // Server shop (infinite stock)
        ADMIN,          // Admin shop (special pricing)
        AUCTION         // Auction house
    }
    
    public record ShopSettings(
        boolean open,
        boolean allowBargaining,
        double taxRate,
        String currency,
        int maxListings,
        boolean restockEnabled,
        Duration restockInterval
    ) {}
}
```

### ShopListing

```java
public class ShopListing {
    private UUID id;
    private UUID shopId;
    private ItemStack item;
    private long buyPrice;
    private long sellPrice;
    private int stock;
    private int maxStock;
    private boolean buying;
    private boolean selling;
    private ListingStatus status;
    
    private long totalSold;
    private long totalBought;
    private long revenue;
    
    public enum ListingStatus {
        ACTIVE,
        OUT_OF_STOCK,
        PAUSED,
        EXPIRED
    }
}
```

### ShopManager

```java
public class ShopManager {
    
    // Shop CRUD
    public Shop createShop(UUID owner, String name, Location location, ShopType type);
    public void deleteShop(UUID shopId);
    public Optional<Shop> getShop(UUID shopId);
    public List<Shop> getPlayerShops(UUID playerId);
    public List<Shop> getNearbyShops(Location center, double radius);
    
    // Listings
    public ShopListing addListing(UUID shopId, ItemStack item, long buyPrice, long sellPrice, int stock);
    public void removeListing(UUID listingId);
    public void updateListing(UUID listingId, ShopListingUpdate update);
    
    // Transactions
    public PurchaseResult buyItem(UUID buyerId, UUID listingId, int quantity);
    public SaleResult sellItem(UUID sellerId, UUID listingId, ItemStack item);
    
    // Search
    public List<ShopListing> searchListings(String query, SearchFilters filters);
    public List<ShopListing> getLowestPrices(String itemId, int limit);
}
```

## Escrow System

```java
public class EscrowService {
    
    public EscrowTransaction createEscrow(
        UUID buyer,
        UUID seller,
        String currency,
        long amount,
        List<ItemStack> items,
        Duration timeout
    );
    
    public void confirmDelivery(UUID escrowId, UUID buyer);
    public void disputeEscrow(UUID escrowId, UUID disputer, String reason);
    public void releaseEscrow(UUID escrowId);
    public void refundEscrow(UUID escrowId);
    public void expireEscrow(UUID escrowId);
    
    public record EscrowTransaction(
        UUID id,
        UUID buyer,
        UUID seller,
        String currency,
        long amount,
        List<ItemStack> items,
        EscrowStatus status,
        long createdAt,
        long expiresAt,
        String disputeReason
    ) {
        public enum EscrowStatus {
            PENDING_PAYMENT,
            PAYMENT_HELD,
            ITEMS_DELIVERED,
            COMPLETED,
            DISPUTED,
            REFUNDED,
            EXPIRED
        }
    }
}
```

## Commands

### Balance Commands

```
/balance [player]               - Show balance
/bal [player]                   - Shorthand
/balance all                    - Show all currency balances
/balance top [currency]         - Show richest players
```

### Transfer Commands

```
/pay <player> <amount> [currency] - Send money to player
/transfer <player> <amount> [currency] - Same as pay
```

### Shop Commands

```
/shop create <name>             - Create shop at location
/shop delete <name>             - Delete your shop
/shop list                      - List your shops
/shop open <name>               - Open shop for business
/shop close <name>              - Close shop
/shop add <item> <buy> <sell> [stock] - Add listing
/shop remove <item>             - Remove listing
/shop stock <item> <amount>     - Restock item
/shop price <item> <buy> <sell> - Update prices
/shop info <name>               - Show shop info
/shop settings <name> <key> <value> - Configure shop
```

### Shop GUI Commands

```
/shop                           - Open nearest shop
/shop browse                    - Browse all shops
/shop search <query>            - Search shop listings
/shop history                   - View purchase history
```

### Admin Commands

```
/eco give <player> <amount> [currency] - Give money
/eco take <player> <amount> [currency] - Take money
/eco set <player> <amount> [currency]  - Set balance
/eco reset <player>             - Reset all balances
/eco freeze <player>            - Freeze account
/eco unfreeze <player>          - Unfreeze account
/eco history <player>           - View transaction history
/eco rollback <transaction-id>  - Rollback transaction
```

## Transaction Logging

### Audit Log Format

```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "TRANSFER",
  "status": "COMPLETED",
  "source": {
    "accountId": "account-uuid-1",
    "playerId": "player-uuid-1",
    "balanceBefore": 1000,
    "balanceAfter": 500
  },
  "target": {
    "accountId": "account-uuid-2",
    "playerId": "player-uuid-2",
    "balanceBefore": 200,
    "balanceAfter": 700
  },
  "currency": "gold",
  "amount": 500,
  "description": "Payment for iron sword",
  "timestamp": "2024-01-15T10:30:00Z",
  "metadata": {
    "command": "/pay Steve 500",
    "location": "world:100,64,-200"
  }
}
```

## Integration Examples

### Quest Rewards

```java
questManager.onQuestCompleted((player, quest) -> {
    for (QuestReward reward : quest.getRewards()) {
        if (reward instanceof CurrencyReward currencyReward) {
            economy.deposit(
                player.getId(),
                currencyReward.getCurrency(),
                currencyReward.getAmount(),
                "Quest completed: " + quest.getName()
            );
        }
    }
});
```

### Party Shared Loot

```java
partyManager.onLootCollected((party, collector, item, value) -> {
    if (party.getSettings().shareLoot()) {
        int memberCount = party.getMembers().size();
        long sharePerMember = value / memberCount;
        
        for (UUID member : party.getMembers()) {
            economy.deposit(
                member,
                economy.getPrimaryCurrency().getId(),
                sharePerMember,
                "Party loot share"
            );
        }
    }
});
```

### Death Penalty

```java
eventBus.subscribe(PlayerDeathEvent.class, (event) -> {
    Player player = event.getPlayer();
    long balance = economy.getBalance(player.getId(), "gold");
    long penalty = (long) (balance * 0.05); // 5% penalty
    
    if (penalty > 0) {
        economy.withdraw(
            player.getId(),
            "gold",
            penalty,
            "Death penalty"
        );
        player.sendMessage("You lost " + Currency.GOLD.format(penalty) + " on death");
    }
});
```

## Security

### Transaction Safety

```java
public class TransactionProcessor {
    
    @Transactional
    public Transaction processTransfer(UUID fromId, UUID toId, String currency, long amount) {
        // Acquire locks in consistent order to prevent deadlock
        UUID first = fromId.compareTo(toId) < 0 ? fromId : toId;
        UUID second = fromId.compareTo(toId) < 0 ? toId : fromId;
        
        Account fromAccount = accounts.lockAccount(first);
        Account toAccount = accounts.lockAccount(second);
        
        try {
            if (!fromAccount.getId().equals(fromId)) {
                Account temp = fromAccount;
                fromAccount = toAccount;
                toAccount = temp;
            }
            
            // Validate
            if (fromAccount.isFrozen()) {
                throw new TransactionException("Source account is frozen");
            }
            if (!fromAccount.hasBalance(currency, amount)) {
                throw new TransactionException("Insufficient funds");
            }
            
            // Execute
            fromAccount.withdraw(currency, amount);
            toAccount.deposit(currency, amount);
            
            // Log
            Transaction tx = new Transaction(/* ... */);
            transactionLog.record(tx);
            
            return tx;
        } finally {
            accounts.unlockAccount(first);
            accounts.unlockAccount(second);
        }
    }
}
```

### Rate Limiting

```java
public class TransactionRateLimiter {
    private Map<UUID, RateLimitState> playerLimits = new ConcurrentHashMap<>();
    
    private static final int MAX_TRANSACTIONS_PER_MINUTE = 30;
    private static final long MAX_AMOUNT_PER_HOUR = 1_000_000;
    
    public boolean checkLimit(UUID playerId, long amount) {
        RateLimitState state = playerLimits.computeIfAbsent(
            playerId, 
            id -> new RateLimitState()
        );
        
        return state.checkAndIncrement(amount);
    }
}
```

## Configuration

```yaml
# economy.yml
currencies:
  primary: "gold"
  
  gold:
    name: "Gold"
    symbol: "G"
    format: "{symbol}{amount}"
    decimals: 0
    tradeable: true
    starting_balance: 100
    
  gems:
    name: "Gems"
    symbol: "💎"
    format: "{amount} {symbol}"
    decimals: 0
    tradeable: false
    starting_balance: 0

transactions:
  enable_logging: true
  log_retention_days: 90
  max_per_minute: 30
  max_amount_per_hour: 1000000

shops:
  tax_rate: 0.05
  max_listings_per_shop: 50
  max_shops_per_player: 3
  restock_interval: "1h"

banking:
  enable_interest: true
  interest_rate: 0.001
  interest_interval: "24h"
  minimum_balance_for_interest: 1000
```
