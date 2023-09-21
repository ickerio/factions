package io.icker.factions.core;

import io.icker.factions.api.events.MiscEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

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

    private static void playerJoin(ServerPlayNetworkHandler handler, PacketSender sender,
            MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        User user = User.get(player.getUuid());

        if (user.isInFaction()) {
            Faction faction = user.getFaction();
            new Message("Welcome back " + player.getName().getString() + "!").send(player, false);
            new Message(faction.getMOTD()).prependFaction(faction).send(player, false);
        }
    }
}
