package rubidium.placeholder;

import rubidium.hytale.api.player.Player;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DatePlaceholder implements PlaceholderProvider {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Override
    public String getName() { return "Current Date"; }
    
    @Override
    public String getIdentifier() { return "date"; }
    
    @Override
    public String parse(Player player, String input) {
        return LocalDate.now().format(FORMATTER);
    }
}
