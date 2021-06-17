package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.database.Member;
import com.mojang.brigadier.Command;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public class LeaveCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();
		
		Member.get(player.getUuid()).remove();
        
		// TODO: set to Open if no players left
        context.getSource().getMinecraftServer().getPlayerManager().sendCommandTree(player);
		
		source.sendFeedback(new TranslatableText("factions.command.leave.success").formatted(Formatting.GREEN), false);
		return 1;
	}
}