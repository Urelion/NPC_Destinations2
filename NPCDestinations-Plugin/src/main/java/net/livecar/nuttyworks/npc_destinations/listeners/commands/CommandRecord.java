package net.livecar.nuttyworks.npc_destinations.listeners.commands;

import net.citizensnpcs.api.npc.NPC;
import net.livecar.nuttyworks.npc_destinations.DestinationsPlugin;
import net.livecar.nuttyworks.npc_destinations.citizens.NPCDestinationsTrait;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CommandRecord {
    public String commandName;
    public String groupName;
    public String languageFile;
    public String[] commandPermission;
    public String helpMessage;
    public String[] arguments;
    public Boolean allowConsole;
    public int minArguments;
    public int maxArguments;

    private final Class<?> commandClass;
    private final Method commandMethod;

    public CommandRecord(String commandName, String groupName, String languageFile, String[] commandPermission, String helpMessage, Boolean allowConsole, int minArguments, int maxArguments, String[] arguments, Class<?> commandClass,
                         String commandMethod) {
        this.commandName = commandName;
        this.groupName = groupName;
        this.languageFile = languageFile;
        this.commandPermission = commandPermission;
        this.helpMessage = helpMessage;
        this.allowConsole = allowConsole;
        this.minArguments = minArguments;
        this.maxArguments = maxArguments;
        this.arguments = arguments;
        this.commandClass = commandClass;
        this.commandMethod = getMethod(commandClass, commandMethod);
    }

    public boolean invokeCommand(DestinationsPlugin destinationsRef, CommandSender sender, NPC npc, String[] inargs, boolean isOwner, NPCDestinationsTrait destTrait) {
        if (destTrait == null)
            destTrait = new NPCDestinationsTrait();

        try {
            Constructor<?> ctr = commandClass.getConstructor();
            ctr.setAccessible(true);

            return (boolean) commandMethod.invoke(ctr.newInstance(), destinationsRef, sender, npc, inargs, isOwner, destTrait);

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException | InstantiationException | NoSuchMethodException e) {
            // Oops!
            e.printStackTrace();
        }
        return false;
    }

    private Method getMethod(Class<?> commandClass, String methodName) {
        try {
            return commandClass.getMethod(methodName, DestinationsPlugin.class, CommandSender.class, NPC.class, String[].class, boolean.class, NPCDestinationsTrait.class);
        } catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

}
