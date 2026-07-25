package io.icker.factions.core;

import io.icker.factions.api.events.ClaimEvents;
import io.icker.factions.api.events.FactionEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.net.ClaimSyncPayload;
import io.icker.factions.util.WorldUtils;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClaimSyncSender {
    private ClaimSyncSender() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> sendToPlayer(handler.getPlayer()));

        ClaimEvents.ADD.register(claim -> broadcastToAll());
        ClaimEvents.REMOVE.register((x, z, level, faction) -> broadcastToAll());

        FactionEvents.MODIFY.register(faction -> broadcastToAll());
        FactionEvents.DISBAND.register(faction -> broadcastToAll());
    }

    public static void sendToPlayer(ServerPlayer player) {
        ClaimSyncPayload payload = buildPayload();
        ServerPlayNetworking.send(player, payload);
    }

    public static void broadcastToAll() {
        var server = WorldUtils.getServer();
        if (server == null) return;

        ClaimSyncPayload payload = buildPayload();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static ClaimSyncPayload buildPayload() {
        List<ClaimSyncPayload.FactionClaims> factionList = new ArrayList<>();
        for (Faction faction : Faction.all()) {
            List<Claim> claims = faction.getClaims();
            if (claims.isEmpty()) continue;

            Map<String, List<Long>> byDim = new LinkedHashMap<>();
            for (Claim claim : claims) {
                byDim.computeIfAbsent(claim.level, ignored -> new ArrayList<>())
                        .add(ChunkPos.pack(claim.x, claim.z));
            }

            List<ClaimSyncPayload.FactionClaims.DimClaims> dims = new ArrayList<>();
            for (var entry : byDim.entrySet()) {
                dims.add(
                        new ClaimSyncPayload.FactionClaims.DimClaims(
                                entry.getKey(), entry.getValue()));
            }
            factionList.add(
                    new ClaimSyncPayload.FactionClaims(
                            faction.getName(), faction.getColorValue(), dims));
        }
        return new ClaimSyncPayload(factionList);
    }
}
