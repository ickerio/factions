package io.icker.factions.event;

import io.icker.factions.util.Message;
import net.minecraft.server.MinecraftServer;

public class ServerEvents {
    public static void started(MinecraftServer server) {
        // Do new database setup things here
        Message.manager = server.getPlayerManager();
    }

    public static void save() {
        // TODO: Add save code
    }
}