package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.database.Faction;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public class CreateCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "name");

		ServerCommandSource source = context.getSource();
		if (Faction.get(name) != null) {
			source.sendFeedback(new TranslatableText("factions.command.create.already_exists").formatted(Formatting.RED), false);
			return 0;
		}

		ServerPlayerEntity player = source.getPlayer();
		Faction.add(name, "No description set", Formatting.RESET.getName(), false, 100).addMember(player.getUuid());
		source.getMinecraftServer().getPlayerManager().sendCommandTree(source.getPlayer());
		
		source.sendFeedback(new TranslatableText("factions.command.create.success").formatted(Formatting.GREEN), false);
		return 1;
	}
}