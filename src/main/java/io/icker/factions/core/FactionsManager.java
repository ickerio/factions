package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;

public class FactionsManager {
    public static void playerDeath(ServerPlayerEntity player) {
        User member = User.get(player.getUuid());
        if (!member.isInFaction()) return;

        Faction faction = member.getFaction();

        int adjusted = adjustPower(faction, -FactionsMod.CONFIG.POWER_DEATH_PENALTY);
        new Message("%s lost %d power from dying", player.getName().asString(), adjusted).send(faction);
    }

    public static void powerTick(ServerPlayerEntity player) {
        User member = User.get(player.getUuid());
        if (!member.isInFaction()) return;

        Faction faction = member.getFaction();

        int adjusted = adjustPower(faction, FactionsMod.CONFIG.TICKS_FOR_POWER_REWARD);
        if (adjusted != 0)
            new Message("%s gained %d power from surviving", player.getName().asString(), adjusted).send(faction);
    }

    public static int adjustPower(Faction faction, int adjustment) {
        int maxPower = FactionsMod.CONFIG.BASE_POWER + (faction.getUsers().size() * FactionsMod.CONFIG.MEMBER_POWER);

        int updated = Math.min(Math.max(0, faction.getPower() + adjustment), maxPower);
        faction.setPower(updated);
        return Math.abs(updated - faction.getPower());
    }

    public static void updatePlayerList(ServerPlayerEntity player) {
        if (player != null) {
            FactionsMod.playerManager.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, player));
        }
    }

    public static void updatePlayerList(Collection<ServerPlayerEntity> players) {
        FactionsMod.playerManager.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, players));
    }
}
