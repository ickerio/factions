package io.icker.factions.event;

import io.icker.factions.database.Database;
import io.icker.factions.util.Message;
import net.minecraft.server.MinecraftServer;

public class ServerEvents {
    public static void started(MinecraftServer server) {
        Database.connect();
        Message.manager = server.getPlayerManager();
    }

    public static void stopped(MinecraftServer server) {
        Database.disconnect();
    }
}