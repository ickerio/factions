package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.Command;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import io.icker.factions.database.PlayerConfig;
import io.icker.factions.util.Message;
import io.icker.factions.FactionsMod;

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