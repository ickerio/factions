package io.icker.factions.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.database.Member;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

public class ModifyCommand {
    public static int description(CommandContext<ServerCommandSource> context) throws CommandSyntaxException{
        String description = StringArgumentType.getString(context, "description");
		ServerCommandSource source = context.getSource();

		Member.get(source.getPlayer().getUuid()).getFaction().setDescription(description);
		source.sendFeedback(new LiteralText("Successfully updated faction description"), false);
		return 1;
    }

    public static int color(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Formatting color = ColorArgumentType.getColor(context, "color");
		ServerCommandSource source = context.getSource();

		Member.get(source.getPlayer().getUuid()).getFaction().setColor(color);
		source.sendFeedback(new LiteralText("Successfully updated faction color"), false);
		return 1;
    }

    public static int open(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean open = BoolArgumentType.getBool(context, "open");
		ServerCommandSource source = context.getSource();

		Member.get(source.getPlayer().getUuid()).getFaction().setOpen(open);
		source.sendFeedback(new LiteralText("Successfully updated faction to  " + (open ? "open" : "closed")), false);
		return 1;
	}
}
