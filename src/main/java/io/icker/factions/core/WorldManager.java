package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.MiscEvents;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import io.icker.factions.util.WorldUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class WorldManager {
    private static final int FADE_STEPS = 6;

    private static final HashMap<UUID, UUID> lastSeenFaction = new HashMap<>();
    private static final HashMap<UUID, Integer> announceTicks = new HashMap<>();
    private static final HashMap<UUID, Component> announceComponent = new HashMap<>();
    private static final HashMap<UUID, Integer> announceFadeTicks = new HashMap<>();
    private static final HashMap<UUID, Faction> announceFaction = new HashMap<>();

    public static void register() {
        PlayerEvents.ON_MOVE.register(WorldManager::onMove);
        MiscEvents.ON_MOB_SPAWN_ATTEMPT.register(WorldManager::onMobSpawnAttempt);
        ServerTickEvents.END_SERVER_TICK.register(
                server -> {
                    if (announceTicks.isEmpty()) return;

                    Iterator<Map.Entry<UUID, Integer>> iterator =
                            announceTicks.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<UUID, Integer> entry = iterator.next();
                        UUID playerId = entry.getKey();
                        int ticks = entry.getValue();
                        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                        Component component = announceComponent.get(playerId);

                        if (ticks > 0) {
                            Integer fade = announceFadeTicks.get(playerId);
                            if (fade != null) {
                                if (fade < FADE_STEPS) {
                                    if (player != null) {
                                        Faction fadeFaction = announceFaction.get(playerId);
                                        player.sendOverlayMessage(
                                                AnnouncerManager.buildFadeStep(
                                                        fadeFaction, fade, FADE_STEPS));
                                    }
                                    announceFadeTicks.put(playerId, fade + 1);
                                } else {
                                    announceFadeTicks.remove(playerId);
                                    if (player != null && component != null) {
                                        player.sendOverlayMessage(component);
                                    }
                                }
                            } else if (player != null && component != null && ticks % 20 == 0) {
                                player.sendOverlayMessage(component);
                            }
                            entry.setValue(ticks - 1);
                        } else {
                            iterator.remove();
                            announceComponent.remove(playerId);
                            announceFadeTicks.remove(playerId);
                            announceFaction.remove(playerId);
                        }
                    }
                });
    }

    private static void onMobSpawnAttempt() {
        // TODO Implement this
    }

    public static void clearPlayerState(UUID playerId) {
        lastSeenFaction.remove(playerId);
        announceTicks.remove(playerId);
        announceComponent.remove(playerId);
        announceFadeTicks.remove(playerId);
        announceFaction.remove(playerId);
    }

    private static void onMove(ServerPlayer player) {
        User user = User.get(player.getUUID());
        if (!user.autoclaim && !user.radar && !FactionsMod.CONFIG.ANNOUNCER.ENABLED) return;

        ServerLevel world = (ServerLevel) player.level();
        String dimension = WorldUtils.dimensionString(world);

        ChunkPos chunkPos = WorldUtils.getChunkPos(player.blockPosition());

        Claim claim = Claim.get(chunkPos.x(), chunkPos.z(), dimension);
        if (user.autoclaim && claim == null) {
            Faction faction = user.getFaction();
            int requiredPower =
                    (faction.getClaimCount() + 1) * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
            int maxPower =
                    faction.getUsers().size() * FactionsMod.CONFIG.POWER.MEMBER
                            + FactionsMod.CONFIG.POWER.BASE
                            + faction.getAdminPower();

            if (maxPower < requiredPower) {
                new Message(Component.translatable("factions.events.autoclaim.fail"))
                        .fail()
                        .send(player, false);
                user.autoclaim = false;
            } else {
                faction.addClaim(chunkPos.x(), chunkPos.z(), dimension);
                claim = Claim.get(chunkPos.x(), chunkPos.z(), dimension);
                new Message(
                                Component.translatable(
                                        "factions.events.autoclaim.success",
                                        chunkPos.x(),
                                        chunkPos.z(),
                                        player.getName().getString()))
                        .send(faction);
            }
        }

        Faction claimFaction = claim == null ? null : claim.getFaction();
        if (user.radar) {
            if (claim != null) {
                new Message(claimFaction.getName())
                        .format(claimFaction.getColor())
                        .send(player, true);
            } else {
                new Message(Component.translatable("factions.radar.wilderness"))
                        .format(ChatFormatting.GREEN)
                        .send(player, true);
            }
        }

        if (!FactionsMod.CONFIG.ANNOUNCER.ENABLED) return;

        UUID playerId = player.getUUID();
        UUID currentFactionId = claim != null ? claimFaction.getID() : null;
        if (Objects.equals(currentFactionId, lastSeenFaction.get(playerId))) return;

        lastSeenFaction.put(playerId, currentFactionId);

        announceTicks.remove(playerId);
        announceComponent.remove(playerId);
        announceFadeTicks.remove(playerId);
        announceFaction.remove(playerId);

        Component component = AnnouncerManager.buildAnnouncement(claimFaction);
        announceComponent.put(playerId, component);
        announceTicks.put(playerId, FactionsMod.CONFIG.ANNOUNCER.DISPLAY_SECONDS * 20);
        announceFaction.put(playerId, claimFaction);
        announceFadeTicks.put(playerId, 0);
        player.sendOverlayMessage(AnnouncerManager.buildFadeStep(claimFaction, 0, FADE_STEPS));
    }
}
