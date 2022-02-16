package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.database.Ally;
import io.icker.factions.util.Message;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class AllyCommand {
	public static int add(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		Faction sourceFaction = Member.get(player.getUuid()).getFaction();
		Faction targetFaction = Member.get(target.getUuid()).getFaction();
		
		if (Ally.checkIfAlly(sourceFaction.name, targetFaction.name)) {
			new Message(targetFaction.name + " is already allied").format(Formatting.RED).send(player, false);
		} else if (sourceFaction.name == targetFaction.name) {
			new Message("You can't ally yourself").format(Formatting.RED).send(player, false);
		} else {
			Ally.add(sourceFaction.name, targetFaction.name);

			new Message(targetFaction.name + " is now allied")
					.send(player, false);
			new Message(
					"You are now allies with " + sourceFaction.name).format(Formatting.YELLOW)
					.hover("Click to ally them back").click("/factions ally add " + source.getName())
					.send(target, false);
		}

		return 1;
	}

  public static int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		Faction sourceFaction = Member.get(player.getUuid()).getFaction();
		Faction targetFaction = Member.get(target.getUuid()).getFaction();

		if (!Ally.checkIfAlly(sourceFaction.name, targetFaction.name)) {
			new Message(targetFaction.name + " is not allied").format(Formatting.RED).send(player, false);
		} else {
			Ally.remove(sourceFaction.name, targetFaction.name);

			new Message(target.getName().getString() + " is no longer allied")
				.send(sourceFaction);
			new Message(
					"You are no longer allies with " + sourceFaction.name).format(Formatting.YELLOW)
				.hover("Click to remove them as an ally").click("/factions ally remove " + sourceFaction.name)
				.send(target, false);
		}

		return 1;
	}
}