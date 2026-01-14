package rubidium.placeholder;

import rubidium.hytale.api.HytaleServer;
import rubidium.hytale.api.player.Player;

public class ServerTpsPlaceholder implements PlaceholderProvider {
    
    @Override
    public String getName() { return "Server TPS"; }
    
    @Override
    public String getIdentifier() { return "server_tps"; }
    
    @Override
    public String parse(Player player, String input) {
        return String.format("%.1f", HytaleServer.getInstance().getTPS());
    }
}
