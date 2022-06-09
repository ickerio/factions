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

public class LeaveCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User user = User.get(player.getUuid());
        Faction faction = user.getFaction();

        user.leaveFaction();
        new Message(player.getName().getString() + " left").send(faction);
        new Message("You have left this faction.")
            .prependFaction(faction)
            .send(player, false);

        context.getSource().getServer().getPlayerManager().sendCommandTree(player);

        if (faction.getUsers().size() == 0) {
            faction.remove();
        } else {
            faction.adjustPower(-FactionsMod.CONFIG.MEMBER_POWER);
        }

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("leave")
            .requires(Requires.hasPerms("factions.leave", 0))
            .requires(Requires.require(m -> m.isInFaction() && m.rank != User.Rank.OWNER))
            .executes(this::run)
            .build();
    }
}