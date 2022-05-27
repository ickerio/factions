package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class KickCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (target.getUuid().equals(player.getUuid())) {
            new Message("Cannot kick yourself").format(Formatting.RED).send(player, false);
            return 0;
        }

        User selfMember = User.get(player.getUuid());
        User targetMember = User.get(target.getUuid());
        Faction faction = selfMember.getFaction();

        if (targetMember.getFaction().getID() != faction.getID()) {
            new Message("Cannot kick someone that is not in your faction");
            return 0;
        }

        if (selfMember.getRank() == User.Rank.LEADER && (targetMember.getRank() == User.Rank.LEADER || targetMember.getRank() == User.Rank.OWNER)) {
            new Message("Cannot kick members with a higher of equivalent rank").format(Formatting.RED).send(player, false);
            return 0;
        }

        targetMember.leaveFaction();
        context.getSource().getServer().getPlayerManager().sendCommandTree(target);
        new Message("Kicked " + target.getName().getString()).send(player, false);

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("kick")
            .requires(Requires.hasPerms("factions.kick", 0))
            .requires(Requires.isLeader())
            .then(
                CommandManager.argument("player", EntityArgumentType.player()).executes(this::run)
            )
            .build();
    }
}
