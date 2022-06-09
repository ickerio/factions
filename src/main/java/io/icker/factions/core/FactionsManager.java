package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.List;

public class FactionsManager {
    public static PlayerManager playerManager;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(FactionsManager::serverStarted);
        FactionEvents.MODIFY.register(FactionsManager::factionModified);
        FactionEvents.MEMBER_JOIN.register(FactionsManager::memberChange);
        FactionEvents.MEMBER_LEAVE.register(FactionsManager::memberChange);
        PlayerEvents.ON_KILLED_BY_PLAYER.register(FactionsManager::playerDeath);
        PlayerEvents.ON_POWER_TICK.register(FactionsManager::powerTick);
    }

    private static void serverStarted(MinecraftServer server) {
        playerManager = server.getPlayerManager();
        Message.manager = server.getPlayerManager();
    }

    private static void factionModified(Faction faction) {
        List<ServerPlayerEntity> players = faction.getUsers().stream().map(user -> playerManager.getPlayer(user.getID())).filter(player -> player != null).toList();
        updatePlayerList(players);
    }

    private static void memberChange(Faction faction, User user) {
        updatePlayerList(playerManager.getPlayer(user.getID()));
    }

    private static void playerDeath(ServerPlayerEntity player, DamageSource source) {
        User member = User.get(player.getUuid());
        if (!member.isInFaction()) return;

        Faction faction = member.getFaction();

        int adjusted = faction.adjustPower(-FactionsMod.CONFIG.POWER_DEATH_PENALTY);
        new Message("%s lost %d power from dying", player.getName().getString(), adjusted).send(faction);
    }

    private static void powerTick(ServerPlayerEntity player) {
        User member = User.get(player.getUuid());
        if (!member.isInFaction()) return;

        Faction faction = member.getFaction();

        int adjusted = faction.adjustPower(FactionsMod.CONFIG.TICKS_FOR_POWER_REWARD);
        if (adjusted != 0)
            new Message("%s gained %d power from surviving", player.getName().getString(), adjusted).send(faction);
    }

    private static void updatePlayerList(ServerPlayerEntity player) {
        if (player != null) {
            playerManager.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, player));
        }
    }

    private static void updatePlayerList(Collection<ServerPlayerEntity> players) {
        playerManager.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, players));
    }
}
