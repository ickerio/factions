package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.teams.Database;
import io.icker.factions.util.FactionsUtil;

import java.util.UUID;
// TODO: sort this order out
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class CreateTeamCommand {
	public static int runWithoutColour(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "name");
		return run(context, name, Formatting.RESET);
	}

	public static int runWithColour(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "name");
		Formatting color = ColorArgumentType.getColor(context, "color");
		return run(context, name, color);
	}

	public static int run(CommandContext<ServerCommandSource> context, String name, Formatting color) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayer();
		UUID uuid = player.getUuid();
		
		if (Database.Members.get(uuid) != null) {
			FactionsUtil.Message.sendError(player, "You are already in a team");
			return 0;
		}
		if (Database.Teams.get(name) != null) {
			FactionsUtil.Message.sendError(player, "Team with that name already exists");
			return 0;
		}

		Database.Teams.add(name, "", color.getName(), 100).addMember(uuid);
		player.server.getPlayerManager().sendCommandTree(player);

		FactionsUtil.Message.sendSuccess(player, "Success! Team created");
		return 1;
	}
}