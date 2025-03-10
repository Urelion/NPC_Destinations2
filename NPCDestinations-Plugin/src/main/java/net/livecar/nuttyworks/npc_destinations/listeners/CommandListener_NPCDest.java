package net.livecar.nuttyworks.npc_destinations.listeners;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCTraitCommandAttachEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.npc.skin.Skin;
import net.citizensnpcs.npc.skin.SkinnableEntity;
import net.citizensnpcs.trait.waypoint.Waypoints;
import net.livecar.nuttyworks.npc_destinations.DebugTarget;
import net.livecar.nuttyworks.npc_destinations.DestinationsPlugin;
import net.livecar.nuttyworks.npc_destinations.api.Destination;
import net.livecar.nuttyworks.npc_destinations.api.LocationAdded;
import net.livecar.nuttyworks.npc_destinations.api.LocationDeleted;
import net.livecar.nuttyworks.npc_destinations.api.LocationUpdated;
import net.livecar.nuttyworks.npc_destinations.utilities.CitizensUtilities;
import net.livecar.nuttyworks.npc_destinations.citizens.NPCDestinationsTrait;
import net.livecar.nuttyworks.npc_destinations.citizens.NPCDestinationsTrait.RequestedAction;
import net.livecar.nuttyworks.npc_destinations.plugins.DestinationsAddon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class CommandListener_NPCDest {

    private DestinationsPlugin destRef = null;

    public CommandListener_NPCDest(DestinationsPlugin storageRef) {
        destRef = storageRef;
    }

    @SuppressWarnings({"deprecation", "unused"})
    public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] inargs) {

        if (inargs.length == 0 || inargs[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.GOLD + "--- " + ChatColor.GREEN + destRef.getDescription().getName() + " Help " + ChatColor.GOLD + " --------------------- " + ChatColor.WHITE + "V " + destRef.getDescription().getVersion());
            if (sender instanceof Player) {
                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_help_infoperm");
                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_help_npc_settingsperm");
                String pluginHelp = "";
                if (!pluginHelp.equals("")) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_help_plugins", pluginHelp);
                }
            }
            return true;
        }

        if (inargs[0].equalsIgnoreCase("backup")) {
            if (!sender.hasPermission("npcdestinations.backup") && !sender.isOp()) {
                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                return true;
            } else {
                CitizensUtilities citizensUtils = new CitizensUtilities(destRef);
                citizensUtils.BackupConfig(true);
                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.backup_command");
                return true;
            }
        }

        if (inargs[0].equalsIgnoreCase("enginestatus")) {
            if (!sender.hasPermission("npcdestinations.enginestatus") && !sender.isOp()) {
                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                return true;
            } else {
                if ((destRef.getAStarPathFinder().getCurrentTask() == null || destRef.getAStarPathFinder().getCurrentTask().getNpc() == null) && destRef.getAStarPathFinder().getPathQueue().size() == 0) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_enginestatus_idle");
                } else if ((destRef.getAStarPathFinder().getCurrentTask() == null || destRef.getAStarPathFinder().getCurrentTask().getNpc() == null) && destRef.getAStarPathFinder().getPathQueue().size() > 0) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_enginestatus_idle_queue");
                } else if ((destRef.getAStarPathFinder().getCurrentTask() != null || destRef.getAStarPathFinder().getCurrentTask().getNpc() != null) && destRef.getAStarPathFinder().getPathQueue().size() == 0) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_enginestatus_processing_noqueue");
                } else if ((destRef.getAStarPathFinder().getCurrentTask() != null || destRef.getAStarPathFinder().getCurrentTask().getNpc() != null) && destRef.getAStarPathFinder().getPathQueue().size() > 0) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_enginestatus_processing_queue");
                }
            }
            return true;
        }

        if (inargs[0].equalsIgnoreCase("allstatus")) {
            if (!sender.hasPermission("npcdestinations.allstatus") && !sender.isOp()) {
                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                return true;
            } else {
                sender.sendMessage(ChatColor.GOLD + "----- " + destRef.getDescription().getName() + " ----- V " + destRef.getDescription().getVersion());
                for (NPC npcItem : CitizensAPI.getNPCRegistry()) {
                    if ((npcItem != null) && (npcItem.hasTrait(NPCDestinationsTrait.class))) {
                        if (!npcItem.isSpawned()) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_allstatus_notspawned", npcItem);

                        } else {
                            NPCDestinationsTrait oCurTrait = npcItem.getTrait(NPCDestinationsTrait.class);
                            switch (oCurTrait.getCurrentAction()) {
                                case IDLE:
                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_allstatus_idle", oCurTrait);
                                    break;
                                case IDLE_FAILED:
                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_allstatus_idle_failure", oCurTrait);
                                    break;
                                case PATH_HUNTING:
                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_allstatus_hunting", oCurTrait);
                                    break;
                                case RANDOM_MOVEMENT:
                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_allstatus_random", oCurTrait);
                                    break;
                                case TRAVELING:
                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_allstatus_pending", oCurTrait);
                                    break;
                                case PATH_FOUND:
                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_allstatus_path_found", oCurTrait);
                                    break;
                                default:
                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_allstatus_pending", oCurTrait);
                                    break;
                            }
                        }
                    }
                }
                return true;
            }
        }

        // 1.6 adding --npc # to allow json clicks
        int npcid = -1;
        List<String> sList = new ArrayList<String>();

        for (int nCnt = 0; nCnt < inargs.length; nCnt++) {
            if (inargs[nCnt].equalsIgnoreCase("--npc")) {
                // Npc ID should be the next one
                if (inargs.length >= nCnt + 2) {
                    npcid = Integer.parseInt(inargs[nCnt + 1]);
                    nCnt++;
                }
            } else {
                sList.add(inargs[nCnt]);
            }
        }

        inargs = sList.toArray(new String[sList.size()]);
        NPC npc = null;
        if (npcid == -1) {
            // Now lets find the NPC this should run on.
            npc = destRef.getCitizensPlugin().getNPCSelector().getSelected(sender);
            if (npc != null) {
                // Gets NPC Selected for this sender
                npcid = npc.getId();
            }
        } else {
            npc = CitizensAPI.getNPCRegistry().getById(npcid);
        }

        if (inargs[0].equalsIgnoreCase("debuglog")) {
            if (!sender.hasPermission("npcdestinations.debug.set") && Level.parse(inargs[1]) == null) {
                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                return true;
            }

            if (Level.parse(inargs[1]) != null && !destRef.getUtilities().isNumeric(inargs[1])) {
                destRef.setDebugLogLevel(Level.parse(inargs[1]));
                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_on");
                return true;
            }

            if (inargs[0].equalsIgnoreCase("debug")) {
                if (inargs.length > 1) {
                    if (!sender.hasPermission("npcdestinations.debug.set") && Level.parse(inargs[1]) == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                        return true;
                    }

                    if (!sender.hasPermission("npcdestinations.debug.own") && inargs[1].equalsIgnoreCase("*")) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                        return true;
                    }

                    if (inargs[1].equalsIgnoreCase("*")) {
                        for (DebugTarget debugOutput : destRef.getDebugTargets()) {
                            if ((debugOutput.targetSender instanceof Player) && debugOutput.targetSender.equals(sender)) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_removed", "*");
                                for (DebugTarget debugTarget : destRef.getDebugTargets())
                                    debugTarget.clearDebugBlocks();
                                destRef.getDebugTargets().clear();
                                return true;
                            }
                        }
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_added", "*");
                        destRef.getDebugTargets().add(new DebugTarget(sender, -1));
                        return true;
                    }

                    if (inargs[1].equalsIgnoreCase("list")) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_listing", npc);
                        return true;
                    }

                    NPC selectedNPC = null;
                    if (destRef.getUtilities().isNumeric(inargs[1])) {
                        // Adding an NPC by ID
                        selectedNPC = CitizensAPI.getNPCRegistry().getById(Integer.parseInt(inargs[1]));
                    }

                    if (selectedNPC == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_invalid");
                        return true;
                    }
                    Owner ownerTrait = selectedNPC.getTrait(Owner.class);
                    if (!ownerTrait.isOwnedBy(sender) && !sender.hasPermission("npcdestinations.debug.all")) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_invalid");
                        return true;
                    }

                    for (DebugTarget debugOutput : destRef.getDebugTargets()) {
                        if ((debugOutput.targetSender instanceof Player) && ((Player) debugOutput.targetSender).getUniqueId().equals(((Player) sender).getUniqueId())) {
                            for (int cnt = 0; cnt < debugOutput.getTargets().size(); cnt++) {
                                if (debugOutput.getTargets().get(cnt).equals(selectedNPC.getId())) {
                                    debugOutput.removeNPCTarget(selectedNPC.getId());
                                    if (debugOutput.getTargets().size() == 0) {
                                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_off");
                                        debugOutput.clearDebugBlocks();
                                        destRef.getDebugTargets().remove(debugOutput);
                                    } else {
                                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_removed", selectedNPC.getFullName());
                                    }
                                    return true;
                                }
                            }

                            debugOutput.addNPCTarget(selectedNPC.getId());
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_added", selectedNPC.getFullName());
                            return true;
                        }
                    }
                    DebugTarget dbgTarget = new DebugTarget(sender, selectedNPC.getId());
                    destRef.getDebugTargets().add(dbgTarget);
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_added", selectedNPC.getFullName());
                    return true;

                } else {
                    if (!sender.hasPermission("npcdestinations.debug.all") && !sender.isOp() && npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                        return true;
                    }

                    if (npc == null) {
                        for (int target = 0; target < destRef.getDebugTargets().size(); target++) {
                            DebugTarget debugOutput = destRef.getDebugTargets().get(target);
                            if (debugOutput.targetSender.equals(sender)) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_off");
                                destRef.getDebugTargets().get(target).clearDebugBlocks();
                                destRef.getDebugTargets().remove(target);
                                return true;
                            }
                        }
                        DebugTarget dbgTarget = new DebugTarget(sender, -1);
                        destRef.getDebugTargets().add(dbgTarget);
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_added", "*");
                        return true;
                    } else {
                        for (int target = 0; target < destRef.getDebugTargets().size(); target++) {
                            DebugTarget debugOutput = destRef.getDebugTargets().get(target);
                            if (debugOutput.targetSender.equals(sender)) {
                                if (debugOutput.getTargets().contains(npc.getId())) {
                                    for (int cnt = 0; cnt < debugOutput.getTargets().size(); cnt++) {
                                        if (debugOutput.getTargets().get(cnt).equals(npc.getId())) {
                                            if (debugOutput.getTargets().size() == 0) {
                                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_off");
                                                destRef.getDebugTargets().get(target).clearDebugBlocks();
                                                destRef.getDebugTargets().remove(target);
                                                return true;
                                            } else {
                                                debugOutput.getTargets().remove(cnt);
                                                if (debugOutput.getTargets().size() == 0) {
                                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_off");
                                                    destRef.getDebugTargets().get(target).clearDebugBlocks();
                                                    destRef.getDebugTargets().remove(target);
                                                } else {
                                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_removed", npc.getFullName());
                                                }
                                                return true;
                                            }
                                        }
                                    }
                                } else if (debugOutput.getTargets().size() == 0) {
                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_removed", "*");
                                    destRef.getDebugTargets().get(target).clearDebugBlocks();
                                    destRef.getDebugTargets().remove(target);
                                    return true;
                                } else {
                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_added", npc.getFullName());
                                    DebugTarget debugger = new DebugTarget(sender, npc.getId());
                                    destRef.getDebugTargets().add(debugOutput);
                                    return true;
                                }
                            }
                        }
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_added", npc.getFullName());
                        DebugTarget debugOutput = new DebugTarget(sender, npc.getId());
                        destRef.getDebugTargets().add(debugOutput);
                        return true;
                    }
                }
            }
            // Validate that the user owns this npc.

            if (npc == null) {
                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc");
                return true;
            }

            boolean isOwner = false;
            if (npc.hasTrait(Owner.class)) {
                if (sender instanceof Player) {
                    // Temp fix for Citizens not using UUID
                    Owner ownerTrait = npc.getTrait(Owner.class);

                    if (ownerTrait.isOwnedBy(sender)) {
                        isOwner = true;
                    }
                }
            }

            if (inargs[0].equalsIgnoreCase("autoset")) {
                // Not valid from console
                if (!sender.hasPermission("npcdestinations.editall.autoset") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.autoset"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    // Add the trait to this NPC
                    Class<? extends Trait> npcDestClass = CitizensAPI.getTraitFactory().getTraitClass("NPCDestinations");
                    if (npcDestClass == null) {
                        // Failed to add the trait.. Odd
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc");
                        return true;
                    } else if (!npc.hasTrait(npcDestClass)) {
                        // Add the trait, and signal other plugins we added the
                        // trait incase they care.
                        npc.addTrait(npcDestClass);
                        Bukkit.getPluginManager().callEvent(new NPCTraitCommandAttachEvent(npc, npcDestClass, sender));
                    }
                    // Setup the waypoint provider
                    Waypoints waypoints = npc.getTrait(Waypoints.class);
                    boolean success = waypoints.setWaypointProvider("NPCDestinations");
                    onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                    return true;
                }
            }

            // If you need access to the instance of MyTrait on the npc, get it
            // like
            // this
            NPCDestinationsTrait trait = null;
            if (npc != null) {
                if (!npc.hasTrait(NPCDestinationsTrait.class)) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc");
                    return true;
                } else
                    trait = npc.getTrait(NPCDestinationsTrait.class);
            }
            if (inargs[0].equalsIgnoreCase("info")) {
                if (!sender.hasPermission("npcdestinations.editall.info") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.info"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc");
                        return true;
                    }
                    sender.sendMessage(ChatColor.GOLD + "----- " + destRef.getDescription().getName() + " ----- V " + destRef.getDescription().getVersion());
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_info_settings", trait, null);

                    if (!npc.isSpawned()) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_info_notspawned");
                    } else if (trait.NPCLocations.size() > 0) {
                        sender.sendMessage(ChatColor.GREEN + "Configured Locations: ");
                        for (int nCnt = 0; nCnt < trait.NPCLocations.size(); nCnt++) {
                            Destination oLoc = trait.NPCLocations.get(nCnt);
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_info_location", trait, trait.NPCLocations.get(nCnt));
                        }
                    } else {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_info_nolocations");
                    }
                    return true;
                }
            }

            if (inargs[0].equalsIgnoreCase("process")) {
                if (!sender.hasPermission("npcdestinations.editall.process") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.process"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc");
                        return true;
                    }
                    if (trait.getRequestedAction() == RequestedAction.NO_PROCESSING) {
                        trait.setRequestedAction(RequestedAction.NORMAL_PROCESSING);
                    } else {
                        trait.setRequestedAction(RequestedAction.NO_PROCESSING);
                    }
                    return true;
                }
            }
            if (inargs[0].equalsIgnoreCase("citizens")) {
                // Not valid from console
                if (!sender.hasPermission("npcdestinations.editall.citizens") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.citizens"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    // parse the commands
                    if (inargs.length < 2) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_citizens_settings", trait);
                        return true;
                    }
                    // citizens {distancemargin} {pathdistancemargin}
                    // {newpathfinder} {swim} {avoidwater}

                    try {
                        trait.citizens_DistanceMargin = Double.parseDouble(inargs[1]);
                    } catch (Exception err) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_citizens_invalid");
                        return true;
                    }

                    if (inargs.length > 2)
                        try {
                            trait.citizens_PathDistanceMargin = Double.parseDouble(inargs[2]);
                        } catch (Exception err) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_citizens_invalid");
                            return true;
                        }

                    if (inargs.length > 3) {
                        trait.citizens_NewPathFinder = inargs[3].equalsIgnoreCase("y");
                    }

                    if (inargs.length > 4) {
                        trait.citizens_Swim = inargs[4].equalsIgnoreCase("y");
                    }

                    if (inargs.length > 5) {
                        trait.citizens_AvoidWater = inargs[5].equalsIgnoreCase("y");
                    }

                    if (inargs.length > 6) {
                        trait.citizens_DefaultStuck = inargs[6].equalsIgnoreCase("y");
                    }

                    onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                    return true;
                }
            }

            if (inargs[0].equalsIgnoreCase("goloc")) {
                // Not valid from console
                if (!sender.hasPermission("npcdestinations.editall.goloc") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.goloc"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc");
                        return true;
                    }

                    if (inargs.length > 1) {
                        int nLocNum = -1;

                        if (inargs[1].matches("\\d+")) {
                            // Location #
                            nLocNum = Integer.parseInt(inargs[1]);
                            if (nLocNum > trait.NPCLocations.size() - 1) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_goloc_invalid");
                                return true;
                            }
                        } else {
                            // Alias
                            for (int nCnt = 0; nCnt < trait.NPCLocations.size(); nCnt++) {
                                // V 1.45 -- Added location ID support for this
                                // command.
                                if (trait.NPCLocations.get(nCnt).aliasName.equalsIgnoreCase(inargs[1]) || trait.NPCLocations.get(nCnt).locationUUID.toString().equalsIgnoreCase(inargs[1])) {
                                    // Exists
                                    nLocNum = nCnt;
                                    break;
                                }
                            }
                        }
                        if (nLocNum > -1) {
                            long nLength = 0;
                            if (inargs.length == 3) {
                                if (destRef.getUtilities().isNumeric(inargs[2])) {
                                    nLength = Long.parseLong(inargs[2]) * 1000;
                                } else {
                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_goloc_badargs");
                                    return true;
                                }
                            } else {
                                nLength = 86400 * 1000; // 1 day
                            }

                            if (!trait.NPCLocations.get(nLocNum).managedLocation.equals("")) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, trait.NPCLocations.get(nLocNum));
                                return true;
                            }

                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_goloc_set", trait, trait.NPCLocations.get(nLocNum));

                            // Notify all plugins that the location has been
                            // reached.
                            for (DestinationsAddon plugin : destRef.getPluginManager().getPlugins()) {
                                if (trait.enabledPlugins.contains(plugin.getActionName())) {
                                    try {
                                        plugin.onNewDestination(npc, trait, trait.NPCLocations.get(nLocNum));
                                    } catch (Exception err) {
                                        StringWriter sw = new StringWriter();
                                        err.printStackTrace(new PrintWriter(sw));
                                        destRef.getMessagesManager().consoleMessage(destRef, "destinations", "Console_Messages.plugin_error", err.getMessage() + "\n" + sw);
                                    }
                                }
                            }

                            // Fire the navigation event
                            destRef.getCitizensProcessing().fireLocationChangedEvent(trait, trait.NPCLocations.get(nLocNum));

                            npc.getNavigator().cancelNavigation();
                            trait.clearPendingDestinations();
                            trait.lastResult = "Forced location";
                            trait.setLocation = trait.NPCLocations.get(nLocNum);
                            trait.currentLocation = trait.NPCLocations.get(nLocNum);
                            trait.setLocationLockUntil(LocalDateTime.now().plusSeconds(nLength));
                            trait.lastPositionChange = LocalDateTime.now();
                            trait.setRequestedAction(RequestedAction.SET_LOCATION);
                            return true;
                        }
                    }

                }
            }
            // V1.44 - Commands
            if (inargs[0].equalsIgnoreCase("loccommands")) {
                // Not valid from console
                if (!sender.hasPermission("npcdestinations.editall.loccommands") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.loccommands"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc");
                        return true;
                    }

                    if (!npc.isSpawned()) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_info_notspawned");
                    } else if (trait.NPCLocations.size() > 0) {
                        if (inargs.length == 2) {
                            int nIndex = Integer.parseInt(inargs[1]);
                            if (nIndex > trait.NPCLocations.size() - 1) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_commands_badargs");
                                return true;
                            }
                            Destination oCurLoc = trait.NPCLocations.get(nIndex);
                            if (oCurLoc == null) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_info_nolocations");
                            } else {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_commands_header", trait, oCurLoc, 0);
                                for (int nCnt = 0; nCnt < oCurLoc.arrivalCommands.size(); nCnt++) {
                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_commands_detail", trait, oCurLoc, nCnt);
                                }
                            }
                        }
                    } else {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_commands_badargs");
                    }
                    return true;
                }
            }
            if (inargs[0].equalsIgnoreCase("locdelcmd")) {
                // Not valid from console
                if (!sender.hasPermission("npcdestinations.editall.locdelcmd") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.locdelcmd"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc");
                        return true;
                    }

                    if (!npc.isSpawned()) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_info_notspawned");
                    } else if (trait.NPCLocations.size() > 0) {
                        if (inargs.length == 3) {
                            int nIndex = Integer.parseInt(inargs[1]);
                            if (nIndex > trait.NPCLocations.size() - 1) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_commands_badargs");
                                return true;
                            }
                            Destination oCurLoc = trait.NPCLocations.get(nIndex);
                            if (oCurLoc == null) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_info_nolocations");
                            } else {

                                if (!oCurLoc.managedLocation.equals("")) {
                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, oCurLoc);
                                    return true;
                                }

                                int listIndex = Integer.parseInt(inargs[2]);
                                if (nIndex > oCurLoc.arrivalCommands.size() - 1) {
                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_commands_badargs");
                                    return true;
                                }
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_commands_delete", trait, oCurLoc, 0);
                                oCurLoc.arrivalCommands.remove(listIndex);
                                LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nIndex));
                                Bukkit.getServer().getPluginManager().callEvent(changedLocation);
                                onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                                return true;
                            }
                        }
                    } else {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_commands_badargs");
                    }
                    return true;
                }
            }
            if (inargs[0].equalsIgnoreCase("locaddcmd")) {
                // Not valid from console
                if (!sender.hasPermission("npcdestinations.editall.locaddcmd") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.locaddcmd"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc");
                        return true;
                    }

                    if (!npc.isSpawned()) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_info_notspawned");
                    } else if (trait.NPCLocations.size() > 0) {
                        if (inargs.length > 1) {
                            int nIndex = Integer.parseInt(inargs[1]);
                            if (nIndex > trait.NPCLocations.size() - 1) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_commands_badargs");
                                return true;
                            }
                            Destination oCurLoc = trait.NPCLocations.get(nIndex);
                            if (!oCurLoc.managedLocation.equals("")) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, oCurLoc);
                                return true;
                            }
                            if (!(sender instanceof Player)) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_commands_badargs");
                                return true;
                            }
                            Player plr = (Player) sender;
                            if (destRef.getMcUtils().getMainHand(plr).getType().toString().contains("BOOK_AND_QUILL") || destRef.getMcUtils().getMainHand(plr).getType().toString().contains("WRITTEN_BOOK")) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_commands_badargs");
                                return true;
                            }

                            BookMeta meta = (BookMeta) plr.getItemInHand().getItemMeta();
                            String commandString = "";
                            for (int pageNum = 1; pageNum <= meta.getPageCount(); pageNum++)
                                commandString += meta.getPage(1).trim();
                            oCurLoc.arrivalCommands.add(commandString);
                            LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nIndex));
                            Bukkit.getServer().getPluginManager().callEvent(changedLocation);
                            onCommand(sender, cmd, cmdLabel, new String[]{"loccommands", "--npc", Integer.toString(npc.getId()), inargs[1]});
                            return true;
                        } else if (inargs.length > 2) {
                            int nIndex = Integer.parseInt(inargs[1]);
                            if (nIndex > trait.NPCLocations.size() - 1) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_commands_badargs");
                                return true;
                            }
                            Destination oCurLoc = trait.NPCLocations.get(nIndex);
                            if (oCurLoc == null) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_info_nolocations");
                            } else {

                                if (!oCurLoc.managedLocation.equals("")) {
                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, oCurLoc);
                                    return true;
                                }

                                String commandString = "";

                                for (int nCnt = 2; nCnt < inargs.length; nCnt++) {
                                    commandString += inargs[nCnt] + " ";
                                }
                                oCurLoc.arrivalCommands.add(commandString.trim());
                                LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nIndex));
                                Bukkit.getServer().getPluginManager().callEvent(changedLocation);
                                onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                                return true;
                            }
                        }
                    } else {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_commands_badargs");
                    }
                    return true;
                }
            }
            if (inargs[0].equalsIgnoreCase("locweather")) {
                // Not valid from console
                if (!sender.hasPermission("npcdestinations.editall.locweather") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.locweather"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc");
                        return true;
                    }

                    if (inargs.length > 2) {
                        int nIndex = Integer.parseInt(inargs[1]);
                        if (nIndex > trait.NPCLocations.size() - 1) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locweather_badargs");
                            return true;
                        }

                        if (!trait.NPCLocations.get(nIndex).managedLocation.equals("")) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, trait.NPCLocations.get(nIndex));
                            return true;
                        }

                        if (inargs[2].equalsIgnoreCase("clear")) {
                            trait.NPCLocations.get(nIndex).weatherFlag = 1;
                        } else if (inargs[2].equalsIgnoreCase("storm")) {
                            trait.NPCLocations.get(nIndex).weatherFlag = 2;
                        } else if (inargs[2].equalsIgnoreCase("any")) {
                            trait.NPCLocations.get(nIndex).weatherFlag = 0;
                        } else {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locweather_badargs");
                            return true;
                        }
                        // V1.39 -- Event
                        LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nIndex));
                        Bukkit.getServer().getPluginManager().callEvent(changedLocation);

                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        return true;
                    } else {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locweather_badargs");
                        return true;
                    }
                }
            }
            if (inargs[0].equalsIgnoreCase("enableplugin")) {
                // Not valid from console
                if (!sender.hasPermission("npcdestinations.editall.enableplugin") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.enableplugin"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc");
                        return true;
                    }

                    if (inargs.length > 1) {
                        if (trait.enabledPlugins.size() == 0) {
                            trait.enabledPlugins.add(inargs[1].toUpperCase());
                            if (destRef.getPluginManager().getPluginByName(inargs[1].toUpperCase()) != null) {
                                destRef.getPluginManager().getPluginByName(inargs[1].toUpperCase()).onEnableChanged(npc, trait, true);
                            }
                            onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                            return true;
                        }
                        if (trait.enabledPlugins.contains(inargs[1].toUpperCase())) {
                            trait.enabledPlugins.remove(inargs[1].toUpperCase());
                            if (destRef.getPluginManager().getPluginByName(inargs[1].toUpperCase()) != null) {
                                destRef.getPluginManager().getPluginByName(inargs[1].toUpperCase()).onEnableChanged(npc, trait, false);
                            }
                        } else {
                            if (destRef.getPluginManager().getPluginByName(inargs[1].toUpperCase()) != null) {
                                trait.enabledPlugins.add(inargs[1].toUpperCase());
                                if (destRef.getPluginManager().getPluginByName(inargs[1].toUpperCase()) != null) {
                                    destRef.getPluginManager().getPluginByName(inargs[1].toUpperCase()).onEnableChanged(npc, trait, true);
                                }
                            } else {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_enableplugin_badargs");
                                return true;
                            }
                        }
                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        return true;
                    } else {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_enableplugin_badargs");
                        return true;
                    }
                }
            }
            if (inargs[0].equalsIgnoreCase("locjobs")) {
                // Not valid from console
                if (!sender.hasPermission("npcdestinations.editall.locjobs") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.locjobs"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc");
                        return true;
                    }

                    if (inargs.length > 3) {
                        int nIndex = Integer.parseInt(inargs[1]);
                        if (nIndex > trait.NPCLocations.size() - 1) {
                            destRef.getMessagesManager().debugMessage(Level.ALL, "locsize");
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locjobs_badargs");
                            return true;
                        }

                        if (!trait.NPCLocations.get(nIndex).managedLocation.equals("")) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, trait.NPCLocations.get(nIndex));
                            return true;
                        }

                        // V1.39 -- Event
                        LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nIndex));
                        Bukkit.getServer().getPluginManager().callEvent(changedLocation);

                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        return true;
                    } else {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locweather_badargs");
                        return true;
                    }
                }
            }
            if (inargs[0].equalsIgnoreCase("localias")) {
                // Not valid from console
                if (!sender.hasPermission("npcdestinations.editall.localias") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.localias"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc");
                        return true;
                    }

                    if (inargs.length > 2) {
                        // Loop the NPC and see if we have an alias
                        for (Destination oDestination : trait.NPCLocations) {
                            if (oDestination.aliasName.equalsIgnoreCase(inargs[2])) {
                                // Exists
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_localias_duplicate");
                                return true;
                            }
                        }

                        if (!destRef.getUtilities().isNumeric(inargs[1])) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_localias_badargs");
                            return true;
                        }

                        int nIndex = Integer.parseInt(inargs[1]);
                        if (nIndex > trait.NPCLocations.size() - 1) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_localias_badargs");
                            return true;
                        }

                        if (!trait.NPCLocations.get(nIndex).managedLocation.equals("")) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, trait.NPCLocations.get(nIndex));
                            return true;
                        }

                        trait.NPCLocations.get(nIndex).aliasName = inargs[2];

                        // V1.39 -- Event
                        LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nIndex));
                        Bukkit.getServer().getPluginManager().callEvent(changedLocation);

                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        return true;
                    } else {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_localias_badargs");
                        return true;
                    }
                }
            }

            if (inargs[0].equalsIgnoreCase("addlocation")) {
                // Not valid from console
                if (!(sender instanceof Player)) {
                    sender.sendMessage("The command you used is not available from the console.");
                    return true;
                } else if (!sender.hasPermission("npcdestinations.editall.addlocation") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.addlocation"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait);
                        return true;
                    }

                    Player player = (Player) sender;
                    if (inargs.length > 1) {
                        String sTimeOfDay = "";
                        int nTimeOfDay = 0;
                        if (inargs[1].equalsIgnoreCase("sunrise")) {
                            nTimeOfDay = 22500;
                        } else if (inargs[1].equalsIgnoreCase("sunset")) {
                            nTimeOfDay = 13000;
                        } else {
                            if (inargs[1].matches("\\d+")) {
                                nTimeOfDay = Integer.parseInt(inargs[1]);
                                if (nTimeOfDay < 1 || nTimeOfDay > 24000) {
                                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_addlocation_badargs", trait);
                                    return true;
                                }
                            } else {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_addlocation_badargs", trait);
                                return true;
                            }
                        }
                        Destination oLoc = new Destination();
                        oLoc.location = new Location(player.getLocation().getWorld(), player.getLocation().getBlockX() + 0.5, player.getLocation().getBlockY(), player.getLocation().getBlockZ() + 0.5, Math.abs(player.getLocation()
                                .getYaw()), 0.0F);
                        oLoc.timeOfDay = nTimeOfDay;
                        oLoc.probability = 100;
                        oLoc.waitMaximum = 0;
                        oLoc.waitMinimum = 0;
                        oLoc.setWanderingDistance(0);
                        oLoc.timeMinimum = 0;
                        oLoc.timeMaximum = 0;
                        oLoc.aliasName = "";
                        oLoc.setMaxDistance(trait.maxDistanceFromDestination);

                        oLoc.locationUUID = UUID.randomUUID();
                        oLoc.arrivalCommands = new ArrayList<String>();

                        oLoc.playerSkinName = "";
                        oLoc.playerSkinUUID = "";
                        oLoc.playerSkinApplyOnArrival = false;
                        oLoc.playerSkinTextureMetadata = "";
                        oLoc.playerSkinTextureSignature = "";

                        oLoc.pauseDistance = -1;
                        oLoc.pauseTimeout = -1;
                        oLoc.pauseType = "ALL";

                        if (trait.NPCLocations == null) {
                            trait.NPCLocations = new ArrayList<Destination>();
                        }

                        // V1.39 -- Event
                        final LocationAdded newLocation = new LocationAdded(npc, oLoc);
                        Bukkit.getServer().getPluginManager().callEvent(newLocation);
                        if (newLocation.isCancelled()) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_addlocation_blocked", trait);
                            return true;
                        }
                        trait.NPCLocations.add(oLoc);

                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        return true;
                    } else {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_addlocation_badargs", trait);
                        return true;
                    }
                }
            }

            if (inargs[0].equalsIgnoreCase("removelocation")) {
                if (!sender.hasPermission("npcdestinations.editall.removelocation") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.removelocation"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait);
                        return true;
                    }
                    if (inargs.length > 1) {
                        int nIndex = Integer.parseInt(inargs[1]);
                        if (nIndex > -1 && nIndex <= trait.NPCLocations.size()) {
                            if (!trait.NPCLocations.get(nIndex).managedLocation.equals("")) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, trait.NPCLocations.get(nIndex));
                                return true;
                            }

                            // V1.39 -- Event
                            final Destination removedDest = trait.NPCLocations.get(Integer.parseInt(inargs[1]));
                            LocationDeleted removeEvent = new LocationDeleted(npc, removedDest);
                            Bukkit.getServer().getPluginManager().callEvent(removeEvent);
                            if (removeEvent.isCancelled()) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_removelocation_blocked", trait);
                                return true;
                            }
                            trait.NPCLocations.remove(Integer.parseInt(inargs[1]));
                            onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                            return true;
                        } else {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_info_nolocations", trait);
                            return true;
                        }
                    } else {
                        int nCnt = 0;
                        sender.sendMessage(ChatColor.GREEN + "Configured Locations: ");
                        Location oCurLoc = trait.GetCurrentLocation().location;
                        if (oCurLoc == null) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_info_nolocations", trait);
                        } else {
                            for (Destination oLoc : trait.NPCLocations) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_info_location", trait, oLoc);
                            }
                        }
                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        return true;
                    }
                }
            }

            if (inargs[0].equalsIgnoreCase("maxdistance")) {
                if (!sender.hasPermission("npcdestinations.editall.maxdistance") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.maxdistance"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                        return true;
                    }
                    if (inargs[1].matches("\\d+")) {
                        trait.maxDistanceFromDestination = Integer.parseInt(inargs[1]);
                    } else {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_maxdistance_badargs", trait);
                        return true;
                    }
                    onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                    return true;
                }
            }

            if (inargs[0].equalsIgnoreCase("pauseplayer")) {
                if (!sender.hasPermission("npcdestinations.editall.pauseplayer") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.pauseplayer"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                        return true;
                    }
                    if (inargs.length == 1) {
                        trait.pauseForPlayers = -1;
                        trait.pauseTimeout = -1;
                        // V1.39 -- Event
                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        return true;
                    } else if (inargs.length == 2) {
                        try {
                            trait.pauseForPlayers = Integer.parseInt(inargs[1]);
                            trait.pauseTimeout = -1;
                            onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        } catch (Exception e) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_pauseplayer_badargs", trait);
                        }
                        return true;
                    } else if (inargs.length == 3) {
                        try {
                            trait.pauseForPlayers = Integer.parseInt(inargs[1]);
                            trait.pauseTimeout = Integer.parseInt(inargs[2]);
                            onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        } catch (Exception e) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_pauseplayer_badargs", trait);
                        }
                        return true;
                    }
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_pauseplayer_badargs", trait);
                    return true;
                }
            }

            if (inargs[0].equalsIgnoreCase("blockstick")) {
                // Not valid from console
                if (!(sender instanceof Player)) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else if (!sender.hasPermission("npcdestinations.editall.blockstick") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.blockstick"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    Player player = (Player) sender;
                    ItemStack stack = new ItemStack(Material.STICK, 1);
                    ItemMeta im = stack.getItemMeta();
                    im.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eNPCDestinations &2[&fBlockStick&2]"));
                    im.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&5Add and remove allowed blocks"), ChatColor.translateAlternateColorCodes('&', "&fRight Click to add a block"), ChatColor
                            .translateAlternateColorCodes('&', "&fShift-Right Click to remove")));
                    stack.setItemMeta(im);
                    player.getInventory().addItem(new ItemStack(stack));
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_blockstick");
                    return true;
                }
            }

            if (inargs[0].equalsIgnoreCase("addblock")) {
                // Not valid from console
                if (!(sender instanceof Player)) {
                    sender.sendMessage("The command you used is not available from the console.");
                    return true;
                } else if (!sender.hasPermission("npcdestinations.editall.addblock") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.addblock"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait);
                        return true;
                    }

                    Player player = (Player) sender;
                    player.sendMessage(ChatColor.RED + " " + player.getItemInHand().getType());
                    try {
                        if (trait.AllowedPathBlocks.contains(player.getItemInHand().getType())) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_addblock_exists", trait, null, player.getItemInHand().getType());
                        } else {
                            trait.AllowedPathBlocks.add(player.getItemInHand().getType());
                        }
                    } catch (Exception err) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_addblock_badargs", trait);
                    }
                    onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                    return true;
                }
            }

            if (inargs[0].equalsIgnoreCase("removeblock")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("The command you used is not available from the console.");
                    return true;
                } else if (!sender.hasPermission("npcdestinations.editall.removeblock") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.removeblock"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                        return true;
                    }

                    if (inargs.length > 1) {
                        if (destRef.getUtilities().isNumeric(inargs[1])) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_removeblock_badargs", trait);
                        } else {
                            Material material = null;
                            try {
                                material = Material.getMaterial(inargs[1]);
                            } catch (Exception err) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_removeblock_badargs", trait);
                            }
                            if (!trait.AllowedPathBlocks.contains(material)) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_removeblock_notinlist", trait);
                            } else {
                                trait.AllowedPathBlocks.remove(material);
                                onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                            }
                        }
                    } else {
                        Player player = (Player) sender;
                        try {
                            if (!trait.AllowedPathBlocks.contains(destRef.getMcUtils().getMainHand(player).getType())) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_removeblock_notinlist", trait);
                            } else {
                                trait.AllowedPathBlocks.remove(destRef.getMcUtils().getMainHand(player).getType());
                                onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                            }
                        } catch (Exception err) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_removeblock_badargs", trait);
                        }
                    }
                    onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                    return true;
                }
            }

            if (inargs[0].equalsIgnoreCase("removeallblocks")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("The command you used is not available from the console.");
                    return true;
                } else if (!sender.hasPermission("npcdestinations.editall.removeallblocks") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.removeallblocks"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                        return true;
                    }
                    trait.AllowedPathBlocks.clear();
                    onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                    return true;
                }
            }

            if (inargs[0].equalsIgnoreCase("opengates")) {
                if (!sender.hasPermission("npcdestinations.settings") && !sender.isOp() && !isOwner) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                        return true;
                    }
                    trait.openGates = !trait.openGates;
                    onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                    return true;
                }
            }

            if (inargs[0].equalsIgnoreCase("openwooddoors")) {
                if (!sender.hasPermission("npcdestinations.editall.openwooddoors") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.openwooddoors"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                        return true;
                    }

                    trait.openWoodDoors = !trait.openWoodDoors;
                    onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                    return true;
                }
            }

            if (inargs[0].equalsIgnoreCase("openmetaldoors")) {
                if (!sender.hasPermission("npcdestinations.editall.openmetaldoors") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.openmetaldoors"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                        return true;
                    }

                    trait.openMetalDoors = !trait.openMetalDoors;
                    onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                    return true;
                }
            }

            if (inargs[0].equalsIgnoreCase("blocksunder")) {
                if (!sender.hasPermission("npcdestinations.editall.blocksunder") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.blocksunder"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                        return true;
                    }

                    if (inargs.length == 1) {
                        trait.blocksUnderSurface = 0;
                    } else {
                        trait.blocksUnderSurface = Integer.parseInt(inargs[1]);
                    }

                    onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                    return true;
                }
            }

            // ****** Location based commands
            if (inargs[0].equalsIgnoreCase("locskin")) {
                // Not valid from console
                if (!sender.hasPermission("npcdestinations.editall.locskin") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.locskin"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc");
                        return true;
                    }

                    if (inargs.length > 1) {
                        int nIndex = Integer.parseInt(inargs[1]);
                        if (nIndex > trait.NPCLocations.size() - 1) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locskin_badargs");
                            return true;
                        }

                        if (!trait.NPCLocations.get(nIndex).managedLocation.equals("")) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, trait.NPCLocations.get(nIndex));
                            return true;
                        }

                        if (inargs[2].equalsIgnoreCase("show")) {
                            if (trait.NPCLocations.get(nIndex).playerSkinUUID.trim().isEmpty() || !(npc.getEntity() instanceof Player)) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locskin_notset");
                                return true;
                            } else {
                                npc.data().remove(NPC.PLAYER_SKIN_UUID_METADATA);
                                npc.data().remove(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_METADATA);
                                npc.data().remove(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_SIGN_METADATA);
                                npc.data().remove("cached-skin-uuid-name");
                                npc.data().remove("cached-skin-uuid");
                                npc.data().remove(NPC.PLAYER_SKIN_UUID_METADATA);

                                // Set the skin
                                npc.data().set(NPC.PLAYER_SKIN_USE_LATEST, false);
                                npc.data().set("cached-skin-uuid-name", trait.NPCLocations.get(nIndex).playerSkinName);
                                npc.data().set("cached-skin-uuid", trait.NPCLocations.get(nIndex).playerSkinUUID);
                                npc.data().setPersistent(NPC.PLAYER_SKIN_UUID_METADATA, trait.NPCLocations.get(nIndex).playerSkinName);
                                npc.data().setPersistent(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_METADATA, trait.NPCLocations.get(nIndex).playerSkinTextureMetadata);
                                npc.data().setPersistent(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_SIGN_METADATA, trait.NPCLocations.get(nIndex).playerSkinTextureSignature);

                                if (npc.isSpawned()) {

                                    SkinnableEntity skinnable = npc.getEntity() instanceof SkinnableEntity ? (SkinnableEntity) npc.getEntity() : null;
                                    if (skinnable != null) {
                                        Skin.get(skinnable).applyAndRespawn(skinnable);

                                    }
                                }
                                onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                                return true;
                            }
                        } else if (inargs[2].equalsIgnoreCase("clear")) {
                            trait.NPCLocations.get(nIndex).playerSkinName = "";
                            trait.NPCLocations.get(nIndex).playerSkinUUID = "";
                            trait.NPCLocations.get(nIndex).playerSkinTextureMetadata = "";
                            trait.NPCLocations.get(nIndex).playerSkinTextureSignature = "";
                            // V1.39 -- Event
                            LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nIndex));
                            Bukkit.getServer().getPluginManager().callEvent(changedLocation);
                            onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                            return true;
                        } else if (inargs[2].equalsIgnoreCase("before")) {
                            trait.NPCLocations.get(nIndex).playerSkinApplyOnArrival = false;
                        } else if (inargs[2].equalsIgnoreCase("after")) {
                            trait.NPCLocations.get(nIndex).playerSkinApplyOnArrival = true;
                        } else {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locskin_badargs");
                            return true;
                        }

                        trait.NPCLocations.get(nIndex).playerSkinName = npc.data().get("cached-skin-uuid-name").toString();
                        trait.NPCLocations.get(nIndex).playerSkinUUID = npc.data().get("cached-skin-uuid").toString();
                        trait.NPCLocations.get(nIndex).playerSkinTextureMetadata = npc.data().get(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_METADATA).toString();
                        trait.NPCLocations.get(nIndex).playerSkinTextureSignature = npc.data().get(NPC.PLAYER_SKIN_TEXTURE_PROPERTIES_SIGN_METADATA).toString();

                        // V1.39 -- Event
                        LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nIndex));
                        Bukkit.getServer().getPluginManager().callEvent(changedLocation);
                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        return true;
                    } else {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locskin_badargs");
                        return true;
                    }
                }
            }
            if (inargs[0].equalsIgnoreCase("locinv")) {
                // Not valid from console
                if (!sender.hasPermission("npcdestinations.editall.locinv") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.locinv"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc");
                        return true;
                    }

                    if (inargs.length > 1) {
                        if (npc == null) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                            return true;
                        }
                        if (inargs.length < 2 || !destRef.getUtilities().isNumeric(inargs[1])) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locmax_badargs", trait);
                            return true;
                        }

                        int nLoc = Integer.parseInt(inargs[1]);
                        if (!trait.NPCLocations.get(nLoc).managedLocation.equals("")) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, trait.NPCLocations.get(nLoc));
                            return true;
                        }

                        Equipment npcINV = npc.getTrait(Equipment.class);
                        trait.NPCLocations.get(nLoc).itemsHead = npcINV.get(EquipmentSlot.HELMET);
                        trait.NPCLocations.get(nLoc).itemsBoots = npcINV.get(EquipmentSlot.BOOTS);
                        trait.NPCLocations.get(nLoc).itemsChest = npcINV.get(EquipmentSlot.CHESTPLATE);
                        trait.NPCLocations.get(nLoc).itemsLegs = npcINV.get(EquipmentSlot.LEGGINGS);
                        trait.NPCLocations.get(nLoc).itemsHand = npcINV.get(EquipmentSlot.HAND);

                        trait.NPCLocations.get(nLoc).itemsClear = inargs.length > 2 && inargs[2].contains("--clear");

                        if (Bukkit.getServer().getClass().getPackage().getName().startsWith("v1_9"))
                            trait.NPCLocations.get(nLoc).itemsOffhand = npcINV.get(EquipmentSlot.OFF_HAND);

                        // V1.39 -- Event
                        LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nLoc));
                        Bukkit.getServer().getPluginManager().callEvent(changedLocation);

                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        return true;
                    }
                }
            }
            if (inargs[0].equalsIgnoreCase("locmax")) {
                if (!sender.hasPermission("npcdestinations.editall.locmax") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.locmax"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                        return true;
                    }
                    if (inargs.length != 3) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locmax_badargs", trait);
                        return true;
                    }
                    try {
                        int nLoc = Integer.parseInt(inargs[1]);
                        int nDist = Integer.parseInt(inargs[2]);

                        if (!trait.NPCLocations.get(nLoc).managedLocation.equals("")) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, trait.NPCLocations.get(nLoc));
                            return true;
                        }

                        trait.NPCLocations.get(nLoc).setMaxDistance(nDist);

                        // V1.39 -- Event
                        LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nLoc));
                        Bukkit.getServer().getPluginManager().callEvent(changedLocation);
                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                    } catch (Exception e) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locmax_badargs", trait);
                    }
                    return true;
                }
            }
            if (inargs[0].equalsIgnoreCase("locpause")) {
                if (!sender.hasPermission("npcdestinations.editall.locpause") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.locpause"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                        return true;
                    }
                    if (inargs.length == 1) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locpause_badargs", trait);
                        return true;
                    }

                    int nLoc = Integer.parseInt(inargs[1]);
                    if (!trait.NPCLocations.get(nLoc).managedLocation.equals("")) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, trait.NPCLocations.get(nLoc));
                        return true;
                    }

                    if (inargs.length == 2) {
                        trait.NPCLocations.get(nLoc).pauseDistance = -1;
                        trait.NPCLocations.get(nLoc).pauseTimeout = -1;
                        trait.NPCLocations.get(nLoc).pauseType = "ALL";

                        // V1.39 -- Event
                        LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nLoc));
                        Bukkit.getServer().getPluginManager().callEvent(changedLocation);
                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        return true;
                    } else if (inargs.length == 3) {
                        try {
                            int nDist = Integer.parseInt(inargs[2]);
                            trait.NPCLocations.get(nLoc).setMaxDistance(nDist);
                            trait.NPCLocations.get(nLoc).pauseTimeout = -1;
                            trait.NPCLocations.get(nLoc).pauseType = "ALL";

                            // V1.39 -- Event
                            LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nLoc));
                            Bukkit.getServer().getPluginManager().callEvent(changedLocation);
                            onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        } catch (Exception e) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locpause_badargs", trait);
                        }
                        return true;
                    } else if (inargs.length == 4) {
                        try {
                            trait.NPCLocations.get(nLoc).pauseDistance = Integer.parseInt(inargs[2]);
                            trait.NPCLocations.get(nLoc).pauseTimeout = Integer.parseInt(inargs[3]);
                            trait.NPCLocations.get(nLoc).pauseType = "ALL";

                            // V1.39 -- Event
                            LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nLoc));
                            Bukkit.getServer().getPluginManager().callEvent(changedLocation);
                            onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        } catch (Exception e) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locpause_badargs", trait);
                        }
                        return true;
                    } else if (inargs.length == 5) {
                        try {
                            trait.NPCLocations.get(nLoc).pauseDistance = Integer.parseInt(inargs[2]);
                            trait.NPCLocations.get(nLoc).pauseTimeout = Integer.parseInt(inargs[3]);

                            if (inargs[4].equalsIgnoreCase("TRAVELING")) {
                                trait.NPCLocations.get(nLoc).pauseType = "TRAVELING";
                            } else if (inargs[4].equalsIgnoreCase("WANDERING")) {
                                trait.NPCLocations.get(nLoc).pauseType = "WANDERING";
                            } else {
                                trait.NPCLocations.get(nLoc).pauseType = "ALL";
                            }

                            // V1.39 -- Event
                            LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nLoc));
                            Bukkit.getServer().getPluginManager().callEvent(changedLocation);
                            onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        } catch (Exception e) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locpause_badargs", trait);
                        }
                        return true;
                    }
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locpause_badargs", trait);
                    return true;
                }
            }
            if (inargs[0].equalsIgnoreCase("locprob")) {
                if (!sender.hasPermission("npcdestinations.editall.locprob") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.locprob"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                        return true;
                    }

                    if (inargs.length != 5) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locprob_badargs", trait);
                        return true;
                    }
                    try {
                        int nLoc = Integer.parseInt(inargs[1]);

                        if (!trait.NPCLocations.get(nLoc).managedLocation.equals("")) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, trait.NPCLocations.get(nLoc));
                            return true;
                        }

                        int nChance = Integer.parseInt(inargs[2]);
                        int nMin = Integer.parseInt(inargs[3]);
                        int nMax = Integer.parseInt(inargs[4]);
                        trait.NPCLocations.get(nLoc).probability = nChance;
                        trait.NPCLocations.get(nLoc).timeMinimum = nMin;
                        trait.NPCLocations.get(nLoc).timeMaximum = nMax;

                        // V1.39 -- Event
                        LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nLoc));
                        Bukkit.getServer().getPluginManager().callEvent(changedLocation);

                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                    } catch (Exception e) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locprob_badargs", trait);
                    }
                    return true;
                }
            }

            if (inargs[0].equalsIgnoreCase("locwand")) {
                if (!sender.hasPermission("npcdestinations.editall.locwand") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.locwand"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                        return true;
                    }
                    if (inargs.length < 5) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locwand_badargs", trait);
                        return true;
                    }
                    try {
                        int nLoc = Integer.parseInt(inargs[1]);
                        if (!trait.NPCLocations.get(nLoc).managedLocation.equals("")) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, trait.NPCLocations.get(nLoc));
                            return true;
                        }
                        int nDist = Integer.parseInt(inargs[2]);
                        int nMin = Integer.parseInt(inargs[3]);
                        int nMax = Integer.parseInt(inargs[4]);
                        trait.NPCLocations.get(nLoc).setWanderingDistance(nDist);
                        trait.NPCLocations.get(nLoc).waitMinimum = nMin;
                        trait.NPCLocations.get(nLoc).waitMaximum = nMax;
                        trait.NPCLocations.get(nLoc).wanderingUseBlocks = false;

                        for (String sArg : inargs) {
                            if (sArg.equalsIgnoreCase("--blocks")) {
                                trait.NPCLocations.get(nLoc).wanderingUseBlocks = true;
                            }
                        }
                        // V1.39 -- Event
                        LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nLoc));
                        Bukkit.getServer().getPluginManager().callEvent(changedLocation);

                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                    } catch (Exception e) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locwand_badargs", trait);
                    }
                    return true;
                }
            }

            if (inargs[0].equalsIgnoreCase("loctime")) {
                if (!sender.hasPermission("npcdestinations.editall.loctime") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.loctime"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                        return true;
                    }

                    if (inargs.length != 3) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_loctime_badargs", trait);
                        return true;
                    }
                    int nLoc = Integer.parseInt(inargs[1]);
                    if (!trait.NPCLocations.get(nLoc).managedLocation.equals("")) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, trait.NPCLocations.get(nLoc));
                        return true;
                    }
                    int nTimeOfDay = 0;
                    if (inargs[2].equalsIgnoreCase("sunrise")) {
                        nTimeOfDay = 22500;
                    } else if (inargs[2].equalsIgnoreCase("sunset")) {
                        nTimeOfDay = 13000;
                    } else {
                        try {
                            nTimeOfDay = Integer.parseInt(inargs[2]);
                            if (nTimeOfDay < 0 || nTimeOfDay > 24000) {
                                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_loctime_badargs", trait);
                                return true;
                            }
                        } catch (Exception err) {
                            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_loctime_badargs", trait);
                            return true;
                        }
                    }
                    trait.NPCLocations.get(nLoc).timeOfDay = nTimeOfDay;
                    // V1.39 -- Event
                    LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nLoc));
                    Bukkit.getServer().getPluginManager().callEvent(changedLocation);

                    onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                    return true;
                }
            }

            if (inargs[0].equalsIgnoreCase("locloc")) {
                if (!sender.hasPermission("npcdestinations.editall.locloc") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.locloc"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                        return true;
                    }
                    int nLoc = Integer.parseInt(inargs[1]);
                    if (!trait.NPCLocations.get(nLoc).managedLocation.equals("")) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, trait.NPCLocations.get(nLoc));
                        return true;
                    }

                    if (inargs.length == 2 && (sender instanceof Player)) {
                        // Set to the users location
                        trait.NPCLocations.get(nLoc).location = ((Player) sender).getLocation().clone();
                        trait.NPCLocations.get(nLoc).location.setYaw(Math.abs(trait.NPCLocations.get(nLoc).location.getYaw()));

                        // V1.39 -- Event
                        LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nLoc));
                        Bukkit.getServer().getPluginManager().callEvent(changedLocation);

                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        return true;
                    }

                    if (inargs.length < 5) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locloc_badargs", trait);
                        return true;
                    }
                    try {
                        int nX = Integer.parseInt(inargs[2]);
                        int nY = Integer.parseInt(inargs[3]);
                        int nZ = Integer.parseInt(inargs[4]);

                        float nYaw = 0.0F;
                        float nPitch = 0.0F;
                        if (inargs.length > 5) {
                            nYaw = Float.parseFloat(inargs[5]);
                        }
                        if (inargs.length > 6) {
                            nPitch = Float.parseFloat(inargs[6]);
                        }

                        trait.NPCLocations.get(nLoc).location = new Location(trait.NPCLocations.get(nLoc).location.getWorld(), nX + 0.5, nY, nZ + 0.5, nYaw, nPitch);

                        // V1.39 -- Event
                        LocationUpdated changedLocation = new LocationUpdated(npc, trait.NPCLocations.get(nLoc));
                        Bukkit.getServer().getPluginManager().callEvent(changedLocation);

                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                    } catch (Exception e) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locloc_badargs", trait);
                    }
                    return true;
                }
            }

            if (inargs[0].equalsIgnoreCase("locsentinel") && destRef.getSentinelPlugin() != null) {
                if (!sender.hasPermission("npcdestinations.editall.locsentinel") && !sender.isOp() && !(isOwner && sender.hasPermission("npcdestinations.editown.locsentinel"))) {
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.no_permissions");
                    return true;
                } else {
                    if (npc == null) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_npc", trait, null);
                        return true;
                    }

                    if (inargs.length != 3) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locsentinel_badargs", trait);
                        return true;
                    }
                    int nLoc = Integer.parseInt(inargs[1]);
                    if (!trait.NPCLocations.get(nLoc).managedLocation.equals("")) {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_managed", trait, trait.NPCLocations.get(nLoc));
                        return true;
                    }

                    if (inargs[2].equalsIgnoreCase("set")) {

                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locsentinel_stored", trait);
                        return true;
                    } else if (inargs[2].equalsIgnoreCase("get")) {

                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locsentinel_recalled", trait);
                        return true;
                    } else if (inargs[2].equalsIgnoreCase("clear")) {

                        onCommand(sender, cmd, cmdLabel, new String[]{"info", "--npc", Integer.toString(npc.getId())});
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locsentinel_cleared", trait);
                        return true;
                    } else {
                        destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_locsentinel_badargs", trait);
                        return true;
                    }
                }
            }

            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.invalid_command");
            onCommand(sender, cmd, cmdLabel, new String[]{"help"});
            return true; // do this if you didn't handle the command.
        }

        return true;
    }
}
