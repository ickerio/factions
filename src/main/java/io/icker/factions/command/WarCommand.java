package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.api.persistents.War;
import io.icker.factions.core.WarManager;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.util.Formatting.RED;
import static net.minecraft.util.Formatting.RESET;

public class WarCommand implements Command {
    private int declare (CommandContext<ServerCommandSource> context) {
        if (FactionsMod.CONFIG.WAR == null) return 0;

        String name = StringArgumentType.getString(context, "faction");
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction targetFaction = Faction.getByName(name);

        if (targetFaction == null) {
            new Message("Cannot go to war with a nonexistent faction").fail().send(player, false);
            return 0;
        }

        Faction sourceFaction = Command.getUser(player).getFaction();

        if (sourceFaction.equals(targetFaction)) {
            new Message("Cannot declare war on yourself").fail().send(player, false);
            return 0;
        }

        if (War.getByFactions(sourceFaction, targetFaction) != null) {
            new Message("You are already at war with that faction").fail().send(player, false);
            return 0;
        }

        targetFaction.getRelationship(sourceFaction.getID()).aggression = 0;
        sourceFaction.getRelationship(targetFaction.getID()).aggression = 0;

        War.add(new War(sourceFaction, targetFaction));

        new Message("You have declared " + RED + "war" + RESET + " on " + targetFaction.getName()).send(sourceFaction);

        PlayerManager playerManager = player.getServer().getPlayerManager();
        sourceFaction.sendCommandTree(playerManager, user -> (user.rank == User.Rank.LEADER || user.rank == User.Rank.OWNER) && playerManager.getPlayer(user.getID()) != null);

        sourceFaction.getUsers().forEach(user -> user.lives = FactionsMod.CONFIG.WAR.NUM_LIVES);

        new Message(sourceFaction.getName() + " have declared " + RED + "war" + RESET + " on you")
            .send(targetFaction);

        return 1;
    }

    private int join (CommandContext<ServerCommandSource> context) {
        if (FactionsMod.CONFIG.WAR == null) return 0;
        String name = StringArgumentType.getString(context, "war");
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        War war = War.getByName(name);
        FactionsMod.LOGGER.info(war);
        Faction faction = User.get(player.getUuid()).getFaction();

        if (war == null) {
            new Message("Couldn't find that war").fail().send(player, false);
            return 0;
        }

        boolean alliesOnTarget = war.getTargetTeam().stream().anyMatch(faction1 -> faction.isMutualAllies(faction1.getID()));
        boolean alliesOnSource = war.getSourceTeam().stream().anyMatch(faction1 -> faction.isMutualAllies(faction1.getID()));

        if (alliesOnSource && alliesOnTarget) {
            new Message("Since you have allies on both sides you cannot take part in this war").fail().send(player, false);
            return 0;
        }

        String teamName;

        if (alliesOnTarget) {
            war.addTarget(faction);
            teamName = war.getTarget().getName();
        } else if (alliesOnSource) {
            war.addSource(faction);
            teamName = war.getSource().getName();
        } else {
            new Message("You must be allied with someone in that war to join").fail().send(player, false);
            return 0;
        }

        new Message("You have joined %s in the %s", teamName, war.getName()).send(faction);

        PlayerManager playerManager = player.getServer().getPlayerManager();
        faction.sendCommandTree(playerManager, user -> (user.rank == User.Rank.LEADER || user.rank == User.Rank.OWNER) && playerManager.getPlayer(user.getID()) != null);

        faction.getUsers().forEach(user -> user.lives = FactionsMod.CONFIG.WAR.NUM_LIVES);

        new Message("%s have joined the war", faction.getName())
                .send(war);

        return 1;
    }

    private int end (CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "war");
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        War war = War.getByName(name);
        Faction faction = User.get(player.getUuid()).getFaction();

        if (war == null) {
            new Message("Could not find the war").fail().send(player, false);
            return 0;
        }

        boolean isSource = war.getSource().equals(faction);

        if (!isSource && !war.getTarget().equals(faction)) {
            new Message("You cannot end the war").fail().send(player, false);
            return 0;
        }

        Faction other = isSource ? war.getTarget() : war.getSource();

        if ((isSource && !war.targetReadyToEnd) || (!isSource && !war.sourceReadyToEnd)) {
            if (isSource) war.sourceReadyToEnd = true;
            else war.targetReadyToEnd = true;

            PlayerManager playerManager = player.getServer().getPlayerManager();
            other.sendCommandTree(playerManager, user -> (user.rank == User.Rank.LEADER || user.rank == User.Rank.OWNER) && playerManager.getPlayer(user.getID()) != null);


            new Message("%s must also agree to end the war (they have been asked if they want to end it)", other.getName()).send(player, false);
            new Message("%s would like to end the war", faction.getName()).hover("Click to end the war").click(String.format("/f war end %s", war.getName())).send(other);
            return 0;
        }

        war.end();

        new Message("You are no longer at war with " + other.getName()).send(faction);
        new Message(faction.getName() + " has ended the war").send(other);

        PlayerManager playerManager = player.getServer().getPlayerManager();
        faction.sendCommandTree(playerManager, user -> (user.rank == User.Rank.LEADER || user.rank == User.Rank.OWNER) && playerManager.getPlayer(user.getID()) != null);
        other.sendCommandTree(playerManager, user -> (user.rank == User.Rank.LEADER || user.rank == User.Rank.OWNER) && playerManager.getPlayer(user.getID()) != null);

        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("war")
            .requires(Requires.multiple(Requires.isLeader(), (source) -> FactionsMod.CONFIG.WAR != null, Requires.hasPerms("factions.war", 0)))
            .then(
                CommandManager.literal("declare")
                .requires(Requires.multiple(WarManager::eligibleToStartWar, Requires.hasPerms("factions.war.declare", 0)))
                .then(
                    CommandManager.argument("faction", StringArgumentType.greedyString())
                    .suggests(Suggests.eligibleForWar())
                    .executes(this::declare)
                )
            )
            .then(
                CommandManager.literal("join")
                .requires(Requires.multiple(WarManager::eligibleToJoinWar, Requires.hasPerms("factions.war.join", 0)))
                .then(
                    CommandManager.argument("war", StringArgumentType.greedyString())
                    .suggests(Suggests.joinableWars())
                    .executes(this::join)
                )
            )
            .then(
                CommandManager.literal("end")
                .requires(Requires.multiple(WarManager::eligibleToEndWar, Requires.hasPerms("factions.war.end", 0)))
                .then(
                    CommandManager.argument("war", StringArgumentType.greedyString())
                        .suggests(Suggests.atWar())
                        .executes(this::end)
                )
            ).build();
    }
}
