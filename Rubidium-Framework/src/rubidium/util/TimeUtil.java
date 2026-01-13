package rubidium.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtil {
    
    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "(\\d+)\\s*(y(?:ear)?s?|mo(?:nth)?s?|w(?:eek)?s?|d(?:ay)?s?|h(?:our)?s?|m(?:in(?:ute)?)?s?|s(?:ec(?:ond)?)?s?)",
        Pattern.CASE_INSENSITIVE
    );
    
    private TimeUtil() {}
    
    public static long parseDuration(String input) {
        if (input == null || input.isBlank()) return 0;
        
        long totalMillis = 0;
        Matcher matcher = DURATION_PATTERN.matcher(input);
        
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            
            totalMillis += switch (unit.charAt(0)) {
                case 'y' -> value * 365 * 24 * 60 * 60 * 1000L;
                case 'w' -> value * 7 * 24 * 60 * 60 * 1000L;
                case 'd' -> value * 24 * 60 * 60 * 1000L;
                case 'h' -> value * 60 * 60 * 1000L;
                case 's' -> value * 1000L;
                case 'm' -> unit.startsWith("mo") 
                    ? value * 30 * 24 * 60 * 60 * 1000L 
                    : value * 60 * 1000L;
                default -> 0;
            };
        }
        
        return totalMillis;
    }
    
    public static String formatDuration(long millis) {
        return formatDuration(millis, false);
    }
    
    public static String formatDuration(long millis, boolean compact) {
        if (millis <= 0) return compact ? "0s" : "0 seconds";
        
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        
        StringBuilder sb = new StringBuilder();
        
        if (compact) {
            if (days > 0) sb.append(days).append("d ");
            if (hours > 0) sb.append(hours).append("h ");
            if (minutes > 0) sb.append(minutes).append("m ");
            if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");
        } else {
            if (days > 0) sb.append(days).append(days == 1 ? " day " : " days ");
            if (hours > 0) sb.append(hours).append(hours == 1 ? " hour " : " hours ");
            if (minutes > 0) sb.append(minutes).append(minutes == 1 ? " minute " : " minutes ");
            if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }
        
        return sb.toString().trim();
    }
    
    public static String formatRelative(Instant instant) {
        Duration duration = Duration.between(instant, Instant.now());
        
        if (duration.isNegative()) {
            return "in " + formatDuration(Math.abs(duration.toMillis()), true);
        }
        
        long seconds = duration.getSeconds();
        
        if (seconds < 60) return "just now";
        if (seconds < 3600) return (seconds / 60) + " minutes ago";
        if (seconds < 86400) return (seconds / 3600) + " hours ago";
        if (seconds < 604800) return (seconds / 86400) + " days ago";
        if (seconds < 2592000) return (seconds / 604800) + " weeks ago";
        if (seconds < 31536000) return (seconds / 2592000) + " months ago";
        
        return (seconds / 31536000) + " years ago";
    }
    
    public static String formatTime(long millis) {
        return formatTime(Instant.ofEpochMilli(millis), "yyyy-MM-dd HH:mm:ss");
    }
    
    public static String formatTime(Instant instant, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern)
            .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }
    
    public static String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    public static long ticksToMillis(long ticks) {
        return ticks * 50;
    }
    
    public static long millisToTicks(long millis) {
        return millis / 50;
    }
    
    public static long secondsToTicks(long seconds) {
        return seconds * 20;
    }
    
    public static long ticksToSeconds(long ticks) {
        return ticks / 20;
    }
}
