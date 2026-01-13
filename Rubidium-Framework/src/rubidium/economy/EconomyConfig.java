package rubidium.economy;

import java.time.Duration;

public record EconomyConfig(
    String primaryCurrency,
    long startingBalance,
    boolean enableLogging,
    int logRetentionDays,
    int maxTransactionsPerMinute,
    long maxAmountPerHour,
    double taxRate,
    boolean enableInterest,
    double interestRate,
    Duration interestInterval,
    long minimumBalanceForInterest
) {
    public static EconomyConfig defaults() {
        return new EconomyConfig(
            "gold",
            100,
            true,
            90,
            30,
            1_000_000,
            0.05,
            false,
            0.001,
            Duration.ofHours(24),
            1000
        );
    }
}
