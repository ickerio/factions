package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.util.Util;

import xyz.nucleoid.server.translations.api.Localization;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class InviteCommand implements Command {
    private int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        ServerPlayer player = source.getPlayerOrException();
        List<UUID> invites = Command.getUser(player).getFaction().invites;
        int count = invites.size();

        new Message(
                        Component.translatable(
                                "factions.command.invite.list",
                                Component.literal(String.valueOf(count))
                                        .withStyle(ChatFormatting.YELLOW)))
                .send(player, false);

        if (count == 0) return 1;

        ProfileResolver resolver = source.getServer().services().profileResolver();
        String players =
                invites.stream()
                        .map(
                                invite ->
                                        resolver.fetchById(invite)
                                                .orElse(
                                                        new GameProfile(
                                                                Util.NIL_UUID,
                                                                Localization.raw(
                                                                        "factions.gui.generic.unknown_player",
                                                                        player)))
                                                .name())
                        .collect(Collectors.joining(", "));

        new Message(players).format(ChatFormatting.ITALIC).send(player, false);
        return 1;
    }

    private int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        Faction faction = Command.getUser(source.getPlayerOrException()).getFaction();
        if (faction.isInvited(player.getUUID())) {
            new Message(
                            Component.translatable(
                                    "factions.command.invite.add.fail.already_invited",
                                    target.getName().getString()))
                    .fail()
                    .send(player, false);
            return 0;
        }

        User targetUser = User.get(target.getUUID());
        UUID targetFaction = targetUser.isInFaction() ? targetUser.getFaction().getID() : null;
        if (faction.getID().equals(targetFaction)) {
            new Message(
                            Component.translatable(
                                    "factions.command.invite.add.fail.already_member",
                                    target.getName().getString()))
                    .fail()
                    .send(player, false);
            return 0;
        }

        faction.invites.add(target.getUUID());

        new Message(
                        Component.translatable(
                                "factions.command.invite.add.success.actor",
                                target.getName().getString()))
                .send(faction);
        new Message(Component.translatable("factions.command.invite.add.success.subject"))
                .format(ChatFormatting.YELLOW)
                .hover(Component.translatable("factions.command.invite.add.success.subject.hover"))
                .click("/factions join " + faction.getName())
                .prependFaction(faction)
                .send(target, false);
        return 1;
    }

    private int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        Faction faction = Command.getUser(player).getFaction();
        if (faction.invites.remove(target.getUUID())) {
            new Message(
                            Component.translatable(
                                    "factions.command.invite.remove.success",
                                    target.getName().getString()))
                    .send(player, false);
            return 1;
        } else {
            new Message(
                            Component.translatable(
                                    "factions.command.invite.remove.fail",
                                    target.getName().getString()))
                    .fail()
                    .send(player, false);
            return 0;
        }
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("invite")
                .requires(Requires.isCommander())
                .then(
                        Commands.literal("list")
                                .requires(Requires.hasPerms("factions.invite.list", 0))
                                .executes(this::list))
                .then(
                        Commands.literal("add")
                                .requires(Requires.hasPerms("factions.invite.add", 0))
                                .then(
                                        Commands.argument("player", EntityArgument.player())
                                                .executes(this::add)))
                .then(
                        Commands.literal("remove")
                                .requires(Requires.hasPerms("factions.invite.remove", 0))
                                .then(
                                        Commands.argument("player", EntityArgument.player())
                                                .executes(this::remove)))
                .build();
    }
}
