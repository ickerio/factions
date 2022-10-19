package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.events.RelationshipEvents;
import io.icker.factions.api.persistents.*;
import io.icker.factions.api.persistents.User.Rank;
import io.icker.factions.util.Message;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;

import static net.minecraft.util.Formatting.RED;
import static net.minecraft.util.Formatting.RESET;

public class WarManager {
    public static void register() {
        PlayerEvents.ON_KILLED_BY_PLAYER.register(WarManager::onKilled);
        PlayerEvents.ON_MOVE.register(WarManager::onMove);
        RelationshipEvents.ON_TRESPASSING.register(WarManager::onTrespassing);
    }

    private static void onKilled(ServerPlayerEntity player, DamageSource source) {
        Faction attackingFaction = User.get(source.getSource().getUuid()).getFaction();
        User killed = User.get(player.getUuid());
        Faction targetFaction = killed.getFaction();

        if (attackingFaction == null || targetFaction == null) return;

        Relationship rel = attackingFaction.getRelationship(targetFaction.getID());
        Relationship rev = attackingFaction.getReverse(rel);

        if (rel.status == Relationship.Status.ENEMY && rev.status == Relationship.Status.ENEMY && FactionsMod.CONFIG.WAR != null && !targetFaction.atWarWith(attackingFaction)) {
            rev.aggression += FactionsMod.CONFIG.WAR.ATTACK_AGGRESSION;
            if (rev.aggression >= FactionsMod.CONFIG.WAR.AGGRESSION_LEVEL) {
                PlayerManager playerManager = player.getServer().getPlayerManager();

                targetFaction.sendCommandTree(playerManager, user -> (user.rank == Rank.LEADER || user.rank == Rank.OWNER) && playerManager.getPlayer(user.getID()) != null);

                new Message("Your faction is now eligible to go to " + RED + "war" + RESET + " with %s", attackingFaction.getName()).hover("Click to go to war").click(String.format("/f war declare %s", attackingFaction.getName())).send(targetFaction);
            }
        } else if (War.getByFactions(attackingFaction, targetFaction) != null) {
            killed.lives--;
        }
    }

    private static void onTrespassing(ServerPlayerEntity player) {
        if (FactionsMod.CONFIG.WAR == null) return;

        User user = User.get(player.getUuid());

        String dimension = player.world.getRegistryKey().getValue().toString();
        ChunkPos chunkPosition = player.getChunkPos();

        Claim claim = Claim.get(chunkPosition.x, chunkPosition.z, dimension);

        claim.getFaction().getRelationship(user.getFaction().getID()).aggression += FactionsMod.CONFIG.WAR.TRESPASSING_AGGRESSION;
    }

    private static void onMove(ServerPlayerEntity player) {
        if (FactionsMod.CONFIG.WAR == null) return;

        User user = User.get(player.getUuid());

        if (!user.isInFaction()) return;

        String dimension = player.world.getRegistryKey().getValue().toString();
        ChunkPos chunkPosition = player.getChunkPos();

        Claim claim = Claim.get(chunkPosition.x, chunkPosition.z, dimension);

        if (claim == null) return;

        if (claim.getFaction().getRelationship(user.getFaction().getID()).status == Relationship.Status.ENEMY && !user.isTrespassing) {
            user.startedTrespassing = player.age;
        }

        user.isTrespassing = claim.getFaction().getRelationship(user.getFaction().getID()).status == Relationship.Status.ENEMY;
    }

    public static boolean eligibleToStartWar(ServerCommandSource source) {
        if (FactionsMod.CONFIG.WAR == null) return false;

        ServerPlayerEntity player = source.getPlayer();
        if (source.getPlayer() == null) return false;

        User user = User.get(player.getUuid());

        if (!user.isInFaction()) return false;

        return user.getFaction().getEnemiesWith().stream().anyMatch(rel -> (rel.aggression >= FactionsMod.CONFIG.WAR.AGGRESSION_LEVEL));
    }

    public static boolean eligibleToJoinWar(ServerCommandSource source) {
        if (FactionsMod.CONFIG.WAR == null) return false;

        ServerPlayerEntity player = source.getPlayer();
        if (source.getPlayer() == null) return false;

        User user = User.get(player.getUuid());

        if (!user.isInFaction()) return false;

        return  War.all().stream().anyMatch(war -> user.getFaction().getMutualAllies().stream().map(rel -> Faction.get(rel.target)).anyMatch(faction -> war.getSourceTeam().contains(faction) || war.getTargetTeam().contains(faction)));
    }

    public static boolean eligibleToEndWar(ServerCommandSource source) {
        if (FactionsMod.CONFIG.WAR == null) return false;

        ServerPlayerEntity player = source.getPlayer();
        if (source.getPlayer() == null) return false;

        User user = User.get(player.getUuid());

        if (!user.isInFaction()) return false;

        Faction faction = user.getFaction();

        return faction.getWars().stream().anyMatch(war -> war.getSource().equals(faction) || war.getTarget().equals(faction));
    }
}
