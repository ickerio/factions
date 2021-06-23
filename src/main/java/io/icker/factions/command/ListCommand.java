package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.database.Faction;

import java.util.ArrayList;

import com.mojang.brigadier.Command;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

public class ListCommand implements Command<ServerCommandSource> {
	@Override
	public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        ArrayList<Faction> factions = Faction.all();

        source.sendFeedback(new LiteralText(String.format("There are %d factions: ", factions.size())), false);
        factions.forEach(f -> source.sendFeedback(InfoCommand.buildFactionMessage(f, source), false));
        return 1;
	}
}