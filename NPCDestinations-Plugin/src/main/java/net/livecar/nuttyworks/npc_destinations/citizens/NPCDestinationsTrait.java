package net.livecar.nuttyworks.npc_destinations.citizens;

import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.MemoryDataKey;
import net.livecar.nuttyworks.npc_destinations.DestinationsPlugin;
import net.livecar.nuttyworks.npc_destinations.api.Destination;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

public class NPCDestinationsTrait extends Trait {
    @Persist
    public int pauseForPlayers = 5;
    @Persist
    public int pauseTimeout = 25;
    @Persist
    public int blocksUnderSurface = 0;
    @Persist
    public Boolean openGates = false;
    @Persist
    public Boolean openWoodDoors = false;
    @Persist
    public Boolean openMetalDoors = false;
    @Persist
    public Boolean teleportOnFailedToStartLocation = true;
    @Persist
    public Boolean teleportOnNoPath = true;
    @Persist
    public int maxDistanceFromDestination = 2;

    public enum CurrentAction {
        RANDOM_MOVEMENT, PATH_HUNTING, PATH_FOUND, TRAVELING, IDLE, IDLE_FAILED,
    }

    public enum RequestedAction {
        NORMAL_PROCESSING, NO_PROCESSING, SET_LOCATION,
    }

    public List<Destination> NPCLocations = new ArrayList<>();
    public String lastResult = "Idle";
    public List<Material> AllowedPathBlocks = new ArrayList<>();
    public LocalDateTime lastPositionChange;
    public LocalDateTime lastPlayerPause;
    public Location lastPauseLocation;
    public Location lastNavigationPoint;
    public Destination currentLocation = new Destination();
    public Destination setLocation = new Destination();
    public Destination lastLocation = new Destination();
    public Destination monitoredLocation = null;

    public List<String> enabledPlugins = new ArrayList<>();
    public Boolean citizens_Swim = true;
    public Boolean citizens_NewPathFinder = true;
    public Boolean citizens_AvoidWater = true;
    public Boolean citizens_DefaultStuck = true;
    public Double citizens_DistanceMargin = 1D;
    public Double citizens_PathDistanceMargin = 1D;

    public Location lastLighting_Loc = null;
    public LocalDateTime lastLighting_Time = null;
    public Integer lightTask = 0;

    public LocalDateTime processingStarted;
    public Long totalProcessingTime;
    public Long totalProcessedBlocks;
    public Long lastProcessingTime = 0L;
    public Long lastBlocksPerSec = 0L;

    public Integer maxProcessingTime = -1;

    // Inner namespace variables
    List<Location> pendingDestinations = new ArrayList<>();
    List<Location> processedDestinations = new ArrayList<>();
    List<Block> openedObjects = new ArrayList<>();
    CurrentAction currentAction = CurrentAction.IDLE;
    RequestedAction requestedAction = RequestedAction.NORMAL_PROCESSING;
    Plugin monitoringPlugin = null;
    LocalDateTime timeLastPathCalc;
    LocalDateTime locationLockUntil;

    Boolean runningDoor = false;
    Block lastOpenedObject = null;

    UUID last_Loc_Reached;
    int requestedPauseTime;

    public NPCDestinationsTrait() {
        super("npcdestinations");
        this.lastPositionChange = LocalDateTime.now();
        this.lastPlayerPause = LocalDateTime.now();
        this.timeLastPathCalc = LocalDateTime.now().minusYears(1);
        this.totalProcessedBlocks = 0L;
        this.totalProcessingTime = 0L;
        this.processingStarted = null;
    }

    @Override
    public void onAttach() {
        load(new MemoryDataKey());
    }

    @Override
    public void load(DataKey key) {
        DestinationsPlugin.getInstance().getCitizensProcessing().load(this, key);
    }

    public Plugin getMonitoringPlugin() {
        return monitoringPlugin;
    }

    public void unsetMonitoringPlugin(String reason) {
        if (DestinationsPlugin.getInstance().getDebugTargets() != null) {
            if (monitoringPlugin != null)
                DestinationsPlugin.getInstance().getMessagesManager().sendDebugMessage("destinations", "Debug_Messages.trait_unmonitored", npc, monitoringPlugin.getName() + (reason.equals("") ? "" : "(" + reason + ")"));
        }
        monitoringPlugin = null;
        monitoredLocation = null;
    }

    public void unsetMonitoringPlugin() {
        unsetMonitoringPlugin("");
    }

    public void setMonitoringPlugin(Plugin plugin, Destination monitoredDestination) {
        monitoringPlugin = plugin;
        monitoredLocation = monitoredDestination;
        if (monitoringPlugin != null)
            DestinationsPlugin.getInstance().getMessagesManager().sendDebugMessage("destinations", "Debug_Messages.trait_monitored", npc, monitoringPlugin.getName());
    }

    public Destination GetCurrentLocation() {
        return GetCurrentLocation(false);
    }

    public Destination GetCurrentLocation(Boolean noNull) {
        return DestinationsPlugin.getInstance().getCitizensProcessing().getCurrentLocation(this, noNull);
    }

    public Destination getCurrentLocation() {
        if (this.currentLocation == null) return new Destination();
        return this.currentLocation;
    }

    public LocalDateTime getLocationLockUntil() {
        return locationLockUntil;
    }

    public void setLocationLockUntil(LocalDateTime lockUntil) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-mm-dd hh:mm:ss");
        DestinationsPlugin.getInstance().getMessagesManager().debugMessage(Level.FINE, "NPC:" + this.npc.getId() + "|" + (lockUntil == null ? "Clear" : dateFormat.format(lockUntil)) + "|" + Arrays.toString(Thread.currentThread().getStackTrace()));
        this.locationLockUntil = lockUntil;
    }

    public void setLocationLockUntil(int seconds) {
        this.requestedPauseTime = seconds;
    }

    public int getPendingLockSeconds() {
        return this.requestedPauseTime;
    }

    public void setRequestedAction(RequestedAction action) {
        DestinationsPlugin.getInstance().getMessagesManager().debugMessage(Level.FINE, "NPCDestinations_Trait.setRequestedAction()|NPC:" + this.npc.getId() + "|" + action.toString());
        this.requestedAction = action;
    }

    public void setPendingDestinations(List<Location> newDestinations) {
        if (pendingDestinations.size() > 0) {
            clearPendingDestinations();
            this.processedDestinations.clear();
        }
        pendingDestinations = newDestinations;
    }

    public List<Location> getPendingDestinations() {
        return pendingDestinations;
    }

    public void removePendingDestination(int index) {
        DestinationsPlugin.getInstance().getCitizensProcessing().removePendingDestination(this, index);
        if (this.pendingDestinations.size() > index) {
            this.processedDestinations.add(this.pendingDestinations.get(index));
            this.pendingDestinations.remove(index);
        }
    }

    public void clearPendingDestinations() {
        DestinationsPlugin.getInstance().getCitizensProcessing().clearPendingDestinations(this);
        this.pendingDestinations.clear();
        this.processedDestinations.clear();
    }

    @Override
    public void save(DataKey key) {
        DestinationsPlugin.getInstance().getCitizensProcessing().save(this, key);
    }

    public RequestedAction getRequestedAction() {
        return this.requestedAction;
    }

    public void setCurrentAction(CurrentAction action) {
        DestinationsPlugin.getInstance().getMessagesManager().debugMessage(Level.FINE, "NPCDestinations_Trait.setCurrentAction()|NPC:" + this.npc.getId() + "|" + action.toString() + Arrays.toString(Thread.currentThread().getStackTrace()));
        this.currentAction = action;
    }

    public CurrentAction getCurrentAction() {
        return this.currentAction;
    }

    public void locationReached() {
        DestinationsPlugin.getInstance().getCitizensProcessing().locationReached(this);
    }

    public void setCurrentLocation(Destination location) {
        if (this.currentLocation.location == null) {
            if (location.location.distanceSquared(this.npc.getEntity().getLocation()) > 5) {
                this.currentLocation = location;
            } else {
                this.currentLocation = location;
                this.locationReached();
            }
        } else this.currentLocation = location;
    }

    public void processOpenableObjects() {
        for (Iterator<Block> iterator = openedObjects.iterator(); iterator.hasNext(); ) {
            Block opened = iterator.next();
            if (opened.getLocation().distanceSquared(this.npc.getEntity().getLocation()) > 4 || (this.pendingDestinations.size() == 0 && this.processedDestinations.size() == 0)) {
                closeOpenable(opened);
                iterator.remove();
            }
        }

        if (DestinationsPlugin.getInstance().getMcUtils().isOpenable(npc.getEntity().getLocation().getBlock())) {
            if (!openedObjects.contains(npc.getEntity().getLocation().getBlock())) {
                Block oBlock = npc.getEntity().getLocation().getBlock();
                if (DestinationsPlugin.getInstance().getMcUtils().isOpenable(oBlock.getRelative(0, -1, 0))) {
                    oBlock = oBlock.getRelative(0, -1, 0);
                } else if (DestinationsPlugin.getInstance().getMcUtils().isOpenable(oBlock.getRelative(0, 1, 0))) {
                    oBlock = oBlock.getRelative(0, 1, 0);
                }
                this.openOpenable(oBlock);
            }
        }
        getOpenableInFront();
    }

    private void closeOpenable(Block oBlock) {
        DestinationsPlugin.getInstance().getMcUtils().closeOpenable(oBlock);
    }

    private void openOpenable(Block oBlock) {
        if (DestinationsPlugin.getInstance().getMcUtils().isGate(oBlock.getType()) && openGates) {
            if (DestinationsPlugin.getInstance().getMcUtils().openOpenable(oBlock)) {
                this.openedObjects.add(oBlock);
            }
        } else if (DestinationsPlugin.getInstance().getMcUtils().isWoodDoor(oBlock.getType()) && openWoodDoors) {
            if (DestinationsPlugin.getInstance().getMcUtils().openOpenable(oBlock)) {
                this.openedObjects.add(oBlock);
            }
        } else if (DestinationsPlugin.getInstance().getMcUtils().isMetalDoor(oBlock.getType()) && openMetalDoors) {
            if (DestinationsPlugin.getInstance().getMcUtils().openOpenable(oBlock)) {
                this.openedObjects.add(oBlock);
            }
        }
    }

    public LocalDateTime getLastPathCalc() {
        return this.timeLastPathCalc;
    }

    public void setLastPathCalc() {
        this.timeLastPathCalc = LocalDateTime.now();
    }

    private void getOpenableInFront() {
        // Validate is the NPC is in the same block as an openable
        if (DestinationsPlugin.getInstance().getMcUtils().openOpenable(npc.getEntity().getLocation().getBlock())) {
            if (DestinationsPlugin.getInstance().getMcUtils().openOpenable(npc.getEntity().getLocation().getBlock())) {
                this.openedObjects.add(npc.getEntity().getLocation().getBlock());
                return;
            }
        }

        int xAxis = 0;
        int zAxis = 0;

        double rotation = this.npc.getEntity().getLocation().getYaw();

        // North: -Z
        // East: +X
        // South: +Z
        // West: -X

        if (rotation < 30.0) {
            xAxis = 0;
            zAxis = 1;
        } else if (rotation < 60) {
            xAxis = -1;
            zAxis = 1;
        } else if (rotation < 120) {
            xAxis = -1;
            zAxis = 0;
        } else if (rotation < 150) {
            xAxis = -1;
            zAxis = -1;
        } else if (rotation < 210) {
            xAxis = 0;
            zAxis = -1;
        } else if (rotation < 240) {
            xAxis = 1;
            zAxis = -1;
        } else if (rotation < 300) {
            xAxis = 1;
            zAxis = 0;
        } else if (rotation < 330) {
            xAxis = 1;
            zAxis = 1;
        } else {
            xAxis = 0;
            zAxis = 1;
        }

        for (byte y = -1; y <= 1; y++) {
            final Location openableLocation = this.npc.getEntity().getLocation().add(xAxis, y, zAxis);
            final Block openableBlock = openableLocation.getBlock();

            if (DestinationsPlugin.getInstance().getMcUtils().isOpenable(openableBlock)) {
                if (!openedObjects.contains(openableBlock)) {
                    this.openOpenable(openableBlock);
                }
            }
        }
    }
}
