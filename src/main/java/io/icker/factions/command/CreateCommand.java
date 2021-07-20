package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.config.Config;
import io.icker.factions.database.Faction;
import io.icker.factions.util.Message;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class CreateCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "name");

		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		if (Faction.get(name) != null) {
			new Message("Cannot create a faction as a one with that name already exists").fail().send(player, false);
			return 0;
		}

		Faction.add(name, "No description set", Formatting.WHITE.getName(), false, Config.BASE_POWER + Config.MEMBER_POWER).addMember(player.getUuid());
		source.getServer().getPlayerManager().sendCommandTree(player);
		
		new Message("Successfully created faction").send(player, false);
		return 1;
	}
}