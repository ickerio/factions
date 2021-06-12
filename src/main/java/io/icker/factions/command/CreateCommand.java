package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.database.Database;
import io.icker.factions.util.FactionsUtil;
import java.util.UUID;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class CreateCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "name");

		ServerPlayerEntity player = context.getSource().getPlayer();
		UUID uuid = player.getUuid();
		
		if (Database.Members.get(uuid) != null) {
			FactionsUtil.Message.sendError(player, "You are already in a faction");
			return 0;
		}

		if (Database.Factions.get(name) != null) {
			FactionsUtil.Message.sendError(player, "Faction with that name already exists");
			return 0;
		}

		Database.Factions.add(name, "No description set", Formatting.RESET.getName(), 100).addMember(uuid);
		FactionsUtil.Message.sendSuccess(player, "Success! Faction created");
		return 1;
	}
}