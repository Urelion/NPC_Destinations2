package net.livecar.nuttyworks.npc_destinations.listeners;

import net.citizensnpcs.api.event.CitizensDisableEvent;
import net.citizensnpcs.api.event.NPCTeleportEvent;
import net.livecar.nuttyworks.npc_destinations.DestinationsPlugin;
import net.livecar.nuttyworks.npc_destinations.citizens.NPCDestinationsTrait;
import net.livecar.nuttyworks.npc_destinations.pathing.PathFindingQueue;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class OnCitizensEvents implements Listener {

    private DestinationsPlugin plugin;

    public OnCitizensEvents(DestinationsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCitizensDisableEvent(final CitizensDisableEvent event) {
        Bukkit.getServer().getScheduler().cancelTasks(this.plugin);
        this.plugin.setAStarPathFinder(null);
        this.plugin.getMessagesManager().consoleMessage(this.plugin, "destinations", "Console_Messages.plugin_ondisable");
        Bukkit.getServer().getPluginManager().disablePlugin(this.plugin);
    }

    // @fixme Fix this - ain't working properly probably because we aren't setting the current action not enough?
//    @EventHandler
//    public void onPlayerTeleportEvent(NPCTeleportEvent event) {
//        NPCDestinationsTrait trait;
//        if (!event.getNPC().isSpawned() || this.plugin.getAStarPathFinder() == null || (trait = event.getNPC().getTraitNullable(NPCDestinationsTrait.class)) == null)
//            return;
//
//        switch (trait.getCurrentAction()) {
//            case TRAVELING:
//                trait.setCurrentAction(NPCDestinationsTrait.CurrentAction.IDLE);
//                break;
//            case PATH_HUNTING:
//                PathFindingQueue pathFindingQueue = this.plugin.getAStarPathFinder().getCurrentTask();
//                if (pathFindingQueue == null || pathFindingQueue.getNpc() != event.getNPC()) return;
//                this.plugin.getAStarPathFinder().cleanTask();
//                break;
//        }
//    }
}
