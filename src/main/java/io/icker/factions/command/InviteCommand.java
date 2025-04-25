package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;

import xyz.nucleoid.server.translations.api.Localization;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class InviteCommand implements Command {
    private int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayer();
        List<UUID> invites = Command.getUser(player).getFaction().invites;
        int count = invites.size();

        new Message(
                        Text.translatable(
                                "factions.command.invite.list",
                                Text.literal(String.valueOf(count)).formatted(Formatting.YELLOW)))
                .send(player, false);

        if (count == 0) return 1;

        UserCache cache = source.getServer().getUserCache();
        String players =
                invites.stream()
                        .map(
                                invite ->
                                        cache.getByUuid(invite)
                                                .orElse(
                                                        new GameProfile(
                                                                Util.NIL_UUID,
                                                                Localization.raw(
                                                                        "factions.gui.generic.unknown_player",
                                                                        player)))
                                                .getName())
                        .collect(Collectors.joining(", "));

        new Message(players).format(Formatting.ITALIC).send(player, false);
        return 1;
    }

    private int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Command.getUser(source.getPlayer()).getFaction();
        if (faction.isInvited(player.getUuid())) {
            new Message(
                            Text.translatable(
                                    "factions.command.invite.add.fail.already_invited",
                                    target.getName().getString()))
                    .fail()
                    .send(player, false);
            return 0;
        }

        User targetUser = User.get(target.getUuid());
        UUID targetFaction = targetUser.isInFaction() ? targetUser.getFaction().getID() : null;
        if (faction.getID().equals(targetFaction)) {
            new Message(
                            Text.translatable(
                                    "factions.command.invite.add.fail.already_member",
                                    target.getName().getString()))
                    .fail()
                    .send(player, false);
            return 0;
        }

        faction.invites.add(target.getUuid());

        new Message(
                        Text.translatable(
                                "factions.command.invite.add.success.actor",
                                target.getName().getString()))
                .send(faction);
        new Message(Text.translatable("factions.command.invite.add.success.subject"))
                .format(Formatting.YELLOW)
                .hover(Text.translatable("factions.command.invite.add.success.subject.hover"))
                .click("/factions join " + faction.getName())
                .prependFaction(faction)
                .send(target, false);
        return 1;
    }

    private int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();
        if (faction.invites.remove(target.getUuid())) {
            new Message(
                            Text.translatable(
                                    "factions.command.invite.remove.success",
                                    target.getName().getString()))
                    .send(player, false);
            return 1;
        } else {
            new Message(
                            Text.translatable(
                                    "factions.command.invite.remove.fail",
                                    target.getName().getString()))
                    .fail()
                    .send(player, false);
            return 0;
        }
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("invite")
                .requires(Requires.isCommander())
                .then(
                        CommandManager.literal("list")
                                .requires(Requires.hasPerms("factions.invite.list", 0))
                                .executes(this::list))
                .then(
                        CommandManager.literal("add")
                                .requires(Requires.hasPerms("factions.invite.add", 0))
                                .then(
                                        CommandManager.argument(
                                                        "player", EntityArgumentType.player())
                                                .executes(this::add)))
                .then(
                        CommandManager.literal("remove")
                                .requires(Requires.hasPerms("factions.invite.remove", 0))
                                .then(
                                        CommandManager.argument(
                                                        "player", EntityArgumentType.player())
                                                .executes(this::remove)))
                .build();
    }
}
