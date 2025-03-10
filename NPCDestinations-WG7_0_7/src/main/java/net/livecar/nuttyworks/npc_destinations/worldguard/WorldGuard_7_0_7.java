package net.livecar.nuttyworks.npc_destinations.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class WorldGuard_7_0_7 implements WorldGuardInterface, Listener {

    private Plugin destRef;

    // FIXME: 3/28/2022 Lazy only going to support the latest and greatest right now; should work with WG 7.0.0 and up!
    public static boolean isValidVersion() {
        return true;
    }

    public WorldGuard_7_0_7(Plugin storageRef) {
        destRef = storageRef;
    }

    public RegionManager getRegionManager(World world) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
    }

    public void registerFlags() {

    }

    public void unregisterFlags() {
    }

    @Override
    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, destRef);
    }

    public Location[] getRegionBounds(World world, String regionName) {
        ProtectedRegion boundRegion = getRegionManager(world).getRegion(regionName);
        if (boundRegion == null) return new Location[0];

        Location[] boundLocs = new Location[2];
        boundLocs[0] = new Location(world, boundRegion.getMinimumPoint().getBlockX(), boundRegion.getMinimumPoint().getBlockY(), boundRegion.getMinimumPoint().getBlockZ());
        boundLocs[1] = new Location(world, boundRegion.getMaximumPoint().getBlockX(), boundRegion.getMaximumPoint().getBlockY(), boundRegion.getMaximumPoint().getBlockZ());

        return boundLocs;
    }

    public boolean isInRegion(Location location, String regionName) {
        ProtectedRegion boundRegion = getRegionManager(location.getWorld()).getRegion(regionName);
        if (boundRegion == null) return false;

        return boundRegion.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public List<String> getRegionList(World world) {
        return new ArrayList<>(getRegionManager(world).getRegions().keySet());
    }

    @Override
    public RegionShape getRegionShape(Location location, String regionName) {
        ProtectedRegion boundRegion = getRegionManager(location.getWorld()).getRegion(regionName);
        if (boundRegion == null) return null;
        switch (boundRegion.getType()) {
            case CUBOID:
                return RegionShape.CUBOID;
            case GLOBAL:
                return RegionShape.GLOBAL;
            case POLYGON:
                return RegionShape.POLYGON;
        }
        return null;
    }
}
