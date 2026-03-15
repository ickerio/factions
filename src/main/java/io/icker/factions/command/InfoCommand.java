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

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.util.Util;

import java.util.List;
import java.util.stream.Collectors;

public class InfoCommand implements Command {
    private int self(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        User user = Command.getUser(player);
        if (!user.isInFaction()) {
            new Message(Component.translatable("factions.command.info.fail.no_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        return info(player, user.getFaction());
    }

    private int any(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String factionName = StringArgumentType.getString(context, "faction");

        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        Faction faction = Faction.getByName(factionName);
        if (faction == null) {
            new Message(Component.translatable("factions.command.info.fail.nonexistent_faction"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        return info(player, faction);
    }

    public static int info(ServerPlayer player, Faction faction) {
        if (FactionsMod.CONFIG.GUI) {
            new InfoGui(player, faction, null);
            return 1;
        }
        List<User> users = faction.getUsers();

        ProfileResolver resolver = player.level().getServer().services().profileResolver();
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
                                                                        "{Uncached Player}"))
                                                        .name())
                                .collect(Collectors.joining(", "));

        String usersList =
                users.stream()
                        .map(
                                user ->
                                        resolver.fetchById(user.getID())
                                                .orElse(
                                                        new GameProfile(
                                                                Util.NIL_UUID, "{Uncached Player}"))
                                                .name())
                        .collect(Collectors.joining(", "));

        String mutualAllies =
                faction.getMutualAllies().stream()
                        .map(rel -> Faction.get(rel.target))
                        .map(fac -> fac.getColor() + fac.getName())
                        .collect(Collectors.joining(ChatFormatting.GRAY + ", "));

        String enemiesWith =
                ChatFormatting.GRAY
                        + faction.getEnemiesWith().stream()
                                .map(rel -> Faction.get(rel.target))
                                .map(fac -> fac.getColor() + fac.getName())
                                .collect(Collectors.joining(ChatFormatting.GRAY + ", "));

        int requiredPower = faction.getClaims().size() * FactionsMod.CONFIG.POWER.CLAIM_WEIGHT;
        int maxPower =
                users.size() * FactionsMod.CONFIG.POWER.MEMBER + FactionsMod.CONFIG.POWER.BASE;

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
                        Component.translatable("factions.gui.info.description")
                                .withStyle(ChatFormatting.GOLD))
                .add(ChatFormatting.WHITE + faction.getDescription())
                .send(player, false);
        new Message(
                        Component.translatable("factions.gui.info.owner")
                                .withStyle(ChatFormatting.GOLD))
                .add(ChatFormatting.WHITE + owner)
                .send(player, false);
        new Message(
                        Component.translatable(
                                        "factions.gui.info.members",
                                        Component.literal(Integer.toString(users.size()))
                                                .withStyle(ChatFormatting.WHITE))
                                .withStyle(ChatFormatting.GOLD))
                .add(usersList)
                .send(player, false);
        new Message(
                        Component.translatable("factions.gui.info.power")
                                .withStyle(ChatFormatting.GOLD))
                .add(
                        ChatFormatting.GREEN.toString()
                                + faction.getPower()
                                + slash()
                                + requiredPower
                                + slash()
                                + maxPower)
                .hover(Component.translatable("factions.gui.info.power.description"))
                .send(player, false);
        new Message(
                        Component.translatable(
                                        "factions.gui.info.allies.some",
                                        Component.literal(
                                                        Integer.toString(
                                                                faction.getMutualAllies().size()))
                                                .withStyle(ChatFormatting.WHITE))
                                .withStyle(ChatFormatting.GREEN))
                .add(mutualAllies)
                .send(player, false);
        new Message(
                        Component.translatable(
                                        "factions.gui.info.enemies.some",
                                        Component.literal(
                                                        Integer.toString(
                                                                faction.getEnemiesWith().size()))
                                                .withStyle(ChatFormatting.WHITE))
                                .withStyle(ChatFormatting.RED))
                .add(enemiesWith)
                .send(player, false);

        return 1;
    }

    private static String slash() {
        return ChatFormatting.GRAY + " / " + ChatFormatting.GREEN;
    }

    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("info")
                .requires(Requires.hasPerms("factions.info", 0))
                .executes(this::self)
                .then(
                        Commands.argument("faction", StringArgumentType.greedyString())
                                .requires(Requires.hasPerms("factions.info.other", 0))
                                .suggests(Suggests.allFactions())
                                .executes(this::any))
                .build();
    }
}
