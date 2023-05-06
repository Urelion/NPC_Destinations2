package net.livecar.nuttyworks.npc_destinations.listeners.commands;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;
import net.livecar.nuttyworks.npc_destinations.DebugTarget;
import net.livecar.nuttyworks.npc_destinations.DestinationsPlugin;
import net.livecar.nuttyworks.npc_destinations.utilities.CitizensUtilities;
import net.livecar.nuttyworks.npc_destinations.citizens.NPCDestinationsTrait;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.logging.Level;

public class Commands_Plugin {
    @CommandInfo(
            name = "reload",
            group = "Plugin Commands",
            languageFile = "destinations",
            helpMessage = "command_reload_help",
            arguments = {""},
            permission = {"npcdestinations.reload"},
            allowConsole = true,
            minArguments = 0,
            maxArguments = 0
    )
    public boolean npcDest_ReloadConfig(DestinationsPlugin destinationsRef, CommandSender sender, NPC npc, String[] inargs, boolean isOwner, NPCDestinationsTrait destTrait) {
        destinationsRef.getLanguageManager().loadLanguages(true);
        if (sender instanceof Player)
            destinationsRef.getMessagesManager().sendMessage("destinations", sender, "messages.configs_reloaded", ((Player) sender).getDisplayName());
        if (!(sender instanceof Player))
            destinationsRef.getMessagesManager().sendMessage("destinations", sender, "console_messages.configs_reloaded");
        return true;
    }

    @CommandInfo(
            name = "version",
            group = "Plugin Commands",
            languageFile = "destinations",
            helpMessage = "command_version_help",
            arguments = {""},
            permission = {"npcdestinations.version"},
            allowConsole = true,
            minArguments = 0,
            maxArguments = 0
    )
    public boolean npcDest_CurrentVersion(DestinationsPlugin destinationsRef, CommandSender sender, NPC npc, String[] inargs, boolean isOwner, NPCDestinationsTrait destTrait) {
        // Configuration Commands
        if (sender instanceof Player) {
            destinationsRef.getMessagesManager().sendJsonRaw((Player) sender, "[{\"text\":\"--\",\"color\":\"gold\"},{\"text\":\"[\",\"color\":\"white\"},{\"text\":\"NPC Destinations By Nutty101\",\"color\":\"green\"},{\"text\":\"]\",\"color\":\"white\"},{\"text\":\"-----------------------\",\"color\":\"gold\"}]");
            destinationsRef.getMessagesManager().sendJsonRaw((Player) sender, "[{\"text\":\"Version\",\"color\":\"green\"},{\"text\":\":\",\"color\":\"yellow\"},{\"text\":\" " + destinationsRef.getDescription().getVersion() + " \",\"color\":\"white\"}]");
            destinationsRef.getMessagesManager().sendJsonRaw((Player) sender, "[{\"text\":\"Plugin Link\",\"color\":\"dark_green\"},{\"text\":\": \",\"color\":\"yellow\"},{\"text\":\"https://www.spigotmc.org/resources/nunpcdestinations-create-living-npcs-1-8-3-1-11.13863/\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://www.spigotmc.org/resources/nunpcdestinations-create-living-npcs-1-8-3-1-11.13863/\",\"color\":\"white\"}}]");
        } else {
            sender.sendMessage("--[NPC Destinations By Nutty101]-----------------------");
            sender.sendMessage("Version: " + destinationsRef.getDescription().getVersion());
            sender.sendMessage("Plugin Link: https://www.spigotmc.org/resources/nunpcdestinations-create-living-npcs-1-8-3-1-11.13863/");
        }
        return true;
    }

    @CommandInfo(
            name = "backup",
            group = "Plugin Commands",
            languageFile = "destinations",
            helpMessage = "command_backup_help",
            arguments = {""},
            permission = {"npcdestinations.backup"},
            allowConsole = true,
            minArguments = 0,
            maxArguments = 0
    )
    public boolean npcDest_Backup(DestinationsPlugin destinationsRef, CommandSender sender, NPC npc, String[] inargs, boolean isOwner, NPCDestinationsTrait destTrait) {
        CitizensUtilities citizensUtils = new CitizensUtilities(destinationsRef);
        citizensUtils.BackupConfig(true);
        destinationsRef.getMessagesManager().sendMessage("destinations", sender, "messages.backup_command");
        return true;

    }

    @CommandInfo(
            name = "enginestatus",
            group = "Plugin Commands",
            languageFile = "destinations",
            helpMessage = "command_enginestatus_help",
            arguments = {""},
            permission = {"npcdestinations.enginestatus"},
            allowConsole = true,
            minArguments = 0,
            maxArguments = 0
    )
    public boolean npcDest_EngineStatus(DestinationsPlugin destinationsRef, CommandSender sender, NPC npc, String[] inargs, boolean isOwner, NPCDestinationsTrait destTrait) {
        if ((destinationsRef.getAStarPathFinder().getCurrentTask() == null || destinationsRef.getAStarPathFinder().getCurrentTask().getNpc() == null)
                && destinationsRef.getAStarPathFinder().getPathQueue().size() == 0) {
            destinationsRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_enginestatus_idle");
        } else if ((destinationsRef.getAStarPathFinder().getCurrentTask() == null || destinationsRef.getAStarPathFinder().getCurrentTask().getNpc() == null)
                && destinationsRef.getAStarPathFinder().getPathQueue().size() > 0) {
            destinationsRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_enginestatus_idle_queue");
        } else if ((destinationsRef.getAStarPathFinder().getCurrentTask() != null || destinationsRef.getAStarPathFinder().getCurrentTask().getNpc() != null)
                && destinationsRef.getAStarPathFinder().getPathQueue().size() == 0) {
            destinationsRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_enginestatus_processing_noqueue");
        } else if ((destinationsRef.getAStarPathFinder().getCurrentTask() != null || destinationsRef.getAStarPathFinder().getCurrentTask().getNpc() != null)
                && destinationsRef.getAStarPathFinder().getPathQueue().size() > 0) {
            destinationsRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_enginestatus_processing_queue");
        }
        return true;
    }

    @CommandInfo(
            name = "allstatus",
            group = "Plugin Commands",
            languageFile = "destinations",
            helpMessage = "command_allstatus_help",
            arguments = {""},
            permission = {"npcdestinations.allstatus"},
            allowConsole = true,
            minArguments = 0,
            maxArguments = 1
    )
    public boolean npcDest_AllStatus(DestinationsPlugin destinationsRef, CommandSender sender, NPC npc, String[] inargs, boolean isOwner, NPCDestinationsTrait destTrait) {
        sender.sendMessage(ChatColor.GOLD + "----- " + destinationsRef.getDescription().getName() + " ----- V "
                + destinationsRef.getDescription().getVersion());

        String messageLevel = "messages";
        if (sender instanceof Player) {
            messageLevel = "messages";
        } else {
            messageLevel = "console_messages";
        }

        for (NPC npcItem : CitizensAPI.getNPCRegistry()) {
            if ((npcItem != null) && (npcItem.hasTrait(NPCDestinationsTrait.class))) {
                if (inargs.length > 1) {
                    String npcName = npcItem.getName().toLowerCase();
                    if (!npcName.contains(inargs[1].toLowerCase()))
                        continue;
                }

                if (!npcItem.isSpawned()) {
                    destinationsRef.getMessagesManager().sendMessage("destinations", sender,
                            messageLevel + ".commands_allstatus_notspawned", npcItem);
                } else {
                    NPCDestinationsTrait oCurTrait = npcItem.getTrait(NPCDestinationsTrait.class);
                    switch (oCurTrait.getRequestedAction()) {
                        case NORMAL_PROCESSING:
                            switch (oCurTrait.getCurrentAction()) {
                                case IDLE:
                                    destinationsRef.getMessagesManager().sendMessage("destinations", sender,
                                            messageLevel + ".commands_allstatus_idle", oCurTrait);
                                    break;
                                case IDLE_FAILED:
                                    destinationsRef.getMessagesManager().sendMessage("destinations", sender,
                                            messageLevel + ".commands_allstatus_idle_failure", oCurTrait);
                                    break;
                                case PATH_HUNTING:
                                    destinationsRef.getMessagesManager().sendMessage("destinations", sender,
                                            messageLevel + ".commands_allstatus_hunting", oCurTrait);
                                    break;
                                case RANDOM_MOVEMENT:
                                    destinationsRef.getMessagesManager().sendMessage("destinations", sender,
                                            messageLevel + ".commands_allstatus_random", oCurTrait);
                                    break;
                                case TRAVELING:
                                    destinationsRef.getMessagesManager().sendMessage("destinations", sender,
                                            messageLevel + ".commands_allstatus_pending", oCurTrait);
                                    break;
                                case PATH_FOUND:
                                    destinationsRef.getMessagesManager().sendMessage("destinations", sender,
                                            messageLevel + ".commands_allstatus_path_found", oCurTrait);
                                    break;
                                default:
                                    destinationsRef.getMessagesManager().sendMessage("destinations", sender,
                                            messageLevel + ".commands_allstatus_pending", oCurTrait);
                                    break;
                            }
                            break;
                        case NO_PROCESSING:
                            destinationsRef.getMessagesManager().sendMessage("destinations", sender,
                                    messageLevel + ".commands_allstatus_noprocessing", oCurTrait);
                            break;
                        case SET_LOCATION:
                            destinationsRef.getMessagesManager().sendMessage("destinations", sender,
                                    messageLevel + ".commands_allstatus_setlocation", oCurTrait);
                            break;
                        default:
                            break;

                    }

                }
            }
        }
        return true;
    }

    @CommandInfo(
            name = "debuglog",
            group = "Plugin Commands",
            languageFile = "destinations",
            helpMessage = "",
            arguments = {""},
            permission = {"npcdestinations.debuglog"},
            allowConsole = true,
            minArguments = 0,
            maxArguments = 1
    )
    public boolean npcDest_DebugLog(DestinationsPlugin destinationsRef, CommandSender sender, NPC npc, String[] inargs, boolean isOwner, NPCDestinationsTrait destTrait) {
        if (Level.parse(inargs[1]) != null && !destinationsRef.getUtilities().isNumeric(inargs[1])) {

            if (!"OFF SEVERE WARNING INFO CONFIG FINE FINER FINEST ALL".contains(inargs[1].toUpperCase())) {
                destinationsRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_off");
                destinationsRef.setDebugLogLevel(Level.OFF);
                return true;
            }
            destinationsRef.setDebugLogLevel(Level.parse(inargs[1].toUpperCase()));

            if (destinationsRef.getDebugLogLevel() == Level.OFF)
                destinationsRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_off");
            else
                destinationsRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_on");

            return true;
        }
        return false;
    }

    @CommandInfo(
            name = "debug",
            group = "Plugin Commands",
            languageFile = "destinations",
            helpMessage = "command_debug_help",
            arguments = {"#|all|*|list"},
            permission = {"npcdestinations.debug.set", "npcdestinations.debug.own", "npcdestinations.debug.all"},
            allowConsole = false,
            minArguments = 0,
            maxArguments = 1
    )
    public boolean npcDest_Debug(DestinationsPlugin destRef, CommandSender sender, NPC npc, String[] inargs, boolean isOwner, NPCDestinationsTrait destTrait) {
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
                        destRef.getMessagesManager().sendMessage("destinations", sender,
                                "messages.commands_debug_removed", "*");
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
                if ((debugOutput.targetSender instanceof Player) && ((Player) debugOutput.targetSender)
                        .getUniqueId().equals(((Player) sender).getUniqueId())) {
                    for (int cnt = 0; cnt < debugOutput.getTargets().size(); cnt++) {
                        if (debugOutput.getTargets().get(cnt).equals(selectedNPC.getId())) {
                            debugOutput.removeNPCTarget(selectedNPC.getId());
                            if (debugOutput.getTargets().size() == 0) {
                                destRef.getMessagesManager().sendMessage("destinations", sender,
                                        "messages.commands_debug_off");
                                debugOutput.clearDebugBlocks();
                                destRef.getDebugTargets().remove(debugOutput);
                            } else {
                                destRef.getMessagesManager().sendMessage("destinations", sender,
                                        "messages.commands_debug_removed", selectedNPC.getFullName());
                            }
                            return true;
                        }
                    }

                    debugOutput.addNPCTarget(selectedNPC.getId());
                    destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_added",
                            selectedNPC.getFullName());
                    return true;
                }
            }
            DebugTarget dbgTarget = new DebugTarget(sender, selectedNPC.getId());
            destRef.getDebugTargets().add(dbgTarget);
            destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_added",
                    selectedNPC.getFullName());
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
                        destRef.getMessagesManager().sendMessage("destinations", sender,
                                "messages.commands_debug_off");
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
                                        destRef.getMessagesManager().sendMessage("destinations", sender,
                                                "messages.commands_debug_off");
                                        destRef.getDebugTargets().get(target).clearDebugBlocks();
                                        destRef.getDebugTargets().remove(target);
                                        return true;
                                    } else {
                                        debugOutput.getTargets().remove(cnt);
                                        if (debugOutput.getTargets().size() == 0) {
                                            destRef.getMessagesManager().sendMessage("destinations", sender,
                                                    "messages.commands_debug_off");
                                            destRef.getDebugTargets().get(target).clearDebugBlocks();
                                            destRef.getDebugTargets().remove(target);
                                        } else {
                                            destRef.getMessagesManager().sendMessage("destinations", sender,
                                                    "messages.commands_debug_removed", npc.getFullName());
                                        }
                                        return true;
                                    }
                                }
                            }
                        } else if (debugOutput.getTargets().size() == 0) {
                            destRef.getMessagesManager().sendMessage("destinations", sender,
                                    "messages.commands_debug_removed", "*");
                            destRef.getDebugTargets().get(target).clearDebugBlocks();
                            destRef.getDebugTargets().remove(target);
                            return true;
                        } else {
                            destRef.getMessagesManager().sendMessage("destinations", sender,
                                    "messages.commands_debug_added", npc.getFullName());
                            DebugTarget debugger = new DebugTarget(sender, npc.getId());
                            destRef.getDebugTargets().add(debugger);
                            return true;
                        }
                    }
                }
                destRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_debug_added", npc
                        .getFullName());
                DebugTarget debugOutput = new DebugTarget(sender, npc.getId());
                destRef.getDebugTargets().add(debugOutput);
                return true;
            }
        }
    }

    @CommandInfo(
            name = "blockstick",
            group = "Plugin Commands",
            languageFile = "destinations",
            helpMessage = "command_blockstick_help",
            arguments = {""},
            permission = {"npcdestinations.editall.blockstick", "npcdestinations.editown.blockstick"},
            allowConsole = false,
            minArguments = 0,
            maxArguments = 0
    )
    public boolean npcDest_BlockStick(DestinationsPlugin destinationsRef, CommandSender sender, NPC npc, String[] inargs, boolean isOwner, NPCDestinationsTrait destTrait) {
        Player player = (Player) sender;
        ItemStack stack = new ItemStack(Material.STICK, 1);
        ItemMeta im = stack.getItemMeta();
        im.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eNPCDestinations &2[&fBlockStick&2]"));
        im.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&5Add and remove allowed blocks"),
                ChatColor.translateAlternateColorCodes('&', "&fRight Click to add a block"), ChatColor
                        .translateAlternateColorCodes('&', "&fShift-Right Click to remove")));
        stack.setItemMeta(im);
        player.getInventory().addItem(new ItemStack(stack));
        destinationsRef.getMessagesManager().sendMessage("destinations", sender, "messages.commands_blockstick");
        return true;
    }
}