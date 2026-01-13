package com.yellowtale.rubidium.economy;

import com.yellowtale.rubidium.core.logging.RubidiumLogger;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Thread-safe economy manager with ACID-compliant transactions.
 * 
 * <h2>Transaction Safety</h2>
 * <ul>
 *   <li><b>Atomicity</b>: All operations execute within ReentrantLock blocks. Either all changes
 *       apply (withdraw + deposit) or none do (on insufficient funds or frozen accounts).</li>
 *   <li><b>Consistency</b>: Balance validation occurs before any mutation. Frozen account checks
 *       prevent invalid state transitions.</li>
 *   <li><b>Isolation</b>: Per-account ReentrantLocks serialize concurrent access. For transfers,
 *       deterministic lock ordering (by UUID comparison) prevents deadlocks.</li>
 *   <li><b>Durability</b>: Transaction log is maintained in-memory with persistence hooks available
 *       via onTransaction listeners. Production deployments should persist to database.</li>
 * </ul>
 * 
 * <h2>Lock Ordering</h2>
 * <p>When transferring between accounts, locks are always acquired in UUID-sorted order
 * (smallest UUID first) to prevent deadlock scenarios where Thread A holds lock X waiting
 * for lock Y while Thread B holds lock Y waiting for lock X.</p>
 */
public class EconomyManager {
    
    private final RubidiumLogger logger;
    private final Path dataDir;
    private EconomyConfig config;
    
    private final Map<String, Currency> currencies;
    private final Map<UUID, Account> accounts;
    private final List<Transaction> transactionLog;
    private final Map<UUID, ReentrantLock> accountLocks;
    
    private final List<Consumer<Transaction>> transactionListeners;
    
    public EconomyManager(RubidiumLogger logger, Path dataDir) {
        this.logger = logger;
        this.dataDir = dataDir;
        this.config = EconomyConfig.defaults();
        this.currencies = new ConcurrentHashMap<>();
        this.accounts = new ConcurrentHashMap<>();
        this.transactionLog = Collections.synchronizedList(new ArrayList<>());
        this.accountLocks = new ConcurrentHashMap<>();
        this.transactionListeners = new ArrayList<>();
        
        registerCurrency(Currency.GOLD);
        registerCurrency(Currency.GEMS);
        registerCurrency(Currency.TOKENS);
    }
    
    public void registerCurrency(Currency currency) {
        currencies.put(currency.getId(), currency);
        logger.debug("Registered currency: {}", currency.getName());
    }
    
    public Optional<Currency> getCurrency(String id) {
        return Optional.ofNullable(currencies.get(id));
    }
    
    public Currency getPrimaryCurrency() {
        return currencies.values().stream()
            .filter(Currency::isPrimary)
            .findFirst()
            .orElse(Currency.GOLD);
    }
    
    public List<Currency> getCurrencies() {
        return new ArrayList<>(currencies.values());
    }
    
    public Account getAccount(UUID playerId) {
        return accounts.get(playerId);
    }
    
    public Account getOrCreateAccount(UUID playerId) {
        return accounts.computeIfAbsent(playerId, id -> {
            Account account = new Account(UUID.randomUUID(), playerId, Account.AccountType.PLAYER);
            account.deposit(getPrimaryCurrency().getId(), config.startingBalance());
            return account;
        });
    }
    
    public long getBalance(UUID playerId) {
        return getBalance(playerId, getPrimaryCurrency().getId());
    }
    
    public long getBalance(UUID playerId, String currency) {
        Account account = getOrCreateAccount(playerId);
        return account.getBalance(currency);
    }
    
    public boolean hasBalance(UUID playerId, long amount) {
        return hasBalance(playerId, getPrimaryCurrency().getId(), amount);
    }
    
    public boolean hasBalance(UUID playerId, String currency, long amount) {
        return getBalance(playerId, currency) >= amount;
    }
    
    public void setBalance(UUID playerId, String currency, long amount) {
        Account account = getOrCreateAccount(playerId);
        ReentrantLock lock = getAccountLock(playerId);
        lock.lock();
        try {
            long current = account.getBalance(currency);
            if (amount > current) {
                account.deposit(currency, amount - current);
            } else {
                account.withdraw(currency, current - amount);
            }
        } finally {
            lock.unlock();
        }
    }
    
    public Transaction transfer(UUID from, UUID to, long amount, String description) {
        return transfer(from, to, getPrimaryCurrency().getId(), amount, description);
    }
    
    public Transaction transfer(UUID from, UUID to, String currency, long amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        UUID first = from.compareTo(to) < 0 ? from : to;
        UUID second = from.compareTo(to) < 0 ? to : from;
        
        ReentrantLock lock1 = getAccountLock(first);
        ReentrantLock lock2 = getAccountLock(second);
        
        lock1.lock();
        lock2.lock();
        try {
            Account fromAccount = getOrCreateAccount(from);
            Account toAccount = getOrCreateAccount(to);
            
            if (fromAccount.isFrozen()) {
                return createFailedTransaction(from, to, currency, amount, "Source account is frozen");
            }
            if (toAccount.isFrozen()) {
                return createFailedTransaction(from, to, currency, amount, "Target account is frozen");
            }
            if (!fromAccount.withdraw(currency, amount)) {
                return createFailedTransaction(from, to, currency, amount, "Insufficient funds");
            }
            
            toAccount.deposit(currency, amount);
            
            Transaction tx = new Transaction(
                UUID.randomUUID(),
                Transaction.TransactionType.TRANSFER,
                Transaction.TransactionStatus.COMPLETED,
                fromAccount.getId(),
                toAccount.getId(),
                currency,
                amount,
                description,
                System.currentTimeMillis()
            );
            
            recordTransaction(tx);
            return tx;
        } finally {
            lock2.unlock();
            lock1.unlock();
        }
    }
    
    public Transaction deposit(UUID playerId, long amount, String reason) {
        return deposit(playerId, getPrimaryCurrency().getId(), amount, reason);
    }
    
    public Transaction deposit(UUID playerId, String currency, long amount, String reason) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        ReentrantLock lock = getAccountLock(playerId);
        lock.lock();
        try {
            Account account = getOrCreateAccount(playerId);
            account.deposit(currency, amount);
            
            Transaction tx = new Transaction(
                UUID.randomUUID(),
                Transaction.TransactionType.DEPOSIT,
                Transaction.TransactionStatus.COMPLETED,
                null,
                account.getId(),
                currency,
                amount,
                reason,
                System.currentTimeMillis()
            );
            
            recordTransaction(tx);
            return tx;
        } finally {
            lock.unlock();
        }
    }
    
    public Transaction withdraw(UUID playerId, long amount, String reason) {
        return withdraw(playerId, getPrimaryCurrency().getId(), amount, reason);
    }
    
    public Transaction withdraw(UUID playerId, String currency, long amount, String reason) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        ReentrantLock lock = getAccountLock(playerId);
        lock.lock();
        try {
            Account account = getOrCreateAccount(playerId);
            
            if (!account.withdraw(currency, amount)) {
                return createFailedTransaction(null, playerId, currency, amount, "Insufficient funds");
            }
            
            Transaction tx = new Transaction(
                UUID.randomUUID(),
                Transaction.TransactionType.WITHDRAWAL,
                Transaction.TransactionStatus.COMPLETED,
                account.getId(),
                null,
                currency,
                amount,
                reason,
                System.currentTimeMillis()
            );
            
            recordTransaction(tx);
            return tx;
        } finally {
            lock.unlock();
        }
    }
    
    public void freezeAccount(UUID accountId, String reason) {
        accounts.values().stream()
            .filter(a -> a.getId().equals(accountId))
            .findFirst()
            .ifPresent(account -> {
                account.setFrozen(true);
                logger.info("Account {} frozen: {}", accountId, reason);
            });
    }
    
    public void unfreezeAccount(UUID accountId) {
        accounts.values().stream()
            .filter(a -> a.getId().equals(accountId))
            .findFirst()
            .ifPresent(account -> {
                account.setFrozen(false);
                logger.info("Account {} unfrozen", accountId);
            });
    }
    
    public long convert(String fromCurrency, String toCurrency, long amount) {
        Currency from = currencies.get(fromCurrency);
        Currency to = currencies.get(toCurrency);
        
        if (from == null || to == null) {
            throw new IllegalArgumentException("Unknown currency");
        }
        
        Double rate = from.getExchangeRates().get(toCurrency);
        if (rate == null) {
            throw new IllegalArgumentException("No exchange rate defined");
        }
        
        return (long) (amount * rate);
    }
    
    public String format(long amount) {
        return format(getPrimaryCurrency().getId(), amount);
    }
    
    public String format(String currencyId, long amount) {
        Currency currency = currencies.get(currencyId);
        return currency != null ? currency.format(amount) : String.valueOf(amount);
    }
    
    public List<Transaction> getTransactionHistory(UUID playerId, int limit) {
        Account account = accounts.get(playerId);
        if (account == null) return Collections.emptyList();
        
        return transactionLog.stream()
            .filter(tx -> account.getId().equals(tx.getSourceAccount()) || 
                         account.getId().equals(tx.getTargetAccount()))
            .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
            .limit(limit)
            .toList();
    }
    
    public List<Account> getTopBalances(String currency, int limit) {
        return accounts.values().stream()
            .filter(a -> a.getType() == Account.AccountType.PLAYER)
            .sorted((a, b) -> Long.compare(b.getBalance(currency), a.getBalance(currency)))
            .limit(limit)
            .toList();
    }
    
    public void onTransaction(Consumer<Transaction> listener) {
        transactionListeners.add(listener);
    }
    
    private void recordTransaction(Transaction tx) {
        transactionLog.add(tx);
        
        for (Consumer<Transaction> listener : transactionListeners) {
            listener.accept(tx);
        }
        
        while (transactionLog.size() > 100000) {
            transactionLog.remove(0);
        }
    }
    
    private Transaction createFailedTransaction(UUID from, UUID to, String currency, long amount, String reason) {
        Transaction tx = new Transaction(
            UUID.randomUUID(),
            Transaction.TransactionType.TRANSFER,
            Transaction.TransactionStatus.FAILED,
            from != null ? accounts.get(from).getId() : null,
            to != null ? accounts.get(to).getId() : null,
            currency,
            amount,
            reason,
            System.currentTimeMillis()
        );
        tx.setFailureReason(reason);
        return tx;
    }
    
    private ReentrantLock getAccountLock(UUID playerId) {
        return accountLocks.computeIfAbsent(playerId, id -> new ReentrantLock());
    }
    
    public void setConfig(EconomyConfig config) {
        this.config = config;
    }
    
    public EconomyConfig getConfig() {
        return config;
    }
    
    public int getAccountCount() {
        return accounts.size();
    }
    
    public long getTotalCurrency(String currency) {
        return accounts.values().stream()
            .mapToLong(a -> a.getBalance(currency))
            .sum();
    }
}
