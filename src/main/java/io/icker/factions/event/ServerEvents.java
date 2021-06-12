package io.icker.factions.event;

import io.icker.factions.teams.Database;
//import io.icker.factions.teams.Member;
import net.minecraft.server.MinecraftServer;
//import net.minecraft.server.network.ServerPlayerEntity;

public class ServerEvents {
    public static void starting(MinecraftServer server) {
        Database.connect();
    }
    public static void stopped(MinecraftServer server) {
        Database.disconnect();
    }
    public static void tick(MinecraftServer server) {
        //for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
        //    Member member = TeamsManager.getMember(player.getUuid());
        //    if (member != null) member.tick();
        //}
    }
}