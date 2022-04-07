package net.livecar.nuttyworks.npc_destinations.api;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NavigationReached extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private Destination targetDestination;
    private NPC                      owningNPC;
    private boolean                  cancelEvent;

    public NavigationReached(NPC npc, Destination newDestination) {
        targetDestination = newDestination;
        owningNPC = npc;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelEvent = cancel;
    }

    @Override
    public boolean isCancelled() {
        return cancelEvent;
    }

    public NPC getNPC() {
        return owningNPC;
    }

    public Destination getDestination() {
        return targetDestination;
    }
}
