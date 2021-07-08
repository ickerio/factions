package io.icker.factions.command;

import java.util.ArrayList;
import java.util.stream.Collectors;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.database.Faction;
import io.icker.factions.database.Invite;
import io.icker.factions.database.Member;
import io.icker.factions.util.Message;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.UserCache;

public class InviteCommand {
	public static int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();

		ArrayList<Invite> invites = Member.get(source.getPlayer().getUuid()).getFaction().getInvites();
		int count = invites.size();

		new Message("You have ")
			.add(new Message(String.valueOf(count)).format(Formatting.YELLOW))
			.add(" outgoing invite%s", count == 1 ? "" : "s")
			.send(source.getPlayer(), false);

		if (count == 0) return 1;

		UserCache cache = source.getServer().getUserCache();
		String players = invites.stream()
			.map(invite -> cache.getByUuid(invite.playerId).orElse(new GameProfile(Util.NIL_UUID, "{Uncached Player}")).getName())
			.collect(Collectors.joining(", "));

		new Message(players).format(Formatting.ITALIC).send(source.getPlayer(), false);
		return 1;
	}

	public static int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		Faction faction = Member.get(source.getPlayer().getUuid()).getFaction();
		Invite invite = Invite.get(target.getUuid(), faction.name);
		if (invite != null) {
			new Message(target.getName().getString() + " was already invited to your faction").format(Formatting.RED).send(player, false);
			return 0;
		}

		Invite.add(target.getUuid(), faction.name); 

		new Message(target.getName().getString() + " has been invited")
			.send(faction);
		new Message("You have been invited to join this faction").format(Formatting.YELLOW)
			.hover("Click to join").click("/factions join " + faction.name)
			.prependFaction(faction)
			.send(target, false);
		return 1;
	}

    public static int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		Faction faction = Member.get(player.getUuid()).getFaction();
		new Invite(target.getUuid(), faction.name).remove();

		new Message(target.getName().getString() + " is no longer invited to your faction").send(player, false);
		return 1;
	}
}