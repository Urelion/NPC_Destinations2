package net.livecar.nuttyworks.npc_destinations;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DebugTarget {

    public CommandSender targetSender;
    private final List<Integer> targetIDS;
    public List<Location> debugBlocksSent;

    public DebugTarget(CommandSender sender, Integer npcID) {
        targetSender = sender;
        targetIDS = new ArrayList<>();
        if (npcID > -1) {
            targetIDS.add(npcID);
        }
        if (debugBlocksSent == null)
            debugBlocksSent = new ArrayList<>();
    }

    public void addNPCTarget(Integer npcID) {
        if (!targetIDS.contains(npcID))
            targetIDS.add(npcID);
    }

    public void removeNPCTarget(Integer npcID) {
        targetIDS.remove(npcID);
    }

    public List<Integer> getTargets() {
        return targetIDS;
    }

    public void addDebugBlockSent(Location blockLocation, Material material) {
        if (debugBlocksSent.contains(blockLocation))
            return;
        debugBlocksSent.add(blockLocation);

        if (((Player) targetSender).isOnline()) {
            DestinationsPlugin.getInstance().getMcUtils().sendClientBlock((Player) targetSender, blockLocation, material);
        }
    }

    public void removeDebugBlockSent(Location blockLocation) {
        if (!debugBlocksSent.contains(blockLocation))
            return;
        debugBlocksSent.remove(blockLocation);
        if (((Player) targetSender).isOnline()) {
            DestinationsPlugin.getInstance().getMcUtils().sendClientBlock((Player) targetSender, blockLocation, null);
        }
    }

    public void clearDebugBlocks() {
        for (Location blockLocation : debugBlocksSent) {
            if (((Player) targetSender).isOnline()) {
                DestinationsPlugin.getInstance().getMcUtils().sendClientBlock((Player) targetSender, blockLocation, null);
            }
        }
        debugBlocksSent.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DebugTarget that = (DebugTarget) o;
        return Objects.equals(targetSender, that.targetSender);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetSender);
    }
}
