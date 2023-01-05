package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.text.Message;
import io.icker.factions.text.TranslatableText;
import io.icker.factions.util.Command;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class KickCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (target.getUuid().equals(player.getUuid())) {
            new Message().append(new TranslatableText("translate:kick.error.self").fail()).send(player, false);
            return 0;
        }

        User selfUser = Command.getUser(player);
        User targetUser = User.get(target.getUuid());
        Faction faction = selfUser.getFaction();

        if (targetUser.getFaction().getID() != faction.getID()) {
            new Message().append(new TranslatableText("translate:kick.error.not-in-faction").fail()).send(player, false);
            return 0;
        }

        if (selfUser.rank == User.Rank.LEADER && (targetUser.rank == User.Rank.LEADER || targetUser.rank == User.Rank.OWNER)) {
            new Message().append(new TranslatableText("translate:kick.error.rank").fail()).send(player, false);
            return 0;
        }

        targetUser.leaveFaction();
        context.getSource().getServer().getPlayerManager().sendCommandTree(target);

        new Message().append(new TranslatableText("translate:kick.notice", player.getName().getString())).send(player, false);
        new Message().append(new TranslatableText("translate:kick.notice.self",  player.getName().getString())).send(target, false);

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("kick")
            .requires(Requires.multiple(Requires.isLeader(), Requires.hasPerms("factions.kick", 0)))
            .then(
                CommandManager.argument("player", EntityArgumentType.player()).executes(this::run)
            )
            .build();
    }
}
