package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.config.Config;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Invite;
import io.icker.factions.event.FactionEvents;
import io.icker.factions.util.Message;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

// TODO: Name suggestions of open factions
public class JoinCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "name");
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

        Faction faction = Faction.get(name);

		if (faction == null) {
		new Message("Cannot join faction as none exist with that name").fail().send(player, false);
			return 0;
		}

		Invite invite = Invite.get(player.getUuid(), faction.name);
        if (!faction.open && invite == null) {
			new Message("Cannot join faction as it is not open and you are not invited").fail().send(player, false);
			return 0;
		}

		if (faction.getMembers().size() >= Config.MAX_FACTION_SIZE && Config.MAX_FACTION_SIZE != -1) {
			new Message("Cannot join faction as it is currently full").fail().send(player, false);
			return 0;
		}

		if (invite != null) invite.remove();
		faction.addMember(player.getUuid());
        source.getServer().getPlayerManager().sendCommandTree(player);
		
		new Message(player.getName().asString() + " joined").send(faction);
		FactionEvents.adjustPower(faction, Config.MEMBER_POWER); // TODO: change this, its ew
		return 1;
	}
}