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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.ChunkPos;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class FactionsManager {
    public static PlayerList playerManager;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(FactionsManager::serverStarted);
        FactionEvents.MODIFY.register(FactionsManager::factionModified);
        FactionEvents.MEMBER_JOIN.register(FactionsManager::memberChange);
        FactionEvents.MEMBER_LEAVE.register(FactionsManager::memberChange);
        PlayerEvents.ON_KILLED_BY_PLAYER.register(FactionsManager::playerDeath);
        PlayerEvents.ON_POWER_TICK.register(FactionsManager::powerTick);
        PlayerEvents.OPEN_SAFE.register(FactionsManager::openSafe);

        if (FactionsMod.CONFIG.HOME != null && FactionsMod.CONFIG.HOME.CLAIM_ONLY) {
            ClaimEvents.REMOVE.register(
                    (x, z, level, faction) -> {
                        Home home = faction.getHome();

                        if (home == null || !Objects.equals(home.level, level)) {
                            return;
                        }

                        BlockPos homePos = BlockPos.containing(home.x, home.y, home.z);

                        ServerLevel world = WorldUtils.getWorld(home.level);

                        ChunkPos homeChunkPos = world.getChunk(homePos).getPos();

                        if (homeChunkPos.x == x && homeChunkPos.z == z) {
                            faction.setHome(null);
                        }
                    });
        }
    }

    private static void serverStarted(MinecraftServer server) {
        playerManager = server.getPlayerList();
        Message.manager = server.getPlayerList();
    }

    private static void factionModified(Faction faction) {
        ServerPlayer[] players =
                faction.getUsers().stream()
                        .map(user -> playerManager.getPlayer(user.getID()))
                        .filter(player -> player != null)
                        .toArray(ServerPlayer[]::new);
        updatePlayerList(players);
    }

    private static void memberChange(Faction faction, User user) {
        ServerPlayer player = playerManager.getPlayer(user.getID());
        if (player != null) {
            updatePlayerList(player);
        }
    }

    private static void playerDeath(ServerPlayer player, DamageSource source) {
        User member = User.get(player.getUUID());
        if (!member.isInFaction()) return;

        Faction faction = member.getFaction();

        int adjusted = faction.adjustPower(-FactionsMod.CONFIG.POWER.DEATH_PENALTY);
        new Message(
                        Component.translatable(
                                "factions.events.lose_power_by_death",
                                player.getName().getString(),
                                adjusted))
                .send(faction);
    }

    private static void powerTick(ServerPlayer player) {
        User member = User.get(player.getUUID());
        if (!member.isInFaction()) return;

        Faction faction = member.getFaction();

        int adjusted = faction.adjustPower(FactionsMod.CONFIG.POWER.POWER_TICKS.REWARD);
        if (adjusted != 0 && FactionsMod.CONFIG.DISPLAY.POWER_MESSAGE)
            new Message(
                            Component.translatable(
                                    "factions.events.get_power_by_tick",
                                    player.getName().getString(),
                                    adjusted))
                    .send(faction);
    }

    private static void updatePlayerList(ServerPlayer... players) {
        playerManager.broadcastAll(
                new ClientboundPlayerInfoUpdatePacket(
                        EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                        List.of(players)));
    }

    private static InteractionResult openSafe(Player player, Faction faction) {
        User user = User.get(player.getUUID());

        if (!user.isInFaction()) {
            if (FactionsMod.CONFIG.SAFE != null && FactionsMod.CONFIG.SAFE.ENDER_CHEST) {
                new Message(
                                Component.translatable(
                                        "factions.events.no_enderchests_without_faction"))
                        .fail()
                        .send(player, false);
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }

        player.openMenu(
                new SimpleMenuProvider(
                        (syncId, inventory, p) -> {
                            if (FactionsMod.CONFIG.SAFE.DOUBLE) {
                                return ChestMenu.sixRows(syncId, inventory, faction.getSafe());
                            } else {
                                return ChestMenu.threeRows(syncId, inventory, faction.getSafe());
                            }
                        },
                        Component.translatable("factions.gui.safe.title", faction.getName())));

        return InteractionResult.SUCCESS;
    }
}
