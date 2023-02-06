package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.text.FactionText;
import io.icker.factions.text.Message;
import io.icker.factions.text.TranslatableText;
import io.icker.factions.util.Command;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class LeaveCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        User user = Command.getUser(player);
        Faction faction = user.getFaction();

        user.leaveFaction();
        new Message().append(new TranslatableText("leave", player.getName().getString())).send(faction);
        new Message().append(new TranslatableText("leave.self"))
            .prepend(new FactionText(faction))
            .send(player, false);

        context.getSource().getServer().getPlayerManager().sendCommandTree(player);

        if (faction.getUsers().size() == 0) {
            faction.remove();
        } else {
            faction.adjustPower(-FactionsMod.CONFIG.POWER.MEMBER);
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