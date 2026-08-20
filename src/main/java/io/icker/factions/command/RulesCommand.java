package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class RulesCommand implements Command {

    private int setElytra(CommandContext<CommandSourceStack> context, boolean value)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        User user = Command.getUser(player);
        Faction faction = user.getFaction();

        if (faction.rules == null) {
            faction.rules = new Faction.Rules();
        }
        faction.rules.elytra = value;

        new Message(
                        Component.translatable(
                                value
                                        ? "factions.command.rules.elytra.enabled"
                                        : "factions.command.rules.elytra.disabled"))
                .send(player, false);
        return 1;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("rules")
                .requires(
                        Requires.multiple(
                                Requires.isLeader(),
                                Requires.hasPerms("factions.rules", 0)))
                .then(
                        Commands.literal("elytra")
                                .then(
                                        Commands.literal("enabled")
                                                .executes(ctx -> setElytra(ctx, true)))
                                .then(
                                        Commands.literal("disabled")
                                                .executes(ctx -> setElytra(ctx, false))))
                .build();
    }
}
