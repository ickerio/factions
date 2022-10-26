package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class LeaveCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return -1;  // Confirm that it's a player executing the command and not an entity with /execute

        User user = Command.getUser(player);
        Faction faction = user.getFaction();

        user.leaveFaction();
        new Message(player.getName().getString() + " left").send(faction);
        new Message("You have left this faction.")
            .prependFaction(faction)
            .send(player, false);

        context.getSource().getServer().getPlayerManager().sendCommandTree(player);

        if (faction.getUsers().size() == 0) {
            faction.remove();
        }

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("leave")
            .requires(Requires.multiple(Requires.require(m -> m.isInFaction() && m.rank != User.Rank.OWNER), Requires.hasPerms("factions.leave", 0)))
            .executes(this::run)
            .build();
    }
}