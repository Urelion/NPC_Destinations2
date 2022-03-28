package net.livecar.nuttyworks.npc_destinations.api;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NavigationNewDestination extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private DestinationSetting targetDestination;
    private NPC                      owningNPC;
    private boolean                  cancelEvent;
    private boolean                  forcedEvent;

    public NavigationNewDestination(NPC npc, DestinationSetting newDestination, boolean forced) {
        targetDestination = newDestination;
        owningNPC = npc;
        forcedEvent = forced;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public NPC getNPC() {
        return owningNPC;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelEvent = cancel;
    }

    @Override
    public boolean isCancelled() {
        return cancelEvent;
    }

    public boolean isForced() {
        return forcedEvent;
    }

    public DestinationSetting getDestination() {
        return targetDestination;
    }
}
