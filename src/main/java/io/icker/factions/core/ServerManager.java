package io.icker.factions.core;

import io.icker.factions.api.persistents.*;
import io.icker.factions.util.Message;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerManager {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register(ServerManager::playerJoin);
    }

    public static void save() {
        Claim.save();
        Faction.save();
        Home.save();
        Invite.save();
        User.save();
        Relationship.save();
    }

    public static void playerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        User user = User.get(player.getUuid());

        if (user.isInFaction()) {
            Faction faction = user.getFaction();
            new Message("Welcome back " + player.getName().asString() + "!").send(player, false);
            new Message(faction.getMOTD()).prependFaction(faction).send(player, false);
        }
    }
}