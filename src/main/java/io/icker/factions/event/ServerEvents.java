package io.icker.factions.event;

import io.icker.factions.database.Database;
import net.minecraft.server.MinecraftServer;

public class ServerEvents {
    public static void starting(MinecraftServer server) {
        Database.connect();
    }
    public static void stopped(MinecraftServer server) {
        Database.disconnect();
    }
    public static void tick(MinecraftServer server) {
    }
}