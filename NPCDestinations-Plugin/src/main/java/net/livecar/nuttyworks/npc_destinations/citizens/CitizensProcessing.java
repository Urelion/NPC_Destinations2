package net.livecar.nuttyworks.npc_destinations.citizens;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.TargetType;
import net.citizensnpcs.api.ai.TeleportStuckAction;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.npc.skin.Skin;
import net.citizensnpcs.npc.skin.SkinnableEntity;
import net.citizensnpcs.trait.SkinTrait;
import net.livecar.nuttyworks.npc_destinations.DebugTarget;
import net.livecar.nuttyworks.npc_destinations.DestinationsPlugin;
import net.livecar.nuttyworks.npc_destinations.api.Destination;
import net.livecar.nuttyworks.npc_destinations.api.NavigationNewDestination;
import net.livecar.nuttyworks.npc_destinations.api.NavigationReached;
import net.livecar.nuttyworks.npc_destinations.api.enumerations.TriBoolean;
import net.livecar.nuttyworks.npc_destinations.bridges.MCUtilsBridge.inHandLightSource;
import net.livecar.nuttyworks.npc_destinations.citizens.NPCDestinationsTrait.CurrentAction;
import net.livecar.nuttyworks.npc_destinations.citizens.NPCDestinationsTrait.RequestedAction;
import net.livecar.nuttyworks.npc_destinations.plugins.DestinationsAddon;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

public class CitizensProcessing {

    private final DestinationsPlugin plugin;

    public CitizensProcessing(DestinationsPlugin plugin) {
        this.plugin = plugin;
    }

    // Called by CitizensGoal
    public boolean shouldExecute(NPC npc, CitizensGoal citizensGoal) {
        if (plugin.getAStarPathFinder() == null) return false;
        if (!npc.isSpawned()) return false;

        if (!npc.hasTrait(NPCDestinationsTrait.class)) {
            Bukkit.getLogger().log(Level.INFO, "NPC [" + npc.getId() + "/" + npc.getName() + "] has not been setup to use the NPCDestinations path provider");
            npc.despawn(DespawnReason.PLUGIN);
            return false;
        }

        if (plugin.getAStarPathFinder().getPathQueue().containsKey(npc.getId())) return false;

        NPCDestinationsTrait trait = npc.getTraitNullable(NPCDestinationsTrait.class);

        // Timeout the pathfinding because it's taking too long or stalled
        if ((trait.getCurrentAction() == CurrentAction.PATH_HUNTING) && (trait.getPendingDestinations().size() == 0)) {
            if (trait.getLastPathCalc() != null) {
                long nSeconds = Duration.between(trait.getLastPathCalc(), LocalDateTime.now()).getSeconds();
                if (nSeconds > plugin.getConfig().getInt("seek-time", 15)) {
                    plugin.getMessagesManager().debugMessage(Level.FINE, "NPCDestinations_Goal.shouldExecute()|NPC:" + npc.getId() + "|Timeout>Path Failed");
                    trait.setCurrentAction(CurrentAction.IDLE_FAILED);
                    trait.setLastPathCalc();
                }
            } else {
                plugin.getMessagesManager().debugMessage(Level.FINE, "NPCDestinations_Goal.shouldExecute()|NPC:" + npc.getId() + "|Path Failed/IDLE");
                trait.setCurrentAction(CurrentAction.IDLE_FAILED);
            }
            return false;
        }

        if ((trait.getCurrentAction() == CurrentAction.IDLE || trait.getCurrentAction() == CurrentAction.IDLE_FAILED) && (trait.getPendingDestinations().size() == 0)) {
            if (trait.getLastPathCalc() != null) {
                long nSeconds = Duration.between(trait.getLastPathCalc(), LocalDateTime.now()).getSeconds();
                if (nSeconds < 3) {
                    return false;
                }
            }
        }

        // Lighting V1.19 - Check for torches in either hand
        Equipment equipment = trait.getNPC().getTraitNullable(Equipment.class);
        if (plugin.getLightAPIPlugin() != null) {
            boolean startLightTask = false;
            if (equipment.get(EquipmentSlot.HAND) != null && (plugin.getMcUtils().isHoldingTorch(equipment.get(EquipmentSlot.HAND).getType()) != inHandLightSource.NOLIGHT)) {
                startLightTask = true;
                plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|Lighting");

            }
            if (equipment.get(EquipmentSlot.OFF_HAND) != null && (plugin.getMcUtils().isHoldingTorch(equipment.get(EquipmentSlot.OFF_HAND).getType()) != inHandLightSource.NOLIGHT)) {
                plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|Lighting-1_10");
                startLightTask = true;
            }

            if (startLightTask) {
                if (trait.lightTask < 1) {
                    final int npcID = trait.getNPC().getId();
                    trait.lightTask = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> updateLighting(npcID), 10);
                }
            }
        }


        if (trait.getCurrentAction() != CurrentAction.IDLE) {
            // Check the area near the NPC for players. Pause if so
            int traitDistance;
            int traitPause;
            if (trait.currentLocation != null && trait.currentLocation.pauseDistance > -1) {
                if (trait.currentLocation.pauseDistance > trait.pauseForPlayers) {
                    traitDistance = trait.currentLocation.pauseDistance * trait.currentLocation.pauseDistance;
                    traitPause = trait.currentLocation.pauseTimeout;

                    switch (trait.getCurrentAction()) {
                        case RANDOM_MOVEMENT:
                            if (!trait.currentLocation.pauseType.equalsIgnoreCase("ALL") && !trait.currentLocation.pauseType.equalsIgnoreCase("WANDERING")) {
                                traitDistance = 0;
                                traitPause = 0;
                            }
                            break;
                        case PATH_FOUND:
                        case TRAVELING:
                            if (!trait.currentLocation.pauseType.equalsIgnoreCase("ALL") && !trait.currentLocation.pauseType.equalsIgnoreCase("TRAVELING")) {
                                traitDistance = 0;
                                traitPause = 0;
                            }
                            break;
                        default:
                            break;
                    }
                } else {
                    traitDistance = trait.pauseForPlayers * trait.pauseForPlayers;
                    traitPause = trait.pauseTimeout;
                }
            } else {
                traitDistance = trait.pauseForPlayers * trait.pauseForPlayers;
                traitPause = trait.pauseTimeout;
            }
            if (traitPause == -1) traitPause = Integer.MAX_VALUE;

            if (trait.lastPauseLocation == null || trait.lastPauseLocation.distanceSquared(npc.getEntity().getLocation()) > traitDistance) {
                for (Player plrEntity : Bukkit.getOnlinePlayers()) {

                    if ((plrEntity.getWorld() == npc.getEntity().getWorld()) && (plrEntity.getLocation().distanceSquared(npc.getEntity().getLocation()) < traitDistance)) {
                        if (trait.lastPlayerPause == null) trait.lastPlayerPause = LocalDateTime.now();

                        if (trait.lastPlayerPause.isBefore(LocalDateTime.now().plusSeconds(traitPause))) {
                            trait.lastPauseLocation = npc.getEntity().getLocation().clone();
                            trait.lastPlayerPause = null;
                            break;
                        }

                        trait.lastResult = ("Paused for player " + plrEntity.getDisplayName());
                        trait.lastPauseLocation = null;
                        return false;
                    }
                }
            }
        }

        // Validate if the item on the list to open is something we can, and open it
        if (npc.getNavigator().getTargetAsLocation() != null && npc.getNavigator().getTargetType() == TargetType.LOCATION) {
            trait.processOpenableObjects();
            // Check if the NPC is stuck and sitting in one place. if so, recalculate the path to the end.
            if ((npc.getNavigator().isNavigating() & npc.getNavigator().getTargetAsLocation().distanceSquared(npc.getEntity().getLocation()) > 3.0D)) {
                long nSeconds = Duration.between(trait.lastPositionChange, LocalDateTime.now()).getSeconds();
                if (nSeconds > 10L) {
                    npc.getNavigator().cancelNavigation();
                    trait.clearPendingDestinations();
                    trait.lastResult = "Stalled on path, recalc";
                    trait.lastPositionChange = LocalDateTime.now();
                    trait.setCurrentAction(CurrentAction.IDLE_FAILED);
                    plugin.getMessagesManager().debugMessage(Level.FINE, "NPCDestinations_Goal.shouldExecute()|NPC:" + npc.getId() + "|NPC_Stuck>Path Failed");
                    return false;
                }
                return false;
            }
        }

        double speed = npc.getEntity().getVelocity().lengthSquared();
        if (trait.getPendingDestinations().size() > 0 && trait.lastNavigationPoint != null) {
            double distTarget = npc.getEntity().getLocation().distanceSquared(trait.lastNavigationPoint);
            if (distTarget > 1 && speed > 0.00615) return false;
        }

        if (npc.getNavigator().isNavigating()) {
            if (npc.getEntity().getLocation().distanceSquared(npc.getNavigator().getTargetAsLocation()) > 1)
                return false;
        }

        // Do we have any pending destinations to walk to?
        if (trait.getPendingDestinations().size() > 0) {
            Location oLastDest;
            Destination oCurDest = trait.GetCurrentLocation();
            if (!trait.getCurrentAction().equals(CurrentAction.RANDOM_MOVEMENT) && oCurDest != null) {
                oLastDest = trait.getPendingDestinations().get(trait.getPendingDestinations().size() - 1);

                if (trait.getMonitoringPlugin() == null && ((oLastDest.getBlockX() != oCurDest.location.getBlockX()) || (oLastDest.getBlockZ() != oCurDest.location.getBlockZ()))) {
                    npc.getNavigator().cancelNavigation();
                    trait.clearPendingDestinations();
                    trait.setCurrentAction(CurrentAction.IDLE);
                    trait.lastResult = "Destination changed, recalc";
                    trait.lastPositionChange = LocalDateTime.now();
                    if (plugin.getDebugTargets() != null)
                        plugin.getMessagesManager().sendDebugMessage("destinations", "Debug_Messages.goal_newdestination", npc, trait);
                    plugin.getMessagesManager().debugMessage(Level.FINE, "NPCDestinations_Goal.shouldExecute()|NPC:" + npc.getId() + "|NewDestination>IDLE");
                    return false;
                }
            }

            //trait.processOpenableObjects();
            plugin.getMessagesManager().debugMessage(Level.FINEST, "NPCDestinations_Goal.shouldExecute()|NPC:" + npc.getId() + "|NewTarget: " + trait.getPendingDestinations().get(0).toString());

            if (oCurDest != null) {
                // V 2.1.1 - Allow for citizen settings per destination to help
                // with pathing issues.
                switch (oCurDest.citizensAvoidWater) {
                    case FALSE:
                        npc.getNavigator().getLocalParameters().avoidWater(false);
                        break;
                    case NOT_SET:
                        npc.getNavigator().getLocalParameters().avoidWater(trait.citizens_AvoidWater);
                        break;
                    case TRUE:
                        npc.getNavigator().getLocalParameters().avoidWater(true);
                        break;

                }

                switch (oCurDest.citizensSwim) {
                    case FALSE:
                        npc.data().set("swim", false);
                        break;
                    case NOT_SET:
                        npc.data().set("swim", trait.citizens_Swim);
                        break;
                    case TRUE:
                        npc.data().set("swim", true);
                        break;
                }

                switch (oCurDest.citizensDefaultStuck) {
                    case FALSE:
                    case NOT_SET:
                        npc.getNavigator().getLocalParameters().stuckAction(trait.citizens_DefaultStuck ? TeleportStuckAction.INSTANCE : null);
                        break;
                    case TRUE:
                        npc.getNavigator().getLocalParameters().stuckAction(TeleportStuckAction.INSTANCE);
                        break;
                }

                npc.getNavigator().getLocalParameters().distanceMargin(oCurDest.citizensDistanceMargin < 0D ? trait.citizens_DistanceMargin : oCurDest.citizensDistanceMargin);
                npc.getNavigator().getLocalParameters().pathDistanceMargin(oCurDest.citizensPathDistanceMargin < 0D ? trait.citizens_PathDistanceMargin : oCurDest.citizensPathDistanceMargin);
            } else {
                npc.data().set("swim", trait.citizens_Swim);

                npc.getNavigator().getLocalParameters().avoidWater(trait.citizens_AvoidWater);
                npc.getNavigator().getLocalParameters().useNewPathfinder(trait.citizens_NewPathFinder);
                npc.getNavigator().getLocalParameters().stuckAction(trait.citizens_DefaultStuck ? TeleportStuckAction.INSTANCE : null);

                npc.getNavigator().getLocalParameters().distanceMargin(trait.citizens_DistanceMargin);
                npc.getNavigator().getLocalParameters().pathDistanceMargin(trait.citizens_PathDistanceMargin);
            }

            for (DebugTarget debugOutput : plugin.getDebugTargets()) {
                if (debugOutput.getTargets().size() == 0 || debugOutput.getTargets().contains(npc.getId())) {
                    Player debugTarget = (Player) debugOutput.targetSender;
                    if (debugTarget.isOnline()) {
                        debugOutput.removeDebugBlockSent(trait.getPendingDestinations().get(0));
                    }
                }
            }

            Location tmpLocation = trait.getPendingDestinations().get(0).clone();
            if (plugin.getAStarPathFinder().requiresOpening(tmpLocation) || plugin.getAStarPathFinder().requiresOpening(tmpLocation.clone().add(0, 1, 0))) {
                if (trait.runningDoor) return false;

                if (npc.getEntity().getLocation().distanceSquared(tmpLocation.clone().add(0, 1, 0)) > 2) {
                    Vector v = tmpLocation.subtract(npc.getEntity().getLocation().clone()).toVector().normalize();
                    npc.getEntity().setVelocity(v.multiply(0.5));
                    return false;
                } else {
                    net.citizensnpcs.util.Util.faceLocation(npc.getEntity(), tmpLocation.clone().add(0, 1, 0));
                }

                //Runable to process the door/gate correctly
                trait.runningDoor = true;
                if (plugin.getAStarPathFinder().requiresOpening(tmpLocation))
                    trait.lastOpenedObject = tmpLocation.clone().add(0, 1, 0).getBlock();
                else if (plugin.getAStarPathFinder().requiresOpening(tmpLocation.clone().add(0, 1, 0)))
                    trait.lastOpenedObject = tmpLocation.clone().add(0, 2, 0).getBlock();

                net.citizensnpcs.util.Util.faceLocation(npc.getEntity(), tmpLocation.clone().add(0, 1, 0));

                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    this.processNPCThruOpenable(1, npc, trait);
                }, 1L);
                return false;
            }

            npc.getNavigator().getLocalParameters().useNewPathfinder(true);

            npc.getNavigator().getLocalParameters().avoidWater(true);
            npc.getNavigator().getLocalParameters().stuckAction(TeleportStuckAction.INSTANCE);
            npc.getNavigator().getLocalParameters().distanceMargin(0.5D);
            npc.getNavigator().getLocalParameters().pathDistanceMargin(0.5D);

            // Loop and validate the angle from the NPC to see how far we can go
            // on the next target, improve the goofy look all over issue
            Double pathAngle = Double.MAX_VALUE;
            int maxDist = 0;
            Location lastLocation = new Location(npc.getEntity().getWorld(), npc.getEntity().getLocation().getBlockX(), npc.getEntity().getLocation().getBlockY(), npc.getEntity().getLocation().getBlockZ());

            do {
                if (maxDist > trait.getPendingDestinations().size()) break;
                if (maxDist > 10) break;
                if (trait.getPendingDestinations().size() == 0) break;

                Vector locVect = lastLocation.toVector().subtract(trait.getPendingDestinations().get(0).toVector()).normalize();
                Vector npcVect = npc.getEntity().getLocation().getDirection();
                double angle = Math.acos(locVect.dot(npcVect));

                if (plugin.getAStarPathFinder().requiresOpening(trait.getPendingDestinations().get(0).clone())) break;

                if (maxDist >= 2 && (Math.toDegrees(angle) != Math.abs(pathAngle))) break;
                pathAngle = Math.toDegrees(angle);
                lastLocation = trait.getPendingDestinations().get(0).clone();
                trait.processedDestinations.add(trait.getPendingDestinations().get(0));
                trait.removePendingDestination(0);
                maxDist++;
            } while (true);

            plugin.getMessagesManager().debugMessage(Level.FINEST, "NPCDestinations_Goal.shouldExecute()|NPC:" + npc.getId() + "|Navigate: " + lastLocation);

            Location newLocation = new Location(npc.getEntity().getWorld(), lastLocation.getBlockX() + 0.5, lastLocation.getBlockY() + 0.0, lastLocation.getBlockZ() + 0.0);
            npc.getNavigator().setTarget(newLocation);
            trait.lastNavigationPoint = newLocation;
            trait.lastPositionChange = LocalDateTime.now();
            return false;
        }

        // Check if we have any locations to move to.
        Destination oLoc = trait.GetCurrentLocation();
        if (oLoc == null) {
            return false;
        }

        // is there a timeout lock?
        if (trait.getLocationLockUntil() == null) {
            Random random = new Random();
            if (trait.currentLocation.timeMinimum > 0) {
                // need to set the time out.
                int nWaitTime = random.nextInt((trait.currentLocation.timeMaximum - trait.currentLocation.timeMinimum) + 1) + trait.currentLocation.timeMinimum;
                trait.setLocationLockUntil(LocalDateTime.now().plusSeconds(nWaitTime));
                trait.setCurrentAction(CurrentAction.IDLE);
                if (plugin.getDebugTargets() != null) {
                    plugin.getMessagesManager().sendDebugMessage("destinations", "Debug_Messages.goal_timeddestination", npc, trait);
                }
            }
        }

        // Is this NPC being monitored by a plugin?
        if (trait.monitoredLocation != null && trait.monitoredLocation.locationUUID == oLoc.locationUUID) {
            // return and do nothing. Let the plugin manage this location
            return false;
        }

        // Main movement thread
        boolean processWander = false;
        if (oLoc.getWanderingDistance() > 0 && !npc.getNavigator().isNavigating() && npc.getEntity().getLocation().distanceSquared(oLoc.location) <= oLoc.getWanderingDistanceSquared())
            processWander = true;
        if (!oLoc.wanderingRegion.equals("") && !npc.getNavigator().isNavigating() && plugin.getWorldGuardPlugin() != null)
            processWander = true;

        if (processWander) {
            Random random = new Random();
            if (trait.getLocationLockUntil() == null && trait.getCurrentAction().equals(CurrentAction.RANDOM_MOVEMENT)) {
                int nWaitTime;
                if (trait.currentLocation.waitMaximum == 0 || trait.currentLocation.waitMinimum == 0) {
                    nWaitTime = 1;
                } else {
                    int nextInt = (trait.currentLocation.waitMaximum - trait.currentLocation.waitMinimum) + 1;
                    if (nextInt < 2) nWaitTime = 1;
                    else nWaitTime = random.nextInt(nextInt) + trait.currentLocation.waitMinimum;
                }

                trait.setLocationLockUntil(LocalDateTime.now().plusSeconds(nWaitTime));
                trait.setCurrentAction(CurrentAction.PATH_FOUND);
                plugin.getMessagesManager().debugMessage(Level.FINEST, "NPCDestinations_Goal.shouldExecute()|NPC:" + npc.getId() + "|SetLockedTime:" + trait.getLocationLockUntil());
                return true;
            } else if (!oLoc.wanderingRegion.equals("") && plugin.getWorldGuardPlugin() != null) {

                // Get the region based on the name.
                Location[] regionPoints = plugin.getWorldGuardPlugin().getRegionBounds(npc.getEntity().getWorld(), oLoc.wanderingRegion);
                if (regionPoints.length == 0) {
                    // bad region, do nothing.
                    return true;
                }
                trait.lastLocation = trait.currentLocation;
                int nTrys = 0;
                while (nTrys < 50) {
                    Location oNewDest = new Location(npc.getEntity().getLocation().getWorld(), regionPoints[0].getBlockX(), npc.getEntity().getLocation().getBlockY(), regionPoints[0].getBlockZ());
                    plugin.getMessagesManager().debugMessage(Level.FINEST, "NPCDestinations_Goal.shouldExecute()|NPC:" + npc.getId() + "|oNewDest.add(random.nextInt((int)" + (oLoc.getWanderingDistance() * 2) + "-" + oLoc.getWanderingDistance() + ", 0, random.nextInt((int)" + (oLoc.getWanderingDistance() * 2) + "-" + oLoc.getWanderingDistance() + ")");
                    oNewDest.add(random.nextInt(regionPoints[1].getBlockX() - regionPoints[0].getBlockX()), 0, random.nextInt(regionPoints[1].getBlockZ() - regionPoints[0].getBlockZ()));
                    for (byte y = -3; y <= 2; y++) {

                        if (plugin.getPlotSquaredPlugin() != null) {
                            if (!plugin.getPlotSquaredPlugin().locationInSamePlotAsNPC(npc, oNewDest)) continue;
                        }
                        int newY = trait.blocksUnderSurface == -1 ? -oNewDest.getBlockY() : trait.blocksUnderSurface > 0 ? y - trait.blocksUnderSurface : y;

                        if (plugin.getWorldGuardPlugin().isInRegion(oNewDest, oLoc.wanderingRegion)) {
                            if (plugin.getAStarPathFinder().isLocationWalkable(oNewDest.getBlock().getRelative(0, y, 0).getLocation(), trait.openGates, trait.openWoodDoors, trait.openMetalDoors)) {
                                if (oLoc.wanderingUseBlocks && trait.AllowedPathBlocks != null && trait.AllowedPathBlocks.size() > 0) {
                                    if (trait.AllowedPathBlocks.contains(oNewDest.getBlock().getRelative(0, newY, 0).getLocation().getBlock().getType())) {
                                        trait.lastPositionChange = LocalDateTime.now();
                                        trait.setCurrentAction(CurrentAction.RANDOM_MOVEMENT);
                                        trait.setLocationLockUntil(null);

                                        trait.setCurrentLocation(oLoc);
                                        plugin.getAStarPathFinder().addToQueue(npc, trait, npc.getEntity().getLocation(), oNewDest.getBlock().getRelative(0, y, 0).getLocation(), plugin.getMaxDistance(), trait.AllowedPathBlocks, trait.blocksUnderSurface, trait.openGates, trait.openWoodDoors, trait.openMetalDoors, "Destinations.Goal.Random");
                                        return true;
                                    }
                                } else {
                                    trait.lastPositionChange = LocalDateTime.now();
                                    trait.setCurrentAction(CurrentAction.RANDOM_MOVEMENT);
                                    trait.setLocationLockUntil(null);

                                    trait.setCurrentLocation(oLoc);

                                    plugin.getAStarPathFinder().addToQueue(npc, trait, npc.getEntity().getLocation(), oNewDest.getBlock().getRelative(0, y, 0).getLocation(), plugin.getMaxDistance(), new ArrayList<Material>(), 0, trait.openGates, trait.openWoodDoors, trait.openMetalDoors, "Destinations.Goal.Random");
                                }
                                return true;
                            }
                        }
                    }
                    nTrys++;
                }
            } else {
                // Continue the random movement
                trait.lastLocation = trait.currentLocation;
                int nTrys = 0;
                while (nTrys < 50) {
                    Location oNewDest = new Location(npc.getEntity().getLocation().getWorld(), npc.getEntity().getLocation().getBlockX(), npc.getEntity().getLocation().getBlockY(), npc.getEntity().getLocation().getBlockZ());
                    plugin.getMessagesManager().debugMessage(Level.FINEST, "NPCDestinations_Goal.shouldExecute()|NPC:" + npc.getId() + "|oNewDest.add(random.nextInt((int)" + (oLoc.getWanderingDistance() * 2) + "-" + oLoc.getWanderingDistance() + ", 0, random.nextInt((int)" + (oLoc.getWanderingDistance() * 2) + "-" + oLoc.getWanderingDistance() + ")");
                    oNewDest.add(random.nextInt((int) oLoc.getWanderingDistance() * 2) - oLoc.getWanderingDistance(), 0, random.nextInt((int) oLoc.getWanderingDistance() * 2) - oLoc.getWanderingDistance());
                    if (oLoc.location.distanceSquared(oNewDest) <= oLoc.getWanderingDistanceSquared()) {
                        for (int y = -3; y <= 2; y++) {

                            if (plugin.getPlotSquaredPlugin() != null) {
                                if (!plugin.getPlotSquaredPlugin().locationInSamePlotAsNPC(npc, oNewDest)) continue;
                            }
                            int newY = trait.blocksUnderSurface == -1 ? -oNewDest.getBlockY() : trait.blocksUnderSurface > 0 ? y - trait.blocksUnderSurface : y;

                            if (plugin.getAStarPathFinder().isLocationWalkable(oNewDest.getBlock().getRelative(0, y, 0).getLocation(), trait.openGates, trait.openWoodDoors, trait.openMetalDoors)) {
                                if (oLoc.wanderingUseBlocks && trait.AllowedPathBlocks != null && trait.AllowedPathBlocks.size() > 0) {
                                    if (trait.AllowedPathBlocks.contains(oNewDest.getBlock().getRelative(0, newY, 0).getLocation().getBlock().getType())) {
                                        trait.lastPositionChange = LocalDateTime.now();
                                        trait.setCurrentAction(CurrentAction.RANDOM_MOVEMENT);
                                        trait.setLocationLockUntil(null);

                                        trait.setCurrentLocation(oLoc);
                                        plugin.getAStarPathFinder().addToQueue(npc, trait, npc.getEntity().getLocation(), oNewDest.getBlock().getRelative(0, y, 0).getLocation(), plugin.getMaxDistance(), trait.AllowedPathBlocks, trait.blocksUnderSurface, trait.openGates, trait.openWoodDoors, trait.openMetalDoors, "Destinations.Goal.Random");
                                        return true;
                                    }
                                } else {
                                    trait.lastPositionChange = LocalDateTime.now();
                                    trait.setCurrentAction(CurrentAction.RANDOM_MOVEMENT);
                                    trait.setLocationLockUntil(null);

                                    trait.setCurrentLocation(oLoc);

                                    plugin.getAStarPathFinder().addToQueue(npc, trait, npc.getEntity().getLocation(), oNewDest.getBlock().getRelative(0, y, 0).getLocation(), plugin.getMaxDistance(), new ArrayList<Material>(), 0, trait.openGates, trait.openWoodDoors, trait.openMetalDoors, "Destinations.Goal.Random");
                                }
                                return true;
                            }
                        }
                    }
                    nTrys++;
                }
            }
        } else if (trait.getCurrentAction().equals(CurrentAction.RANDOM_MOVEMENT) && !npc.getNavigator().isNavigating()) {
            if (npc.getEntity().getLocation() != null && trait.currentLocation.location != null && trait.lastLocation.location != null) {
                if (npc.getEntity().getLocation().distanceSquared(trait.lastLocation.location) > 3 && trait.currentLocation.location != trait.lastLocation.location) {
                    plugin.getMessagesManager().debugMessage(Level.FINEST, "NPCDestinations_Goal.shouldExecute()|NPC:" + npc.getId() + "|RandomWander");
                    trait.lastPositionChange = LocalDateTime.now();
                    plugin.getAStarPathFinder().addToQueue(npc, trait, npc.getEntity().getLocation(), (trait.lastLocation != null ? trait.lastLocation.location : trait.currentLocation.location), plugin.getMaxDistance(), new ArrayList<Material>(), 0, trait.openGates, trait.openWoodDoors, trait.openMetalDoors, "Destinations.Goal.Random");
                } else {
                    trait.setCurrentAction(CurrentAction.IDLE);
                }
            } else {
                trait.setCurrentAction(CurrentAction.IDLE);
            }
            return false;
        } else if (trait.getCurrentAction().equals(CurrentAction.RANDOM_MOVEMENT) && npc.getNavigator().isNavigating()) {
            return false;
        }

        if (!npc.isSpawned() || oLoc.location == null) return false;

        double nDist = npc.getEntity().getLocation().distanceSquared(oLoc.location);
        if (nDist > oLoc.getMaxDistanceSquared()) {
            trait.setCurrentAction(CurrentAction.PATH_HUNTING);
            if ((trait.teleportOnNoPath) && (trait.lastResult.startsWith("unable to find a path"))) {
                citizensGoal.failedPathCount += 1;
                if (citizensGoal.failedPathCount > 2) {
                    trait.lastResult = "Failed to locate a valid path to destination";
                    citizensGoal.failedPathCount = 0;
                    teleportSurface(npc.getEntity(), oLoc.location.clone().add(0, 1, 0));
                    trait.locationReached();
                    if (plugin.getDebugTargets() != null)
                        plugin.getMessagesManager().sendDebugMessage("destinations", "Debug_Messages.path_novalidpath", npc, trait);
                    plugin.getMessagesManager().debugMessage(Level.FINEST, "NPCDestinations_Goal.shouldExecute()|NPC:" + npc.getId() + "|NoValidPath");
                    trait.setLastPathCalc();
                    return false;
                }
                trait.lastResult = "Failed to locate a valid path to destination";
            }
            int nCnt;
            if (npc.getEntity().getLocation().getBlock().isLiquid()) {
                nCnt = 1;
                for (; ; ) {
                    if ((npc.getEntity().getLocation().add(0.0D, nCnt, 0.0D).getBlock().isEmpty()) && (npc.getEntity().getLocation().add(0.0D, nCnt + 1, 0.0D).getBlock().isEmpty())) {
                        teleportSurface(npc.getEntity(), npc.getEntity().getLocation().add(0.5D, nCnt, 0.5D));
                        break;
                    }
                    nCnt++;
                }
            }
            trait.lastLocation = trait.currentLocation;

            if (trait.currentLocation == null) fireLocationChangedEvent(trait, oLoc);

            trait.setCurrentLocation(oLoc);

            // V1.33 - Skins (Change the skin if the new one has a skin set and
            // is set to false
            if (!oLoc.playerSkinName.isEmpty() && !oLoc.playerSkinApplyOnArrival && (npc.getEntity() instanceof Player) && !npc.getNavigator().isNavigating()) {

                plugin.getMessagesManager().debugMessage(Level.FINEST, "NPCDestinations_Goal.shouldExecute()|NPC:" + npc.getId() + "|SKINCOMPARE|\r\n" + npc.data().get(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_METADATA).toString() + "\r\n" + oLoc.playerSkinTextureMetadata);
                if (!npc.data().get(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_METADATA).toString().equals(oLoc.playerSkinTextureMetadata) && !npc.data().get(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_SIGN_METADATA).toString().equals(oLoc.playerSkinTextureSignature)) {
                    npc.data().remove(NPC.PLAYER_SKIN_UUID_METADATA);
                    npc.data().remove(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_METADATA);
                    npc.data().remove(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_SIGN_METADATA);
                    npc.data().remove("cached-skin-uuid-name");
                    npc.data().remove("cached-skin-uuid");
                    npc.data().remove(NPC.PLAYER_SKIN_UUID_METADATA);

                    // Set the skin
                    npc.data().set(NPC.PLAYER_SKIN_USE_LATEST, false);
                    npc.data().set("cached-skin-uuid-name", oLoc.playerSkinName);
                    npc.data().set("cached-skin-uuid", oLoc.playerSkinUUID);
                    npc.data().setPersistent(NPC.PLAYER_SKIN_UUID_METADATA, oLoc.playerSkinName);
                    npc.data().setPersistent(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_METADATA, oLoc.playerSkinTextureMetadata);
                    npc.data().setPersistent(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_SIGN_METADATA, oLoc.playerSkinTextureSignature);

                    if (npc.isSpawned()) {

                        SkinnableEntity skinnable = npc.getEntity() instanceof SkinnableEntity ? (SkinnableEntity) npc.getEntity() : null;
                        if (skinnable != null) {
                            Skin.get(skinnable).applyAndRespawn(skinnable);

                        }
                    }
                }
            }

            plugin.getMessagesManager().debugMessage(Level.FINEST, "NPCDestinations_Goal.shouldExecute()|NPC:" + npc.getId() + "|NewPathFinding: " + oLoc.location.toString());

            plugin.getAStarPathFinder().addToQueue(npc, trait, npc.getEntity().getLocation(), new Location(oLoc.location.getWorld(), oLoc.location.getBlockX(), oLoc.location.getBlockY(), oLoc.location.getBlockZ()).add(0.5D, 0, 0.5D), plugin.getMaxDistance(), trait.AllowedPathBlocks, trait.blocksUnderSurface, trait.openGates, trait.openWoodDoors, trait.openMetalDoors, "Destinations.Goal.Destination");

            trait.setCurrentAction(CurrentAction.TRAVELING);
        } else if (trait.getCurrentAction() == CurrentAction.PATH_FOUND && trait.getPendingDestinations().size() == 0) {
            // path ended
            plugin.getMessagesManager().debugMessage(Level.FINEST, "NPCDestinations_Goal.shouldExecute()|NPC:" + npc.getId() + "|PathEnded-found");
            teleportSurface(npc.getEntity(), trait.currentLocation.location.clone().add(0, 0, 0), TeleportCause.PLUGIN);
            trait.setCurrentAction(CurrentAction.IDLE);
            this.locationReached(trait);
            trait.lastLocation = trait.currentLocation;
            trait.setCurrentLocation(oLoc);

        } else if (trait.getCurrentAction() == CurrentAction.TRAVELING && trait.getPendingDestinations().size() == 0) {
            Vector vel = trait.getNPC().getEntity().getVelocity();
            if (Math.abs(vel.getX()) > 0.05 || Math.abs(vel.getY()) > 0.1 || Math.abs(vel.getZ()) > 0.05) {
                // Still moving
                return false;
            }
            // path ended
            trait.setCurrentAction(CurrentAction.IDLE);
            this.locationReached(trait);
            trait.lastLocation = trait.currentLocation;
            trait.setCurrentLocation(oLoc);
        }
        return false;
    }

    public Destination getCurrentLocation(NPCDestinationsTrait trait, boolean noNull) {
        // Locked locations
        if (trait.requestedAction == RequestedAction.SET_LOCATION && trait.getPendingLockSeconds() > 0) {
            plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|setLocation:" + trait.currentLocation.location.toString());
            if (trait.last_Loc_Reached == null) trait.last_Loc_Reached = trait.setLocation.locationUUID;
            return trait.setLocation;
        } else if (trait.requestedAction == RequestedAction.SET_LOCATION && trait.getLocationLockUntil() != null && LocalDateTime.now().isBefore(trait.getLocationLockUntil())) {
            plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|setLocation:" + trait.currentLocation.location.toString());
            if (trait.last_Loc_Reached == null) trait.last_Loc_Reached = trait.setLocation.locationUUID;
            return trait.setLocation;
        } else if (trait.requestedAction == RequestedAction.SET_LOCATION && trait.getLocationLockUntil() != null && LocalDateTime.now().isAfter(trait.getLocationLockUntil())) {
            trait.requestedAction = RequestedAction.NORMAL_PROCESSING;
            trait.setLocationLockUntil(null);
            trait.setLocation = null;
        } else if (trait.requestedAction == RequestedAction.SET_LOCATION && trait.getLocationLockUntil() == null) {
            trait.requestedAction = RequestedAction.NORMAL_PROCESSING;
        }

        if (trait.requestedAction == RequestedAction.NO_PROCESSING) {
            plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|NO_PROCESSING:" + trait.currentLocation.location.toString());
            return null;
        }

        // Random location support
        if (trait.getLocationLockUntil() != null && LocalDateTime.now().isBefore(trait.getLocationLockUntil()) && !noNull) {
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-mm-dd hh:mm:ss");
            if (trait.currentLocation.location == null) {
                plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|Locked Location > Null|Lock: " + dateFormat.format(trait.getLocationLockUntil()) + ">" + dateFormat.format(LocalDateTime.now()));
            } else {
                plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|Locked Location > Null|Lock: " + dateFormat.format(trait.getLocationLockUntil()) + ">" + dateFormat.format(LocalDateTime.now()) + trait.currentLocation.location.toString());
            }
            return null;
        }

        Destination oCurrentLoc = null;
        int nMinDiff = Integer.MAX_VALUE;
        int nTimeOfDay = ((Long) plugin.getTimeManager().getNPCTime(trait.getNPC())).intValue();

        // Plotsquared, time of plot!
        if (plugin.getPlotSquaredPlugin() != null) {
            nTimeOfDay = plugin.getPlotSquaredPlugin().getNPCPlotTime(trait.getNPC());
        }

        for (Destination oLoc : trait.NPCLocations) {
            if (oLoc.timeOfDay == -1) continue;

            int nDiff = Math.abs(oLoc.timeOfDay - nTimeOfDay);
            boolean pluginBlocked = false;
            // 1.29 - Check weather flags

            for (DestinationsAddon plugin : plugin.getPluginManager().getPlugins()) {
                if (trait.enabledPlugins.contains(plugin.getActionName())) {
                    try {
                        if (!plugin.isDestinationEnabled(trait.getNPC(), trait, oLoc)) {
                            pluginBlocked = true;
                            break;
                        }
                    } catch (Exception err) {
                        StringWriter sw = new StringWriter();
                        err.printStackTrace(new PrintWriter(sw));

                        this.plugin.getMessagesManager().consoleMessage(this.plugin, "destinations", "Console_Messages.plugin_debug", err.getMessage() + "\n" + sw);
                    }
                }
            }

            if (pluginBlocked) continue;

            if (oLoc.weatherFlag > 0) {
                if (!oLoc.location.getWorld().hasStorm() && oLoc.weatherFlag == 1) {
                    if (nMinDiff > nDiff && oLoc.timeOfDay <= nTimeOfDay) {
                        plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|Weather: Only Clear|" + oLoc.location.toString() + "|" + oLoc.location.getWorld().hasStorm());
                        nMinDiff = nDiff;
                        oCurrentLoc = oLoc;
                    }
                } else if (oLoc.location.getWorld().hasStorm() && oLoc.weatherFlag == 2) {
                    if (nMinDiff > nDiff && oLoc.timeOfDay <= nTimeOfDay) {
                        plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|Weather: Storming|" + oLoc.location.toString() + "|" + oLoc.location.getWorld().hasStorm());
                        nMinDiff = nDiff;
                        oCurrentLoc = oLoc;
                    }
                }
            } else if (nMinDiff > nDiff && oLoc.timeOfDay <= nTimeOfDay) {
                nMinDiff = nDiff;
                oCurrentLoc = oLoc;
            }
        }

        if (oCurrentLoc == null) {
            nMinDiff = 0;
            for (Destination oLoc : trait.NPCLocations) {
                boolean pluginBlocked = false;
                // 1.29 - Check weather flags

                for (DestinationsAddon plugin : plugin.getPluginManager().getPlugins()) {
                    if (trait.enabledPlugins.contains(plugin.getActionName().toUpperCase())) {
                        try {
                            if (!plugin.isDestinationEnabled(trait.getNPC(), trait, oLoc)) {
                                pluginBlocked = true;
                                break;
                            }
                        } catch (Exception err) {
                            StringWriter sw = new StringWriter();
                            err.printStackTrace(new PrintWriter(sw));

                        }
                    }
                }
                if (pluginBlocked) continue;

                if (oLoc.weatherFlag > 0) {
                    if (!oLoc.location.getWorld().hasStorm() && oLoc.weatherFlag == 1) {
                        if (oLoc.timeOfDay > nMinDiff) {
                            plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|Weather: Clear Only|" + oLoc.location.toString() + "|" + oLoc.location.getWorld().hasStorm());
                            nMinDiff = oLoc.timeOfDay;
                            oCurrentLoc = oLoc;
                        }
                    } else if (oLoc.location.getWorld().hasStorm() && oLoc.weatherFlag == 2) {
                        if (oLoc.timeOfDay > nMinDiff) {
                            plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|Weather: Storms Only|" + oLoc.location.toString() + "|" + oLoc.location.getWorld().hasStorm());
                            nMinDiff = oLoc.timeOfDay;
                            oCurrentLoc = oLoc;
                        }
                    }

                } else if (oLoc.timeOfDay > nMinDiff) {
                    nMinDiff = oLoc.timeOfDay;
                    oCurrentLoc = oLoc;
                }
            }
        }

        if (oCurrentLoc == null) {
            plugin.getMessagesManager().debugMessage(Level.FINEST, "NPC:" + trait.getNPC().getId() + "|curLocation: return null");
            return null;
        } else {
            if (trait.currentLocation != null && trait.currentLocation.timeOfDay == oCurrentLoc.timeOfDay && trait.pendingDestinations != null && trait.pendingDestinations.size() > 0) {
                plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|curLocation: CurLoc Time matches, return current");
                return trait.currentLocation;
            }

            // 1.6: Final check to see if we have a random situation
            // 1.29: updated to allow weather changes
            if (trait.currentLocation != null && trait.currentLocation.timeOfDay == oCurrentLoc.timeOfDay && trait.getLocationLockUntil() == null) {
                if (trait.currentLocation.location != null && oCurrentLoc.location.toString() == trait.currentLocation.location.toString()) {
                    plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|Weather: return current|" + trait.currentLocation.location.toString());
                    return trait.currentLocation;
                }
            }

            if (trait.getLocationLockUntil() != null && LocalDateTime.now().isAfter(trait.getLocationLockUntil()))
                trait.setLocationLockUntil(null);

            ArrayList<Destination> oTmpDests = new ArrayList<Destination>();
            boolean bHasPercent = false;
            for (Destination oLoc : trait.NPCLocations) {
                if (oLoc.timeOfDay == oCurrentLoc.timeOfDay) {
                    if (oLoc.probability > 0 && oLoc.probability != 100) {
                        bHasPercent = true;
                        plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|PercLocation:" + oLoc.location.toString());
                        oTmpDests.add(oLoc);
                    }
                }
            }

            if (oTmpDests.size() == 1) {
                NavigationReached navReached = new NavigationReached(trait.getNPC(), trait.currentLocation);
                Bukkit.getServer().getPluginManager().callEvent(navReached);

                if (fireLocationChangedEvent(trait, oTmpDests.get(0))) return trait.currentLocation;

                trait.setCurrentLocation(oTmpDests.get(0));
                plugin.getMessagesManager().debugMessage(Level.FINEST, "NPC:" + trait.getNPC().getId() + "|curLocation: single loc, returned");
                return oTmpDests.get(0);
            } else if (oTmpDests.size() > 1) {
                Random random = new Random();
                if (!bHasPercent) {
                    while (true) {
                        int nRnd = random.nextInt(oTmpDests.size());

                        oCurrentLoc = oTmpDests.get(nRnd);
                        // Does this destination have a max/min time to spend
                        // there?

                        if (trait.currentLocation != null && oCurrentLoc == trait.currentLocation) {
                            // Try again
                        } else {
                            if (fireLocationChangedEvent(trait, oCurrentLoc)) {
                                return trait.currentLocation;
                            }
                            trait.setCurrentLocation(oCurrentLoc);
                            break;
                        }
                    }
                } else {

                    int nCnt = 0;
                    int nPercent = random.nextInt(100);

                    for (Destination oLoc : oTmpDests) {
                        if (oLoc.probability > 0 && oLoc.probability != 100) {
                            nCnt += oLoc.probability;
                            if (nPercent <= nCnt) {
                                oCurrentLoc = oLoc;
                                if (fireLocationChangedEvent(trait, oCurrentLoc)) {
                                    return trait.currentLocation;
                                }
                                trait.setCurrentLocation(oCurrentLoc);
                                break;
                            }
                        }
                    }
                    // Get the first 0% one (really 100%)
                    for (Destination oLoc : oTmpDests) {
                        if (oLoc.probability == 0) {
                            oCurrentLoc = oLoc;
                            if (fireLocationChangedEvent(trait, oCurrentLoc)) {
                                return trait.currentLocation;
                            }
                            trait.setCurrentLocation(oCurrentLoc);
                            break;
                        }
                    }
                }
            }
        }

        if (trait.monitoredLocation != null && trait.monitoredLocation == oCurrentLoc) {
            plugin.getMessagesManager().debugMessage(Level.FINEST, "NPC:" + trait.getNPC().getId() + "|curLocation: Monitored Location, no change");
            return null;
        }

        plugin.getMessagesManager().debugMessage(Level.FINEST, "NPC:" + trait.getNPC().getId() + "|curLocation: default return");
        if (oCurrentLoc != trait.currentLocation) {
            if (fireLocationChangedEvent(trait, oCurrentLoc)) {
                return trait.currentLocation;
            }
        }
        trait.setCurrentLocation(oCurrentLoc);
        trait.monitoredLocation = null;
        return oCurrentLoc;
    }

    public void locationReached(NPCDestinationsTrait trait) {
        if (trait.currentLocation.location == null) {
            return;
        }

        if (trait.last_Loc_Reached != null && trait.last_Loc_Reached == trait.currentLocation.locationUUID) {
            plugin.getMessagesManager().debugMessage(Level.FINEST, "NPC:" + trait.getNPC().getId() + "|locationReached: last: " + trait.last_Loc_Reached.toString() + " CurLoc:" + trait.currentLocation.locationUUID.toString());
            return;
        }

        plugin.getMessagesManager().sendDebugMessage("destinations", "debug_messages.goal_reacheddestination", trait.getNPC(), trait);
        Location finalLoc = new Location(trait.currentLocation.location.getWorld(), trait.currentLocation.location.getBlockX() + 0.5D, trait.currentLocation.location.getBlockY(), trait.currentLocation.location.getBlockZ() + 0.5D, trait.currentLocation.location.getYaw(), trait.currentLocation.location.getPitch());
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> trait.getNPC().teleport(finalLoc, TeleportCause.PLUGIN), 1);

        // Notify all plugins that the location has been reached.
        boolean cancelProcessing = false;

        for (DestinationsAddon plugin : plugin.getPluginManager().getPlugins()) {
            if (trait.enabledPlugins.contains(plugin.getActionName().toUpperCase())) {
                try {
                    if (plugin.onNavigationReached(trait.getNPC(), trait, trait.currentLocation))
                        cancelProcessing = true;
                } catch (Exception err) {
                    StringWriter sw = new StringWriter();
                    err.printStackTrace(new PrintWriter(sw));
                    this.plugin.getMessagesManager().consoleMessage(this.plugin, "destinations", "console_Messages.plugin_error", err.getMessage() + "\n" + sw);
                }
            }
        }

        // Fire the navigation event
        NavigationReached navReached = new NavigationReached(trait.getNPC(), trait.currentLocation);
        Bukkit.getServer().getPluginManager().callEvent(navReached);
        if (navReached.isCancelled() || cancelProcessing) return;

        if (trait.getPendingLockSeconds() > 0) {
            trait.setLocationLockUntil(LocalDateTime.now().plusSeconds(trait.getPendingLockSeconds()));
            trait.setLocationLockUntil(0);
        }

        trait.last_Loc_Reached = trait.currentLocation.locationUUID;
        plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|Destinations_Trait.LocationReached:" + trait.currentLocation.location.toString());

        // 1.44 -- Process the commands in the command subset for this NPC
        for (String commandString : trait.currentLocation.arrivalCommands)
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), commandString);

        if (!(trait.getNPC().getEntity() instanceof Player)) {
            // Stuff below really does not work on other NPC's
            return;
        }

        Equipment locEquip = trait.getNPC().getTrait(Equipment.class);

        // Are we to clear the inventory?
        if (trait.currentLocation.itemsClear) {
            locEquip.set(EquipmentSlot.HELMET, null);
            locEquip.set(EquipmentSlot.CHESTPLATE, null);
            locEquip.set(EquipmentSlot.LEGGINGS, null);
            locEquip.set(EquipmentSlot.BOOTS, null);
            locEquip.set(EquipmentSlot.HAND, null);
            locEquip.set(EquipmentSlot.HELMET, null);

            locEquip.set(EquipmentSlot.OFF_HAND, null);
        }

        if (trait.currentLocation.itemsHead != null) {

            locEquip.set(EquipmentSlot.HELMET, trait.currentLocation.itemsHead);
        }
        if (trait.currentLocation.itemsChest != null)
            locEquip.set(EquipmentSlot.CHESTPLATE, trait.currentLocation.itemsChest);
        if (trait.currentLocation.itemsLegs != null)
            locEquip.set(EquipmentSlot.LEGGINGS, trait.currentLocation.itemsLegs);
        if (trait.currentLocation.itemsBoots != null)
            locEquip.set(EquipmentSlot.BOOTS, trait.currentLocation.itemsBoots);

        if (trait.currentLocation.itemsHand != null) locEquip.set(EquipmentSlot.HAND, trait.currentLocation.itemsHand);

        if (trait.currentLocation.itemsOffhand != null)
            locEquip.set(EquipmentSlot.OFF_HAND, trait.currentLocation.itemsOffhand);

        // Lighting V1.19 - Check for torches in either hand
        if (plugin.getLightAPIPlugin() != null) {
            boolean startLightTask = false;
            if (locEquip.get(EquipmentSlot.HAND) != null && (plugin.getMcUtils().isHoldingTorch(locEquip.get(EquipmentSlot.HAND).getType()) != inHandLightSource.NOLIGHT)) {
                startLightTask = true;
                plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|Lighting");

            }
            if (locEquip.get(EquipmentSlot.OFF_HAND) != null && (plugin.getMcUtils().isHoldingTorch(locEquip.get(EquipmentSlot.OFF_HAND).getType()) != inHandLightSource.NOLIGHT)) {
                plugin.getMessagesManager().debugMessage(Level.FINE, "NPC:" + trait.getNPC().getId() + "|Lighting-1_10");
                startLightTask = true;
            }

            if (startLightTask) {
                if (trait.lightTask < 1) {
                    final int npcID = trait.getNPC().getId();
                    trait.lightTask = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                        public void run() {
                            updateLighting(npcID);
                        }
                    }, 10);
                }
            }
        }

        // V1.33 - Skins
        if (!trait.currentLocation.playerSkinName.isEmpty() && trait.getNPC().getEntity() instanceof Player) {
            SkinTrait skinTrait = trait.getNPC().getOrAddTrait(SkinTrait.class);

            plugin.getMessagesManager().debugMessage(Level.FINEST, "NPCDestinations_Goal.shouldExecute()|NPC:" + trait.getNPC().getId() + "|SKINCOMPARE");

            if (!skinTrait.getSignature().equals(trait.currentLocation.playerSkinTextureSignature) || !skinTrait.getTexture().equals(trait.currentLocation.playerSkinTextureMetadata)) {

                skinTrait.setSkinPersistent(trait.currentLocation.playerSkinName, trait.currentLocation.playerSkinTextureSignature, trait.currentLocation.playerSkinTextureMetadata);
            }
        }
    }

    public void clearPendingDestinations(NPCDestinationsTrait trait) {
        for (Location pendDestination : trait.getPendingDestinations()) {
            for (DebugTarget debugOutput : plugin.getDebugTargets()) {
                if (debugOutput.getTargets().size() == 0 || debugOutput.getTargets().contains(trait.getNPC().getId())) {
                    if (((Player) debugOutput.targetSender).isOnline()) {
                        Player player = ((Player) debugOutput.targetSender);
                        if (player.getWorld().equals(pendDestination.getWorld())) {
                            this.plugin.getMcUtils().sendClientBlock(player, pendDestination, null);
                        }
                    }
                }
            }
        }
    }

    public void removePendingDestination(NPCDestinationsTrait trait, int index) {
        for (DebugTarget debugOutput : plugin.getDebugTargets()) {
            if (debugOutput.getTargets().size() == 0 || debugOutput.getTargets().contains(trait.getNPC().getId())) {
                if (((Player) debugOutput.targetSender).isOnline()) {
                    Player player = ((Player) debugOutput.targetSender);
                    if (trait.getPendingDestinations().size() >= index)
                        if (player.getWorld().equals(trait.getPendingDestinations().get(index).getWorld())) {
                            this.plugin.getMcUtils().sendClientBlock(player, trait.getPendingDestinations().get(index), null);
                        }
                }
            }
        }
    }

    private void updateLighting(final int npcID) {
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcID);
        if (npc == null) return;

        NPCDestinationsTrait trait = npc.getTrait(NPCDestinationsTrait.class);
        if (trait == null) return;

        trait.lightTask = 0;
        if (!trait.getNPC().isSpawned()) {
            plugin.getLightAPIPlugin().DeleteLight(trait.lastLighting_Loc);
            trait.lastLighting_Loc = null;
            trait.lastLighting_Time = null;
            return;
        }

        boolean startLightTask = false;
        int lightLevel = 0;
        Equipment locEquip = trait.getNPC().getTrait(Equipment.class);

        if (locEquip.get(EquipmentSlot.HAND) != null) {
            if (plugin.getMcUtils().isHoldingTorch(locEquip.get(EquipmentSlot.HAND).getType()) == inHandLightSource.REDSTONE_TORCH) {
                lightLevel = 7;
                startLightTask = true;
            } else if (plugin.getMcUtils().isHoldingTorch(locEquip.get(EquipmentSlot.HAND).getType()) == inHandLightSource.WOODEN_TORCH) {
                lightLevel = 14;
                startLightTask = true;
            }
        }
        if (locEquip.get(EquipmentSlot.OFF_HAND) != null) {
            if (plugin.getMcUtils().isHoldingTorch(locEquip.get(EquipmentSlot.OFF_HAND).getType()) == inHandLightSource.REDSTONE_TORCH) {
                lightLevel = 7;
                startLightTask = true;
            } else if (plugin.getMcUtils().isHoldingTorch(locEquip.get(EquipmentSlot.OFF_HAND).getType()) == inHandLightSource.WOODEN_TORCH) {
                lightLevel = 14;
                startLightTask = true;
            }
        }

        if (startLightTask) {
            if (trait.lastLighting_Loc != null && trait.lastLighting_Loc.distanceSquared(trait.getNPC().getEntity().getLocation()) > 5) {
                if (trait.lastLighting_Loc != null) plugin.getLightAPIPlugin().DeleteLight(trait.lastLighting_Loc);

                trait.lastLighting_Loc = trait.getNPC().getEntity().getLocation();
                trait.lastLighting_Time = LocalDateTime.now();
                plugin.getLightAPIPlugin().CreateLight(trait.lastLighting_Loc, lightLevel);
            } else if (trait.lastLighting_Loc != null && (Duration.between(trait.lastLighting_Time, LocalDateTime.now()).getSeconds() > 5)) {
                if (trait.lastLighting_Loc != null) plugin.getLightAPIPlugin().DeleteLight(trait.lastLighting_Loc);

                trait.lastLighting_Loc = trait.getNPC().getEntity().getLocation();
                trait.lastLighting_Time = LocalDateTime.now();
                plugin.getLightAPIPlugin().CreateLight(trait.lastLighting_Loc, lightLevel);
            } else if (trait.lastLighting_Loc == null) {
                trait.lastLighting_Loc = trait.getNPC().getEntity().getLocation();
                trait.lastLighting_Time = LocalDateTime.now();
                plugin.getLightAPIPlugin().CreateLight(trait.lastLighting_Loc, lightLevel);
            }

            trait.lightTask = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    updateLighting(npcID);
                }
            }, 5);
        } else if (trait.lastLighting_Loc != null) {
            plugin.getLightAPIPlugin().DeleteLight(trait.lastLighting_Loc);
            trait.lastLighting_Loc = null;
            trait.lastLighting_Time = null;
        }
    }

    public boolean fireLocationChangedEvent(NPCDestinationsTrait trait, Destination newDestination) {

        if (trait.currentLocation.locationUUID == null || newDestination.locationUUID == null) {
            return false;
        } else if (trait.currentLocation.locationUUID.equals(newDestination.locationUUID)) {
            return true;
        }

        // Notify all plugins that the location has been reached.
        boolean cancelProcessing = false;

        for (DestinationsAddon plugin : plugin.getPluginManager().getPlugins()) {
            if (trait.enabledPlugins.contains(plugin.getActionName().toUpperCase())) {
                try {
                    if (plugin.onNewDestination(trait.getNPC(), trait, newDestination)) cancelProcessing = true;
                } catch (Exception err) {
                    StringWriter sw = new StringWriter();
                    err.printStackTrace(new PrintWriter(sw));
                    this.plugin.getMessagesManager().consoleMessage(this.plugin, "destinations", "Console_Messages.plugin_error", err.getMessage() + "\n" + sw);
                }
            }
        }

        // Fire the navigation event
        NavigationNewDestination changeEvent = new NavigationNewDestination(trait.getNPC(), newDestination, false);
        Bukkit.getServer().getPluginManager().callEvent(changeEvent);
        if (changeEvent.isCancelled() || cancelProcessing) {
            trait.lastPauseLocation = null;
            trait.lastPlayerPause = null;
            return true;
        }
        return false;
    }

    private void processNPCThruOpenable(int lastOpenedStep, NPC npc, NPCDestinationsTrait trait) {
        if (npc.getEntity().getVelocity().lengthSquared() > 0.015) {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> this.processNPCThruOpenable(lastOpenedStep, npc, trait), 5L);
            return;
        }

        switch (lastOpenedStep) {
            case 1:
                // We need to look at the door
                net.citizensnpcs.util.Util.faceLocation(npc.getEntity(), trait.lastOpenedObject.getLocation().clone().add(0, 1, 0));
                Location newLoc = trait.lastOpenedObject.getLocation().clone().add(0.5, 1, 0.5);
                newLoc.setYaw(npc.getEntity().getLocation().getYaw());
                newLoc.setPitch(npc.getEntity().getLocation().getPitch());
                //npc.teleport(newLoc, TeleportCause.PLUGIN);

                if (npc.getEntity() instanceof Player)
                    net.citizensnpcs.util.PlayerAnimation.ARM_SWING.play((Player) npc.getEntity());

                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    this.processNPCThruOpenable(2, npc, trait);
                }, 5L);
                return;
            case 2:
                plugin.getMcUtils().openOpenable(trait.lastOpenedObject);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    this.processNPCThruOpenable(3, npc, trait);
                }, 5L);
                return;
            case 3:
                if (trait.getPendingDestinations().size() > 1) {
                    Location newTPLocation = new Location(npc.getEntity().getLocation().getWorld(), trait.getPendingDestinations().get(1).getBlockX() + 0.5, trait.getPendingDestinations().get(1).getBlockY() + 1.1, trait.getPendingDestinations().get(1).getBlockZ() + 0.5);
                    npc.teleport(newTPLocation, TeleportCause.PLUGIN);
                }
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    this.processNPCThruOpenable(4, npc, trait);
                }, 15L);
                return;
            case 4:
                net.citizensnpcs.util.Util.faceLocation(npc.getEntity(), trait.lastOpenedObject.getLocation().clone().add(0, 1, 0));
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    this.processNPCThruOpenable(5, npc, trait);
                }, 15L);
                return;
            case 5:
                if (npc.getEntity() instanceof Player)
                    net.citizensnpcs.util.PlayerAnimation.ARM_SWING.play((Player) npc.getEntity());

                trait.removePendingDestination(0);
                DestinationsPlugin.getInstance().getMcUtils().closeOpenable(trait.lastOpenedObject);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    this.processNPCThruOpenable(6, npc, trait);
                }, 15L);
                return;
            default:
                trait.lastOpenedObject = null;
                trait.runningDoor = false;
                break;
        }
    }

    private void teleportSurface(Entity npc, Location loc, TeleportCause reason) {
        if (!plugin.getAStarPathFinder().isLocationWalkable(loc, false, false, false)) {
            if (!plugin.getAStarPathFinder().isLocationWalkable(loc.clone().add(0, 1, 0), false, false, false))
                npc.teleport(loc, reason);
            else npc.teleport(loc.clone().add(0, 1, 0), reason);
        } else {
            npc.teleport(loc, reason);
        }

    }

    private void teleportSurface(Entity npc, Location loc) {
        teleportSurface(npc, loc, TeleportCause.PLUGIN);
    }

    public void load(NPCDestinationsTrait trait, DataKey key) {
        int nCnt = 0;

        if (trait.AllowedPathBlocks == null) trait.AllowedPathBlocks = new ArrayList<Material>();

        while (true) {
            if (key.keyExists("AllowedBlocks." + nCnt)) {
                String materialType = key.getString("AllowedBlocks." + nCnt);

                Material allowMat = Material.getMaterial(materialType);
                if (allowMat != null) trait.AllowedPathBlocks.add(allowMat);
                else this.plugin.getMessagesManager().logToConsole(plugin, "Block conversion failure-" + materialType);
            } else {
                break;
            }
            nCnt++;
        }
        nCnt = 0;

        trait.citizens_Swim = key.getBoolean("Destinations.citizens_Swim", true);
        trait.citizens_NewPathFinder = key.getBoolean("Destinations.citizens_NewPathFinder", true);
        trait.citizens_AvoidWater = key.getBoolean("Destinations.citizens_AvoidWater", true);
        trait.citizens_DistanceMargin = key.getDouble("Destinations.citizens_DistanceMargin", 1D);
        trait.citizens_PathDistanceMargin = key.getDouble("Destinations.citizens_PathDistanceMargin ", 1D);
        trait.maxProcessingTime = key.getInt("Destinations.maxprocessingtime", -1);

        // To convert the blocks under to the new format.
        if (key.keyExists("LookOneBlockDown")) {
            if (key.getBoolean("LookOneBlockDown")) {
                trait.blocksUnderSurface = 1;
            } else {
                trait.blocksUnderSurface = 0;
            }
            key.removeKey("LookOneBlockDown");
        }

        while (true) {
            if (key.getString("Destinations." + nCnt) != "") {
                Destination oLoc = null;
                try {
                    oLoc = new Destination();
                    if (key.getString("Destinations." + nCnt + ".locationid", "").trim().isEmpty()) {
                        oLoc.locationUUID = UUID.randomUUID();
                    } else {
                        oLoc.locationUUID = UUID.fromString(key.getString("Destinations." + nCnt + ".locationid"));
                    }
                    oLoc.location = new Location(plugin.getServer().getWorld(key.getString("Destinations." + nCnt + ".Location.world")), key.getInt("Destinations." + nCnt + ".Location.x"), key.getInt("Destinations." + nCnt + ".Location.y"), key.getInt("Destinations." + nCnt + ".Location.z"), Float.parseFloat(key.getString("Destinations." + nCnt + ".Location.yaw", "0.0")), Float.parseFloat(key.getString("Destinations." + nCnt + ".Location.pitch", "0.0")));
                    oLoc.location.add(0.5, 0, 0.5);
                    oLoc.setMaxDistance(key.getDouble("Destinations." + nCnt + ".MaxDistance"));
                    oLoc.probability = key.getInt("Destinations." + nCnt + ".Probability.ChancePercent");
                    oLoc.timeMinimum = key.getInt("Destinations." + nCnt + ".Probability.Min_Time");
                    oLoc.timeMaximum = key.getInt("Destinations." + nCnt + ".Probability.Max_Time");
                    oLoc.timeOfDay = key.getInt("Destinations." + nCnt + ".TimeOfDay") == 0 ? 1 : key.getInt("Destinations." + nCnt + ".TimeOfDay");
                    oLoc.waitMaximum = key.getInt("Destinations." + nCnt + ".WanderSettings.Wait_Maximum");
                    oLoc.waitMinimum = key.getInt("Destinations." + nCnt + ".WanderSettings.Wait_Minimum");
                    oLoc.setWanderingDistance(key.getDouble("Destinations." + nCnt + ".WanderSettings.Wandering_Distance"));
                    oLoc.aliasName = key.getString("Destinations." + nCnt + ".AliasName", "");

                    oLoc.wanderingUseBlocks = key.getBoolean("Destinations." + nCnt + ".UseBlockSetting", false);

                    // 1.29 Weather
                    oLoc.weatherFlag = key.getInt("Destinations." + nCnt + ".WeatherFlag", 0);

                    oLoc.wanderingRegion = "";

                    if (key.keyExists("Destinations." + nCnt + ".WanderSettings.Wandering_Region"))
                        oLoc.wanderingRegion = key.getString("Destinations." + nCnt + ".WanderSettings.Wandering_Region", "");

                    if (key.keyExists("Destinations." + nCnt + ".WanderSettings.Use_Blocks"))
                        oLoc.wanderingUseBlocks = key.getBoolean("Destinations." + nCnt + ".Use_Blocks");

                    if (key.keyExists("Destinations." + nCnt + ".Items.Head"))
                        oLoc.itemsHead = (ItemStack) key.getRaw("Destinations." + nCnt + ".Items.Head");
                    if (key.keyExists("Destinations." + nCnt + ".Items.Chest"))
                        oLoc.itemsChest = (ItemStack) key.getRaw("Destinations." + nCnt + ".Items.Chest");
                    if (key.keyExists("Destinations." + nCnt + ".Items.Legs"))
                        oLoc.itemsLegs = (ItemStack) key.getRaw("Destinations." + nCnt + ".Items.Legs");
                    if (key.keyExists("Destinations." + nCnt + ".Items.Boots"))
                        oLoc.itemsBoots = (ItemStack) key.getRaw("Destinations." + nCnt + ".Items.Boots");
                    if (key.keyExists("Destinations." + nCnt + ".Items.Hand"))
                        oLoc.itemsHand = (ItemStack) key.getRaw("Destinations." + nCnt + ".Items.Hand");
                    if (key.keyExists("Destinations." + nCnt + ".Items.OffHand"))
                        oLoc.itemsOffhand = (ItemStack) key.getRaw("Destinations." + nCnt + ".Items.OffHand");
                    if (key.keyExists("Destinations." + nCnt + ".Items.Clear"))
                        oLoc.itemsClear = key.getBoolean("Destinations." + nCnt + ".Items.Clear", false);

                    // V1.33 - Skins
                    oLoc.playerSkinName = key.getString("Destinations." + nCnt + ".Skin.Name", "");
                    oLoc.playerSkinUUID = key.getString("Destinations." + nCnt + ".Skin.UUID", "");
                    oLoc.playerSkinApplyOnArrival = key.getBoolean("Destinations." + nCnt + ".Skin.ApplyOnArrival", false);
                    oLoc.playerSkinTextureMetadata = key.getString("Destinations." + nCnt + ".Skin.MetaData", "");
                    oLoc.playerSkinTextureSignature = key.getString("Destinations." + nCnt + ".Skin.Signature", "");

                    // 2.1.1 -- Citizens pathfinding changes.
                    if (key.keyExists("Destinations." + nCnt + ".Citizens.Swim"))
                        oLoc.citizensSwim = key.getString("Destinations." + nCnt + ".Citizens.Swim").equalsIgnoreCase("true") ? TriBoolean.TRUE : TriBoolean.FALSE;
                    if (key.keyExists("Destinations." + nCnt + ".Citizens.NewPathfinder"))
                        oLoc.citizensNewPathFinder = key.getString("Destinations." + nCnt + ".Citizens.NewPathfinder").equalsIgnoreCase("true") ? TriBoolean.TRUE : TriBoolean.FALSE;
                    if (key.keyExists("Destinations." + nCnt + ".Citizens.AvoidWater"))
                        oLoc.citizensAvoidWater = key.getString("Destinations." + nCnt + ".Citizens.AvoidWater").equalsIgnoreCase("true") ? TriBoolean.TRUE : TriBoolean.FALSE;
                    if (key.keyExists("Destinations." + nCnt + ".Citizens.DefaultStuck"))
                        oLoc.citizensDefaultStuck = key.getString("Destinations." + nCnt + ".Citizens.DefaultStuck").equalsIgnoreCase("true") ? TriBoolean.TRUE : TriBoolean.FALSE;
                    if (key.keyExists("Destinations." + nCnt + ".Citizens.DistanceMargin"))
                        oLoc.citizensDistanceMargin = key.getDouble("Destinations." + nCnt + ".Citizens.DistanceMargin");
                    if (key.keyExists("Destinations." + nCnt + ".Citizens.PathDistance"))
                        oLoc.citizensPathDistanceMargin = key.getDouble("Destinations." + nCnt + ".Citizens.PathDistance");

                    // V1.44 - Commands
                    oLoc.arrivalCommands = new ArrayList<String>();
                    if (key.keyExists("Destinations." + nCnt + ".Commands.arrival"))
                        oLoc.arrivalCommands = (ArrayList<String>) key.getRaw("Destinations." + nCnt + ".Commands.arrival");

                    // V1.50 - Location pausing
                    if (key.keyExists("Destinations." + nCnt + ".Pause.Distance"))
                        oLoc.pauseDistance = key.getInt("Destinations." + nCnt + ".Pause.Distance", -1);
                    if (key.keyExists("Destinations." + nCnt + ".Pause.TimeOut"))
                        oLoc.pauseTimeout = key.getInt("Destinations." + nCnt + ".Pause.TimeOut", -1);
                    if (key.keyExists("Destinations." + nCnt + ".Pause.Type"))
                        oLoc.pauseType = key.getString("Destinations." + nCnt + ".Pause.Type", "ALL");

                    if (!key.keyExists("Destinations." + nCnt + ".PluginSettings")) {
                        key.setString("Destinations." + nCnt + ".PluginSettings", "");
                    }

                    for (DestinationsAddon plugin : plugin.getPluginManager().getPlugins()) {
                        if (plugin.getActionName().equalsIgnoreCase("sentinel")) {
                            // V2.1.X - Convert to the internal plugin based
                            // addon
                            if (key.keyExists("Destinations." + nCnt + ".Sentinel") && !key.getString("Destinations." + nCnt + ".Sentinel.lastSet", "0").equals("0")) {
                                this.plugin.getMessagesManager().consoleMessage(this.plugin, "destinations", "Console_Messages.plugin_conversion", plugin.getActionName());
                                try {
                                    plugin.onLocationLoading(trait.getNPC(), trait, oLoc, key.getRelative("Destinations." + nCnt));
                                } catch (Exception err) {
                                    StringWriter sw = new StringWriter();
                                    err.printStackTrace(new PrintWriter(sw));
                                    this.plugin.getMessagesManager().consoleMessage(this.plugin, "destinations", "Console_Messages.plugin_debug", err.getMessage() + "\n" + sw);
                                }

                                for (DataKey storageKey : key.getRelative("Destinations." + nCnt + ".Sentinel").getSubKeys()) {
                                    key.removeKey("Destinations." + nCnt + ".Sentinel." + storageKey.name());
                                }
                                key.removeKey("Destinations." + nCnt + ".Sentinel");
                            } else {
                                try {
                                    plugin.onLocationLoading(trait.getNPC(), trait, oLoc, key.getRelative("Destinations." + nCnt + ".PluginSettings"));
                                } catch (Exception err) {
                                    StringWriter sw = new StringWriter();
                                    err.printStackTrace(new PrintWriter(sw));
                                    this.plugin.getMessagesManager().consoleMessage(this.plugin, "destinations", "Console_Messages.plugin_debug", err.getMessage() + "\n" + sw);
                                }
                            }
                        } else if (plugin.getActionName().equalsIgnoreCase("jobsreborn")) {
                            // V2.1.X - Convert to the internal plugin based
                            // jobs from the old location
                            if (key.keyExists("Destinations." + nCnt + ".JobsReborn") && !key.getString("Destinations." + nCnt + ".JobsReborn.JobName", "").equals("")) {
                                this.plugin.getMessagesManager().consoleMessage(this.plugin, "destinations", "Console_Messages.plugin_conversion", plugin.getActionName());
                                try {
                                    plugin.onLocationLoading(trait.getNPC(), trait, oLoc, key.getRelative("Destinations." + nCnt));
                                } catch (Exception err) {
                                    StringWriter sw = new StringWriter();
                                    err.printStackTrace(new PrintWriter(sw));
                                    this.plugin.getMessagesManager().consoleMessage(this.plugin, "destinations", "Console_Messages.plugin_debug", err.getMessage() + "\n" + sw);
                                }

                                for (DataKey storageKey : key.getRelative("Destinations." + nCnt + ".JobsReborn").getSubKeys()) {
                                    key.removeKey("Destinations." + nCnt + ".JobsReborn." + storageKey.name());
                                }
                                key.removeKey("Destinations." + nCnt + ".JobsReborn");
                            } else {

                                try {
                                    plugin.onLocationLoading(trait.getNPC(), trait, oLoc, key.getRelative("Destinations." + nCnt + ".PluginSettings"));
                                } catch (Exception err) {
                                    StringWriter sw = new StringWriter();
                                    err.printStackTrace(new PrintWriter(sw));
                                    this.plugin.getMessagesManager().consoleMessage(this.plugin, "destinations", "Console_Messages.plugin_debug", err.getMessage() + "\n" + sw);
                                }
                            }
                        } else {
                            plugin.onLocationLoading(trait.getNPC(), trait, oLoc, key.getRelative("Destinations." + nCnt + ".PluginSettings"));

                            /* try {
                                plugin.onLocationLoading(trait.getNPC(), trait, oLoc, key.getRelative("Destinations." + nCnt + ".PluginSettings"));
                            } catch (Exception err) {
                                StringWriter sw = new StringWriter();
                                err.printStackTrace(new PrintWriter(sw));
                                plugin.getMessageManager.consoleMessage(plugin, "destinations", "Console_Messages.plugin_debug", err.getMessage() + "\n" + sw.toString());
                            } */
                        }
                    }
                } catch (Exception err) {
                    plugin.getMessagesManager().logToConsole(plugin, "Failure Loading NPC. " + trait.getNPC().getId());
                    StringWriter sw = new StringWriter();
                    err.printStackTrace(new PrintWriter(sw));
                    plugin.getMessagesManager().logToConsole(plugin, sw.toString());
                    break;
                }

                if (oLoc.location != null) {
                    trait.NPCLocations.add(oLoc);
                }
            } else {
                break;
            }
            nCnt++;
        }

        if (key.keyExists("enabledplugins")) {
            trait.enabledPlugins = (ArrayList<String>) key.getRaw("enabledplugins");
        }
    }

    public void save(NPCDestinationsTrait trait, DataKey key) {
        key.removeKey("Destinations");
        key.removeKey("AllowedBlocks");

        for (int n = 0; n < trait.AllowedPathBlocks.size(); n++) {
            key.setString("AllowedBlocks." + n, trait.AllowedPathBlocks.get(n).name());
        }
        key.setRaw("enabledplugins", trait.enabledPlugins);

        key.setBoolean("Destinations.citizens_Swim", trait.citizens_Swim);
        key.setBoolean("Destinations.citizens_NewPathFinder", trait.citizens_NewPathFinder);
        key.setBoolean("Destinations.citizens_AvoidWater", trait.citizens_AvoidWater);
        key.setDouble("Destinations.citizens_DistanceMargin", trait.citizens_DistanceMargin);
        key.setDouble("Destinations.citizens_PathDistanceMargin", trait.citizens_PathDistanceMargin);
        key.setInt("Destinations.maxprocessingtime", trait.maxProcessingTime);

        int i = 0;
        for (int nCnt = 0; nCnt < trait.NPCLocations.size(); nCnt++) {
            // Do not save any managed locations!
            if (trait.NPCLocations.get(i).managedLocation.length() == 0) {
                key.setString("Destinations." + i + ".locationid", trait.NPCLocations.get(i).locationUUID == null ? UUID.randomUUID().toString() : trait.NPCLocations.get(i).locationUUID.toString());
                key.setString("Destinations." + i + ".Location.world", trait.NPCLocations.get(i).location.getWorld().getName());
                key.setInt("Destinations." + i + ".Location.x", trait.NPCLocations.get(i).location.getBlockX());
                key.setInt("Destinations." + i + ".Location.y", trait.NPCLocations.get(i).location.getBlockY());
                key.setInt("Destinations." + i + ".Location.z", trait.NPCLocations.get(i).location.getBlockZ());
                key.setString("Destinations." + i + ".Location.pitch", trait.NPCLocations.get(i).location.getPitch() + "");
                key.setString("Destinations." + i + ".Location.yaw", trait.NPCLocations.get(i).location.getYaw() + "");
                key.setDouble("Destinations." + i + ".MaxDistance", trait.NPCLocations.get(i).getMaxDistance());
                key.setInt("Destinations." + i + ".Probability.ChancePercent", trait.NPCLocations.get(i).probability);
                key.setInt("Destinations." + i + ".Probability.Min_Time", trait.NPCLocations.get(i).timeMinimum);
                key.setInt("Destinations." + i + ".Probability.Max_Time", trait.NPCLocations.get(i).timeMaximum);
                key.setInt("Destinations." + i + ".TimeOfDay", trait.NPCLocations.get(i).timeOfDay);
                key.setInt("Destinations." + i + ".WanderSettings.Wait_Maximum", trait.NPCLocations.get(i).waitMaximum);
                key.setInt("Destinations." + i + ".WanderSettings.Wait_Minimum", trait.NPCLocations.get(i).waitMinimum);
                key.setString("Destinations." + i + ".WanderSettings.Wandering_Region", trait.NPCLocations.get(i).wanderingRegion);
                key.setDouble("Destinations." + i + ".WanderSettings.Wandering_Distance", trait.NPCLocations.get(i).getWanderingDistance());
                key.setBoolean("Destinations." + i + ".WanderSettings.Wandering_UseBlocks", trait.NPCLocations.get(i).wanderingUseBlocks);
                key.setString("Destinations." + i + ".AliasName", trait.NPCLocations.get(i).aliasName);
                key.setBoolean("Destinations." + i + ".UseBlockSetting", trait.NPCLocations.get(i).wanderingUseBlocks);
                key.setInt("Destinations." + i + ".WeatherFlag", trait.NPCLocations.get(i).weatherFlag);

                key.setRaw("Destinations." + i + ".Items.Head", trait.NPCLocations.get(i).itemsHead);
                key.setRaw("Destinations." + i + ".Items.Chest", trait.NPCLocations.get(i).itemsChest);
                key.setRaw("Destinations." + i + ".Items.Boots", trait.NPCLocations.get(i).itemsBoots);
                key.setRaw("Destinations." + i + ".Items.Legs", trait.NPCLocations.get(i).itemsLegs);
                key.setRaw("Destinations." + i + ".Items.OffHand", trait.NPCLocations.get(i).itemsOffhand);
                key.setRaw("Destinations." + i + ".Items.Hand", trait.NPCLocations.get(i).itemsHand);
                key.setRaw("Destinations." + i + ".Items.Clear", trait.NPCLocations.get(i).itemsClear);

                // V1.33 - Skins
                key.setString("Destinations." + i + ".Skin.Name", trait.NPCLocations.get(i).playerSkinName);
                key.setString("Destinations." + i + ".Skin.UUID", trait.NPCLocations.get(i).playerSkinUUID);
                key.setBoolean("Destinations." + i + ".Skin.ApplyOnArrival", trait.NPCLocations.get(i).playerSkinApplyOnArrival);
                key.setString("Destinations." + i + ".Skin.MetaData", trait.NPCLocations.get(i).playerSkinTextureMetadata);
                key.setString("Destinations." + i + ".Skin.Signature", trait.NPCLocations.get(i).playerSkinTextureSignature);

                // V2.1.1 - Citizens
                if (trait.NPCLocations.get(i).citizensSwim != TriBoolean.NOT_SET)
                    key.setString("Destinations." + i + ".Citizens.Swim", trait.NPCLocations.get(i).citizensSwim == TriBoolean.TRUE ? "True" : "False");
                if (trait.NPCLocations.get(i).citizensNewPathFinder != TriBoolean.NOT_SET)
                    key.setString("Destinations." + i + ".Citizens.NewPathfinder", trait.NPCLocations.get(i).citizensNewPathFinder == TriBoolean.TRUE ? "True" : "False");
                if (trait.NPCLocations.get(i).citizensAvoidWater != TriBoolean.NOT_SET)
                    key.setString("Destinations." + i + ".Citizens.AvoidWater", trait.NPCLocations.get(i).citizensAvoidWater == TriBoolean.TRUE ? "True" : "False");
                if (trait.NPCLocations.get(i).citizensDefaultStuck != TriBoolean.NOT_SET)
                    key.setString("Destinations." + i + ".Citizens.DefaultStuck", trait.NPCLocations.get(i).citizensDefaultStuck == TriBoolean.TRUE ? "True" : "False");
                if (trait.NPCLocations.get(i).citizensDistanceMargin > -1D)
                    key.setDouble("Destinations." + i + ".Citizens.DistanceMargin", trait.NPCLocations.get(i).citizensDistanceMargin);
                if (trait.NPCLocations.get(i).citizensPathDistanceMargin > -1D)
                    key.setDouble("Destinations." + i + ".Citizens.PathDistance", trait.NPCLocations.get(i).citizensPathDistanceMargin);

                // V1.44 - Commands
                key.setRaw("Destinations." + i + ".Commands.arrival", trait.NPCLocations.get(i).arrivalCommands);

                // V1.50 - Location pausing
                key.setInt("Destinations." + i + ".Pause.Distance", trait.NPCLocations.get(i).pauseDistance);
                key.setInt("Destinations." + i + ".Pause.TimeOut", trait.NPCLocations.get(i).pauseTimeout);
                key.setString("Destinations." + i + ".Pause.Type", trait.NPCLocations.get(i).pauseType);

                if (!key.keyExists("Destinations." + i + ".PluginSettings")) {
                    key.setString("Destinations." + i + ".PluginSettings", "");
                }

                for (DestinationsAddon plugin : plugin.getPluginManager().getPlugins()) {
                    try {
                        plugin.onLocationSaving(trait.getNPC(), trait, trait.NPCLocations.get(i), key.getRelative("Destinations." + i + ".PluginSettings"));
                    } catch (Exception err) {
                        StringWriter sw = new StringWriter();
                        err.printStackTrace(new PrintWriter(sw));
                        this.plugin.getMessagesManager().logToConsole(this.plugin, err.getMessage() + "\n" + sw);
                    }
                }
                i++;
            }
        }
    }
}
