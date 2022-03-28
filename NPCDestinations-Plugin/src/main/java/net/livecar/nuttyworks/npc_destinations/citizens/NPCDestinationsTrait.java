package net.livecar.nuttyworks.npc_destinations.citizens;

import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import net.livecar.nuttyworks.npc_destinations.DestinationsPlugin;
import net.livecar.nuttyworks.npc_destinations.api.DestinationSetting;
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
    public int PauseForPlayers = 5;
    @Persist
    public int PauseTimeout = 25;
    // @Persist public Boolean LookOneBlockDown = false;
    @Persist
    public int blocksUnderSurface = 0;
    // Format X:Y:Z:TIMEOFDAY -Old, will convert this to new format on loading
    @Persist
    public List<String> Locations = new ArrayList<String>();
    @Persist
    public Boolean OpensGates = false;
    @Persist
    public Boolean OpensWoodDoors = false;
    @Persist
    public Boolean OpensMetalDoors = false;
    @Persist
    public Boolean TeleportOnFailedStartLoc = true;
    @Persist
    public Boolean TeleportOnNoPath = true;
    @Persist
    public int MaxDistFromDestination = 2;

    public enum en_CurrentAction {
        RANDOM_MOVEMENT, PATH_HUNTING, PATH_FOUND, TRAVELING, IDLE, IDLE_FAILED,
    }

    public enum en_RequestedAction {
        NORMAL_PROCESSING, NO_PROCESSING, SET_LOCATION,
    }

    public List<DestinationSetting> NPCLocations = new ArrayList<>();
    public String lastResult = "Idle";
    public List<Material> AllowedPathBlocks = new ArrayList<>();
    public LocalDateTime lastPositionChange;
    public LocalDateTime lastPlayerPause;
    public Location lastPauseLocation;
    public Location lastNavigationPoint;
    public DestinationSetting currentLocation = new DestinationSetting();
    public DestinationSetting setLocation = new DestinationSetting();
    public DestinationSetting lastLocation = new DestinationSetting();
    public DestinationSetting monitoredLocation = null;

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

    public String lastDebugMessage = "";

    // Inner namespace variables
    ArrayList<Location> pendingDestinations = new ArrayList<>();
    ArrayList<Location> processedDestinations = new ArrayList<>();
    ArrayList<Block> openedObjects = new ArrayList<Block>();
    en_CurrentAction currentAction = en_CurrentAction.IDLE;
    en_RequestedAction requestedAction = en_RequestedAction.NORMAL_PROCESSING;
    Plugin monitoringPlugin = null;
    LocalDateTime timeLastPathCalc;
    LocalDateTime locationLockUntil;

    Boolean runningDoor = false;
    Block lastOpenedObject = null;

    UUID last_Loc_Reached;
    int requestedPauseTime;

    // Public methods
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
        load(new net.citizensnpcs.api.util.MemoryDataKey());
    }

    public Plugin getMonitoringPlugin() {
        return monitoringPlugin;
    }

    public void unsetMonitoringPlugin(String reason) {
        if (DestinationsPlugin.getInstance().getDebugTargets() != null) {
            if (monitoringPlugin != null)
                DestinationsPlugin.getInstance().getMessageManager.sendDebugMessage("destinations", "Debug_Messages.trait_unmonitored", npc, monitoringPlugin.getName() + (reason.equals("") ? "" : "(" + reason + ")"));
        }
        monitoringPlugin = null;
        monitoredLocation = null;
    }

    public void unsetMonitoringPlugin() {
        unsetMonitoringPlugin("");
    }

    public void setMonitoringPlugin(Plugin plugin, DestinationSetting monitoredDestination) {
        monitoringPlugin = plugin;
        monitoredLocation = monitoredDestination;
        if (monitoringPlugin != null)
            DestinationsPlugin.getInstance().getMessageManager.sendDebugMessage("destinations", "Debug_Messages.trait_monitored", npc, monitoringPlugin.getName());
    }

    public DestinationSetting GetCurrentLocation() {
        return GetCurrentLocation(false);
    }

    public DestinationSetting GetCurrentLocation(Boolean noNull) {
        return CitizensProcessing.traitGetCurrentLocation(this, noNull);
    }


    public LocalDateTime getLocationLockUntil() {
        return locationLockUntil;
    }

    public void setLocationLockUntil(LocalDateTime lockUntil) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-mm-dd hh:mm:ss");
        CitizensProcessing.debugMessage(Level.FINE, "NPC:" + this.npc.getId() + "|" + (lockUntil == null ? "Clear" : dateFormat.format(lockUntil)) + "|" + Arrays.toString(Thread.currentThread().getStackTrace()));
        this.locationLockUntil = lockUntil;
    }

    public void setLocationLockUntil(int seconds) {
        this.requestedPauseTime = seconds;
    }

    public int getPendingLockSeconds() {
        return this.requestedPauseTime;
    }

    public void setRequestedAction(en_RequestedAction action) {
        CitizensProcessing.debugMessage(Level.FINE, "NPCDestinations_Trait.setRequestedAction()|NPC:" + this.npc.getId() + "|" + action.toString());
        this.requestedAction = action;
    }

    public void setPendingDestinations(ArrayList<Location> newDestinations) {
        if (pendingDestinations.size() > 0) {
            clearPendingDestinations();
            this.processedDestinations.clear();
        }
        pendingDestinations = newDestinations;
    }

    public ArrayList<Location> getPendingDestinations() {
        return pendingDestinations;
    }

    public void removePendingDestination(int index) {
        CitizensProcessing.traitRemovePendingDestination(this, index);
        if (this.pendingDestinations.size() > index) {
            this.processedDestinations.add(this.pendingDestinations.get(index));
            this.pendingDestinations.remove(index);
        }
    }

    public void clearPendingDestinations() {
        CitizensProcessing.traitClearPendingDestinations(this);
        this.pendingDestinations.clear();
        this.processedDestinations.clear();
    }

    @Override
    public void load(DataKey key) {
        CitizensProcessing.traitLoadSettings(this, key);
    }

    @Override
    public void save(DataKey key) {
        CitizensProcessing.traitSaveSettings(this, key);
    }

    public en_RequestedAction getRequestedAction() {
        return this.requestedAction;
    }

    public void setCurrentAction(en_CurrentAction action) {
        CitizensProcessing.debugMessage(Level.FINE, "NPCDestinations_Trait.setCurrentAction()|NPC:" + this.npc.getId() + "|" + action.toString() + Arrays.toString(Thread.currentThread().getStackTrace()));
        this.currentAction = action;
    }

    public en_CurrentAction getCurrentAction() {
        return this.currentAction;
    }

    public void locationReached() {
        CitizensProcessing.traitLocationReached(this);
    }

    public void setCurrentLocation(DestinationSetting location) {
        if (this.currentLocation.destination == null) {
            if (location.destination.distanceSquared(this.npc.getEntity().getLocation()) > 5) {
                this.currentLocation = location;
            } else {
                this.currentLocation = location;
                this.locationReached();
            }
        } else this.currentLocation = location;
    }

    public DestinationSetting getCurrentLocation() {
        if (this.currentLocation == null) return new DestinationSetting();
        return this.currentLocation;
    }

    public void processOpenableObjects() {
        for (Iterator<Block> iterator = openedObjects.iterator(); iterator.hasNext(); ) {
            Block opened = iterator.next();
            if (opened.getLocation().distanceSquared(this.npc.getEntity().getLocation()) > 4 || (this.pendingDestinations.size() == 0 && this.processedDestinations.size() == 0)) {
                closeOpenable(opened);
                iterator.remove();
            }
        }

        if (DestinationsPlugin.getInstance().getMCUtils.isOpenable(npc.getEntity().getLocation().getBlock())) {
            if (!openedObjects.contains(npc.getEntity().getLocation().getBlock())) {
                Block oBlock = npc.getEntity().getLocation().getBlock();
                if (DestinationsPlugin.getInstance().getMCUtils.isOpenable(oBlock.getRelative(0, -1, 0))) {
                    oBlock = oBlock.getRelative(0, -1, 0);
                } else if (DestinationsPlugin.getInstance().getMCUtils.isOpenable(oBlock.getRelative(0, 1, 0))) {
                    oBlock = oBlock.getRelative(0, 1, 0);
                }
                this.openOpenable(oBlock);
            }
        }
        getOpenableInFront();
    }

    private void closeOpenable(Block oBlock) {
        DestinationsPlugin.getInstance().getMCUtils.closeOpenable(oBlock);
    }

    private void openOpenable(Block oBlock) {
        if (DestinationsPlugin.getInstance().getMCUtils.isGate(oBlock.getType()) && OpensGates) {
            if (DestinationsPlugin.getInstance().getMCUtils.openOpenable(oBlock)) {
                this.openedObjects.add(oBlock);
            }
        } else if (DestinationsPlugin.getInstance().getMCUtils.isWoodDoor(oBlock.getType()) && OpensWoodDoors) {
            if (DestinationsPlugin.getInstance().getMCUtils.openOpenable(oBlock)) {
                this.openedObjects.add(oBlock);
            }
        } else if (DestinationsPlugin.getInstance().getMCUtils.isMetalDoor(oBlock.getType()) && OpensMetalDoors) {
            if (DestinationsPlugin.getInstance().getMCUtils.openOpenable(oBlock)) {
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
        if (DestinationsPlugin.getInstance().getMCUtils.openOpenable(npc.getEntity().getLocation().getBlock())) {
            if (DestinationsPlugin.getInstance().getMCUtils.openOpenable(npc.getEntity().getLocation().getBlock())) {
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

            if (DestinationsPlugin.getInstance().getMCUtils.isOpenable(openableBlock)) {
                if (!openedObjects.contains(openableBlock)) {
                    this.openOpenable(openableBlock);
                }
            }
        }
    }
}
