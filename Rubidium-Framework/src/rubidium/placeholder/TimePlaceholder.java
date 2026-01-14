package rubidium.placeholder;

import rubidium.hytale.api.player.Player;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimePlaceholder implements PlaceholderProvider {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    @Override
    public String getName() { return "Current Time"; }
    
    @Override
    public String getIdentifier() { return "time"; }
    
    @Override
    public String parse(Player player, String input) {
        return LocalTime.now().format(FORMATTER);
    }
}
