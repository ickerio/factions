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

public class JoinCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "name");
		// TODO: Name suggestions of open factions
		ServerCommandSource source = context.getSource();
        Faction faction = Faction.get(name);

		if (faction == null) {
			source.sendFeedback(new TranslatableText("factions.command.join.not_exist").formatted(Formatting.RED), false);
			return 0;
		}
        if (!faction.open) {
			source.sendFeedback(new TranslatableText("factions.command.join.closed").formatted(Formatting.RED), false);
			return 0;
		}

		ServerPlayerEntity player = source.getPlayer();
		faction.addMember(player.getUuid());
        source.getMinecraftServer().getPlayerManager().sendCommandTree(player);

		source.sendFeedback(new TranslatableText("factions.command.join.success").formatted(Formatting.GREEN), false);
		return 1;
	}
}