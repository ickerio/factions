package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.ui.MemberGui;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;

import xyz.nucleoid.server.translations.api.Localization;

import java.util.List;
import java.util.stream.Collectors;

public class MemberCommand implements Command {
    private int self(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User user = Command.getUser(player);
        if (!user.isInFaction()) {
            new Message(Text.translatable("factions.command.members.fail.no_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        return members(player, user.getFaction());
    }

    private int any(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String factionName = StringArgumentType.getString(context, "faction");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Faction.getByName(factionName);
        if (faction == null) {
            new Message(Text.translatable("factions.command.members.faction.nonexistent_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        return members(player, faction);
    }

    public static int members(ServerPlayerEntity player, Faction faction) {
        if (FactionsMod.CONFIG.GUI) {
            new MemberGui(player, faction, null);
            return 1;
        }
        List<User> users = faction.getUsers();
        UserCache cache = player.getServer().getUserCache();

        long memberCount = users.stream().filter(u -> u.rank == User.Rank.MEMBER).count();
        String members =
                Formatting.WHITE
                        + users.stream()
                                .filter(u -> u.rank == User.Rank.MEMBER)
                                .map(
                                        user ->
                                                cache.getByUuid(user.getID())
                                                        .orElse(
                                                                new GameProfile(
                                                                        Util.NIL_UUID,
                                                                        Localization.raw(
                                                                                "factions.gui.generic.unknown_player",
                                                                                player)))
                                                        .getName())
                                .collect(Collectors.joining(", "));

        long commanderCount = users.stream().filter(u -> u.rank == User.Rank.COMMANDER).count();
        String commanders =
                Formatting.WHITE
                        + users.stream()
                                .filter(u -> u.rank == User.Rank.COMMANDER)
                                .map(
                                        user ->
                                                cache.getByUuid(user.getID())
                                                        .orElse(
                                                                new GameProfile(
                                                                        Util.NIL_UUID,
                                                                        Localization.raw(
                                                                                "factions.gui.generic.unknown_player",
                                                                                player)))
                                                        .getName())
                                .collect(Collectors.joining(", "));

        long leaderCount = users.stream().filter(u -> u.rank == User.Rank.LEADER).count();
        String leaders =
                Formatting.WHITE
                        + users.stream()
                                .filter(u -> u.rank == User.Rank.LEADER)
                                .map(
                                        user ->
                                                cache.getByUuid(user.getID())
                                                        .orElse(
                                                                new GameProfile(
                                                                        Util.NIL_UUID,
                                                                        Localization.raw(
                                                                                "factions.gui.generic.unknown_player",
                                                                                player)))
                                                        .getName())
                                .collect(Collectors.joining(", "));

        String owner =
                Formatting.WHITE
                        + users.stream()
                                .filter(u -> u.rank == User.Rank.OWNER)
                                .map(
                                        user ->
                                                cache.getByUuid(user.getID())
                                                        .orElse(
                                                                new GameProfile(
                                                                        Util.NIL_UUID,
                                                                        Localization.raw(
                                                                                "factions.gui.generic.unknown_player",
                                                                                player)))
                                                        .getName())
                                .collect(Collectors.joining(", "));

        // generate the ---
        int numDashes = 32 - faction.getName().length();
        String dashes =
                new StringBuilder("--------------------------------").substring(0, numDashes / 2);

        new Message(
                        Formatting.BLACK
                                + dashes
                                + "[ "
                                + faction.getColor()
                                + faction.getName()
                                + Formatting.BLACK
                                + " ]"
                                + dashes)
                .send(player, false);

        new Message(
                        Text.translatable(
                                        "factions.command.members.faction.title",
                                        Formatting.WHITE.toString() + users.size())
                                .formatted(Formatting.GOLD))
                .send(player, false);
        new Message(
                        Text.translatable("factions.command.members.faction.owner", owner)
                                .formatted(Formatting.GOLD))
                .send(player, false);
        new Message(
                        Text.translatable(
                                "factions.command.members.faction.leaders", leaderCount, leaders))
                .send(player, false);
        new Message(
                        Text.translatable(
                                "factions.command.members.faction.commanders",
                                commanderCount,
                                commanders))
                .send(player, false);
        new Message(
                        Text.translatable(
                                "factions.command.members.faction.members", memberCount, members))
                .send(player, false);

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("members")
                .requires(Command.Requires.hasPerms("factions.members", 0))
                .executes(this::self)
                .then(
                        CommandManager.argument("faction", StringArgumentType.greedyString())
                                .requires(Command.Requires.hasPerms("factions.members.other", 0))
                                .suggests(Command.Suggests.allFactions())
                                .executes(this::any))
                .build();
    }
}
