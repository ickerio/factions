package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.Collection;

public class ListCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Collection<Faction> factions = Faction.all();
        int size = factions.size();

        new Message("There %s ", size == 1 ? "is" : "are")
                .add(new Message(String.valueOf(size)).format(Formatting.YELLOW))
                .add(" faction%s", size == 1 ? "" : "s")
                .send(source.getPlayer(), false);

        factions.forEach(f -> InfoCommand.info(player, f));
        // TODO, rewrite to just show a comma seperated
        // and when clicked, it runs the full info command
        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("list")
            .requires(Requires.hasPerms("factions.list", 0))
            .executes(this::run)
            .build();
    }
}