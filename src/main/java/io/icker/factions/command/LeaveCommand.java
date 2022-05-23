package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Member;
import io.icker.factions.api.persistents.Member.Rank;
import io.icker.factions.config.Config;
import io.icker.factions.event.FactionEvents;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class LeaveCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Member member = Member.get(player.getUuid());
        Faction faction = member.getFaction();

        member.leaveFaction();
        new Message(player.getName().asString() + " left").send(faction);
        context.getSource().getServer().getPlayerManager().sendCommandTree(player);

        if (faction.getMembers().size() == 0) {
            faction.remove();
        } else {
            FactionEvents.adjustPower(faction, -Config.MEMBER_POWER);
        }

        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("leave")
            .requires(Requires.hasPerms("factions.leave", 0))
            .requires(Requires.require(m -> m.isInFaction() && m.getRank() != Rank.OWNER))
            .executes(this::run)
            .build();
    }
}