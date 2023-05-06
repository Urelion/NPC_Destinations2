package net.livecar.nuttyworks.npc_destinations.listeners;

import net.livecar.nuttyworks.npc_destinations.DestinationsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class OnPlayerJoinLeaveEvent implements Listener {

    private DestinationsPlugin plugin;

    public OnPlayerJoinLeaveEvent(DestinationsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {

    }

    @EventHandler
    public void onPlayerLeaveEvent(PlayerQuitEvent event) {
        // Remove this player from the debug if they are in it
        plugin.getDebugTargets().remove(event.getPlayer());
    }
}
