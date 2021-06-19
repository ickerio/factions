package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.database.Member;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

public class DescriptionCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String description = StringArgumentType.getString(context, "description");
		ServerCommandSource source = context.getSource();

		Member.get(source.getPlayer().getUuid()).getFaction().setDescription(description);
        MutableText reply = new LiteralText("Succesfully updated faction description").formatted(Formatting.GREEN);
            
		source.sendFeedback(reply, false);
		return 1;
	}
}