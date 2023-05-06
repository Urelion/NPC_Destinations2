package net.livecar.nuttyworks.npc_destinations.api;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NavigationFailed extends Event {
    private static final HandlerList handlers = new HandlerList();

    private Destination targetDestination;
    private NPC                      owningNPC;

    public NavigationFailed(NPC npc, Destination newDestination) {
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

    public NPC getNPC() {
        return owningNPC;
    }

    public Destination getDestination() {
        return targetDestination;
    }
}
