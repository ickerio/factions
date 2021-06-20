package io.icker.factions.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.database.Member;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class CommandRegistry {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralCommandNode<ServerCommandSource> factions = CommandManager
			.literal("factions")
			.build();

		LiteralCommandNode<ServerCommandSource> alias = CommandManager
			.literal("f")
			.redirect(factions)
			.build();

		LiteralCommandNode<ServerCommandSource> create = CommandManager
			.literal("create")
			.requires(CommandRegistry::isFactionless)
			.then(
				CommandManager.argument("name", StringArgumentType.greedyString())
				.executes(new CreateCommand())
			)
			.build();

		LiteralCommandNode<ServerCommandSource> disband = CommandManager
			.literal("disband")
			.requires(CommandRegistry::isFactionMember)
			.executes(new DisbandCommand())
			.build();

		LiteralCommandNode<ServerCommandSource> join = CommandManager
			.literal("join")
			.requires(CommandRegistry::isFactionless)
			.then(
				CommandManager.argument("name", StringArgumentType.greedyString())
				.executes(new JoinCommand())
			)
			.build();

		LiteralCommandNode<ServerCommandSource> leave = CommandManager
			.literal("leave")
			.requires(CommandRegistry::isFactionMember)
			.executes(new LeaveCommand())
			.build();

		LiteralCommandNode<ServerCommandSource> modify = CommandManager
			.literal("modify")
			.requires(CommandRegistry::isFactionMember)
			.build();

		LiteralCommandNode<ServerCommandSource> description = CommandManager
			.literal("description")
			.then(
				CommandManager.argument("description", StringArgumentType.greedyString())
				.executes(ModifyCommand::description)
			)
			.build();

		LiteralCommandNode<ServerCommandSource> color = CommandManager
			.literal("color")
			.then(
				CommandManager.argument("color", ColorArgumentType.color())
				.executes(ModifyCommand::color)
			)
			.build();

		LiteralCommandNode<ServerCommandSource> open = CommandManager
			.literal("open")
			.then(
				CommandManager.argument("open", BoolArgumentType.bool())
				.executes(ModifyCommand::open)
			)
			.build();

		LiteralCommandNode<ServerCommandSource> invite = CommandManager
			.literal("invite")
			.requires(CommandRegistry::isFactionMember)
			.build();

		LiteralCommandNode<ServerCommandSource> listInvites = CommandManager
			.literal("list")
			.executes(InviteCommand::list)
			.build();

		LiteralCommandNode<ServerCommandSource> addInvite = CommandManager
			.literal("add")
			.then(
				CommandManager.argument("player", EntityArgumentType.player())
				.executes(InviteCommand::add)
			)
			.build();

		LiteralCommandNode<ServerCommandSource> removeInvite = CommandManager
			.literal("remove")
			.then(
				CommandManager.argument("player", EntityArgumentType.player())
				.executes(InviteCommand::remove)
			)
			.build();

		LiteralCommandNode<ServerCommandSource> claim = CommandManager
			.literal("claim")
			.requires(CommandRegistry::isFactionMember)
			.executes(ClaimCommand::claim)
			.build();
		
		LiteralCommandNode<ServerCommandSource> removeClaim = CommandManager
			.literal("remove")
			.executes(ClaimCommand::remove)
			.build();

		dispatcher.getRoot().addChild(factions);
		dispatcher.getRoot().addChild(alias);

		factions.addChild(create);
		factions.addChild(disband);
		factions.addChild(join);
		factions.addChild(leave);

		factions.addChild(modify);
		modify.addChild(description);
		modify.addChild(color);
		modify.addChild(open);

		factions.addChild(invite);
		invite.addChild(listInvites);
		invite.addChild(addInvite);
		invite.addChild(removeInvite);

		factions.addChild(claim);
		claim.addChild(removeClaim);
	}

	public static boolean isFactionMember(ServerCommandSource source) {
		try {
			ServerPlayerEntity player = source.getPlayer();
			return Member.get(player.getUuid()) != null;
		} catch (CommandSyntaxException e) { return false; }
	}

	public static boolean isFactionless(ServerCommandSource source) {
		return !isFactionMember(source);
	}
}