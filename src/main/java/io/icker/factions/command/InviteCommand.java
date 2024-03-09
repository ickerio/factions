package io.icker.factions.command;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;

public class InviteCommand implements Command {
    private int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        List<String> invites = User.get(source.getPlayer().getName().getString()).getFaction().invites;
        int count = invites.size();

        new Message("You have ")
                .add(new Message(String.valueOf(count)).format(Formatting.YELLOW))
                .add(" outgoing invite%s", count == 1 ? "" : "s")
                .send(source.getPlayer(), false);

        if (count == 0) return 1;

        StringBuilder players = new StringBuilder();
        int length = invites.size();
        for(int i = 0; i < length; i++){
            players.append(invites.get(i));
            if(i == length - 1) players.append(",");
        }

        new Message(players.toString()).format(Formatting.ITALIC).send(source.getPlayer(), false);
        return 1;
    }

    private int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = User.get(source.getPlayer().getName().getString()).getFaction();
        if (faction.isInvited(player.getName().getString())) {
            new Message(target.getName().getString() + " was already invited to your faction").format(Formatting.RED).send(player, false);
            return 0;
        }

        User targetUser = User.get(target.getName().getString());
        UUID targetFaction = targetUser.isInFaction() ? targetUser.getFaction().getID() : null;
        if (faction.getID().equals(targetFaction)) {
            new Message(target.getName().getString() + " is already in your faction").format(Formatting.RED).send(player, false);
            return 0;
        }

        faction.invites.add(target.getName().getString());

        new Message(target.getName().getString() + " has been invited")
                .send(faction);
        new Message("You have been invited to join this faction").format(Formatting.YELLOW)
                .hover("Click to join").click("/factions join " + faction.getName())
                .prependFaction(faction)
                .send(target, false);
        return 1;
    }

    private int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = User.get(player.getName().getString()).getFaction();
        faction.invites.remove(target.getName().getString());

        new Message(target.getName().getString() + " is no longer invited to your faction").send(player, false);
        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("invite")
            .requires(Requires.isCommander())
            .then(
                CommandManager
                .literal("list")
                .requires(Requires.hasPerms("factions.invite.list", 0))
                .executes(this::list)
            )
            .then(
                CommandManager
                .literal("add")
                .requires(Requires.hasPerms("factions.invite.add", 0))
                .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                    .executes(this::add)
                )
            )
            .then(
                CommandManager
                .literal("remove")
                .requires(Requires.hasPerms("factions.invite.remove", 0))
                .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                    .executes(this::remove)
                )
            )
            .build();
    }
}