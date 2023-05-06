package net.livecar.nuttyworks.npc_destinations.api;

import lombok.Getter;
import lombok.Setter;
import net.livecar.nuttyworks.npc_destinations.api.enumerations.TriBoolean;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Destination {

    public UUID locationUUID;
    public String aliasName;
    public Location location;

    public int timeOfDay;
    public int probability;
    public int timeMinimum;
    public int timeMaximum;
    public Boolean wanderingUseBlocks = false;
    public int waitMinimum;
    public int waitMaximum;
    public int pauseDistance = 5;
    public int pauseTimeout = 25;
    public String pauseType = "";
    public Boolean itemsClear = false;
    public ItemStack itemsHead;
    public ItemStack itemsChest;
    public ItemStack itemsLegs;
    public ItemStack itemsBoots;
    public ItemStack itemsHand;
    public ItemStack itemsOffhand;

    public TriBoolean citizensSwim = TriBoolean.NOT_SET;
    public TriBoolean citizensNewPathFinder = TriBoolean.NOT_SET;
    public TriBoolean citizensAvoidWater = TriBoolean.NOT_SET;
    public TriBoolean citizensDefaultStuck = TriBoolean.NOT_SET;
    public Double citizensDistanceMargin = -1D;
    public Double citizensPathDistanceMargin = -1D;

    public int weatherFlag;

    public String playerSkinTextureSignature;
    public String playerSkinTextureMetadata;
    public String playerSkinName;
    public String playerSkinUUID;
    public Boolean playerSkinApplyOnArrival;

    public List<String> arrivalCommands;
    public String managedLocation = "";
    public String wanderingRegion = "";

    private double maxDistance;
    private double maxDistanceSquared;
    private double wanderingDistance;
    private double wanderingDistanceSquared;

    public void setWanderingDistance(double distance) {
        this.wanderingDistance = distance;
        this.wanderingDistanceSquared = distance * distance;
    }

    public void setMaxDistance(double distance) {
        this.maxDistance = distance;
        this.maxDistanceSquared = distance * distance;
    }
}
