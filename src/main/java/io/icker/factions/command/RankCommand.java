package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import xyz.nucleoid.server.translations.api.Localization;

import java.util.UUID;

public class RankCommand implements Command {
    private int promote(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        if (target.getUuid().equals(player.getUuid())) {
            new Message(Text.translatable("factions.command.rank.promote.fail.self"))
                    .fail()
                    .send(player, false);

            return 0;
        }

        Faction faction = Command.getUser(player).getFaction();

        for (User user : faction.getUsers())
            if (user.getID().equals(target.getUuid())) {

                try {
                    execPromote(user, player);
                } catch (Exception e) {
                    new Message(e.getMessage()).fail().send(player, false);
                    return 0;
                }

                context.getSource().getServer().getPlayerManager().sendCommandTree(target);

                new Message(
                                Text.translatable(
                                        "factions.command.rank.promote.success",
                                        target.getName().getString(),
                                        Text.translatable(
                                                "factions.command.rank."
                                                        + User.get(target.getUuid())
                                                                .getRankName())))
                        .prependFaction(faction)
                        .send(player, false);

                return 1;
            }

        new Message(
                        Text.translatable(
                                "factions.command.rank.promote.fail.not_in_faction",
                                target.getName().getString()))
                .fail()
                .send(player, false);
        return 0;
    }

    public static void execPromote(User target, ServerPlayerEntity initiator) throws Exception {
        switch (target.rank) {
            case GUEST -> target.rank = User.Rank.MEMBER;
            case MEMBER -> target.rank = User.Rank.COMMANDER;
            case COMMANDER -> target.rank = User.Rank.LEADER;
            case LEADER -> {
                throw new Exception(
                        Localization.raw("factions.command.rank.promote.fail.leader", initiator));
            }
            case OWNER -> {
                throw new Exception(
                        Localization.raw("factions.command.rank.promote.fail.owner", initiator));
            }
        }
    }

    private int demote(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        if (target.getUuid().equals(player.getUuid())) {
            new Message(Text.translatable("factions.command.rank.demote.fail.self"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        Faction faction = Command.getUser(player).getFaction();

        for (User user : faction.getUsers())
            if (user.getID().equals(target.getUuid())) {

                try {
                    execDemote(user, player);
                } catch (Exception e) {
                    new Message(e.getMessage()).fail().send(player, false);
                    return 0;
                }

                context.getSource().getServer().getPlayerManager().sendCommandTree(target);

                new Message(
                                Text.translatable(
                                        "factions.command.rank.demote.success",
                                        target.getName().getString(),
                                        Text.translatable(
                                                "factions.command.rank."
                                                        + User.get(target.getUuid())
                                                                .getRankName())))
                        .prependFaction(faction)
                        .send(player, false);

                return 1;
            }

        new Message(
                        Text.translatable(
                                "factions.command.rank.demote.fail.not_in_faction",
                                target.getName().getString()))
                .fail()
                .send(player, false);
        return 0;
    }

    public static void execDemote(User target, ServerPlayerEntity initiator) throws Exception {
        switch (target.rank) {
            case GUEST ->
                    throw new Exception(
                            Localization.raw("factions.command.rank.demote.fail.guest", initiator));
            case MEMBER -> target.rank = User.Rank.GUEST;
            case COMMANDER -> target.rank = User.Rank.MEMBER;
            case LEADER -> {
                if (Command.getUser(initiator).rank == User.Rank.LEADER) {
                    throw new Exception(
                            Localization.raw(
                                    "factions.command.rank.demote.fail.leader", initiator));
                }
                target.rank = User.Rank.COMMANDER;
            }
            case OWNER ->
                    throw new Exception(
                            Localization.raw("factions.command.rank.demote.fail.owner", initiator));
        }
    }

    private int transfer(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        if (target.getUuid().equals(player.getUuid())) {
            new Message(Text.translatable("factions.command.rank.transfer.fail.self"))
                    .fail()
                    .send(player, false);

            return 0;
        }

        User targetUser = User.get(target.getUuid());
        UUID targetFaction = targetUser.isInFaction() ? targetUser.getFaction().getID() : null;
        if (Command.getUser(player).getFaction().getID().equals(targetFaction)) {
            targetUser.rank = User.Rank.OWNER;
            Command.getUser(player).rank = User.Rank.LEADER;

            context.getSource().getServer().getPlayerManager().sendCommandTree(player);
            context.getSource().getServer().getPlayerManager().sendCommandTree(target);

            new Message(
                            Text.translatable(
                                    "factions.command.rank.transfer.success",
                                    target.getName().getString()))
                    .prependFaction(Faction.get(targetFaction))
                    .send(player, false);

            return 1;
        }

        new Message(
                        Text.translatable(
                                "factions.command.rank.transfer.fail.not_in_faction",
                                target.getName().getString()))
                .fail()
                .send(player, false);
        return 0;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("rank")
                .requires(Requires.isLeader())
                .then(
                        CommandManager.literal("promote")
                                .requires(Requires.hasPerms("factions.rank.promote", 0))
                                .then(
                                        CommandManager.argument(
                                                        "player", EntityArgumentType.player())
                                                .executes(this::promote)))
                .then(
                        CommandManager.literal("demote")
                                .requires(Requires.hasPerms("factions.rank.demote", 0))
                                .then(
                                        CommandManager.argument(
                                                        "player", EntityArgumentType.player())
                                                .executes(this::demote)))
                .then(
                        CommandManager.literal("transfer")
                                .requires(
                                        Requires.multiple(
                                                Requires.hasPerms("factions.rank.transfer", 0),
                                                Requires.isOwner()))
                                .then(
                                        CommandManager.argument(
                                                        "player", EntityArgumentType.player())
                                                .executes(this::transfer)))
                .build();
    }
}
