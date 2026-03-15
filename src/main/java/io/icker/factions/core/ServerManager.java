package io.icker.factions.core;

import io.icker.factions.api.events.MiscEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class ServerManager {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register(ServerManager::playerJoin);
        MiscEvents.ON_SAVE.register(ServerManager::save);
    }

    private static void save(MinecraftServer server) {
        Claim.save();
        Faction.save();
        User.save();
    }

    private static void playerJoin(
            ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
        ServerPlayer player = handler.getPlayer();
        User user = User.get(player.getUUID());

        if (user.isInFaction()) {
            Faction faction = user.getFaction();
            new Message(
                            Component.translatable(
                                    "factions.events.member_returns", player.getName().getString()))
                    .send(player, false);
            new Message(faction.getMOTD()).prependFaction(faction).send(player, false);
        }
    }
}
