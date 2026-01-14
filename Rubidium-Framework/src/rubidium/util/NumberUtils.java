package rubidium.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Number formatting utilities.
 */
public final class NumberUtils {
    
    private static final String[] SUFFIXES = {"", "k", "m", "b", "t", "q", "Q"};
    private static final DecimalFormat FORMAT;
    private static final DecimalFormat PERCENT_FORMAT;
    
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        FORMAT = new DecimalFormat("#.##", symbols);
        PERCENT_FORMAT = new DecimalFormat("0.0", symbols);
    }
    
    private NumberUtils() {}
    
    public static String formatNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }
        
        double absValue = Math.abs(value);
        
        int suffixIndex = 0;
        if (absValue >= 1000) {
            suffixIndex = (int) (Math.log10(absValue) / 3);
            suffixIndex = Math.min(suffixIndex, SUFFIXES.length - 1);
        }
        
        double scaledValue = value / Math.pow(1000, suffixIndex);
        String formatted = FORMAT.format(scaledValue);
        
        return formatted + SUFFIXES[suffixIndex];
    }
    
    public static String formatPercent(double value) {
        return PERCENT_FORMAT.format(value * 100) + "%";
    }
    
    public static String formatWithCommas(long value) {
        return String.format("%,d", value);
    }
    
    public static String formatRoman(int number) {
        if (number <= 0 || number > 3999) return String.valueOf(number);
        
        String[] thousands = {"", "M", "MM", "MMM"};
        String[] hundreds = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] tens = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        
        return thousands[number / 1000] +
               hundreds[(number % 1000) / 100] +
               tens[(number % 100) / 10] +
               ones[number % 10];
    }
    
    public static double parseNumber(String input) {
        if (input == null || input.isEmpty()) return 0;
        
        input = input.toLowerCase().replace(",", "").trim();
        
        double multiplier = 1;
        if (input.endsWith("k")) {
            multiplier = 1000;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("m")) {
            multiplier = 1000000;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("b")) {
            multiplier = 1000000000;
            input = input.substring(0, input.length() - 1);
        }
        
        try {
            return Double.parseDouble(input) * multiplier;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
