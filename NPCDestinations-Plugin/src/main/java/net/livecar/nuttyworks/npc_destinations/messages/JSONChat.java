package net.livecar.nuttyworks.npc_destinations.messages;

import net.livecar.nuttyworks.npc_destinations.DestinationsPlugin;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class JSONChat {
    private final DestinationsPlugin destRef;

    public JSONChat(DestinationsPlugin storageRef) {
        destRef = storageRef;
    }

    public void sendJsonMessage(Player player, String msg) {
        sendMessage(player, ChatColor.translateAlternateColorCodes('&', msg));
    }

    public void sendJsonMessage(CommandSender player, String jsonMsg) {
        sendMessage(player, ChatColor.translateAlternateColorCodes('&', jsonMsg));
    }

    private void sendMessage(CommandSender player, String jsonMsg) {
        try {
            player.spigot().sendMessage(ComponentSerializer.parse(jsonMsg));
        } catch (Exception error) {
            destRef.getMessagesManager().logToConsole(destRef, "Json Failure: " + error.getMessage() + "\n" + jsonMsg);
        }
    }

    @SuppressWarnings("rawtypes")
    private void sendMessageLess12(CommandSender player, String jsonMsg) {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
            Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
            Class<?> chatSerializer = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            Class<?> chatComponent = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> packet = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            Constructor constructor = packet.getConstructor(chatComponent);

            Object text = chatSerializer.getMethod("a", String.class).invoke(chatSerializer, jsonMsg);
            Object packetFinal = constructor.newInstance(text);

            Field field = packetFinal.getClass().getDeclaredField("a");
            field.setAccessible(true);
            field.set(packetFinal, text);
            connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet")).invoke(connection, packetFinal);

        } catch (Exception ex) {
            destRef.getMessagesManager().logToConsole(destRef, "Json Failure: " + ex.getMessage() + "\n" + jsonMsg);
        }
    }
}
