package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.text.FactionText;
import io.icker.factions.text.Message;
import io.icker.factions.text.TranslatableText;
import io.icker.factions.util.Command;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Objects;
import java.util.UUID;

public class RankCommand implements Command {
    private int change(CommandContext<ServerCommandSource> context, boolean promote) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (target.getUuid().equals(player.getUuid())) {
            new Message().append(new TranslatableText("rank.error.self").fail()).send(player, false);

            return 0;
        }

        Faction faction = Command.getUser(player).getFaction();

        User user = User.get(target.getUuid());

        if (!Objects.equals(user.getFaction(), faction)) {
            new Message().append(new TranslatableText("rank.error.not-in-faction", target.getName().getString()).fail()).send(player, false);
            return 0;
        }

        if (promote) {
            switch (user.rank) {
                case GUEST -> user.rank = User.Rank.MEMBER;
                case MEMBER -> user.rank = User.Rank.COMMANDER;
                case COMMANDER -> user.rank = User.Rank.LEADER;
                case LEADER -> {
                    new Message().append(new TranslatableText("rank.error.leader-owner").fail()).send(player, false);
                    return 0;
                }
                case OWNER -> {
                    new Message().append(new TranslatableText("rank.error.owner").fail()).send(player, false);
                    return 0;
                }
            }
        } else {
            switch (user.rank) {
                case GUEST -> {
                    new Message().append(new TranslatableText("rank.error.guest").fail()).send(player, false);
                    return 0;
                }
                case MEMBER -> user.rank = User.Rank.GUEST;
                case COMMANDER -> user.rank = User.Rank.MEMBER;
                case LEADER -> {
                    if (Command.getUser(player).rank == User.Rank.LEADER) {
                        new Message().append(new TranslatableText("rank.error.leader").fail()).send(player, false);
                        return 0;
                    }

                    user.rank = User.Rank.COMMANDER;
                }
                case OWNER -> {
                    new Message().append(new TranslatableText("rank.error.owner").fail()).send(player, false);
                    return 0;
                }
            }
        }

        context.getSource().getServer().getPlayerManager().sendCommandTree(target);

        new Message().append(new TranslatableText("rank.success", target.getName().getString(), User.get(target.getUuid()).getRankName()))
                .prepend(new FactionText(faction))
                .send(player, false);

        return 1;
    }

    private int promote(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return change(context, true);
    }

    private int demote(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return change(context, false);
    }

    private int transfer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (target.getUuid().equals(player.getUuid())) {
            new Message().append(new TranslatableText("rank.transfer.error.self").fail()).send(player, false);

            return 0;
        }

        User targetUser = User.get(target.getUuid());
        UUID targetFaction = targetUser.isInFaction() ? targetUser.getFaction().getID() : null;
        if (Command.getUser(player).getFaction().getID().equals(targetFaction)) {
            targetUser.rank = User.Rank.OWNER;
            Command.getUser(player).rank = User.Rank.LEADER;

            context.getSource().getServer().getPlayerManager().sendCommandTree(player);
            context.getSource().getServer().getPlayerManager().sendCommandTree(target);

            new Message().append(new TranslatableText("rank.transfer.success", target.getName().getString()))
                .prepend(new FactionText(Faction.get(targetFaction)))
                .send(player, false);

            return 1;
        }

        new Message().append(new TranslatableText("rank.error.not-in-faction", target.getName().getString()).fail()).send(player, false);
        return 0;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("rank")
            .requires(Requires.isLeader())
            .then(
                CommandManager
                .literal("promote")
                .requires(Requires.hasPerms("factions.rank.promote", 0))
                .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                    .executes(this::promote)
                )
            )
            .then(
                CommandManager
                .literal("demote")
                .requires(Requires.hasPerms("factions.rank.demote", 0))
                .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                    .executes(this::demote)
                )
            )
            .then(
                CommandManager
                .literal("transfer")
                .requires(Requires.multiple(Requires.hasPerms("factions.rank.transfer", 0), Requires.isOwner()))
                .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                    .executes(this::transfer)
                )
            )
            .build();
    }
}
