package net.livecar.nuttyworks.npc_destinations.api;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class LocationDeleted extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private DestinationSetting destinationChanged;
    private NPC                      owningNPC;
    private boolean                  cancelEvent;

    public LocationDeleted(NPC referencedNPC, final DestinationSetting removedDestination) {
        destinationChanged = removedDestination;
        owningNPC = referencedNPC;
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

    public DestinationSetting getDestination() {
        return destinationChanged;
    }
}
