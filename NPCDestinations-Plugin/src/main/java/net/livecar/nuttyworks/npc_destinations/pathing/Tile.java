package net.livecar.nuttyworks.npc_destinations.pathing;

import org.bukkit.Location;

import java.util.Objects;

public class Tile {

    private final String uid;
    private Tile parent;

    // As offset from starting point
    private final short x, y, z;

    private double g = -1, h = -1;

    public Tile(short x, short y, short z, Tile parent) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.parent = parent;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(x);
        stringBuilder.append(y);
        stringBuilder.append(z);
        this.uid = stringBuilder.toString();
    }

    public void destroy() {
        this.parent = null;
    }

    public boolean isInRange(int range) {
        return ((range - abs(x) >= 0) && (range - abs(y) >= 0) && (range - abs(z) >= 0));
    }

    public Location getLocation(Location start) {
        return new Location(start.getWorld(), start.getBlockX() + x, start.getBlockY() + y, start.getBlockZ() + z);
    }

    public void calculateBoth(Location start, Location end, boolean update) {
        this.calculateG(update);
        this.calculateH(start.getBlockX(), start.getBlockY(), start.getBlockZ(), end.getBlockX(), end.getBlockY(), end.getBlockZ(), update);
    }

    public void calculateBoth(int sx, int sy, int sz, int ex, int ey, int ez, boolean update) {
        this.calculateG(update);
        this.calculateH(sx, sy, sz, ex, ey, ez, update);
    }

    public void calculateH(int sx, int sy, int sz, int ex, int ey, int ez, boolean update) {
        // Only update if h hasn't been calculated, or if forced
        if (update || h == -1) {
            int hx = sx + x, hy = sy + y, hz = sz + z;
            this.h = this.getEuclideanDistance(hx, hy, hz, ex, ey, ez);
        }
    }

    // G = the movement cost to move from the starting point A to a given square
    // on the grid, following the path generated to get there.
    public void calculateG(boolean update) {
        if (update || g == -1) {
            // Only update if g hasn't been calculated, or if forced
            Tile currentParent, currentTile = this;
            int gCost = 0;

            // Follow path back to start
            while ((currentParent = currentTile.getParent()) != null) {
                int dx = currentTile.getX() - currentParent.getX(), dy = currentTile.getY() - currentParent.getY(), dz = currentTile.getZ() - currentParent.getZ();

                dx = abs(dx);
                dy = abs(dy);
                dz = abs(dz);

                if (dx == 1 && dy == 1 && dz == 1) {
                    gCost += 1.7;
                } else if (((dx == 1 || dz == 1) && dy == 1) || ((dx == 1 || dz == 1) && dy == 0)) {
                    gCost += 1.4;
                } else {
                    gCost += 1.0;
                }

                // Move backwards a tile
                currentTile = currentParent;
            }
            this.g = gCost;
        }
    }

    private double getEuclideanDistance(int sx, int sy, int sz, int ex, int ey, int ez) {
        double dx = sx - ex, dy = sy - ey, dz = sz - ez;
        return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    }

    private int abs(int i) {
        return (i < 0 ? -i : i);
    }

    public String getUID() {
        return this.uid;
    }

    public Tile getParent() {
        return this.parent;
    }

    public void setParent(Tile parent) {
        this.parent = parent;
    }

    public short getX() {
        return x;
    }

    public int getX(Location i) {
        return (i.getBlockX() + x);
    }

    public short getY() {
        return y;
    }

    public int getY(Location i) {
        return (i.getBlockY() + y);
    }

    public short getZ() {
        return z;
    }

    public int getZ(Location i) {
        return (i.getBlockZ() + z);
    }

    public double getG() {
        return g;
    }

    public double getH() {
        return h;
    }

    public double getF() {
        // f = h + g
        return (h + g);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tile)) return false;
        Tile tile = (Tile) o;
        return x == tile.x && y == tile.y && z == tile.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
