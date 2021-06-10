package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.teams.TeamsManager;
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
		
		if (TeamsManager.getMember(uuid) != null) {
			FactionsUtil.Message.sendError(player, "Error! You are already in a team");
			return 0;
		}
		if (TeamsManager.getTeam(name) != null) {
			FactionsUtil.Message.sendError(player, "Error! Team with that name already exists");
			return 0;
		}

		TeamsManager.addTeam(name, color).addMember(uuid);
		player.server.getPlayerManager().sendCommandTree(player);

		FactionsUtil.Message.sendSuccess(player, "Success! Team created");
		return 1;
	}
}