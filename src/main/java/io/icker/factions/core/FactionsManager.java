package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.List;

public class FactionsManager {
    public static PlayerManager playerManager;

    public static void serverStarted(MinecraftServer server) {
        playerManager = server.getPlayerManager();
        Message.manager = server.getPlayerManager();
    }

    public static void factionModified(Faction faction) {
        List<ServerPlayerEntity> players = faction.getUsers().stream().map(user -> playerManager.getPlayer(user.getID())).toList();
        updatePlayerList(players);
    }

    public static void memberChange(Faction faction, User user) {
        updatePlayerList(playerManager.getPlayer(user.getID()));
    }

    public static void playerDeath(ServerPlayerEntity player) {
        User member = User.get(player.getUuid());
        if (!member.isInFaction()) return;

        Faction faction = member.getFaction();

        int adjusted = faction.adjustPower(-FactionsMod.CONFIG.POWER_DEATH_PENALTY);
        new Message("%s lost %d power from dying", player.getName().asString(), adjusted).send(faction);
    }

    public static void powerTick(ServerPlayerEntity player) {
        User member = User.get(player.getUuid());
        if (!member.isInFaction()) return;

        Faction faction = member.getFaction();

        int adjusted = faction.adjustPower(FactionsMod.CONFIG.TICKS_FOR_POWER_REWARD);
        if (adjusted != 0)
            new Message("%s gained %d power from surviving", player.getName().asString(), adjusted).send(faction);
    }

    public static void updatePlayerList(ServerPlayerEntity player) {
        if (player != null) {
            playerManager.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, player));
        }
    }

    public static void updatePlayerList(Collection<ServerPlayerEntity> players) {
        playerManager.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, players));
    }
}
