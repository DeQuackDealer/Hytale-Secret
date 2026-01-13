package rubidium.economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Transaction {
    private final UUID id;
    private final TransactionType type;
    private TransactionStatus status;
    
    private final UUID sourceAccount;
    private final UUID targetAccount;
    private final String currency;
    private final long amount;
    
    private final String description;
    private final Map<String, String> metadata;
    
    private final long createdAt;
    private long completedAt;
    private String failureReason;
    
    public Transaction(UUID id, TransactionType type, TransactionStatus status,
                      UUID sourceAccount, UUID targetAccount, String currency, long amount,
                      String description, long createdAt) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.currency = currency;
        this.amount = amount;
        this.description = description;
        this.metadata = new HashMap<>();
        this.createdAt = createdAt;
        this.completedAt = status == TransactionStatus.COMPLETED ? createdAt : 0;
    }
    
    public UUID getId() { return id; }
    public TransactionType getType() { return type; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public UUID getSourceAccount() { return sourceAccount; }
    public UUID getTargetAccount() { return targetAccount; }
    public String getCurrency() { return currency; }
    public long getAmount() { return amount; }
    public String getDescription() { return description; }
    public Map<String, String> getMetadata() { return metadata; }
    public long getCreatedAt() { return createdAt; }
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public boolean isSuccessful() {
        return status == TransactionStatus.COMPLETED;
    }
    
    public void addMetadata(String key, String value) {
        metadata.put(key, value);
    }
    
    public enum TransactionType {
        TRANSFER,
        PURCHASE,
        SALE,
        DEPOSIT,
        WITHDRAWAL,
        REWARD,
        PENALTY,
        ESCROW_HOLD,
        ESCROW_RELEASE,
        ADMIN
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
