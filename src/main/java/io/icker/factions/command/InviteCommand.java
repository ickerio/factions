package io.icker.factions.command;

import java.util.ArrayList;
import java.util.stream.Collectors;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.database.Faction;
import io.icker.factions.database.Invite;
import io.icker.factions.database.Member;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;

public class InviteCommand {
	public static int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();

		ArrayList<Invite> invites = Member.get(source.getPlayer().getUuid()).getFaction().getInvites();
		int count = invites == null ? 0 : invites.size();

		MutableText reply = new LiteralText("You have ")
			.append(new LiteralText(String.valueOf(count)).formatted(Formatting.YELLOW))
			.append(new LiteralText(String.format(" outgoing invite%s", count == 1 ? "" : "s")));

		source.sendFeedback(reply, false);

		if (count == 0) return 1;

		UserCache cache = source.getMinecraftServer().getUserCache();
		String players = invites.stream()
			.map(invite -> cache.getByUuid(invite.playerId).getName()) // TODO: Fix Null when player offline
			.collect(Collectors.joining(", "));

		source.sendFeedback(new LiteralText(players).formatted(Formatting.ITALIC), false);
		return 1;
	}

	public static int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
		ServerCommandSource source = context.getSource();

		Faction faction = Member.get(source.getPlayer().getUuid()).getFaction();
		Invite invite = Invite.get(target.getUuid(), faction.name);
		if (invite != null) {
			MutableText reply = new LiteralText(target.getName().getString() + " was already invited to your faction").formatted(Formatting.RED);
			source.sendFeedback(reply, false);
			return 0;
		}

		Invite.add(target.getUuid(), faction.name); 

		MutableText reply = new LiteralText(target.getName().getString() + " has been invited to your faction.");
		MutableText alert = new LiteralText(String.format("You have been invited to join ")).formatted(Formatting.YELLOW)
			.append(new LiteralText(faction.name).formatted(faction.color))
			.styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to join")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/factions join " + faction.name)));

		source.sendFeedback(reply, false);
		target.sendMessage(alert, false);
		return 1;
	}

    public static int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		Faction faction = Member.get(player.getUuid()).getFaction();
		new Invite(target.getUuid(), faction.name).remove();

		MutableText reply = new LiteralText(target.getName().getString() + " is no longer invited to your faction");
		source.sendFeedback(reply, false);
		return 1;
	}
}