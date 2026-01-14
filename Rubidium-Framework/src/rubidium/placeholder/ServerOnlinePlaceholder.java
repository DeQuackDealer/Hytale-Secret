package rubidium.placeholder;

import rubidium.hytale.api.HytaleServer;
import rubidium.hytale.api.player.Player;

public class ServerOnlinePlaceholder implements PlaceholderProvider {
    
    @Override
    public String getName() { return "Server Online Count"; }
    
    @Override
    public String getIdentifier() { return "server_online"; }
    
    @Override
    public String parse(Player player, String input) {
        return String.valueOf(HytaleServer.getInstance().getOnlinePlayerCount());
    }
}
