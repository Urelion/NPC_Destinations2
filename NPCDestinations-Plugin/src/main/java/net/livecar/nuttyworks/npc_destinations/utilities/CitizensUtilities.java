package net.livecar.nuttyworks.npc_destinations.utilities;

import net.livecar.nuttyworks.npc_destinations.DestinationsPlugin;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CitizensUtilities {
    public static long lastBackupTime;

    private final DestinationsPlugin destRef;

    public CitizensUtilities(DestinationsPlugin storageRef) {
        destRef = storageRef;
    }

    public void BackupConfig(Boolean forced) {
        if (!forced) {
            // Check when we last ran a backup
            if (lastBackupTime > new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(destRef.getConfig().getLong("backup-interval", 24))).getTime()) {
                return;
            }

            // Make the backup folder if it does not exist
            File citizensBackups = new File(destRef.getDataFolder(), "/CitizensBackups/");
            if (!citizensBackups.exists())
                citizensBackups.mkdirs();

            // Clean up backup files.
            for (final File fileEntry : Objects.requireNonNull(citizensBackups.listFiles())) {
                if (fileEntry.lastModified() <= (new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(destRef.getConfig().getLong("backup-history", 30)))).getTime()) {
                    fileEntry.delete();
                }
            }
        }
        // Save the NPC file
        destRef.getCitizensPlugin().storeNPCs();

        // Format the filename
        SimpleDateFormat fileDate = new SimpleDateFormat("MMddyyyy_HHmmss");

        File backupFile = new File(destRef.getDataFolder() + "/CitizensBackups/" + fileDate.format(new Date()) + ".zip");

        try {
            BufferedInputStream origin;

            FileOutputStream dest = new FileOutputStream(backupFile.toString());
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            byte[] data = new byte[2048];
            FileInputStream fi = new FileInputStream(destRef.getCitizensPlugin().getDataFolder() + "/saves.yml");
            origin = new BufferedInputStream(fi, 2048);
            ZipEntry entry = new ZipEntry("saves.yml");
            out.putNextEntry(entry);
            int count;
            while ((count = origin.read(data, 0, 2048)) != -1) {
                out.write(data, 0, count);
            }
            origin.close();
            fi.close();
            out.close();
            dest.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        lastBackupTime = new Date().getTime();

        if (!forced) {
            destRef.getMessagesManager().consoleMessage(destRef, "destinations", "Console_Messages.citizens_backup");
        }
    }
}
