package net.livecar.nuttyworks.npc_destinations.thirdpartyplugins.betonquest.v2;

import net.livecar.nuttyworks.npc_destinations.DestinationsPlugin;
import net.livecar.nuttyworks.npc_destinations.thirdpartyplugins.betonquest.BetonQuest_Interface;
import org.betonquest.betonquest.BetonQuest;
import org.bukkit.event.Listener;

public class BetonQuest_Plugin implements Listener, BetonQuest_Interface {
    static DestinationsPlugin destRef = null;

    public BetonQuest_Plugin(DestinationsPlugin storageRef) {
        destRef = storageRef;

        BetonQuest_Plugin.this.onStart();
    }

    private void onStart() {
        BetonQuest.getInstance().registerEvents("npcdest_goloc", Event_goloc.class);
        destRef.getMessagesManager().consoleMessage(destRef, "destinations", "Console_Messages.betonquest_events", "npcdest_goloc");

        BetonQuest.getInstance().registerConditions("npcdest_currentlocation", Condition_CurrentLocation.class);
        destRef.getMessagesManager().consoleMessage(destRef, "destinations", "Console_Messages.betonquest_conditions", "npcdest_currentlocation");
        BetonQuest.getInstance().registerConditions("npcdest_distancetolocation", Condition_DistanceToLocation.class);
        destRef.getMessagesManager().consoleMessage(destRef, "destinations", "Console_Messages.betonquest_conditions", "npcdest_distancetolocation");
    }
}
