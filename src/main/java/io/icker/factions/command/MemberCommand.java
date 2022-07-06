package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;

import java.util.List;
import java.util.stream.Collectors;

public class MemberCommand implements Command {
    private int self(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return -1;  // Confirm that it's a player executing the command and not an entity with /execute

        User user = Command.getUser(player);
        if (!user.isInFaction()) {
            new Message("Command can only be used whilst in a faction").fail().send(player, false);
            return 0;
        }

        return members(player, user.getFaction());
    }

    private int any(CommandContext<ServerCommandSource> context) {
        String factionName = StringArgumentType.getString(context, "faction");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return -1;  // Confirm that it's a player executing the command and not an entity with /execute

        Faction faction = Faction.getByName(factionName);
        if (faction == null) {
            new Message("Faction does not exist").fail().send(player, false);
            return 0;
        }

        return members(player, faction);
    }

    public static int members(ServerPlayerEntity player, Faction faction) {
        List<User> users = faction.getUsers();
        if (player.getServer() == null) return -1;
        UserCache cache = player.getServer().getUserCache();

        long memberCount = users.stream().filter(u -> u.rank == User.Rank.MEMBER).count();
        String members = Formatting.WHITE +
            users.stream()
                .filter(u -> u.rank == User.Rank.MEMBER)
                .map(user -> cache.getByUuid(user.getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
                .collect(Collectors.joining(", "));

        long commanderCount = users.stream().filter(u -> u.rank == User.Rank.COMMANDER).count();
        String commanders = Formatting.WHITE +
            users.stream()
                .filter(u -> u.rank == User.Rank.COMMANDER)
                .map(user -> cache.getByUuid(user.getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
                .collect(Collectors.joining(", "));

        long leaderCount = users.stream().filter(u -> u.rank == User.Rank.LEADER).count();
        String leaders = Formatting.WHITE +
            users.stream()
                .filter(u -> u.rank == User.Rank.LEADER)
                .map(user -> cache.getByUuid(user.getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
                .collect(Collectors.joining(", "));

        String owner = Formatting.WHITE +
            users.stream()
                .filter(u -> u.rank == User.Rank.OWNER)
                .map(user -> cache.getByUuid(user.getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
                .collect(Collectors.joining(", "));

        // generate the ---
        int totalChars = 32;
        StringBuilder dashes = new StringBuilder();
        dashes.append("-".repeat(Math.max(0, (totalChars - faction.getName().length()) / 2)));

        new Message(Formatting.BLACK + dashes.toString() + "[ " + faction.getColor() + faction.getName() + Formatting.BLACK + " ]" + dashes)
            .send(player, false);
        new Message(Formatting.GOLD + "Total Members: ")
            .add(Formatting.WHITE.toString() + users.size())
            .send(player, false);
        new Message(Formatting.GOLD + "Owner: ")
            .add(owner)
            .send(player, false);
        new Message(Formatting.GOLD + "Leaders (" + Formatting.WHITE + leaderCount + Formatting.GOLD + "): ")
            .add(leaders)
            .send(player, false);
        new Message(Formatting.GOLD + "Commanders (" + Formatting.WHITE + commanderCount + Formatting.GOLD + "): ")
            .add(commanders)
            .send(player, false);
        new Message(Formatting.GOLD + "Members (" + Formatting.WHITE + memberCount + Formatting.GOLD + "): ")
            .add(members)
            .send(player, false);

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("members")
            .requires(Command.Requires.hasPerms("factions.members", 0))
            .executes(this::self)
            .then(
                CommandManager.argument("faction", StringArgumentType.greedyString())
                    .requires(Command.Requires.hasPerms("factions.members.other", 0))
                    .suggests(Command.Suggests.allFactions())
                    .executes(this::any)
            )
            .build();
    }
}
