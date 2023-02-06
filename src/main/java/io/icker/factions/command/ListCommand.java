package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.text.Message;
import io.icker.factions.text.PlainText;
import io.icker.factions.text.TranslatableText;
import io.icker.factions.util.Command;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;

public class ListCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Collection<Faction> factions = Faction.all();
        int size = factions.size();

        new Message().append(new TranslatableText("list", size))
                .send(player, false);

        if (size == 0) return 1;

        Message list = new Message();
        for (Faction faction : factions) {
            String name = faction.getName();
            list.append(new PlainText(name).click("/factions info " + name).format(faction.getColor())).append(new PlainText(", "));
        }

        list.send(player, false);
        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("list")
            .requires(Requires.hasPerms("factions.list", 0))
            .executes(this::run)
            .build();
    }
}