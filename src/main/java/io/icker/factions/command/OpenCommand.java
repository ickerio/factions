package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.database.Database;
import io.icker.factions.database.Member;
import io.icker.factions.util.FactionsUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class OpenCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean open = BoolArgumentType.getBool(context, "open");

		ServerPlayerEntity player = context.getSource().getPlayer();
		Member member = Database.Members.get(player.getUuid());
		
		if (member == null) {
			FactionsUtil.Message.sendError(player, "You must be in a faction to open a faction"); //TODO: Faction requirement command idea
			return 0;
		}

        //member.getFaction().setOpen(open); // TODO
		FactionsUtil.Message.sendSuccess(player, "Success! Faction is now %s".formatted(open ? "open" : "closed"));
		return 1;
	}
}