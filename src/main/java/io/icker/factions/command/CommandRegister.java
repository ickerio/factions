package io.icker.factions.command;

import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.teams.Database;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class CommandRegister {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {

			LiteralCommandNode<ServerCommandSource> factionsNode = CommandManager
				.literal("factions")
				.build();

			LiteralCommandNode<ServerCommandSource> factionsAliasNode = CommandManager
				.literal("f")
				.redirect(factionsNode)
				.build();

			
			LiteralCommandNode<ServerCommandSource> claimNode = CommandManager
				.literal("claim")
				//.requires(source -> inFaction(source))
				.executes(ClaimCommand::claim)
				.build();

			
			LiteralCommandNode<ServerCommandSource> unclaimNode = CommandManager
				.literal("unclaim")
				//.requires(source -> inFaction(source))
				.executes(ClaimCommand::unclaim)
				.build();

			LiteralCommandNode<ServerCommandSource> createNode = CommandManager
				.literal("create")
				//.requires(source -> !inFaction(source))
				.then(
					CommandManager.argument("name", StringArgumentType.string())
					.executes(CreateTeamCommand::runWithoutColour)
					.then(
						CommandManager.argument("color", ColorArgumentType.color())
						.executes(CreateTeamCommand::runWithColour)
					)
				)
				.build();

			dispatcher.getRoot().addChild(factionsNode);
			dispatcher.getRoot().addChild(factionsAliasNode);
			factionsNode.addChild(claimNode);
			factionsNode.addChild(unclaimNode);
			factionsNode.addChild(createNode);

    }

	// public static boolean inFaction(ServerCommandSource source) {
	// 	try {
	// 		UUID uuid = source.getPlayer().getUuid();
	// 		return Database.Members.get(uuid) != null;
	// 	} catch (CommandSyntaxException e) {
	// 		return false;
	// 	}
	// }
}