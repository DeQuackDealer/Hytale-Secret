package rubidium.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.*;

/**
 * Time formatting and parsing utilities.
 */
public final class TimeUtils {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhdwMy])");
    
    private TimeUtils() {}
    
    public static String formatDuration(long millis) {
        if (millis < 0) return "Permanent";
        
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        
        StringBuilder result = new StringBuilder();
        if (days > 0) result.append(days).append("d ");
        if (hours > 0 || days > 0) result.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) result.append(minutes).append("m ");
        result.append(seconds).append("s");
        
        return result.toString().trim();
    }
    
    public static String formatDurationShort(long millis) {
        if (millis < 0) return "∞";
        
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "s";
        
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m";
        
        long hours = minutes / 60;
        if (hours < 24) return hours + "h";
        
        long days = hours / 24;
        if (days < 7) return days + "d";
        
        long weeks = days / 7;
        if (weeks < 4) return weeks + "w";
        
        long months = days / 30;
        return months + "mo";
    }
    
    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }
    
    public static String formatShortDate(long timestamp) {
        return SHORT_DATE_FORMAT.format(new Date(timestamp));
    }
    
    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) return -1;
        
        long total = 0;
        Matcher matcher = DURATION_PATTERN.matcher(input.toLowerCase());
        
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            
            total += switch (unit) {
                case "s" -> value * 1000;
                case "m" -> value * 60 * 1000;
                case "h" -> value * 60 * 60 * 1000;
                case "d" -> value * 24 * 60 * 60 * 1000;
                case "w" -> value * 7 * 24 * 60 * 60 * 1000;
                case "M" -> value * 30 * 24 * 60 * 60 * 1000;
                case "y" -> value * 365 * 24 * 60 * 60 * 1000;
                default -> 0;
            };
        }
        
        return total > 0 ? total : -1;
    }
    
    public static String getTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        
        if (diff < 60000) return "just now";
        if (diff < 3600000) return (diff / 60000) + " minutes ago";
        if (diff < 86400000) return (diff / 3600000) + " hours ago";
        if (diff < 604800000) return (diff / 86400000) + " days ago";
        if (diff < 2592000000L) return (diff / 604800000) + " weeks ago";
        if (diff < 31536000000L) return (diff / 2592000000L) + " months ago";
        
        return (diff / 31536000000L) + " years ago";
    }
}
