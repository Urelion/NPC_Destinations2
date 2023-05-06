package net.livecar.nuttyworks.npc_destinations.api;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class LocationUpdated extends Event {
    private static final HandlerList handlers = new HandlerList();

    private Destination destinationChanged;
    private NPC                      owningNPC;

    public LocationUpdated(NPC changedNPC, Destination changedDestination) {
        destinationChanged = changedDestination;
        owningNPC = changedNPC;
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

    public Destination getDestination() {
        return destinationChanged;
    }
}
