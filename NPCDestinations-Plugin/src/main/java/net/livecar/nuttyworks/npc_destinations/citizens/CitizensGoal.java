package net.livecar.nuttyworks.npc_destinations.citizens;

import net.citizensnpcs.api.ai.event.NavigationCompleteEvent;
import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter;
import net.citizensnpcs.api.ai.tree.BehaviorStatus;
import net.citizensnpcs.api.npc.NPC;
import net.livecar.nuttyworks.npc_destinations.DestinationsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CitizensGoal extends BehaviorGoalAdapter implements Listener {
    private final NPC npc;
    private boolean forceFinish;
    public int failedPathCount = 0;

    public CitizensGoal(NPC npc) {
        this.npc = npc;
        Bukkit.getServer().getPluginManager().registerEvents(this, DestinationsPlugin.getInstance());
    }

    @EventHandler
    public void onFinish(NavigationCompleteEvent event) {
        if (event.getNPC() == this.npc) {
            this.forceFinish = true;
        }
    }

    public void reset() {
        this.forceFinish = false;
    }

    public BehaviorStatus run() {
        if ((!this.npc.getNavigator().isNavigating()) || (this.forceFinish)) {
            return BehaviorStatus.SUCCESS;
        }
        return BehaviorStatus.RUNNING;
    }

    public boolean shouldExecute() {
        return DestinationsPlugin.getInstance().getCitizensProcessing().shouldExecute(this.npc, this);
    }
}
