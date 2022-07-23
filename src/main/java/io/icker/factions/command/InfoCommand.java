package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
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

import static net.minecraft.util.Formatting.*;

public class InfoCommand implements Command {
    //region Constants
    private static final String
            UNCACHED_PLAYER_TEXT = "{Uncached Player}",
            DELIMITER = ", ";
    //endregion

    private int self(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return -1;  // Confirm that it's a player executing the command and not an entity with /execute

        User user = Command.getUser(player);
        if (!user.isInFaction()) {  //return player power
            final String dashes = new StringBuilder("--------------------------------").substring(0, (32 - player.getName().getString().length())/2);
//            final MutableText pinfo = Text.literal(dashes).append("[ ").formatted(BLACK).append(player.getName()).append(" ]").formatted(BLACK).append(dashes);
            final var color = player.getName().getStyle().getColor() == null ?
                    WHITE : Formatting.byName(player.getName().getStyle().getColor().getName());
            new Message(BLACK + dashes + "[ " + color + player.getName().getString() + BLACK + " ]" + dashes).send(player, false);
            new Message(GOLD + "Power: " + GREEN + user.getPower() + "/" + user.getMaxPower())
                    .hover("Current / Max")
                    .send(player, false);
            return 1;
        }

        return info(player, user.getFaction());
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

        return info(player, faction);
    }

    public static int info(ServerPlayerEntity player, Faction faction) {
        List<User> users = faction.getUsers();

        if (player.getServer() == null) return -1;
        UserCache cache = player.getServer().getUserCache();

        String owner = WHITE +
            users.stream()
                .filter(u -> u.rank == User.Rank.OWNER)
                .map(user -> cache.getByUuid(user.getID()).orElse(new GameProfile(Util.NIL_UUID, UNCACHED_PLAYER_TEXT)).getName())
                .collect(Collectors.joining(DELIMITER));

        String usersList = users.stream()
            .map(user -> cache.getByUuid(user.getID()).orElse(new GameProfile(Util.NIL_UUID, UNCACHED_PLAYER_TEXT)).getName())
            .collect(Collectors.joining(DELIMITER));
        
        String mutualAllies = faction.getMutualAllies().stream()
            .map(rel -> Faction.get(rel.target))
            .map(fac -> fac.getColor() + fac.getName())
            .collect(Collectors.joining(Formatting.GRAY + DELIMITER));

        String enemiesWith = Formatting.GRAY + faction.getEnemiesWith().stream()
            .map(rel -> Faction.get(rel.target))
            .map(fac -> fac.getColor() + fac.getName())
            .collect(Collectors.joining(Formatting.GRAY + DELIMITER));

        int requiredPower = faction.getClaims().size() * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;

        // generate the ---
        int numDashes = 32 - faction.getName().length();
        String dashes = new StringBuilder("--------------------------------").substring(0, numDashes/2);

        new Message(BLACK + dashes + "[ " + faction.getColor() + faction.getName() + BLACK + " ]" + dashes)
            .send(player, false);
        new Message(GOLD + "Description: ")
            .add(WHITE + faction.getDescription())
            .send(player, false);
        new Message(GOLD + "Owner: ")
            .add(WHITE + owner)
            .send(player, false);
        new Message(GOLD + "Members (" + WHITE + users.size() + GOLD + "): ")
            .add(usersList)
            .send(player, false);
        new Message(GOLD + "Power: ")
            .add(GREEN.toString() + faction.getPower() + slash() + requiredPower + slash() + faction.calculateMaxPower())
            .hover("Current / Required / Max")
            .send(player, false);
        new Message(GREEN + "Allies (" + WHITE + faction.getMutualAllies().size() + GREEN + "): ")
            .add(mutualAllies)
            .send(player, false);
        new Message(RED + "Enemies (" + WHITE + faction.getEnemiesWith().size() + RED + "): ")
            .add(enemiesWith)
            .send(player, false);

        return 1;
    }

    private static String slash() {
        return Formatting.GRAY + " / " + GREEN;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("info")
            .requires(Requires.hasPerms("factions.info", 0))
            .executes(this::self)
            .then(
                CommandManager.argument("faction", StringArgumentType.greedyString())
                .requires(Requires.hasPerms("factions.info.other", 0))
                .suggests(Suggests.allFactions())
                .executes(this::any)
            )
            .build();
    }
}
