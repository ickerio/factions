package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.database.Faction;
import io.icker.factions.util.Message;

import java.util.ArrayList;

import com.mojang.brigadier.Command;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class ListCommand implements Command<ServerCommandSource> {
    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        ArrayList<Faction> factions = Faction.all();
        int size = factions.size();

        new Message("There %s ", size == 1 ? "is" : "are")
            .add(new Message(String.valueOf(size)).format(Formatting.YELLOW))
            .add(" faction%s", size == 1 ? "" : "s")
            .send(source.getPlayer(), false);

        factions.forEach(f -> InfoCommand.info(player, f));
        return 1;
    }
}