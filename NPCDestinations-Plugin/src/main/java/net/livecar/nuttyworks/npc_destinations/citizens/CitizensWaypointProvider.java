package net.livecar.nuttyworks.npc_destinations.citizens;

import net.citizensnpcs.api.ai.Goal;
import net.citizensnpcs.api.command.CommandContext;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.trait.waypoint.WaypointEditor;
import net.citizensnpcs.trait.waypoint.WaypointProvider;
import org.bukkit.command.CommandSender;

public class CitizensWaypointProvider implements WaypointProvider {
    private Goal currentGoal;
    private NPC npc;
    private volatile boolean paused;

    public WaypointEditor createEditor(CommandSender sender, CommandContext args) {
        return new WaypointEditor() {
            public void begin() {
            }

            public void end() {
            }
        };
    }

    public boolean isPaused() {
        return this.paused;
    }

    public void load(DataKey key) {
    }

    public void onRemove() {
        this.npc.getDefaultGoalController().removeGoal(this.currentGoal);
    }

    public void onSpawn(NPC npc) {
        this.npc = npc;

        NPCDestinationsTrait trait = npc.getOrAddTrait(NPCDestinationsTrait.class);

        if (trait.teleportOnFailedToStartLocation == null) trait.teleportOnFailedToStartLocation = true;
        if (trait.teleportOnNoPath == null) trait.teleportOnNoPath = true;

        if (this.currentGoal == null) this.currentGoal = new CitizensGoal(npc);
        npc.getDefaultGoalController().addGoal(this.currentGoal, 1);
    }

    public void save(DataKey key) {
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }
}