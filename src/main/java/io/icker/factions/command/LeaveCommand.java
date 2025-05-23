package io.icker.factions.command;

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
import net.minecraft.text.Text;

public class LeaveCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        User user = Command.getUser(player);
        Faction faction = user.getFaction();

        user.leaveFaction();
        new Message(Text.translatable("factions.command.leave.success.actor"))
                .prependFaction(faction)
                .send(player, false);
        new Message(
                        Text.translatable(
                                "factions.command.leave.success.subject",
                                player.getName().getString()))
                .send(faction);

        context.getSource().getServer().getPlayerManager().sendCommandTree(player);

        if (faction.getUsers().size() == 0) {
            faction.remove();
        } else {
            faction.adjustPower(-FactionsMod.CONFIG.POWER.MEMBER);
        }

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("leave")
                .requires(
                        Requires.multiple(
                                Requires.require(m -> m.isInFaction() && m.rank != User.Rank.OWNER),
                                Requires.hasPerms("factions.leave", 0)))
                .executes(this::run)
                .build();
    }
}
