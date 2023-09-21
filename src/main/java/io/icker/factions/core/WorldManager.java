package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.MiscEvents;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Message;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;

public class WorldManager {
    public static void register() {
        PlayerEvents.ON_MOVE.register(WorldManager::onMove);
        MiscEvents.ON_MOB_SPAWN_ATTEMPT.register(WorldManager::onMobSpawnAttempt);
    }

    private static void onMobSpawnAttempt() {
        // TODO Implement this
    }

    private static void onMove(ServerPlayerEntity player) {
        User user = User.get(player.getUuid());
        ServerWorld world = (ServerWorld) player.getWorld();
        String dimension = world.getRegistryKey().getValue().toString();

        ChunkPos chunkPos = world.getChunk(player.getBlockPos()).getPos();

        Claim claim = Claim.get(chunkPos.x, chunkPos.z, dimension);
        if (user.autoclaim && claim == null) {
            Faction faction = user.getFaction();
            int requiredPower =
                    (faction.getClaims().size() + 1) * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
            int maxPower = faction.getUsers().size() * FactionsMod.CONFIG.POWER.MEMBER
                    + FactionsMod.CONFIG.POWER.BASE;

            if (maxPower < requiredPower) {
                new Message("Not enough faction power to claim chunk, autoclaim toggled off").fail()
                        .send(player, false);
                user.autoclaim = false;
            } else {
                faction.addClaim(chunkPos.x, chunkPos.z, dimension);
                claim = Claim.get(chunkPos.x, chunkPos.z, dimension);
                new Message("Chunk (%d, %d) claimed by %s", chunkPos.x, chunkPos.z,
                        player.getName().getString()).send(faction);
            }
        }
        if (user.radar) {
            if (claim != null) {
                new Message(claim.getFaction().getName()).format(claim.getFaction().getColor())
                        .send(player, true);
            } else {
                new Message("Wilderness").format(Formatting.GREEN).send(player, true);
            }
        }
    }
}
