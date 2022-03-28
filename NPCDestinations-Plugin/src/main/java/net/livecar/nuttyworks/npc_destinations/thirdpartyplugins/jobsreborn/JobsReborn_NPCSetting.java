package net.livecar.nuttyworks.npc_destinations.thirdpartyplugins.jobsreborn;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.livecar.nuttyworks.npc_destinations.citizens.NPCDestinationsTrait;
import org.bukkit.Location;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class JobsReborn_NPCSetting {

    public int npcID;
    public HashMap<UUID, JobsReborn_LocationSetting> locations;
    public Location currentDestination;
    public NPCDestinationsTrait destinationsTrait;
    public Date lastAction;

    public JobsReborn_NPCSetting() {
        locations = new HashMap<>();
        lastAction = new Date();
    }

    public void setNPC(Integer npcid) {
        this.npcID = npcid;
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcid);
        destinationsTrait = npc.getTrait(NPCDestinationsTrait.class);
        locations = new HashMap<>();
    }

    public Integer getNPCID() {
        return npcID;
    }
}
