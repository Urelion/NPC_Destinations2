package net.livecar.nuttyworks.npc_destinations;

import lombok.Getter;
import lombok.Setter;
import net.citizensnpcs.Citizens;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.trait.waypoint.Waypoints;
import net.livecar.nuttyworks.npc_destinations.bridges.MCUtil_1_18_R1R2;
import net.livecar.nuttyworks.npc_destinations.bridges.MCUtilsBridge;
import net.livecar.nuttyworks.npc_destinations.citizens.CitizensProcessing;
import net.livecar.nuttyworks.npc_destinations.citizens.CitizensWaypointProvider;
import net.livecar.nuttyworks.npc_destinations.citizens.NPCDestinationsTrait;
import net.livecar.nuttyworks.npc_destinations.listeners.OnCitizensEvents;
import net.livecar.nuttyworks.npc_destinations.listeners.OnPlayerInteractEvent;
import net.livecar.nuttyworks.npc_destinations.listeners.OnPlayerJoinLeaveEvent;
import net.livecar.nuttyworks.npc_destinations.listeners.commands.CommandManager;
import net.livecar.nuttyworks.npc_destinations.listeners.commands.CommandsLocation;
import net.livecar.nuttyworks.npc_destinations.listeners.commands.CommandsNPC;
import net.livecar.nuttyworks.npc_destinations.listeners.commands.Commands_Plugin;
import net.livecar.nuttyworks.npc_destinations.messages.JSONChat;
import net.livecar.nuttyworks.npc_destinations.messages.LanguageManager;
import net.livecar.nuttyworks.npc_destinations.messages.MessagesManager;
import net.livecar.nuttyworks.npc_destinations.metrics.BStat_Metrics;
import net.livecar.nuttyworks.npc_destinations.particles.PlayParticleInterface;
import net.livecar.nuttyworks.npc_destinations.particles.PlayParticle_1_18_R1R2;
import net.livecar.nuttyworks.npc_destinations.pathing.AStarPathFinder;
import net.livecar.nuttyworks.npc_destinations.plugins.Plugin_Manager;
import net.livecar.nuttyworks.npc_destinations.plugins.timemanager.DestinationsTimeManager;
import net.livecar.nuttyworks.npc_destinations.plugins.timemanager.realworldtime.DestinationsRealWorldTimeManager;
import net.livecar.nuttyworks.npc_destinations.thirdpartyplugins.betonquest.BetonQuest_Interface;
import net.livecar.nuttyworks.npc_destinations.thirdpartyplugins.jobsreborn.JobsReborn_Plugin;
import net.livecar.nuttyworks.npc_destinations.thirdpartyplugins.lightapi.LightAPI_Plugin;
import net.livecar.nuttyworks.npc_destinations.thirdpartyplugins.plotsquared.PlotSquared;
import net.livecar.nuttyworks.npc_destinations.thirdpartyplugins.plotsquared.PlotSquared_Plugin_V6;
import net.livecar.nuttyworks.npc_destinations.thirdpartyplugins.sentinel.Sentinel_Plugin;
import net.livecar.nuttyworks.npc_destinations.utilities.CitizensUtilities;
import net.livecar.nuttyworks.npc_destinations.utilities.Utilities;
import net.livecar.nuttyworks.npc_destinations.worldguard.WorldGuardInterface;
import net.livecar.nuttyworks.npc_destinations.worldguard.WorldGuard_7_0_7;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Getter
@Setter
public class DestinationsPlugin extends org.bukkit.plugin.java.JavaPlugin implements org.bukkit.event.Listener {

    @Getter
    private static DestinationsPlugin instance;

    private String currentLanguage = "en_def";
    private Level debugLogLevel = Level.OFF;
    private List<DebugTarget> debugTargets;
    private JSONChat jsonChat = null;
    private int maxDistance = 500;
    private int entityRadius = 47 * 47;

    private FileConfiguration defaultConfig;
    private File languagePath;
    private File loggingPath;

    private Plugin_Manager pluginManager = null;
    private LanguageManager languageManager = null;
    private MessagesManager messagesManager = null;
    private CommandManager commandManager = null;
    private AStarPathFinder aStarPathFinder = null;
    private CitizensProcessing citizensProcessing = null;
    private DestinationsTimeManager timeManager = null;
    private Utilities utilities = null;

    private MCUtilsBridge mcUtils = null;
    private PlayParticleInterface particleManager = null;

    private Citizens citizensPlugin = null;
    private PlotSquared plotSquaredPlugin = null;
    private BetonQuest_Interface betonQuestPlugin = null;
    private WorldGuardInterface worldGuardPlugin = null;
    private LightAPI_Plugin lightAPIPlugin = null;
    private JobsReborn_Plugin jobsRebornPlugin = null;
    private Sentinel_Plugin sentinelPlugin = null;

    public void onLoad() {
        instance = this;
        this.utilities = new Utilities(this);

        // Register custom WorldGuard flags
        Plugin worldGuardPlugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if (worldGuardPlugin == null) {
            getServer().getLogger().log(Level.WARNING, "WorldGuard not found, custom flags will not be enabled");
        } else {
            this.worldGuardPlugin = new WorldGuard_7_0_7(this);
            this.worldGuardPlugin.registerFlags();
        }

        if (getServer().getPluginManager().getPlugin("Quests") != null) {
            //Write out the quests addon to the quests modules folder.
            if (new File(this.getDataFolder().getParentFile(), "/Quests/modules").exists())
                exportFile(new File(this.getDataFolder().getParentFile(), "/Quests/modules"), "NPCDestinations_Quests-2.3.0.jar", true);
        }
    }

    public void onEnable() {
        // Setup defaults
        this.debugTargets = new ArrayList<>();
        this.languageManager = new LanguageManager(this);
        this.messagesManager = new MessagesManager(this);
        this.pluginManager = new Plugin_Manager(this);
        this.commandManager = new CommandManager(this);
        this.citizensProcessing = new CitizensProcessing(this);

        // Setup default paths in the storage folder.
        languagePath = new File(this.getDataFolder(), "/Languages/");
        loggingPath = new File(this.getDataFolder(), "/Logs/");

        // Generate the default folders and files.
        getDefaultConfigs();

        // Get languages
        this.languageManager.loadLanguages();

        // Init Default settings
        if (this.defaultConfig.contains("language")) this.currentLanguage = this.defaultConfig.getString("language");
        if (this.getCurrentLanguage().equalsIgnoreCase("en-default")) this.currentLanguage = "en_def";

        if (this.defaultConfig.contains("max-distance"))
            this.maxDistance = this.defaultConfig.getInt("max-distance", 500);
        if (this.defaultConfig.contains("max-distance"))
            this.maxDistance = this.defaultConfig.getInt("max-distance", 500);

        // Register commands
        this.commandManager.registerCommandClass(Commands_Plugin.class);
        this.commandManager.registerCommandClass(CommandsNPC.class);
        this.commandManager.registerCommandClass(CommandsLocation.class);

        // Right now I'm only going to support the latest Minecraft versions
        if (Bukkit.getServer().getClass().getPackage().getName().contains("v1_18")) {
            this.particleManager = new PlayParticle_1_18_R1R2();
            this.mcUtils = new MCUtil_1_18_R1R2();
            this.messagesManager.consoleMessage(this, "destinations", "console_messages.plugin_version", getServer().getVersion().substring(getServer().getVersion().indexOf('(')));
        } else {
            this.messagesManager.consoleMessage(this, "destinations", "console_messages.plugin_unknownversion", Bukkit.getServer().getClass().getPackage().getName());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Determine the time engine
        String timePlugin = this.getConfig().getString("timeplugin", "default");
        if ("REALWORLD".equals(timePlugin.toUpperCase())) {
            this.timeManager = new DestinationsRealWorldTimeManager();
        } else {
            this.timeManager = new DestinationsTimeManager();
        }

        this.aStarPathFinder = new AStarPathFinder(this);

        // Init links to other plugins
        if (getServer().getPluginManager().getPlugin("Citizens") == null || getServer().getPluginManager().getPlugin("Citizens").isEnabled() == false || !(getServer().getPluginManager().getPlugin("Citizens") instanceof Citizens)) {
            this.messagesManager.debugMessage(Level.CONFIG, "nuNPCDestinations.onEnable()|CitizensNotFound");
            this.messagesManager.consoleMessage(this, "destinations", "Console_Messages.citizens_notfound");
            getServer().getPluginManager().disablePlugin(this);
            return;
        } else {
            this.citizensPlugin = (Citizens) getServer().getPluginManager().getPlugin("Citizens");
            this.messagesManager.consoleMessage(this, "destinations", "Console_Messages.citizens_found", getCitizensPlugin().getDescription().getVersion());
        }

        if (getServer().getPluginManager().getPlugin("BetonQuest") == null) {
            this.messagesManager.debugMessage(Level.CONFIG, "nuNPCDestinations.onEnable()|BetonQuest_NotFound");
            this.messagesManager.consoleMessage(this, "destinations", "Console_Messages.betonquest_notfound");
        } else {
            String versionString = getServer().getPluginManager().getPlugin("BetonQuest").getDescription().getVersion();
            if (versionString.startsWith("1.")) {
                this.betonQuestPlugin = new net.livecar.nuttyworks.npc_destinations.thirdpartyplugins.betonquest.v1.BetonQuest_Plugin(this);
            } else {
                this.betonQuestPlugin = new net.livecar.nuttyworks.npc_destinations.thirdpartyplugins.betonquest.v2.BetonQuest_Plugin(this);
            }
            this.messagesManager.consoleMessage(this, "destinations", "Console_Messages.betonquest_found", versionString);
            this.messagesManager.debugMessage(Level.CONFIG, "nuNPCDestinations.onEnable()|BetonQuestFound");
        }

        if (getServer().getPluginManager().getPlugin("LightAPI") == null) {
            this.messagesManager.debugMessage(Level.CONFIG, "nuNPCDestinations.onEnable()|LightAPI_NotFound");
            this.messagesManager.consoleMessage(this, "destinations", "Console_Messages.lightapi_notfound");
        } else {
            this.lightAPIPlugin = new LightAPI_Plugin(this);
            this.messagesManager.debugMessage(Level.CONFIG, "nuNPCDestinations.onEnable()|LightAPI_Found");
            this.messagesManager.consoleMessage(this, "destinations", "Console_Messages.lightapi_found", getServer().getPluginManager().getPlugin("LightAPI").getDescription().getVersion());
        }

        // 1.31 - Jobs Reborn
        if (getServer().getPluginManager().getPlugin("Jobs") == null) {
            this.messagesManager.debugMessage(Level.CONFIG, "nuNPCDestinations.onEnable()|JobsReborn_NotFound");
            this.messagesManager.consoleMessage(this, "destinations", "Console_Messages.jobsreborn_notfound");
        } else {
            this.jobsRebornPlugin = new JobsReborn_Plugin(this);
            this.messagesManager.debugMessage(Level.CONFIG, "nuNPCDestinations.onEnable()|JobsReborn_Found");
            this.messagesManager.consoleMessage(this, "destinations", "Console_Messages.jobsreborn_found");
        }

        // 1.39 - Sentinel!
        if (getServer().getPluginManager().getPlugin("Sentinel") == null) {
            this.messagesManager.debugMessage(Level.CONFIG, "nuNPCDestinations.onEnable()|Sentinel_NotFound");
            this.messagesManager.consoleMessage(this, "sentinel", "Console_Messages.sentinel_notfound");
        } else {
            this.sentinelPlugin = new Sentinel_Plugin(this);
            this.messagesManager.consoleMessage(this, "sentinel", "Console_Messages.sentinel_found", sentinelPlugin.getVersionString());
        }

        // 2.1.8 - Plotsquared compliance
        if (getServer().getPluginManager().getPlugin("PlotSquared") == null) {
            this.messagesManager.debugMessage(Level.CONFIG, "nuNPCDestinations.onEnable()|plotsquared_NotFound");
            this.messagesManager.consoleMessage(this, "destinations", "Console_Messages.plotsquared_notfound");
        } else {
            if (this.plotSquaredPlugin == null) {
                try {
                    Class.forName("com.github.intellectualsites.plotsquared.plot.flag.Flag");
                    this.plotSquaredPlugin = new PlotSquared_Plugin_V6();
                    this.messagesManager.consoleMessage(this, "destinations", "Console_Messages.plotsquared_found", "V4-" + getServer().getPluginManager().getPlugin("PlotSquared").getDescription().getVersion());
                } catch (Exception ignored) {
                }
            }
            this.messagesManager.debugMessage(Level.CONFIG, "nuNPCDestinations.onEnable()|plotsquared_Found");
        }

        // Currently this actually does nothing lol, because there's no events in the WorldGuard interface implementation
//        if (getServer().getPluginManager().getPlugin("WorldGuard") == null) {
//            this.messagesManager.consoleMessage(this, "destinations", "console_messages.worldguard_notfound");
//        } else {
//                this.messagesManager.consoleMessage(this, "destinations", "console_messages.worldguard_found", getServer().getPluginManager().getPlugin("WorldGuard").getDescription().getVersion());
//                this.worldGuardPlugin.registerEvents();
//        }

        this.jsonChat = new JSONChat(this);

        // Register your trait with Citizens.
        CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(NPCDestinationsTrait.class).withName("npcdestinations"));

        // Register events
        registerEventListeners();

        Waypoints.registerWaypointProvider(CitizensWaypointProvider.class, "npcdestinations");

        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> aStarPathFinder.checkStatus(), 30L, 5L);

        // 1.34 - Citizens save.yml backup monitor
        final CitizensUtilities backupClass = new CitizensUtilities(this);

        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            try {
                backupClass.BackupConfig(false);
            } catch (Exception ignored) {
            }
        }, 1200L, 1200L);

        final BStat_Metrics statsReporting = new BStat_Metrics(this);

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, statsReporting::Start, 500L);
    }

    private void registerEventListeners() {
        Bukkit.getPluginManager().registerEvents(new OnCitizensEvents(this), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerJoinLeaveEvent(this), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerInteractEvent(this), this);
    }

    public void onDisable() {
        this.getMessagesManager().debugMessage(Level.CONFIG, "nuNPCDestinations.onDisable()|Stopping Internal Processes");
        Bukkit.getServer().getScheduler().cancelTasks(this);
        this.aStarPathFinder.setCurrentTask(null);
        this.aStarPathFinder.getPathQueue().clear();
        this.aStarPathFinder = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] inargs) {
        if (cmd.getName().equalsIgnoreCase("npcdest") | cmd.getName().equalsIgnoreCase("nd")) {
            return getCommandManager().onCommand(sender, inargs);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String cmdLabel, String[] inargs) {
        if (cmd.getName().equalsIgnoreCase("npcdest") | cmd.getName().equalsIgnoreCase("nd")) {
            return getCommandManager().onTabComplete(sender, inargs);
        }
        return new ArrayList<>();
    }

    public Boolean hasPermissions(CommandSender player, String[] permissions) {
        for (String perm : permissions) {
            if (hasPermissions(player, perm)) return true;
        }
        return false;
    }

    public Boolean hasPermissions(CommandSender player, String permission) {
        if (player instanceof Player) {
            if (player.isOp()) return true;

            if (permission.toLowerCase().startsWith("npcdestinations.editall.") && player.hasPermission("npcdestinations.editall.*"))
                return true;

            if (permission.toLowerCase().startsWith("npcdestinations.editown.") && player.hasPermission("npcdestinations.editown.*"))
                return true;

            return player.hasPermission(permission);
        }
        return true;
    }

    private void getDefaultConfigs() {
        // Create the default folders
        if (!this.getDataFolder().exists()) this.getDataFolder().mkdirs();
        if (!languagePath.exists()) languagePath.mkdirs();
        if (!loggingPath.exists()) loggingPath.mkdirs();

        // Validate that the default package is in the MountPackages folder. If
        // not, create it.
        if (!(new File(getDataFolder(), "config.yml").exists())) exportFile(getDataFolder(), "config.yml", false);
        exportFile(languagePath, "en_def-destinations.yml", true);
        exportFile(languagePath, "en_def-jobsreborn.yml", true);
        exportFile(languagePath, "en_def-sentinel.yml", true);

        this.defaultConfig = this.utilities.loadConfiguration(new File(this.getDataFolder(), "config.yml"));
    }

    private void exportFile(File path, String filename, boolean overwrite) {
        if (this.messagesManager != null)
            this.messagesManager.debugMessage(Level.FINEST, "nuDestinationsPlugin.exportFile()|");
        File fileConfig = new File(path, filename);
        if (!fileConfig.isDirectory()) {
            try {
                exportFile(filename, fileConfig, overwrite);
            } catch (IOException e1) {
                if (this.messagesManager != null) {
                    this.messagesManager.debugMessage(Level.SEVERE, "nuDestinationsPlugin.exportFile()|FailedToExtractFile(" + filename + ")");
                    this.messagesManager.logToConsole(this, " Failed to extract default file (" + filename + ")");
                }
            }
        }
    }

    private void exportFile(String source, File destination, boolean overwrite) throws IOException {
        //We overwrite the files anyway
        if (!overwrite && destination.exists()) return;

        if (destination.exists()) destination.delete();

        if (!destination.getParentFile().exists()) throw new IOException("Folders missing.");

        if (!destination.createNewFile()) throw new IOException("Failed to create a new file");

        URL sourceURL = getClass().getResource("/" + source);
        if (sourceURL == null) throw new IOException("Missing resource file");

        byte[] ioBuffer = new byte[1024];
        int bytesRead = 0;

        try {
            URLConnection inputConnection = sourceURL.openConnection();
            inputConnection.setUseCaches(false);

            InputStream fileIn = inputConnection.getInputStream();
            OutputStream fileOut = new FileOutputStream(destination);

            while ((bytesRead = fileIn.read(ioBuffer)) > 0) {
                fileOut.write(ioBuffer, 0, bytesRead);
            }

            fileOut.flush();
            fileOut.close();
            fileIn.close();
        } catch (Exception error) {
            throw new IOException("Failure exporting file");
        }
    }
}
