package net.livecar.nuttyworks.npc_destinations.messages;

import net.livecar.nuttyworks.npc_destinations.DestinationsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.HashMap;

public class LanguageManager {
    public HashMap<String, FileConfiguration> languageStorage = new HashMap<>();
    private DestinationsPlugin destRef = null;

    public LanguageManager(DestinationsPlugin storageRef) {
        destRef = storageRef;
    }

    public void loadLanguages() {
        loadLanguages(false);
    }

    public void loadLanguages(boolean silent) {
        if (languageStorage == null)
            languageStorage = new HashMap<>();
        languageStorage.clear();

        File[] languageFiles = destRef.getLanguagePath().listFiles(file -> file.getName().endsWith(".yml"));

        for (File ymlFile : languageFiles) {
            FileConfiguration oConfig = destRef.getUtilities().loadConfiguration(ymlFile);
            if (oConfig == null) {
                destRef.getMessagesManager().logToConsole(destRef, "Problem loading language file (" + ymlFile.getName().toLowerCase().replace(".yml", "") + ")");
            } else {
                languageStorage.put(ymlFile.getName().toLowerCase().replace(".yml", ""), oConfig);
                if (!silent) {
                    // destRef.getMessageManager.consoleMessage(destRef,"destinations","console_messages.plugin_langloaded",ymlFile.getName().toLowerCase().replace(".yml","")
                    // );
                }

            }
        }
    }
}
