package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Member;
import io.icker.factions.util.Message;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class RankCommand {
    public static int promote(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (target.getUuid().equals(player.getUuid())) {
            new Message("You cannot promote yourself").format(Formatting.RED).send(player, false);

            return 0;
        }

        Faction faction = Member.get(player.getUuid()).getFaction();

        for (Member member : faction.getMembers())
            if (member.getID().equals(target.getUuid())) {

                switch (member.getRank()) {
                    case MEMBER -> member.changeRank(Member.Rank.COMMANDER);
                    case COMMANDER -> member.changeRank(Member.Rank.LEADER);
                    case LEADER -> {
                        new Message("You cannot promote a member to owner").format(Formatting.RED).send(player, false);
                        return 0;
                    }
                    case OWNER -> {
                        new Message("You cannot promote the owner").format(Formatting.RED).send(player, false);
                        return 0;
                    }
                }

                context.getSource().getServer().getPlayerManager().sendCommandTree(target);

                new Message("Promoted " + target.getName().getString() + " to " + Member.get(target.getUuid()).getRank().name().toLowerCase().replace("_", " ")).send(player, false);
                return 1;
            }

        new Message(target.getName().getString() + " is not in your faction").format(Formatting.RED).send(player, false);
        return 0;
    }

    public static int demote(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (target.getUuid().equals(player.getUuid())) {
            new Message("You cannot demote yourself").format(Formatting.RED).send(player, false);

            return 0;
        }

        Faction faction = Member.get(player.getUuid()).getFaction();

        for (Member member : faction.getMembers())
            if (member.getID().equals(target.getUuid())) {

                switch (member.getRank()) {
                    case MEMBER -> {
                        new Message("You cannot demote a civilian").format(Formatting.RED).send(player, false);
                        return 0;
                    }
                    case COMMANDER -> member.changeRank(Member.Rank.MEMBER);
                    case LEADER -> {
                        if (Member.get(player.getUuid()).getRank() == Member.Rank.LEADER) {
                            new Message("You cannot demote a fellow co-owner").format(Formatting.RED).send(player, false);
                            return 0;
                        }

                        member.changeRank(Member.Rank.COMMANDER);
                    }
                    case OWNER -> {
                        new Message("You cannot demote the owner").format(Formatting.RED).send(player, false);
                        return 0;
                    }
                }

                context.getSource().getServer().getPlayerManager().sendCommandTree(target);

                new Message("Demoted " + target.getName().getString() + " to " + Member.get(target.getUuid()).getRank().name().toLowerCase().replace("_", " ")).send(player, false);
                return 1;
            }

        new Message(target.getName().getString() + " is not in your faction").format(Formatting.RED).send(player, false);
        return 0;
    }
}
