package io.icker.factions.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.config.Config;
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
			.requires(CommandRegistry::isOwner)
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
			.requires(s -> CommandRegistry.isFactionMember(s) && !CommandRegistry.isOwner(s))
			.executes(new LeaveCommand())
			.build();

		LiteralCommandNode<ServerCommandSource> info = CommandManager
			.literal("info")
			.executes(InfoCommand::self)
			.then(
				CommandManager.argument("faction", StringArgumentType.greedyString())
				.executes(InfoCommand::any)
			)
			.build();

		LiteralCommandNode<ServerCommandSource> list = CommandManager
			.literal("list")
			.executes(new ListCommand())
			.build();

			LiteralCommandNode<ServerCommandSource> chat = CommandManager
			.literal("chat")
			.then(
				CommandManager.literal("global")
				.executes(ChatCommand::global)
			)
			.then(
				CommandManager.literal("faction")
				.executes(ChatCommand::faction)
			)
			.then(
				CommandManager.literal("focus")
				.executes(ChatCommand::focus)
			)
			.build();

		LiteralCommandNode<ServerCommandSource> modify = CommandManager
			.literal("modify")
			.requires(CommandRegistry::isRankAboveOfficer)
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
			.requires(CommandRegistry::isRankAboveCivilian)
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

		LiteralCommandNode<ServerCommandSource> ally = CommandManager
				.literal("ally")
				.requires(CommandRegistry::isRankAboveCivilian)
				.build();

		LiteralCommandNode<ServerCommandSource> addAlly = CommandManager
				.literal("add")
				.then(
						CommandManager.argument("player", EntityArgumentType.player())
								.executes(AllyCommand::add)
				)
				.build();

		LiteralCommandNode<ServerCommandSource> acceptAlly = CommandManager
				.literal("accept")
				.then(
						CommandManager.argument("player", EntityArgumentType.player())
								.executes(AllyCommand::accept)
				)
				.build();
		
		LiteralCommandNode<ServerCommandSource> listAlly = CommandManager
				.literal("list")
				.executes(AllyCommand::list)
				.build();

		LiteralCommandNode<ServerCommandSource> removeAlly = CommandManager
				.literal("remove")
				.then(
						CommandManager.argument("player", EntityArgumentType.player())
								.executes(AllyCommand::remove)
				)
				.build();

		LiteralCommandNode<ServerCommandSource> claim = CommandManager
			.literal("claim")
			.requires(CommandRegistry::isRankAboveCivilian)
			.executes(ClaimCommand::add)
			.build();

		LiteralCommandNode<ServerCommandSource> listClaim = CommandManager
			.literal("list")
			.executes(ClaimCommand::list)
			.build();
		
		LiteralCommandNode<ServerCommandSource> removeClaim = CommandManager
			.literal("remove")
			.executes(ClaimCommand::remove)
			.build();

		LiteralCommandNode<ServerCommandSource> removeAllClaims = CommandManager
			.literal("all")
			.executes(ClaimCommand::removeAll)
			.build();

		LiteralCommandNode<ServerCommandSource> home = CommandManager
			.literal("home")
			.requires(s -> isFactionMember(s) && Config.HOME != Config.HomeOptions.DISABLED)
			.executes(HomeCommand::go)
			.build();
		
		LiteralCommandNode<ServerCommandSource> setHome = CommandManager
			.literal("set")
			.requires(CommandRegistry::isRankAboveOfficer)
			.executes(HomeCommand::set)
			.build();
		
		LiteralCommandNode<ServerCommandSource> adminBypass = CommandManager
			.literal("bypass")
			.requires(s -> s.hasPermissionLevel(Config.REQUIRED_BYPASS_LEVEL))
			.executes(new BypassCommand())
			.build();

		LiteralCommandNode<ServerCommandSource> admin = CommandManager
			.literal("admin")
			.build();

		LiteralCommandNode<ServerCommandSource> migrateAlly = CommandManager
			.literal("migrate")
			.executes(AdminCommand::migrateAlly)
			.build();

		LiteralCommandNode<ServerCommandSource> rank = CommandManager
			.literal("rank")
			.requires(CommandRegistry::isRankAboveOfficer)
			.build();

		LiteralCommandNode<ServerCommandSource> promote = CommandManager
				.literal("promote")
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.executes(RankCommand::promote))
				.build();

		LiteralCommandNode<ServerCommandSource> demote = CommandManager
				.literal("demote")
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.executes(RankCommand::demote))
				.build();

		LiteralCommandNode<ServerCommandSource> transferOwner = CommandManager
			.literal("transferOwner")
			.requires(CommandRegistry::isOwner)
			.then(CommandManager.argument("player", EntityArgumentType.player())
					.executes(c -> new TransferOwnerCommand().run(c)))
			.build();

		LiteralCommandNode<ServerCommandSource> kickMember = CommandManager
			.literal("kickMember")
			.requires(CommandRegistry::isRankAboveOfficer)
			.then(CommandManager.argument("player", EntityArgumentType.player())
					.executes(c -> new KickMemberCommand().run(c)))
			.build();

		dispatcher.getRoot().addChild(factions);
		dispatcher.getRoot().addChild(alias);

		factions.addChild(create);
		factions.addChild(disband);
		factions.addChild(join);
		factions.addChild(leave);
		factions.addChild(info);
		factions.addChild(list);
		factions.addChild(chat);

		factions.addChild(modify);
		modify.addChild(description);
		modify.addChild(color);
		modify.addChild(open);

		factions.addChild(invite);
		invite.addChild(listInvites);
		invite.addChild(addInvite);
		invite.addChild(removeInvite);

		factions.addChild(ally);
		ally.addChild(addAlly);
		ally.addChild(acceptAlly);
		ally.addChild(listAlly);
		ally.addChild(removeAlly);

		factions.addChild(admin);
		admin.addChild(adminBypass);

		factions.addChild(claim);
		claim.addChild(listClaim);
		claim.addChild(removeClaim);
		removeClaim.addChild(removeAllClaims);

		factions.addChild(home);

		factions.addChild(rank);
		rank.addChild(promote);
		rank.addChild(demote);
		factions.addChild(transferOwner);
		factions.addChild(kickMember);
		
		home.addChild(setHome);
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

	public static boolean isCivilian(ServerCommandSource source) {
		try {
			ServerPlayerEntity player = source.getPlayer();
			return isFactionMember(source) && Member.get(player.getUuid()).getRank() == Member.Rank.CIVILIAN;
		} catch (CommandSyntaxException e) { return false; }
	}

	public static boolean isOfficer(ServerCommandSource source) {
		try {
			ServerPlayerEntity player = source.getPlayer();
			return isFactionMember(source) && Member.get(player.getUuid()).getRank() == Member.Rank.OFFICER;
		} catch (CommandSyntaxException e) { return false; }
	}

	public static boolean isCoOwner(ServerCommandSource source) {
		try {
			ServerPlayerEntity player = source.getPlayer();
			return isFactionMember(source) && Member.get(player.getUuid()).getRank() == Member.Rank.CO_OWNER;
		} catch (CommandSyntaxException e) { return false; }
	}

	public static boolean isOwner(ServerCommandSource source) {
		try {
			ServerPlayerEntity player = source.getPlayer();
			return isFactionMember(source) && Member.get(player.getUuid()).getRank() == Member.Rank.OWNER;
		} catch (CommandSyntaxException e) { return false; }
	}

	public static boolean isRankAboveOfficer(ServerCommandSource source) {
		return isOwner(source) || isCoOwner(source);
	}

	public static boolean isRankAboveCivilian(ServerCommandSource source) {
		return isOwner(source) || isCoOwner(source) || isOfficer(source);
	}
}