package io.icker.factions.command;

import java.util.Collection;
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

public class ListCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Collection<Faction> factions = Faction.all();
        int size = factions.size();

        new Message("There %s ", size == 1 ? "is" : "are")
                .add(new Message(String.valueOf(size)).format(Formatting.YELLOW))
                .add(" faction%s", size == 1 ? "" : "s").send(player, false);

        if (size == 0)
            return 1;

        Message list = new Message("");
        for (Faction faction : factions) {
            String name = faction.getName();
            list.add(new Message(name).click("/factions info " + name).format(faction.getColor()))
                    .add(", ");
        }

        list.send(player, false);
        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("list").requires(Requires.hasPerms("factions.list", 0))
                .executes(this::run).build();
    }
}
