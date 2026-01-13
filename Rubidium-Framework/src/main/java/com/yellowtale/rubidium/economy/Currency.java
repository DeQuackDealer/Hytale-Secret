package com.yellowtale.rubidium.economy;

import java.util.HashMap;
import java.util.Map;

public class Currency {
    private final String id;
    private String name;
    private String symbol;
    private String format;
    private int decimalPlaces;
    private boolean primary;
    private boolean tradeable;
    private boolean convertible;
    private final Map<String, Double> exchangeRates;
    
    public static final Currency GOLD = new Currency(
        "gold", "Gold", "G", "{symbol}{amount}", 0, true, true, true
    );
    public static final Currency GEMS = new Currency(
        "gems", "Gems", "💎", "{amount} {symbol}", 0, false, false, true
    );
    public static final Currency TOKENS = new Currency(
        "tokens", "Tokens", "T", "{amount}{symbol}", 0, false, true, false
    );
    
    public Currency(String id, String name, String symbol, String format, int decimalPlaces, boolean primary, boolean tradeable, boolean convertible) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.format = format;
        this.decimalPlaces = decimalPlaces;
        this.primary = primary;
        this.tradeable = tradeable;
        this.convertible = convertible;
        this.exchangeRates = new HashMap<>();
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public int getDecimalPlaces() { return decimalPlaces; }
    public void setDecimalPlaces(int decimalPlaces) { this.decimalPlaces = decimalPlaces; }
    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
    public boolean isTradeable() { return tradeable; }
    public void setTradeable(boolean tradeable) { this.tradeable = tradeable; }
    public boolean isConvertible() { return convertible; }
    public void setConvertible(boolean convertible) { this.convertible = convertible; }
    public Map<String, Double> getExchangeRates() { return exchangeRates; }
    
    public void setExchangeRate(String toCurrency, double rate) {
        exchangeRates.put(toCurrency, rate);
    }
    
    public String format(long amount) {
        String formatted;
        if (decimalPlaces > 0) {
            double displayAmount = amount / Math.pow(10, decimalPlaces);
            formatted = String.format("%." + decimalPlaces + "f", displayAmount);
        } else {
            formatted = formatWithCommas(amount);
        }
        return format.replace("{amount}", formatted).replace("{symbol}", symbol);
    }
    
    private String formatWithCommas(long amount) {
        return String.format("%,d", amount);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Currency currency = (Currency) o;
        return id.equals(currency.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
