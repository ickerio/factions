package io.icker.factions.core;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Relationship;
import io.icker.factions.api.persistents.User;
import io.icker.factions.api.persistents.User.Rank;
import io.icker.factions.util.Message;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class WarManager {
    public static void register() {
        PlayerEvents.ON_KILLED_BY_PLAYER.register(WarManager::onKilled);
    }

    private static void onKilled(ServerPlayerEntity player, DamageSource source) {
        Faction attackingFaction = User.get(source.getSource().getUuid()).getFaction();
        Faction targetFaction = User.get(player.getUuid()).getFaction();

        if (attackingFaction == null || targetFaction == null) return;

        Relationship rel = attackingFaction.getRelationship(targetFaction.getID());
        Relationship rev = attackingFaction.getReverse(rel);

        if (rel.status == Relationship.Status.ENEMY && rev.status == Relationship.Status.ENEMY && FactionsMod.CONFIG.WAR != null) {
            rev.aggression += FactionsMod.CONFIG.WAR.ATTACK_AGGRESSION;
            if (rev.aggression >= FactionsMod.CONFIG.WAR.AGGRESSION_LEVEL) {
                PlayerManager playerManager = player.getServer().getPlayerManager();
                targetFaction.getUsers()
                    .stream()
                    .filter(user -> (user.rank == Rank.LEADER || user.rank == Rank.OWNER) && playerManager.getPlayer(user.getID()) != null)
                    .forEach(user -> playerManager.sendCommandTree(playerManager.getPlayer(user.getID())));

                new Message("Your faction is now eligible to go to war with %s", attackingFaction.getName()).hover("Click to go to war").click(String.format("/f war declare %s", attackingFaction.getName())).send(targetFaction);
            }
        }
    }

    public static boolean eligibleForWar(ServerCommandSource source) {
        if (FactionsMod.CONFIG.WAR == null) return false;

        ServerPlayerEntity player = source.getPlayer();
        if (source.getPlayer() == null) return false;

        User user = User.get(player.getUuid());
        if (!user.isInFaction()) return false;

        Faction faction = user.getFaction();

        return faction.getEnemiesWith().stream().anyMatch(rel -> rel.aggression >= FactionsMod.CONFIG.WAR.AGGRESSION_LEVEL || faction.getReverse(rel).status == Relationship.Status.WARRING);
    }

    public static boolean eligibleForWar(Faction source, Faction target) {
        if (FactionsMod.CONFIG.WAR == null) return false;
        FactionsMod.LOGGER.info(source.getName(), target.getName());
        return target.getRelationship(source.getID()).status == Relationship.Status.WARRING || source.getRelationship(target.getID()).aggression >= FactionsMod.CONFIG.WAR.AGGRESSION_LEVEL;
    }
}
