package io.icker.factions.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.ui.InfoGui;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.server.GameProfileResolver;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.List;
import java.util.stream.Collectors;

public class InfoCommand implements Command {
    private int self(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        User user = Command.getUser(player);
        if (!user.isInFaction()) {
            new Message(Text.translatable("factions.command.info.fail.no_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        return info(player, user.getFaction());
    }

    private int any(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String factionName = StringArgumentType.getString(context, "faction");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        Faction faction = Faction.getByName(factionName);
        if (faction == null) {
            new Message(Text.translatable("factions.command.info.fail.nonexistent_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        return info(player, faction);
    }

    public static int info(ServerPlayerEntity player, Faction faction) {
        if (FactionsMod.CONFIG.GUI) {
            new InfoGui(player, faction, null);
            return 1;
        }
        List<User> users = faction.getUsers();

        GameProfileResolver resolver =
                player.getEntityWorld().getServer().getApiServices().profileResolver();
        String owner =
                Formatting.WHITE
                        + users.stream()
                                .filter(u -> u.rank == User.Rank.OWNER)
                                .map(
                                        user ->
                                                resolver.getProfileById(user.getID())
                                                        .orElse(
                                                                new GameProfile(
                                                                        Util.NIL_UUID,
                                                                        "{Uncached Player}"))
                                                        .name())
                                .collect(Collectors.joining(", "));

        String usersList =
                users.stream()
                        .map(
                                user ->
                                        resolver.getProfileById(user.getID())
                                                .orElse(
                                                        new GameProfile(
                                                                Util.NIL_UUID, "{Uncached Player}"))
                                                .name())
                        .collect(Collectors.joining(", "));

        String mutualAllies =
                faction.getMutualAllies().stream()
                        .map(rel -> Faction.get(rel.target))
                        .map(fac -> fac.getColor() + fac.getName())
                        .collect(Collectors.joining(Formatting.GRAY + ", "));

        String enemiesWith =
                Formatting.GRAY
                        + faction.getEnemiesWith().stream()
                                .map(rel -> Faction.get(rel.target))
                                .map(fac -> fac.getColor() + fac.getName())
                                .collect(Collectors.joining(Formatting.GRAY + ", "));

        int requiredPower = faction.getClaims().size() * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
        int maxPower =
                users.size() * FactionsMod.CONFIG.POWER.MEMBER + FactionsMod.CONFIG.POWER.BASE;

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
        new Message(Text.translatable("factions.gui.info.description").formatted(Formatting.GOLD))
                .add(Formatting.WHITE + faction.getDescription())
                .send(player, false);
        new Message(Text.translatable("factions.gui.info.owner").formatted(Formatting.GOLD))
                .add(Formatting.WHITE + owner)
                .send(player, false);
        new Message(
                        Text.translatable(
                                        "factions.gui.info.members",
                                        Text.literal(Integer.toString(users.size()))
                                                .formatted(Formatting.WHITE))
                                .formatted(Formatting.GOLD))
                .add(usersList)
                .send(player, false);
        new Message(Text.translatable("factions.gui.info.power").formatted(Formatting.GOLD))
                .add(
                        Formatting.GREEN.toString()
                                + faction.getPower()
                                + slash()
                                + requiredPower
                                + slash()
                                + maxPower)
                .hover(Text.translatable("factions.gui.info.power.description"))
                .send(player, false);
        new Message(
                        Text.translatable(
                                        "factions.gui.info.allies.some",
                                        Text.literal(
                                                        Integer.toString(
                                                                faction.getMutualAllies().size()))
                                                .formatted(Formatting.WHITE))
                                .formatted(Formatting.GREEN))
                .add(mutualAllies)
                .send(player, false);
        new Message(
                        Text.translatable(
                                        "factions.gui.info.enemies.some",
                                        Text.literal(
                                                        Integer.toString(
                                                                faction.getEnemiesWith().size()))
                                                .formatted(Formatting.WHITE))
                                .formatted(Formatting.RED))
                .add(enemiesWith)
                .send(player, false);

        return 1;
    }

    private static String slash() {
        return Formatting.GRAY + " / " + Formatting.GREEN;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("info")
                .requires(Requires.hasPerms("factions.info", 0))
                .executes(this::self)
                .then(
                        CommandManager.argument("faction", StringArgumentType.greedyString())
                                .requires(Requires.hasPerms("factions.info.other", 0))
                                .suggests(Suggests.allFactions())
                                .executes(this::any))
                .build();
    }
}
