package io.icker.factions.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
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

		LiteralCommandNode<ServerCommandSource> createNode = CommandManager
			.literal("create")
			.then(
				CommandManager.argument("name", StringArgumentType.greedyString())
				.executes(new CreateCommand())
			)
			.build();

		LiteralCommandNode<ServerCommandSource> disbandNode = CommandManager
			.literal("disband")
			.executes(new DisbandCommand())
			.build();

		LiteralCommandNode<ServerCommandSource> openNode = CommandManager
			.literal("open")
			.then(
				CommandManager.argument("open", BoolArgumentType.bool())
				.executes(new OpenCommand())
			)
			.build();
		
		LiteralCommandNode<ServerCommandSource> claimNode = CommandManager
			.literal("claim")
			.executes(ClaimCommand::claim)
			.build();
		
		LiteralCommandNode<ServerCommandSource> removeNode = CommandManager
			.literal("remove")
			.executes(ClaimCommand::remove)
			.build();

		dispatcher.getRoot().addChild(factionsNode);
		dispatcher.getRoot().addChild(factionsAliasNode);

		factionsNode.addChild(createNode);
		factionsNode.addChild(disbandNode);
		factionsNode.addChild(openNode);

		factionsNode.addChild(claimNode);
		claimNode.addChild(removeNode);
	}
}