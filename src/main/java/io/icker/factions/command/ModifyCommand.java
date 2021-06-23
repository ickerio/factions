package io.icker.factions.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.icker.factions.database.Member;
import io.icker.factions.util.Message;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class ModifyCommand {
    public static int description(CommandContext<ServerCommandSource> context) throws CommandSyntaxException{
        String description = StringArgumentType.getString(context, "description");

		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		Member.get(player.getUuid()).getFaction().setDescription(description);
		new Message("Successfully updated faction description").send(player, false);
		return 1;
    }

    public static int color(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Formatting color = ColorArgumentType.getColor(context, "color");

		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		Member.get(player.getUuid()).getFaction().setColor(color);
		new Message("Successfully updated faction color").send(player, false);
		return 1;
    }

    public static int open(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean open = BoolArgumentType.getBool(context, "open");

		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();

		Member.get(player.getUuid()).getFaction().setOpen(open);
		new Message("Successfully updated faction to  " + (open ? "open" : "closed")).send(player, false);
		return 1;
	}
}
