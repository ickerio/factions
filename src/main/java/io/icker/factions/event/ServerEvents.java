package io.icker.factions.event;

import io.icker.factions.teams.Member;
import io.icker.factions.teams.TeamsManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerEvents {
    public static void starting(MinecraftServer server) {
        TeamsManager.load();
    }
    public static void stopped(MinecraftServer server) {
        TeamsManager.save();
    }
    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Member member = TeamsManager.getMember(player.getUuid());
            if (member != null) member.tick();
        }
    }
}