package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.config.Config;
import io.icker.factions.util.Command;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class SafeCommand implements Command {

    private int run(CommandContext<ServerCommandSource> context) {
        PlayerEvents.OPEN_SAFE.invoker().onOpenSafe(context.getSource().getPlayer());

        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
                .literal("safe")
                .requires(Requires.multiple(Requires.isMember(), Requires.hasPerms("faction.safe", 0), (serverCommandSource -> (
                    FactionsMod.CONFIG.FACTION_SAFE == Config.SafeOptions.COMMAND || FactionsMod.CONFIG.FACTION_SAFE == Config.SafeOptions.ON
                ))))
                .executes(this::run)
                .build();
    }
}
