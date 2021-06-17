package io.icker.factions.command;

import java.util.ArrayList;
import java.util.Collection;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.database.Faction;
import io.icker.factions.database.Invite;
import io.icker.factions.database.Member;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public class InviteCommand {
	public static int invite(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		// Requires being a member
		Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "target");

		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		Faction faction = Member.get(player.getUuid()).getFaction();

		for (ServerPlayerEntity target : targets) {
			System.out.println(Invite.add(target.getUuid(), faction.name));
			target.sendMessage(new TranslatableText("factions.invite.recieved"), false);
		}

		source.sendFeedback(new TranslatableText("factions.command.invite.success").formatted(Formatting.GREEN), false);
		return 1;
	}

    public static int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		// Lists all outgoing (if in faction) and incoming invites (if not in faction)
		Member member = Member.get(player.getUuid());

		if (member == null) {
			ArrayList<Invite> invites = Invite.get(player.getUuid());

			MutableText text = new TranslatableText("factions.command.invite_list.total_invites").formatted(Formatting.GREEN);
			if (invites.size() == 0) return 1;

			for (Invite invite : invites) {
				text.append(new TranslatableText(invite.getFaction().name).formatted(Formatting.YELLOW)); // todo: finish this after finishing text reply
			}

			source.sendFeedback(text, false);
		} else {
			// get faction outgoing
		}
		return 1;
	}

    public static int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		// Uninvites someone from the players current faction
		return 1;
	}

}