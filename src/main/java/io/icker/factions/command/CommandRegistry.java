package io.icker.factions.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Member;
import io.icker.factions.config.Config;
import io.icker.factions.util.FactionSuggestions;
import io.icker.factions.util.PermissionsWrapper;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.command.CommandSource.suggestMatching;

public class CommandRegistry {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> factions = CommandManager
                .literal("factions")
                .requires(s -> PermissionsWrapper.require(s, "factions"))
                .build();

        LiteralCommandNode<ServerCommandSource> alias = CommandManager
                .literal("f")
                .requires(s -> PermissionsWrapper.require(s, "factions"))
                .redirect(factions)
                .build();

        LiteralCommandNode<ServerCommandSource> create = new CreateCommand().getNode();

        LiteralCommandNode<ServerCommandSource> disband = new DisbandCommand().getNode();

        LiteralCommandNode<ServerCommandSource> join = new JoinCommand().getNode();

        LiteralCommandNode<ServerCommandSource> leave = new LeaveCommand().getNode();

        LiteralCommandNode<ServerCommandSource> info = CommandManager
                .literal("info")
                .requires(s -> PermissionsWrapper.require(s, "factions.info"))
                .executes(InfoCommand::self)
                .then(
                        CommandManager.argument("faction", StringArgumentType.greedyString())
                                .executes(InfoCommand::any)
                )
                .build();

        LiteralCommandNode<ServerCommandSource> list = new ListCommand().getNode();

        LiteralCommandNode<ServerCommandSource> chat = new ChatCommand().getNode();

        LiteralCommandNode<ServerCommandSource> modify = new ModifyCommand().getNode();

        LiteralCommandNode<ServerCommandSource> invite = CommandManager
                .literal("invite")
                .requires(s -> PermissionsWrapper.require(s, "factions.invite"))
                .requires(CommandRegistry::isRankAboveCivilian)
                .build();

        LiteralCommandNode<ServerCommandSource> listInvites = CommandManager
                .literal("list")
                .requires(s -> PermissionsWrapper.require(s, "factions.invite.list"))
                .executes(InviteCommand::list)
                .build();

        LiteralCommandNode<ServerCommandSource> addInvite = CommandManager
                .literal("add")
                .requires(s -> PermissionsWrapper.require(s, "factions.invite.add"))
                .then(
                        CommandManager.argument("player", EntityArgumentType.player())
                                .executes(InviteCommand::add)
                )
                .build();

        LiteralCommandNode<ServerCommandSource> removeInvite = CommandManager
                .literal("remove")
                .requires(s -> PermissionsWrapper.require(s, "factions.invite.remove"))
                .then(
                        CommandManager.argument("player", EntityArgumentType.player())
                                .executes(InviteCommand::remove)
                )
                .build();

        LiteralCommandNode<ServerCommandSource> claim = new ClaimCommand().getNode();

        LiteralCommandNode<ServerCommandSource> home = new HomeCommand().getNode();

        LiteralCommandNode<ServerCommandSource> map = new MapCommand().getNode();

        LiteralCommandNode<ServerCommandSource> admin = new AdminCommand().getNode();

        LiteralCommandNode<ServerCommandSource> rank = CommandManager
                .literal("rank")
                .requires(s -> PermissionsWrapper.require(s, "factions.rank"))
                .requires(CommandRegistry::isRankAboveOfficer)
                .build();

        LiteralCommandNode<ServerCommandSource> promote = CommandManager
                .literal("promote")
                .requires(s -> PermissionsWrapper.require(s, "factions.rank.promote"))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(RankCommand::promote))
                .build();

        LiteralCommandNode<ServerCommandSource> demote = CommandManager
                .literal("demote")
                .requires(s -> PermissionsWrapper.require(s, "factions.rank.demote"))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(RankCommand::demote))
                .build();

        LiteralCommandNode<ServerCommandSource> transferOwner = CommandManager
                .literal("transferOwner")
                .requires(s -> PermissionsWrapper.require(s, "factions.transfer"))
                .requires(CommandRegistry::isOwner)
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(c -> new TransferOwnerCommand().run(c)))
                .build();

        LiteralCommandNode<ServerCommandSource> kickMember = new KickCommand().getNode();

        LiteralCommandNode<ServerCommandSource> zoneMsg = CommandManager
                .literal("zoneMsg")
                .requires(s -> Config.ZONE_MESSAGE)
                .requires(s -> PermissionsWrapper.require(s, "factions.zonemsg"))
                .executes(new ZoneMsgCommand())
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
        factions.addChild(zoneMsg);

        factions.addChild(modify);

        factions.addChild(invite);
        invite.addChild(listInvites);
        invite.addChild(addInvite);
        invite.addChild(removeInvite);

        factions.addChild(admin);

        factions.addChild(claim);

        factions.addChild(map);

        factions.addChild(home);

        factions.addChild(rank);
        rank.addChild(promote);
        rank.addChild(demote);
        factions.addChild(transferOwner);
        factions.addChild(kickMember);
    }

    public static boolean isFactionMember(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            return Member.get(player.getUuid()) != null;
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    public static boolean isFactionless(ServerCommandSource source) {
        return !isFactionMember(source);
    }

    public static boolean isCivilian(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            return isFactionMember(source) && Member.get(player.getUuid()).getRank() == Member.Rank.MEMBER;
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    public static boolean isOfficer(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            return isFactionMember(source) && Member.get(player.getUuid()).getRank() == Member.Rank.COMMANDER;
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    public static boolean isCoOwner(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            return isFactionMember(source) && Member.get(player.getUuid()).getRank() == Member.Rank.LEADER;
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    public static boolean isOwner(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            return isFactionMember(source) && Member.get(player.getUuid()).getRank() == Member.Rank.OWNER;
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    public static boolean isRankAboveOfficer(ServerCommandSource source) {
        return isOwner(source) || isCoOwner(source);
    }

    public static boolean isRankAboveCivilian(ServerCommandSource source) {
        return isOwner(source) || isCoOwner(source) || isOfficer(source);
    }
}