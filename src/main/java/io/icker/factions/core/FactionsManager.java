package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Home;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import io.icker.factions.util.WorldUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

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

        if (FactionsMod.CONFIG.HOME != null && FactionsMod.CONFIG.HOME.CLAIM_ONLY) {
            ClaimEvents.REMOVE.register((x, z, level, faction) -> {
                Home home = faction.getHome();

                if (home == null || !Objects.equals(home.level, level)) {
                    return;
                }

                BlockPos homePos = BlockPos.ofFloored(home.x, home.y, home.z);

                ServerWorld world = WorldUtils.getWorld(home.level);

                ChunkPos homeChunkPos = world.getChunk(homePos).getPos();

                if (homeChunkPos.x == x && homeChunkPos.z == z) {
                    faction.setHome(null);
                }
            });
        }
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

        int adjusted = faction.adjustPower(-FactionsMod.CONFIG.POWER.DEATH_PENALTY);
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
        if (adjusted != 0 && FactionsMod.CONFIG.DISPLAY.POWER_MESSAGE)
            new Message(
                "%s gained %d power from surviving",
                player.getName().getString(),
                adjusted
            ).send(faction);
    }

    private static void updatePlayerList(ServerPlayerEntity ...players) {
        playerManager.sendToAll(new PlayerListS2CPacket(EnumSet.of(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME), List.of(players)));
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
