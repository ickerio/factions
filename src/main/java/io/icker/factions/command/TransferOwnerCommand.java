package io.icker.factions.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.util.Message;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class TransferOwnerCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		if (target.getUuid().equals(player.getUuid())) {
			new Message("You cannot transfer ownership to yourself").format(Formatting.RED).send(player, false);

			return 0;
		}

		Faction faction = Member.get(player.getUuid()).getFaction();

		for (Member member : faction.getMembers())
			if (member.uuid.equals(target.getUuid())) {

				member.updateRank(Member.Rank.OWNER);
				Member.get(player.getUuid()).updateRank(Member.Rank.CO_OWNER);

				context.getSource().getServer().getPlayerManager().sendCommandTree(player);
				context.getSource().getServer().getPlayerManager().sendCommandTree(target);

				new Message("Transferred ownership to " + target.getName().getString()).send(player, false);
				return 1;
			}

		new Message(target.getName().getString() + " is not in your faction").format(Formatting.RED).send(player, false);

		return 0;
	}
}
