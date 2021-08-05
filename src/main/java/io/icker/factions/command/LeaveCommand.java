package io.icker.factions.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.config.Config;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Member;
import io.icker.factions.event.FactionEvents;
import io.icker.factions.util.Message;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class LeaveCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();
		
		Member member = Member.get(player.getUuid());
		Faction faction = member.getFaction();
        
		new Message(player.getName().asString() + " left").send(faction);
		member.remove();
        context.getSource().getServer().getPlayerManager().sendCommandTree(player);

		if (faction.getMembers().size() == 0) {
			faction.remove();
		} else {
			FactionEvents.adjustPower(faction, -Config.MEMBER_POWER);
		}
		
		return 1;
	}
}