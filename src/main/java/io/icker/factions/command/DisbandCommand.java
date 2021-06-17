package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.database.Member;

import com.mojang.brigadier.Command;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public class DisbandCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		Member.get(player.getUuid()).getFaction().remove();

		PlayerManager manager = source.getMinecraftServer().getPlayerManager();
		for (ServerPlayerEntity p : manager.getPlayerList()) {
			manager.sendCommandTree(p);
		}

		source.sendFeedback(new TranslatableText("factions.command.disband.success").formatted(Formatting.GREEN), false);
		return 1;
	}
}