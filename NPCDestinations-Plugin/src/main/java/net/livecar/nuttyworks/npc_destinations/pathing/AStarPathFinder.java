package net.livecar.nuttyworks.npc_destinations.pathing;

import lombok.Getter;
import lombok.Setter;
import net.citizensnpcs.api.npc.NPC;
import net.livecar.nuttyworks.npc_destinations.DebugTarget;
import net.livecar.nuttyworks.npc_destinations.DestinationsPlugin;
import net.livecar.nuttyworks.npc_destinations.citizens.NPCDestinationsTrait;
import net.livecar.nuttyworks.npc_destinations.citizens.NPCDestinationsTrait.en_CurrentAction;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.material.Openable;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

public class AStarPathFinder {

    private final DestinationsPlugin plugin;
    private boolean bInWater = false;
    private List<Player> playToPlayers;
    private Long last_Pause = 0L;

    @Getter @Setter
    private PathFindingQueue currentTask = null;
    @Getter
    private LinkedHashMap<Integer, PathFindingQueue> pathQueue = new LinkedHashMap<>();

    public AStarPathFinder(DestinationsPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkStatus() {
        if (plugin.getDebugLogLevel() == Level.FINEST)
            plugin.getMessagesManager().debugMessage(Level.FINEST, Arrays.toString(Thread.currentThread().getStackTrace()));

        if ((currentTask == null || currentTask.getNpc() == null) && pathQueue.size() > 0) {
            // Fire off the initial task
            nextQueue();
        }
    }

    public boolean isLocationWalkable(Location location, boolean opensGates, boolean opensWoodDoors, boolean opensMetalDoors) {
        if (plugin.getDebugLogLevel() == Level.FINEST)
            plugin.getMessagesManager().debugMessage(Level.FINEST, "(Location " + location.toString() + ",boolean " + opensGates + ", boolean " + opensWoodDoors + ", boolean " + opensMetalDoors + ") " + Arrays.toString(Thread.currentThread().getStackTrace()));
        return plugin.getMcUtils().isLocationWalkable(location, opensGates, opensWoodDoors, opensMetalDoors);
    }

    public boolean isLocationWalkable(Location location) {
        if (currentTask == null) return false;
        return isLocationWalkable(location, currentTask.getOpensGates(), currentTask.getOpensWoodDoors(), currentTask.getOpensMetalDoors());
    }

    public boolean requiresOpening(Location location) {
        plugin.getMessagesManager().debugMessage(Level.FINEST, "(Location " + location.toString() + ")" + Arrays.toString(Thread.currentThread().getStackTrace()));

        return plugin.getMcUtils().requiresOpening(location);
    }

    public void addToQueue(NPC npc, NPCDestinationsTrait npcTrait, Location start, Location end, int range, List<Material> AllowedPathBlocks, int blocksBelow, Boolean OpensGates, Boolean OpensWoodDoors, Boolean OpensMetalDoors, String requestedBy) {
        plugin.getMessagesManager().debugMessage(Level.FINEST, "(NPC " + npc.getId() + ", NPCDestinationsTrait " + (npcTrait == null) + ", Location " + start.toString() + ", Location " + end.toString() + ", int " + range + ", List<Material> " + AllowedPathBlocks.size() + ", int " + blocksBelow + ", Boolean " + OpensGates + ", Boolean " + OpensWoodDoors + ", Boolean " + OpensMetalDoors + ",String " + requestedBy + ") " + Arrays.toString(Thread.currentThread().getStackTrace()));

        if (playToPlayers != null) playToPlayers.clear();

        // For Short pathing faults just teleport the NPC @fixme verify this behaviour why would NPC teleport to itself? shouldn't it just teleport to the end location?
        if (npc.getEntity().getLocation().distanceSquared(end) < 4) {
            Location toLocation = new Location(npc.getEntity().getLocation().getWorld(), npc.getEntity().getLocation().getBlockX(), npc.getEntity().getLocation().getBlockY(), npc.getEntity().getLocation().getBlockZ());
            if (plugin.getMcUtils().isHalfBlock(npc.getEntity().getLocation().getBlock().getType()))
                toLocation.add(0, 1, 0);
            npc.getEntity().teleport(toLocation);
            return;
        }

        if (plugin.getDebugTargets() != null && plugin.getDebugTargets().size() > 0) {
            playToPlayers = new ArrayList<>();
            for (DebugTarget debugOutput : plugin.getDebugTargets()) {
                if ((debugOutput.targetSender instanceof Player) && (debugOutput.getTargets().size() == 0 || debugOutput.getTargets().contains(npc.getId())))
                    playToPlayers.add((Player) debugOutput.targetSender);
            }
        }

        if (pathQueue == null) pathQueue = new LinkedHashMap<>();

        if (pathQueue.containsKey(npc.getId())) {
            if (currentTask == null || currentTask.getNpc() == null) {
                if (last_Pause < new Date().getTime()) nextQueue();
            }
            return;
        }

        Location cleanStart = start.clone();
        Location cleanEnd = end.clone();

        if (!cleanStart.getBlock().getType().isSolid()) cleanStart = findSurface(cleanStart.clone());
        if (!cleanEnd.getBlock().getType().isSolid()) cleanEnd = findSurface(cleanEnd.clone());

        // Add to the queue
        PathFindingQueue pathFindingQueue = new PathFindingQueue();
        pathFindingQueue.setWorld(cleanStart.getWorld());
        pathFindingQueue.setStartX(cleanStart.getBlockX());
        pathFindingQueue.setStartY(cleanStart.getBlockY());
        pathFindingQueue.setStartZ(cleanStart.getBlockZ());
        pathFindingQueue.setEndX(cleanEnd.getBlockX());
        pathFindingQueue.setEndY(cleanEnd.getBlockY());
        pathFindingQueue.setEndZ(cleanEnd.getBlockZ());
        pathFindingQueue.setRange(range);
        pathFindingQueue.setNpcTrait(npcTrait);
        pathFindingQueue.setNpc(npc);
        pathFindingQueue.setOpensGates(OpensGates);
        pathFindingQueue.setOpensWoodDoors(OpensWoodDoors);
        pathFindingQueue.setOpensMetalDoors(OpensMetalDoors);
        pathFindingQueue.setAllowedPathBlocks(new ArrayList<>(AllowedPathBlocks));
        pathFindingQueue.setBlocksBelow(blocksBelow);
        pathFindingQueue.setRequestedBy(requestedBy);
        pathFindingQueue.setTimeSpent(0L);
        pathFindingQueue.setBlocksProcessed(0L);
        pathFindingQueue.setOpen(new HashMap<>());
        pathFindingQueue.setClosed(new HashMap<>());

        // Check if the start location is a 1/2 slab
        if (plugin.getMcUtils().isHalfBlock(cleanStart.getBlock().getRelative(0, 1, 0).getType())) {
            if (!cleanStart.getBlock().getRelative(0, 2, 0).getType().isSolid() && !cleanStart.getBlock().getRelative(0, 3, 0).getType().isSolid()) {
                pathFindingQueue.addStartXYZ(0, 1, 0);
            }
        }

        // Check if the end location is a 1/2 slab
        if (plugin.getMcUtils().isHalfBlock(cleanEnd.getBlock().getRelative(0, 1, 0).getType())) {
            if (!cleanEnd.getBlock().getRelative(0, 2, 0).getType().isSolid() && !cleanEnd.getBlock().getRelative(0, 3, 0).getType().isSolid()) {
                pathFindingQueue.addEndXYZ(0, 1,0);
            }
        }

        if (currentTask != null && currentTask.getNpc() != null) {
            if (currentTask.getProcessingStarted() != null) {
                long nSeconds = (new Date().getTime() - currentTask.getProcessingStarted().getTime()) / 1000L % 60L;
                if (nSeconds > 5) {
                    plugin.getMessagesManager().debugMessage(Level.FINEST, "CurrentTask to long, " + currentTask.getNpc().getId());
                    cleanTask();
                    currentTask = null;
                    return;
                }
            }

            if (currentTask.getNpc().getId() == npc.getId()) {
                return;
            }
            pathQueue.put(npc.getId(), pathFindingQueue);
            plugin.getMessagesManager().debugMessage(Level.FINEST, "QUEUED NPC: " + pathFindingQueue.getNpc().getId() + "|" + pathFindingQueue.getEndX() + "," + pathFindingQueue.getEndY() + "," + pathFindingQueue.getEndZ());
        } else {
            currentTask = pathFindingQueue;
            currentTask.setProcessingStarted(new Date());
            plugin.getMessagesManager().debugMessage(Level.FINEST, "CurrentTask Idle, Processing " + currentTask.getNpc().getId());
            processQueueItem();
        }
    }

    private void addToOpenList(Tile tile) {
        currentTask.getOpen().putIfAbsent(tile.getUID(), tile);

        if (playToPlayers != null && playToPlayers.size() > 0) {
            for (Player player : playToPlayers)
                plugin.getParticleManager().PlayOutHeartParticle(tile.getLocation(new Location(currentTask.getWorld(), currentTask.getStartX(), currentTask.getStartY(), currentTask.getStartZ()).add(0.5, 0, 0.5)), player);
        }
    }

    private void addToClosedList(Tile tile) {
        currentTask.getClosed().putIfAbsent(tile.getUID(), tile);
    }

    private void processQueueItem() {
        if (currentTask == null) return;
        if (currentTask.getNpcTrait() == null) {
            cleanTask();
            return;
        }

        if (plugin.getDebugLogLevel() == Level.FINEST)
            plugin.getMessagesManager().debugMessage(Level.FINEST, "NPC: " + currentTask.getNpc().getId() + "|" + Arrays.toString(Thread.currentThread().getStackTrace()));

        bInWater = false;

        // Start looking for a path on this NPC
        if (getStartLocation().getBlock().isLiquid() && plugin.getMcUtils().isLocationWalkable(getEndLocation(), currentTask.getOpensGates(), currentTask.getOpensWoodDoors(), currentTask.getOpensMetalDoors())) {
            bInWater = true;
        } else {
            if (!plugin.getMcUtils().isLocationWalkable(getStartLocation(), currentTask.getOpensGates(), currentTask.getOpensWoodDoors(), currentTask.getOpensMetalDoors())) {
                for (byte y = -1; y <= 1; y++) {
                    for (byte x = -1; x <= 1; x++) {
                        for (byte z = -1; z <= 1; z++) {

                            if (plugin.getMcUtils().isLocationWalkable(getStartLocation().add(x, y, z), currentTask.getOpensGates(), currentTask.getOpensWoodDoors(), currentTask.getOpensMetalDoors())) {
                                // start here?
                                currentTask.addStartXYZ(x,y,z);
                            }
                        }
                    }
                }
            }

            if (!plugin.getMcUtils().isLocationWalkable(getEndLocation(), currentTask.getOpensGates(), currentTask.getOpensWoodDoors(), currentTask.getOpensMetalDoors())) {
                if (!getEndLocation().getBlock().getType().isSolid()) currentTask.subtractEndXYZ(0, 1, 0);
            }

            if ((abs(currentTask.getStartX() - currentTask.getEndX()) > currentTask.getRange()) || (abs(currentTask.getStartY() - currentTask.getEndY()) > currentTask.getRange()) || (abs(currentTask.getStartZ() - currentTask.getEndZ()) > currentTask.getRange())) {
                plugin.getMessagesManager().sendDebugMessage("destinations", "debug_messages.path_outofrange", currentTask.getNpc(), currentTask.getNpcTrait());
                plugin.getMessagesManager().debugMessage(Level.INFO, "NPCDestinations_astar.ProcessQueueItem().FailedPath|NPC:" + currentTask.getNpc().getId() + "|Requested: " + currentTask.getRequestedBy());
                currentTask.getNpcTrait().lastResult = "Unable to find a path";
                currentTask.getNpcTrait().setCurrentAction(en_CurrentAction.IDLE_FAILED);
                currentTask.getNpcTrait().setLastPathCalc();
                currentTask.getNpcTrait().setLocationLockUntil(LocalDateTime.now().plusSeconds(10));
                cleanTask();
                return;// jump out
            }

            if (!plugin.getMcUtils().isLocationWalkable(getStartLocation(), currentTask.getOpensGates(), currentTask.getOpensWoodDoors(), currentTask.getOpensMetalDoors()) && !plugin.getMcUtils().isLocationWalkable(getEndLocation(), currentTask.getOpensGates(), currentTask.getOpensWoodDoors(), currentTask.getOpensMetalDoors())) {
                plugin.getMessagesManager().sendDebugMessage("destinations", "debug_messages.path_badendloc", currentTask.getNpc(), currentTask.getNpcTrait(), "S/E Fail [" + currentTask.getEndX() + "," + currentTask.getEndY() + "," + currentTask.getEndZ() + "]");
                plugin.getMessagesManager().debugMessage(Level.INFO, "NPCDestinations_astar.ProcessQueueItem().FailedPath|NPC:" + currentTask.getNpc().getId() + "|Requested: " + currentTask.getRequestedBy());
                currentTask.getNpcTrait().lastResult = "Start/End location is not walkable";
                currentTask.getNpcTrait().setCurrentAction(en_CurrentAction.IDLE_FAILED);
                currentTask.getNpcTrait().setLastPathCalc();
                currentTask.getNpcTrait().setLocationLockUntil(LocalDateTime.now().plusSeconds(10));

                // 1.6 Teleport the NPC as the start is wacked.
                currentTask.getNpc().teleport(getEndLocation().add(0, 1, 0), TeleportCause.PLUGIN);
                currentTask.getNpcTrait().locationReached();
                cleanTask();
                return;
            } else if (plugin.getMcUtils().isLocationWalkable(getStartLocation(), currentTask.getOpensGates(), currentTask.getOpensWoodDoors(), currentTask.getOpensMetalDoors()) && !plugin.getMcUtils().isLocationWalkable(getEndLocation(), currentTask.getOpensGates(), currentTask.getOpensWoodDoors(), currentTask.getOpensMetalDoors())) {
                // Cannot move the NPC at all.
                plugin.getMessagesManager().sendDebugMessage("destinations", "debug_messages.path_badendloc", currentTask.getNpc(), currentTask.getNpcTrait(), "E Fail [" + currentTask.getEndX() + "," + currentTask.getEndY() + "," + currentTask.getEndZ() + "]");
                plugin.getMessagesManager().debugMessage(Level.INFO, "NPCDestinations_astar.ProcessQueueItem().FailedPath|NPC:" + currentTask.getNpc().getId() + "|Start/End No Walk|Requested: " + currentTask.getRequestedBy());

                currentTask.getNpcTrait().lastResult = "End location is not walkable";
                currentTask.getNpcTrait().setCurrentAction(en_CurrentAction.IDLE_FAILED);
                currentTask.getNpcTrait().setLastPathCalc();
                currentTask.getNpcTrait().setLocationLockUntil(LocalDateTime.now().plusSeconds(10));
                cleanTask();
                return;
            }

            // 1.6: current task null, check to see if other tasks exist.
            if (pathQueue.size() > 0 && currentTask == null) {
                plugin.getServer().getScheduler().runTask(plugin, this::nextQueue);
                return;
            } else if (currentTask == null) {
                return;
            }

            // 1.6 Ensure they are on a walkable block
            if (currentTask.getAllowedPathBlocks().size() > 0 && !currentTask.getAllowedPathBlocks().contains(currentTask.getPathLocation(getStartLocation()).getBlock().getType())) {
                // remove the list of blocks to ensure that the NPC can walk home.
                currentTask.getAllowedPathBlocks().clear();
            }
        }

        currentTask.getNpcTrait().setLastPathCalc();

        short sh = 0;
        Tile tile = new Tile(sh, sh, sh, null);
        tile.calculateBoth(currentTask.getStartX(), currentTask.getStartY(), currentTask.getStartZ(), currentTask.getEndX(), currentTask.getEndY(), currentTask.getEndZ(), true);
        currentTask.getOpen().put(tile.getUID(), tile);
        processAdjacentTiles(tile);
        iterate();
    }

    private Location getEndLocation() {
        Location endLoc = new Location(currentTask.getWorld(), currentTask.getEndX(), currentTask.getEndY(), currentTask.getEndZ());
        return findSurface(endLoc);
    }

    private Location getStartLocation() {
        Location startLoc = new Location(currentTask.getWorld(), currentTask.getStartX(), currentTask.getStartY(), currentTask.getStartZ());
        return findSurface(startLoc);
    }

    private Location findSurface(Location location) {
        if (location.getBlock().getType().isSolid() && !location.clone().add(0, 1, 0).getBlock().getType().isSolid())
            return location;

        if (!location.getBlock().getType().isSolid() && location.clone().add(0, -1, 0).getBlock().getType().isSolid())
            return location.clone().add(0, -1, 0);

        for (int y = 0; y <= 3; y++) {
            if (location.clone().add(0, -y, 0).getBlock().getType().isSolid() && !location.clone().add(0, (-y) + 1, 0).getBlock().getType().isSolid())
                return location.clone().add(0, -y, 0);
        }

        return location;
    }

    private int abs(int i) {
        return (i < 0 ? -i : i);
    }

    private void iterate() {
        // while not at end
        Tile current = null;
        int nRepCount = 0;
        Long startTime = new Date().getTime();

        if (currentTask == null) return;
        if (currentTask.getNpc() == null) {
            cleanTask();
            return;
        }

        if (!currentTask.getNpc().isSpawned()) {
            cleanTask();
            return;
        }

        if (currentTask.getNpcTrait() == null) {
            cleanTask();
            return;
        }

        plugin.getMessagesManager().debugMessage(Level.FINEST, "NPCID:" + currentTask.getNpc().getId());

        NPCDestinationsTrait trait = currentTask.getNpc().getTrait(NPCDestinationsTrait.class);

        Integer maxSeek = plugin.getConfig().getInt("seek-time", 10);
        if (currentTask.getNpcTrait().maxProcessingTime > 0) maxSeek = currentTask.getNpcTrait().maxProcessingTime;

        while (canContinue()) {
            if (currentTask == null) return;
            if (currentTask.getNpcTrait() == null) {
                cleanTask();
                return;
            }

            if (currentTask.getNpcTrait().getLastPathCalc() == null) {
                plugin.getMessagesManager().debugMessage(Level.FINEST, "NPCID:" + currentTask.getNpc().getId() + "|No last path calc time");
                cleanTask();
                return;
            }

            if (plugin.getDebugTargets() != null && currentTask.getTimeSpent() == 0L) {
                currentTask.setTimeSpent(1L);
                currentTask.setProcessingStarted(new Date());
                trait.processingStarted = LocalDateTime.now();
                plugin.getMessagesManager().sendDebugMessage("destinations", "debug_messages.path_processing", currentTask.getNpc(), currentTask.getNpcTrait());
            }

            // get lowest F cost square on open list
            current = getLowestFTile();

            // process tiles
            processAdjacentTiles(current);

            // How long has this been running, to long, lets exit out.
            long nSeconds = Duration.ofMillis(currentTask.getTimeSpent() + (new Date().getTime() - startTime)).getSeconds();

            if (nSeconds > maxSeek) {
                // Kill the search, to long.
                trait.lastProcessingTime += (new Date().getTime() - startTime) / 1000 % 60;
                trait.totalProcessedBlocks += currentTask.getBlocksProcessed();
                trait.totalProcessingTime += (currentTask.getTimeSpent() + (new Date().getTime() - startTime));

                currentTask.setPathFindingResult(PathingResult.NO_PATH);
                trait.lastResult = "Unable to find a path";
                trait.setCurrentAction(en_CurrentAction.IDLE_FAILED);
                trait.setLastPathCalc();
                trait.setLocationLockUntil(LocalDateTime.now().plusSeconds(10));

                plugin.getMessagesManager().sendDebugMessage("destinations", "debug_messages.path_timeout", currentTask.getNpc(), trait);
                plugin.getMessagesManager().debugMessage(Level.INFO, "NPCDestinations_astar.ProcessQueueItem().FailedPath|NPC:" + currentTask.getNpc().getId() + "|Timeout|Requested: " + currentTask.getRequestedBy());

                cleanTask();
                return;
            }

            nRepCount++;
            if (nRepCount > 50) {
                currentTask.setTimeSpent(currentTask.getTimeSpent() + (new Date().getTime() - startTime));
                pathQueue.put(currentTask.getNpc().getId(), currentTask);
                Double distance = (new Location(currentTask.getWorld(), currentTask.getStartX(), currentTask.getStartY(), currentTask.getStartZ())).distance(new Location(currentTask.getWorld(), current.getX(), current.getY(), current.getZ()));
                plugin.getMessagesManager().debugMessage(Level.FINEST, "NPCID:" + currentTask.getNpc().getId() + "|Block limit reached, re-adding task to queue (Distance: " + distance + ")");
                currentTask = null;
                last_Pause = new Date().getTime() + 200;

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        nextQueue();
                    }
                }.runTaskLater(plugin, 2);
                return;
            }
        }

        if (currentTask == null) return;

        if (currentTask.getPathFindingResult() != PathingResult.SUCCESS || current == null) {
            trait.lastProcessingTime += (new Date().getTime() - startTime) / 1000 % 60;

            plugin.getMessagesManager().sendDebugMessage("destinations", "debug_messages.path_novalidpath", currentTask.getNpc(), currentTask.getNpcTrait());
            plugin.getMessagesManager().debugMessage(Level.FINEST, "NPCDestinations_astar.ProcessQueueItem().FailedPath|NPC:" + currentTask.getNpc().getId() + "|Timeout/Len|Requested: " + currentTask.getRequestedBy());

            trait.lastResult = "Unable to find a path";
            trait.setCurrentAction(en_CurrentAction.IDLE_FAILED);
            trait.setLocationLockUntil(LocalDateTime.now().plusSeconds(10));
            trait.setLastPathCalc();
            cleanTask();
            return;
        } else {
            // path found
            LinkedList<Tile> routeTrace = new LinkedList<Tile>();
            Tile parent;

            routeTrace.add(current);

            while ((parent = current.getParent()) != null) {
                routeTrace.add(parent);
                current = parent;
            }

            Collections.reverse(routeTrace);

            ArrayList<Location> locationArray = new ArrayList<Location>();
            for (Tile tLoc : routeTrace) {
                locationArray.add(tLoc.getLocation(getStartLocation()));
            }

            trait.setPendingDestinations(locationArray);
            long nSeconds = (currentTask.getTimeSpent() + (new Date().getTime() - startTime)) / 1000 % 60;
            trait.lastProcessingTime = nSeconds;
            trait.totalProcessedBlocks += currentTask.getBlocksProcessed();
            trait.totalProcessingTime += (currentTask.getTimeSpent() + (new Date().getTime() - startTime));

            if (nSeconds < 1 || currentTask.getBlocksProcessed() == 0L) {
                trait.lastBlocksPerSec = currentTask.getBlocksProcessed();
            } else {
                trait.lastBlocksPerSec = currentTask.getBlocksProcessed() / nSeconds;
            }

            trait.lastResult = "Path found (" + routeTrace.size() + ")";
            if (!trait.getCurrentAction().equals(en_CurrentAction.RANDOM_MOVEMENT))
                trait.setCurrentAction(en_CurrentAction.PATH_FOUND);
            trait.setLastPathCalc();
            plugin.getMessagesManager().debugMessage(Level.INFO, "astarpath.iterate()|NPC:" + currentTask.getNpc().getId() + "|Path Found (" + locationArray.size() + ")|Requested: " + currentTask.getRequestedBy());
            plugin.getMessagesManager().sendDebugMessage("destinations", "debug_messages.path_found", currentTask.getNpc(), currentTask.getNpcTrait());

            if (plugin.getDebugTargets().size() > 0) {
                final ArrayList<Location> debugTrace = (ArrayList<Location>) locationArray.clone();
                for (DebugTarget debugOutput : plugin.getDebugTargets()) {
                    if (debugOutput.getTargets().size() == 0 || debugOutput.getTargets().contains(currentTask.getNpc().getId())) {
                        if (((Player) debugOutput.targetSender).isOnline()) {
                            Player player = ((Player) debugOutput.targetSender);
                            for (int count = 1; count < (debugTrace.size() - 1); count++) {
                                if (player.getWorld().equals(debugTrace.get(count).getWorld())) {
                                    debugOutput.addDebugBlockSent(debugTrace.get(count), Material.REDSTONE_BLOCK);
                                }
                            }
                            debugOutput.addDebugBlockSent(debugTrace.get(0), Material.GOLD_BLOCK);
                            debugOutput.addDebugBlockSent(debugTrace.get(debugTrace.size() - 1), Material.DIAMOND_BLOCK);
                        }
                    }
                }
            }

            for (Tile tLoc : routeTrace) {
                tLoc.destroy();
            }
            routeTrace.clear();

            cleanTask();
        }

        if (pathQueue.size() > 0) {
            nextQueue();
        }
    }

    private boolean canContinue() {

        if (currentTask == null) return false;

        if (currentTask.getNpc() == null) {
            cleanTask();
            return false;
        }

        if (!currentTask.getNpc().isSpawned()) {
            cleanTask();
            return false;
        }

        if (currentTask.getNpcTrait() == null) {
            cleanTask();
            return false;
        }

        // check if open list is empty, if it is no path has been found
        if (currentTask.getOpen().size() == 0) {
            currentTask.setPathFindingResult(PathingResult.NO_PATH);
            return false;
        } else {
            if (currentTask != null) {
                StringBuilder b = new StringBuilder();
                b.append(currentTask.getEndX() - currentTask.getStartX()).append(currentTask.getEndY() - currentTask.getStartY()).append(currentTask.getEndZ() - currentTask.getStartZ());
                if (currentTask.getClosed().containsKey(b.toString())) {
                    currentTask.setPathFindingResult(PathingResult.SUCCESS);
                    return false;
                } else {
                    b = new StringBuilder();
                    b.append(currentTask.getEndX() - currentTask.getStartX()).append((currentTask.getEndY() + 1) - currentTask.getStartY()).append(currentTask.getEndZ() - currentTask.getStartZ());
                    if (currentTask.getClosed().containsKey(b.toString())) {
                        currentTask.setPathFindingResult(PathingResult.SUCCESS);
                        return false;
                    } else {
                        return true;
                    }
                }
            } else {
                return false;
            }
        }
    }

    private Tile getLowestFTile() {
        double f = 0;
        Tile drop = null;

        // get lowest F cost square
        for (Tile t : currentTask.getOpen().values()) {
            if (f == 0) {
                t.calculateBoth(currentTask.getStartX(), currentTask.getStartY(), currentTask.getStartZ(), currentTask.getEndX(), currentTask.getEndY(), currentTask.getEndZ(), true);
                f = t.getF();
                drop = t;
            } else {
                t.calculateBoth(currentTask.getStartX(), currentTask.getStartY(), currentTask.getStartZ(), currentTask.getEndX(), currentTask.getEndY(), currentTask.getEndZ(), true);
                double posF = t.getF();
                if (posF < f) {
                    f = posF;
                    drop = t;
                }
            }
        }

        // drop from open list and add to closed

        currentTask.getOpen().remove(drop.getUID());
        addToClosedList(drop);

        return drop;
    }

    private boolean isOnClosedList(Tile tile) {
        return currentTask.getClosed().containsKey(tile.getUID());
    }

    // pass in the current tile as the parent
    private void processAdjacentTiles(Tile current) {

        // set of possible walk to locations adjacent to current tile
        HashSet<Tile> possible = new HashSet<>(26);

        currentTask.setBlocksProcessed(currentTask.getBlocksProcessed() + 1);

        for (byte x = -1; x <= 1; x++) {
            for (byte y = -1; y <= 1; y++) {
                for (byte z = -1; z <= 1; z++) {

                    if (x == 0 && y == 0 && z == 0) {
                        continue;// don't check current square
                    }

                    Tile tile = new Tile((short) (current.getX() + x), (short) (current.getY() + y), (short) (current.getZ() + z), current);
                    Location location = new Location(currentTask.getWorld(), (currentTask.getStartX() + tile.getX()), (currentTask.getStartY() + tile.getY()), (currentTask.getStartZ() + tile.getZ()));

                    //Validate the current tile has 3 spaces open above.
                    if (y == 1) {
                        if (location.clone().add(0, 2, 0).getBlock().getType().isSolid()) continue;
                    }
                    if (y == -1) {
                        if (location.clone().add(x, 2, z).getBlock().getType().isSolid()) continue;
                    }

                    Block b = location.getBlock();
                    if (currentTask.getAllowedPathBlocks().size() > 0) {
                        location = new Location(currentTask.getWorld(), (currentTask.getStartX() + tile.getX()), (currentTask.getStartY() + tile.getY()), (currentTask.getStartZ() + tile.getZ()));

                        b = location.getBlock();
                        if (bInWater && b.isLiquid()) {
                            // anything?
                        } else {
                            // Validate the block types
                            if (currentTask.getBlocksBelow() != 0) {
                                if (!currentTask.getAllowedPathBlocks().contains(currentTask.getPathLocation(location).getBlock().getType())) {
                                    continue;
                                }
                            } else if (!currentTask.getAllowedPathBlocks().contains(b.getType())) {
                                continue;
                            }
                        }
                    }

                    if (!tile.isInRange(currentTask.getRange())) {
                        // if block is out of bounds continue
                        continue;
                    }

                    if (!bInWater && b.isLiquid()) {
                        continue;
                    }

                    if (x != 0 && z != 0 && (y == 0 || y == 1)) {
                        // check to stop jumping through diagonal blocks
                        Tile xOff = new Tile((short) (current.getX() + x), (short) (current.getY() + y), current.getZ(), current);
                        Tile zOff = new Tile(current.getX(), (short) (current.getY() + y), (short) (current.getZ() + z), current);
                        if (!isTileWalkable(xOff) && !isTileWalkable(zOff)) {
                            continue;
                        }
                    }
                    if (x != 0 && z != 0 && (y == 0 || y == -1)) {
                        // check to stop jumping through diagonal blocks
                        Tile xOff = new Tile((short) (current.getX() + x), (short) (current.getY() + y), current.getZ(), current);
                        Tile zOff = new Tile(current.getX(), (short) (current.getY() + y), (short) (current.getZ() + z), current);
                        if (!isTileWalkable(xOff) && !isTileWalkable(zOff)) {
                            continue;
                        }
                    }

                    //Openables?
                    if (plugin.getMcUtils().isOpenable(b) || plugin.getMcUtils().isOpenable(b.getLocation().add(0, 1, 0).getBlock()))
                        if (x != 0 && z != 0) continue;

                    if (isOnClosedList(tile)) {
                        // ignore tile
                        continue;
                    }

                    // only process the tile if it can be walked on
                    if (!isTileWalkable(tile)) {
                        continue;
                    }

                    tile.calculateBoth(currentTask.getStartX(), currentTask.getStartY(), currentTask.getStartZ(), currentTask.getEndX(), currentTask.getEndY(), currentTask.getEndZ(), true);
                    possible.add(tile);
                }
            }
        }

        for (Tile t : possible) {
            // get the reference of the object in the array
            Tile openRef = null;
            if ((openRef = isOnOpenList(t)) == null) {
                // not on open list, so add
                addToOpenList(t);
            } else {
                // is on open list, check if path to that square is better using
                // G cost
                if (t.getG() < openRef.getG()) {
                    // if current path is better, change parent
                    openRef.setParent(current);
                    // force updates of F, G and H values.
                    openRef.calculateBoth(currentTask.getStartX(), currentTask.getStartY(), currentTask.getStartZ(), currentTask.getEndX(), currentTask.getEndY(), currentTask.getEndZ(), true);
                }

            }
        }

    }

    private Tile isOnOpenList(Tile tile) {
        return currentTask.getOpen().getOrDefault(tile.getUID(), null);
    }

    private boolean isTileWalkable(Tile tile) {
        return isTileWalkable(tile, true);
    }

    private boolean isTileWalkable(Tile t, Boolean allowDoors) {
        Location l = new Location(currentTask.getWorld(), (currentTask.getStartX() + t.getX()), (currentTask.getStartY() + t.getY()), (currentTask.getStartZ() + t.getZ()));
        if (!plugin.getMcUtils().isLocationWalkable(l, currentTask.getOpensGates(), currentTask.getOpensWoodDoors(), currentTask.getOpensMetalDoors()))
            return false;

        Block b = l.getBlock();

        if (b.getRelative(0, 1, 0).getState().getData() instanceof Openable) {
            org.bukkit.block.BlockState oBlockState = b.getRelative(0, 1, 0).getState();

            if (plugin.getMcUtils().isGate(b.getRelative(0, 1, 0).getType())) {
                if (!allowDoors) return false;
                if (currentTask.getOpensGates()) {
                    return true;
                } else {
                    Openable oOpenable = (Openable) oBlockState.getData();
                    return (oOpenable.isOpen() && (!b.getRelative(0, 2, 0).getType().isSolid()));
                }
            } else if (plugin.getMcUtils().isWoodDoor(b.getRelative(0, 1, 0).getType())) {
                if (!allowDoors) return false;
                if (currentTask.getOpensWoodDoors()) {
                    return true;
                } else {
                    Openable oOpenable = (Openable) oBlockState.getData();
                    return (oOpenable.isOpen() && (!b.getRelative(0, 2, 0).getType().isSolid()));
                }
            } else if (plugin.getMcUtils().isMetalDoor(b.getRelative(0, 1, 0).getType())) {
                if (!allowDoors) return false;
                if (currentTask.getOpensMetalDoors()) {
                    return true;
                } else {
                    Openable oOpenable = (Openable) oBlockState.getData();
                    return (oOpenable.isOpen() && (!b.getRelative(0, 2, 0).getType().isSolid()));
                }
            }
        }

        return true;
    }

    private void nextQueue() {
        if (currentTask != null) return;

        if (pathQueue == null) return;

        if (last_Pause > new Date().getTime()) return;

        if (pathQueue.size() > 0) {
            Entry<Integer, PathFindingQueue> entryItem = pathQueue.entrySet().iterator().next();
            currentTask = entryItem.getValue();
            currentTask.getNpcTrait().setCurrentAction(en_CurrentAction.PATH_HUNTING);
            pathQueue.remove(entryItem.getKey());
            processQueueItem();
        }
    }

    private void cleanTask() {
        if (plugin.getDebugLogLevel() == Level.FINEST)
            plugin.getMessagesManager().debugMessage(Level.FINEST, Arrays.toString(Thread.currentThread().getStackTrace()));

        if (currentTask.getOpen() != null) {
            for (Tile t : currentTask.getOpen().values()) {
                t.destroy();
            }
            currentTask.getOpen().clear();
        }

        if (currentTask.getClosed() != null) {
            for (Tile t : currentTask.getClosed().values()) {
                t.destroy();
            }
            currentTask.getClosed().clear();
        }
        currentTask = null;
    }
}