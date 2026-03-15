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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.util.Util;

import xyz.nucleoid.server.translations.api.Localization;

import java.util.List;
import java.util.stream.Collectors;

public class MemberCommand implements Command {
    private int self(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        User user = Command.getUser(player);
        if (!user.isInFaction()) {
            new Message(Component.translatable("factions.command.members.fail.no_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        return members(player, user.getFaction());
    }

    private int any(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String factionName = StringArgumentType.getString(context, "faction");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        Faction faction = Faction.getByName(factionName);
        if (faction == null) {
            new Message(Component.translatable("factions.command.members.faction.nonexistent_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        return members(player, faction);
    }

    public static int members(ServerPlayer player, Faction faction) {
        if (FactionsMod.CONFIG.GUI) {
            new MemberGui(player, faction, null);
            return 1;
        }
        List<User> users = faction.getUsers();
        ProfileResolver resolver =
                player.level().getServer().services().profileResolver();

        long memberCount = users.stream().filter(u -> u.rank == User.Rank.MEMBER).count();
        String members =
                ChatFormatting.WHITE
                        + users.stream()
                                .filter(u -> u.rank == User.Rank.MEMBER)
                                .map(
                                        user ->
                                                resolver.fetchById(user.getID())
                                                        .orElse(
                                                                new GameProfile(
                                                                        Util.NIL_UUID,
                                                                        Localization.raw(
                                                                                "factions.gui.generic.unknown_player",
                                                                                player)))
                                                        .name())
                                .collect(Collectors.joining(", "));

        long commanderCount = users.stream().filter(u -> u.rank == User.Rank.COMMANDER).count();
        String commanders =
                ChatFormatting.WHITE
                        + users.stream()
                                .filter(u -> u.rank == User.Rank.COMMANDER)
                                .map(
                                        user ->
                                                resolver.fetchById(user.getID())
                                                        .orElse(
                                                                new GameProfile(
                                                                        Util.NIL_UUID,
                                                                        Localization.raw(
                                                                                "factions.gui.generic.unknown_player",
                                                                                player)))
                                                        .name())
                                .collect(Collectors.joining(", "));

        long leaderCount = users.stream().filter(u -> u.rank == User.Rank.LEADER).count();
        String leaders =
                ChatFormatting.WHITE
                        + users.stream()
                                .filter(u -> u.rank == User.Rank.LEADER)
                                .map(
                                        user ->
                                                resolver.fetchById(user.getID())
                                                        .orElse(
                                                                new GameProfile(
                                                                        Util.NIL_UUID,
                                                                        Localization.raw(
                                                                                "factions.gui.generic.unknown_player",
                                                                                player)))
                                                        .name())
                                .collect(Collectors.joining(", "));

        String owner =
                ChatFormatting.WHITE
                        + users.stream()
                                .filter(u -> u.rank == User.Rank.OWNER)
                                .map(
                                        user ->
                                                resolver.fetchById(user.getID())
                                                        .orElse(
                                                                new GameProfile(
                                                                        Util.NIL_UUID,
                                                                        Localization.raw(
                                                                                "factions.gui.generic.unknown_player",
                                                                                player)))
                                                        .name())
                                .collect(Collectors.joining(", "));

        // generate the ---
        int numDashes = 32 - faction.getName().length();
        String dashes =
                new StringBuilder("--------------------------------").substring(0, numDashes / 2);

        new Message(
                        ChatFormatting.BLACK
                                + dashes
                                + "[ "
                                + faction.getColor()
                                + faction.getName()
                                + ChatFormatting.BLACK
                                + " ]"
                                + dashes)
                .send(player, false);

        new Message(
                        Component.translatable(
                                        "factions.command.members.faction.title",
                                        ChatFormatting.WHITE.toString() + users.size())
                                .withStyle(ChatFormatting.GOLD))
                .send(player, false);
        new Message(
                        Component.translatable("factions.command.members.faction.owner", owner)
                                .withStyle(ChatFormatting.GOLD))
                .send(player, false);
        new Message(
                        Component.translatable(
                                "factions.command.members.faction.leaders", leaderCount, leaders))
                .send(player, false);
        new Message(
                        Component.translatable(
                                "factions.command.members.faction.commanders",
                                commanderCount,
                                commanders))
                .send(player, false);
        new Message(
                        Component.translatable(
                                "factions.command.members.faction.members", memberCount, members))
                .send(player, false);

        return 1;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("members")
                .requires(Command.Requires.hasPerms("factions.members", 0))
                .executes(this::self)
                .then(
                        Commands.argument("faction", StringArgumentType.greedyString())
                                .requires(Command.Requires.hasPerms("factions.members.other", 0))
                                .suggests(Command.Suggests.allFactions())
                                .executes(this::any))
                .build();
    }
}
