package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.database.Faction;
import io.icker.factions.database.Invite;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

public class JoinCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "name");
		// TODO: Name suggestions of open factions
		ServerCommandSource source = context.getSource();
        Faction faction = Faction.get(name);

		if (faction == null) {
			source.sendFeedback(new LiteralText("Cannot join faction as none exist with that name").formatted(Formatting.RED), false);
			return 0;
		}

		ServerPlayerEntity player = source.getPlayer();
		Invite invite = Invite.get(player.getUuid(), faction.name);
        if (!faction.open && invite == null) {
			source.sendFeedback(new LiteralText("Cannot join faction as it is not open you are not invited").formatted(Formatting.RED), false);
			return 0;
		}

		if (invite != null) invite.remove();
		faction.addMember(player.getUuid());
        source.getMinecraftServer().getPlayerManager().sendCommandTree(player);
		
		MutableText reply = new LiteralText("You are now a member of ").formatted(Formatting.GREEN)
			.append(new LiteralText(faction.name).formatted(faction.color));
		source.sendFeedback(reply, false);
		return 1;
	}
}