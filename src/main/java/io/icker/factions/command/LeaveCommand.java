package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.database.Database;
import io.icker.factions.database.Member;
import io.icker.factions.util.FactionsUtil;
import com.mojang.brigadier.Command;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class LeaveCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		Member member = Database.Members.get(player.getUuid());

		member.remove();
        // TODO: remove faction if no members left - think this can be done in pure SQL
        context.getSource().getMinecraftServer().getPlayerManager().sendCommandTree(player);
		FactionsUtil.Message.sendSuccess(player, "Success! You have left your faction");
		return 1;
	}
}