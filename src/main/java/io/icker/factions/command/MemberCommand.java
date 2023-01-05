package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.text.Message;
import io.icker.factions.text.PlainText;
import io.icker.factions.text.TranslatableText;
import io.icker.factions.util.Command;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MemberCommand implements Command {
    private int self(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User user = Command.getUser(player);
        if (!user.isInFaction()) {
            new Message().append(new TranslatableText("translate:info.error.factionless").fail()).send(player, false);
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
            new Message().append(new TranslatableText("translate:info.error.not-exist").fail()).send(player, false);
            return 0;
        }

        return members(player, faction);
    }

    private static String getListString(List<User> users, UserCache cache, Predicate<User> predicate) {
        return Formatting.WHITE +
                users.stream()
                        .filter(predicate)
                        .map(user -> cache.getByUuid(user.getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
                        .collect(Collectors.joining(", "));
    }

    public static int members(ServerPlayerEntity player, Faction faction) {
        List<User> users = faction.getUsers();
        UserCache cache = player.getServer().getUserCache();

        long memberCount = users.stream().filter(u -> u.rank == User.Rank.MEMBER).count();
        String members = getListString(users, cache, u -> u.rank == User.Rank.MEMBER);

        long commanderCount = users.stream().filter(u -> u.rank == User.Rank.COMMANDER).count();
        String commanders = getListString(users, cache, u -> u.rank == User.Rank.COMMANDER);

        long leaderCount = users.stream().filter(u -> u.rank == User.Rank.LEADER).count();
        String leaders = getListString(users, cache, u -> u.rank == User.Rank.LEADER);

        long guestCount = users.stream().filter(u -> u.rank == User.Rank.GUEST).count();
        String guests = getListString(users, cache, u -> u.rank == User.Rank.GUEST);

        Optional<User> ownerUser = users.stream()
                .filter(u -> u.rank == User.Rank.OWNER)
                .findFirst();

        String owner = Formatting.WHITE + (ownerUser.isPresent() ? cache.getByUuid(ownerUser.get().getID()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName() : "Not found");

        // generate the ---
        int numDashes = 32 - faction.getName().length();
        String dashes = new StringBuilder("--------------------------------").substring(0, numDashes/2);

        new Message()
                .append(new PlainText(Formatting.BLACK + dashes + "[ " + faction.getColor() + faction.getName() + Formatting.BLACK + " ]" + dashes))
                .append(new TranslatableText("translate:member.total", users.size()))
                .append(new TranslatableText("translate:member.owner", owner))
                .append(new TranslatableText("translate:member.leaders", leaderCount, leaders))
                .append(new TranslatableText("translate:member.commanders", commanderCount, commanders))
                .append(new TranslatableText("translate:member.members", memberCount, members))
                .append(new TranslatableText("translate:member.guests", guestCount, guests))
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
