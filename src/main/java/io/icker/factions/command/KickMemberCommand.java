package io.icker.factions.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Member;
import io.icker.factions.util.Message;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class KickMemberCommand implements Command<ServerCommandSource> {
    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (target.getUuid().equals(player.getUuid())) {
            new Message("You cannot kick yourself").format(Formatting.RED).send(player, false);

            return 0;
        }

        Faction faction = Member.get(player.getUuid()).getFaction();

        for (Member member : faction.getMembers())
            if (member.uuid.equals(target.getUuid())) {

                if (Member.get(player.getUuid()).getRank() == Member.Rank.CO_OWNER && (member.getRank() == Member.Rank.CO_OWNER || member.getRank() == Member.Rank.OWNER)) {
                    new Message("You can only kick members with a lower rank than yours").format(Formatting.RED).send(player, false);

                    return 0;
                }

                member.remove();
                context.getSource().getServer().getPlayerManager().sendCommandTree(player);
                new Message("Kicked " + target.getName().getString()).send(player, false);

                return 1;
            }

        new Message("That player is not a member of your faction");

        return 0;
    }
}
