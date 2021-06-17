package io.icker.factions.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.database.Member;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class CommandRegister {
	// TODO: Rewrite this nicer
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
			.requires(src -> !isMember(src))
			.then(
				CommandManager.argument("name", StringArgumentType.greedyString())
				.executes(new CreateCommand())
			)
			.build();

		LiteralCommandNode<ServerCommandSource> disbandNode = CommandManager
			.literal("disband")
			.requires(CommandRegister::isMember)
			.executes(new DisbandCommand())
			.build();

		LiteralCommandNode<ServerCommandSource> openNode = CommandManager
			.literal("open")
			.requires(CommandRegister::isMember)
			.then(
				CommandManager.argument("open", BoolArgumentType.bool())
				.executes(new OpenCommand())
			)
			.build();
		
		LiteralCommandNode<ServerCommandSource> claimNode = CommandManager
			.literal("claim")
			.requires(CommandRegister::isMember)
			.executes(ClaimCommand::claim)
			.build();
		
		LiteralCommandNode<ServerCommandSource> removeNode = CommandManager
			.literal("remove")
			.requires(CommandRegister::isMember)
			.executes(ClaimCommand::remove)
			.build();

		LiteralCommandNode<ServerCommandSource> joinNode = CommandManager
			.literal("join")
			.requires(src -> !isMember(src))
			.then(
				CommandManager.argument("name", StringArgumentType.greedyString())
				.executes(new JoinCommand())
			)
			.build();

		LiteralCommandNode<ServerCommandSource> leaveNode = CommandManager
			.literal("leave")
			.requires(CommandRegister::isMember)
			.executes(new JoinCommand())
			.build();

		dispatcher.getRoot().addChild(factionsNode);
		dispatcher.getRoot().addChild(factionsAliasNode);

		factionsNode.addChild(createNode);
		factionsNode.addChild(disbandNode);
		factionsNode.addChild(openNode);
		factionsNode.addChild(joinNode);
		factionsNode.addChild(leaveNode);

		factionsNode.addChild(claimNode);
		claimNode.addChild(removeNode);
	}

	public static boolean isMember(ServerCommandSource source) {
		try {
			ServerPlayerEntity player = source.getPlayer();
			return Member.get(player.getUuid()) != null;
		} catch (CommandSyntaxException e) { return false; }
	}
}