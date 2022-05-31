package io.icker.factions.event;

import io.icker.factions.api.persistents.*;
import io.icker.factions.util.Message;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerEvents {
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
            new Message("Welcome back " + player.getName().toString() + "!").send(player, false);
            new Message(user.getFaction().getMOTD()).send(player, false);
        }
    }
}