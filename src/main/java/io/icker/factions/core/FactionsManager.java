package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class FactionsManager {
    public static PlayerManager playerManager;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(FactionsManager::serverStarted);
        FactionEvents.MODIFY.register(FactionsManager::factionModified);
        FactionEvents.MEMBER_JOIN.register(FactionsManager::memberChange);
        FactionEvents.MEMBER_LEAVE.register(FactionsManager::memberChange);
        PlayerEvents.ON_KILLED_BY_PLAYER.register(FactionsManager::playerDeath);
        PlayerEvents.ON_POWER_TICK.register(FactionsManager::powerTick);
        PlayerEvents.OPEN_SAFE.register(FactionsManager::openSafe);
    }

    private static void serverStarted(MinecraftServer server) {
        playerManager = server.getPlayerManager();
        Message.manager = server.getPlayerManager();
    }

    private static void factionModified(Faction faction) {
        ServerPlayerEntity[] players = faction.getUsers()
            .stream()
            .map(user -> playerManager.getPlayer(user.getID()))
            .filter(player -> player != null)
            .toArray(ServerPlayerEntity[]::new);
        updatePlayerList(players);
    }

    private static void memberChange(Faction faction, User user) {
        ServerPlayerEntity player = playerManager.getPlayer(user.getID());
        if (player != null) {
            updatePlayerList(player);
        }
    }

    private static void playerDeath(ServerPlayerEntity player, DamageSource source) {
        User member = User.get(player.getUuid());
        if (!member.isInFaction()) return;

        Faction faction = member.getFaction();

        int adjusted = faction.adjustPower(-FactionsMod.CONFIG.POWER.POWER_TICKS.DEATH_PENALTY);
        new Message(
            "%s lost %d power from dying",
            player.getName().getString(),
            adjusted
        ).send(faction);
    }

    private static void powerTick(ServerPlayerEntity player) {
        User member = User.get(player.getUuid());
        if (!member.isInFaction()) return;

        Faction faction = member.getFaction();

        int adjusted = faction.adjustPower(FactionsMod.CONFIG.POWER.POWER_TICKS.REWARD);
        if (adjusted != 0)
            new Message(
                "%s gained %d power from surviving",
                player.getName().getString(),
                adjusted
            ).send(faction);
    }

    private static void updatePlayerList(ServerPlayerEntity ...players) {
        playerManager.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, players));
    }

    private static ActionResult openSafe(PlayerEntity player, Faction faction) {
        User user =  User.get(player.getUuid());

        if (!user.isInFaction()) {
            if (FactionsMod.CONFIG.SAFE != null && FactionsMod.CONFIG.SAFE.ENDER_CHEST) {
                new Message("Cannot use enderchests when not in a faction").fail().send(player, false);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        }

        player.openHandledScreen(
            new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, p) -> {
                    if (FactionsMod.CONFIG.SAFE.DOUBLE) {
                        return GenericContainerScreenHandler.createGeneric9x6(syncId, inventory, faction.getSafe());
                    } else {
                        return GenericContainerScreenHandler.createGeneric9x3(syncId, inventory, faction.getSafe());
                    }
                },
                Text.of(String.format("%s's Safe", faction.getName()))
            )
        );

        return ActionResult.SUCCESS;
    }
}
