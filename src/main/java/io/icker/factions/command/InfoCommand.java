package io.icker.factions.command;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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

public class InfoCommand implements Command {
    private int self(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User user = User.get(player.getName().getString());
        if (!user.isInFaction()) {
            new Message("Command can only be used whilst in a faction").fail().send(player, false);
            return 0;
        }

        return info(player, user.getFaction());
    }

    private int any(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String factionName = StringArgumentType.getString(context, "faction");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction faction = Faction.getByName(factionName);
        if (faction == null) {
            new Message("Faction does not exist").fail().send(player, false);
            return 0;
        }

        return info(player, faction);
    }

    public static int info(ServerPlayerEntity player, Faction faction) {
        List<User> users = faction.getUsers();

        String userText = Formatting.WHITE.toString() + users.size() + Formatting.GRAY + 
            (FactionsMod.CONFIG.MAX_FACTION_SIZE != -1 ? "/" + FactionsMod.CONFIG.MAX_FACTION_SIZE : (" Member" + (users.size() != 1 ? "s" : "")));

        String commanderText = Formatting.WHITE + 
            String.valueOf(users.stream().filter(u -> u.rank == User.Rank.COMMANDER).count()) + Formatting.GRAY + " Commanders";
        
        String leaderText = Formatting.WHITE + 
            String.valueOf(users.stream().filter(u -> u.rank == User.Rank.LEADER).count()) + Formatting.GRAY + " Leaders";

        UserCache cache = player.getServer().getUserCache();
        String usersList = users.stream()
            .map(user -> cache.findByName(user.getName()).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
            .collect(Collectors.joining(", "));
        
        String mutualAllies = faction.getMutualAllies().stream()
            .map(rel -> Faction.get(rel.target))
            .map(fac -> fac.getColor() + fac.getName())
            .collect(Collectors.joining(Formatting.GRAY + ", "));

        String enemiesWith = Formatting.GRAY + faction.getEnemiesWith().stream()
            .map(rel -> Faction.get(rel.target))
            .map(fac -> fac.getColor() + fac.getName())
            .collect(Collectors.joining(Formatting.GRAY + ", "));

        String enemiesOf = Formatting.GRAY + faction.getEnemiesOf().stream()
            .map(rel -> Faction.get(rel.target))
            .map(fac -> fac.getColor() + fac.getName())
            .collect(Collectors.joining(Formatting.GRAY + ", "));

        int tax = faction.getClaims().size() * FactionsMod.CONFIG.DAILY_TAX_PER_CHUNK;
        int maxPower = FactionsMod.CONFIG.MAX_POWER;

        new Message(Formatting.GRAY + faction.getDescription())
            .prependFaction(faction)
            .send(player, false);
        new Message(userText)
            .filler("·")
            .add(commanderText)
            .filler("·")
            .add(leaderText)
            .hover(usersList)
            .send(player, false);
        new Message("Power")
            .filler("·")
            .add(Formatting.GREEN.toString() + faction.getPower() + slash() + Formatting.RED.toString() + tax + Formatting.GREEN.toString() + slash() + maxPower)
            .hover("Current / Taxes / Max")
            .send(player, false);
        if (mutualAllies.length() > 0)
            new Message("Mutual allies: ")
                .add(mutualAllies)
                .send(player, false);
        if (enemiesWith.length() > 0)
            new Message("Enemies with: ")
                .add(enemiesWith)
                .send(player, false);
        if (enemiesOf.length() > 0)
            new Message("Enemies of: ")
                .add(enemiesOf)
                .send(player, false);

        User user = User.get(player.getName().getString());
        UUID userFaction = user.isInFaction() ? user.getFaction().getID() : null;
        if (faction.getID().equals(userFaction))
            new Message("Your Rank: ")
                .add(Formatting.GRAY + user.getRankName())
                .send(player, false);

        return 1;
    }

    private static String slash() {
        return Formatting.GRAY + " / " + Formatting.GREEN;
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