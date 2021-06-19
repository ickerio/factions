package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.database.Member;
import com.mojang.brigadier.Command;

import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

public class ColorCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Formatting color = ColorArgumentType.getColor(context, "color");
		ServerCommandSource source = context.getSource();

		Member.get(source.getPlayer().getUuid()).getFaction().setColor(color);
        MutableText reply = new LiteralText("Succesfully updated faction color to ").formatted(Formatting.GREEN)
            .append(new LiteralText(color.getName()).formatted(color));
            
		source.sendFeedback(reply, false);
		return 1;
	}
}