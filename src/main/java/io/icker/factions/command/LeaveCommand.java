package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import com.mojang.brigadier.Command;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

public class LeaveCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();
		
		Member member = Member.get(player.getUuid());
		Faction faction = member.getFaction();
        
		// TODO: set to Open if no players left
		member.remove();
        context.getSource().getMinecraftServer().getPlayerManager().sendCommandTree(player);
		
		source.sendFeedback(new LiteralText("You are no longer a member of " + faction.name), false);
		return 1;
	}
}