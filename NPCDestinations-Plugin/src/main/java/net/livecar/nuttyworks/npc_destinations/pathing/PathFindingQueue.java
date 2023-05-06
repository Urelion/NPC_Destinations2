package net.livecar.nuttyworks.npc_destinations.pathing;

import lombok.Getter;
import lombok.Setter;
import net.citizensnpcs.api.npc.NPC;
import net.livecar.nuttyworks.npc_destinations.citizens.NPCDestinationsTrait;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.*;

@Getter
@Setter
public class PathFindingQueue {

    private String requestedBy;
    private NPC npc;
    private NPCDestinationsTrait npcTrait;

    private List<Material> allowedPathBlocks;
    private Boolean opensGates;
    private Boolean opensWoodDoors;
    private Boolean opensMetalDoors;

    private int range;
    private int startX, startY, startZ;
    private int endX, endY, endZ;
    private World world;

    private TreeSet<Tile> open;
    private HashMap<Tile, Tile> openLookup;
    private HashSet<Tile> closed;

    private int blocksBelow;

    private Long blocksProcessed;
    private Long timeSpent;
    private Date processingStarted = null;

    private PathingResult pathFindingResult;

    public Location getPathLocation(Location source) {
        if (blocksBelow < 0) // Means we want the absolute 0 block to find a path
            return new Location(source.getWorld(), source.getX(), Math.abs(blocksBelow) - 1, source.getZ());
        return new Location(source.getWorld(), source.getX(), source.getY() - blocksBelow, source.getZ());
    }

    public void addStartXYZ(int x, int y, int z) {
        this.startX = this.startX + x;
        this.startY = this.startY + y;
        this.startZ = this.startZ + z;
    }

    public void subtractStartXYZ(int x, int y, int z) {
        this.startX = this.startX - x;
        this.startY = this.startY - y;
        this.startZ = this.startZ - z;
    }

    public void addEndXYZ(int x, int y, int z) {
        this.endX = this.endX + x;
        this.endY = this.endY + y;
        this.endZ = this.endZ + z;
    }

    public void subtractEndXYZ(int x, int y, int z) {
        this.endX = this.endX - x;
        this.endY = this.endY - y;
        this.endZ = this.endZ - z;
    }

    public Location getStartLocation() {
        return new Location(this.world, this.startX, this.startY, this.startZ);
    }

    public Location getEndLocation() {
        return new Location(this.world, this.endX, this.endY, this.endZ);
    }
}
