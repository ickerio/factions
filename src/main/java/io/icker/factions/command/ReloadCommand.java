package io.icker.factions.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.FactionsMod;
import io.icker.factions.util.Message;
import net.minecraft.server.command.ServerCommandSource;

public class ReloadCommand implements Command<ServerCommandSource> {
    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (FactionsMod.dynmapEnabled) {
            FactionsMod.dynmap.reloadAll();
            new Message("Reloaded dynmap marker").send(context.getSource().getPlayer(), false);
        } else {
            new Message("Dynmap not found").fail().send(context.getSource().getPlayer(), false);
        }

        return 1;
    }
}