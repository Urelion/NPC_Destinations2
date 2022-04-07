package net.livecar.nuttyworks.npc_destinations.listeners;

import net.citizensnpcs.api.npc.NPC;
import net.livecar.nuttyworks.npc_destinations.DestinationsPlugin;
import net.livecar.nuttyworks.npc_destinations.citizens.NPCDestinationsTrait;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class OnPlayerInteractEvent implements Listener {
    private DestinationsPlugin plugin;

    private PlayerInteractEvent lastClickEvent;

    public OnPlayerInteractEvent(DestinationsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        //@fixme crazy up in here

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (plugin.getMcUtils().getMainHand(player) == null) return;
            ItemStack mainHandItem = plugin.getMcUtils().getMainHand(player);

            if (mainHandItem.getType() == Material.STICK && mainHandItem.hasItemMeta() && mainHandItem.getItemMeta().hasDisplayName()) {
                if (mainHandItem.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', "&eNPCDestinations &2[&fBlockStick&2]"))) {
                    if (lastClickEvent == null) {
                        lastClickEvent = event;
                    } else if (lastClickEvent.getPlayer().equals(event.getPlayer()) && lastClickEvent.getClickedBlock().equals(event.getClickedBlock())) {
                        // Return and ignore this event.
                        event.setCancelled(true);
                        return;
                    }
                    lastClickEvent = event;

                    NPC npc = plugin.getCitizensPlugin().getNPCSelector().getSelected(player);
                    if (npc == null) {
                        plugin.getMessagesManager().sendMessage("destinations", player, "messages.invalid_npc");
                        event.setCancelled(true);
                    } else {
                        NPCDestinationsTrait trait = null;
                        if (!npc.hasTrait(NPCDestinationsTrait.class)) {
                            plugin.getMessagesManager().sendMessage("destinations", player, "messages.invalid_npc");
                            event.setCancelled(true);
                            return;
                        } else trait = npc.getTrait(NPCDestinationsTrait.class);
                        if (!player.isSneaking()) {
                            if (!trait.AllowedPathBlocks.contains(event.getClickedBlock().getType())) {
                                trait.AllowedPathBlocks.add(event.getClickedBlock().getType());
                                plugin.getMessagesManager().sendMessage("destinations", player, "messages.commands_addblock_added", event.getClickedBlock().getType());
                            } else {
                                plugin.getMessagesManager().sendMessage("destinations", player, "messages.commands_addblock_exists", event.getClickedBlock().getType());
                            }
                        } else {
                            if (trait.AllowedPathBlocks.size() > 0) {
                                if (trait.AllowedPathBlocks.contains(event.getClickedBlock().getType())) {
                                    trait.AllowedPathBlocks.remove(event.getClickedBlock().getType());
                                    plugin.getMessagesManager().sendMessage("destinations", player, "messages.commands_removeblock_removed", event.getClickedBlock().getType());
                                } else {
                                    plugin.getMessagesManager().sendMessage("destinations", player, "messages.commands_removeblock_notinlist", event.getClickedBlock().getType());
                                }
                            } else {
                                plugin.getMessagesManager().sendMessage("destinations", player, "messages.commands_removeblock_notinlist", event.getClickedBlock().getType());
                            }
                        }
                        event.setCancelled(true);

                    }
                }
            }
        }
    }
}
