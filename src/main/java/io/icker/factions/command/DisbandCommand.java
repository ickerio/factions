package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;

import com.mojang.brigadier.Command;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

public class DisbandCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		Member member = Member.get(player.getUuid());
		Faction faction = member.getFaction();
		faction.remove();

		// TODO: notify online players
		PlayerManager manager = source.getMinecraftServer().getPlayerManager();
		for (ServerPlayerEntity p : manager.getPlayerList()) {
			manager.sendCommandTree(p);
		}

		source.sendFeedback(new LiteralText(faction.name + " has been disbanded"), false);
		return 1;
	}
}