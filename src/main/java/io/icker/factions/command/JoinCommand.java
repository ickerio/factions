package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.database.Database;
import io.icker.factions.database.Faction;
import io.icker.factions.util.FactionsUtil;
import java.util.UUID;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class JoinCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "name");

		ServerPlayerEntity player = context.getSource().getPlayer();
		UUID uuid = player.getUuid();

        Faction faction = Database.Factions.get(name);

		if (faction == null) {
			FactionsUtil.Message.sendError(player, "Faction with that name doesn't exist");
			return 0;
		}
        if (!faction.open) {
			FactionsUtil.Message.sendError(player, "Faction is currently closed");
			return 0;
		}

		faction.addMember(uuid);
        context.getSource().getMinecraftServer().getPlayerManager().sendCommandTree(player);
		FactionsUtil.Message.sendSuccess(player, "Success! Joined faction");
		return 1;
	}
}