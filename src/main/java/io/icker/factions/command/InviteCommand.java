package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.text.FactionText;
import io.icker.factions.text.Message;
import io.icker.factions.text.PlainText;
import io.icker.factions.text.TranslatableText;
import io.icker.factions.util.Command;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class InviteCommand implements Command {
    private int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        List<UUID> invites = Command.getUser(source.getPlayer()).getFaction().invites;
        int count = invites.size();

        new Message().append(new TranslatableText("translate:invite.info", count))
                .send(source.getPlayer(), false);

        if (count == 0) return 1;

        UserCache cache = source.getServer().getUserCache();
        String players = invites.stream()
            .map(invite -> cache.getByUuid(invite).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
            .collect(Collectors.joining(", "));

        new Message().append(new PlainText(players).format(Formatting.ITALIC)).send(source.getPlayer(), false);
        return 1;
    }

    private int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Command.getUser(source.getPlayer()).getFaction();
        if (faction.isInvited(player.getUuid())) {
            new Message().append(new TranslatableText("translate:invite.error.already-invited", target.getName().getString()).fail()).send(player, false);
            return 0;
        }

        User targetUser = User.get(target.getUuid());
        UUID targetFaction = targetUser.isInFaction() ? targetUser.getFaction().getID() : null;
        if (faction.getID().equals(targetFaction)) {
            new Message().append(new TranslatableText("translate:invite.error.already-in", target.getName().getString()).fail()).send(player, false);
            return 0;
        }

        faction.invites.add(target.getUuid());

        new Message().append(new TranslatableText("translate:invite.notice.self", target.getName().getString()))
                .send(faction);

        new Message().append(new TranslatableText("translate:invite.notice")
                .hover("translate:invite.notice.hover").click("/factions join " + faction.getName()).format(Formatting.YELLOW))
                .prepend(new FactionText(faction))
                .send(target, false);
        return 1;
    }

    private int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Command.getUser(player).getFaction();
        faction.invites.remove(target.getUuid());

        new Message().append(new TranslatableText("translate:invite.notice.not-invited", target.getName().getString())).send(player, false);
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