package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.api.persistents.User;
import io.icker.factions.config.Config;
import io.icker.factions.util.Command;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class SafeCommand implements Command {

    private int run(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        PlayerEvents.OPEN_SAFE.invoker().onOpenSafe(player, User.get(player.getUuid()).getFaction());
        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("safe")
            .requires(
                Requires.multiple(
                    Requires.hasPerms("faction.safe", 0),
                    Requires.isMember(),
                    s -> FactionsMod.CONFIG.FACTION_SAFE == Config.SafeOptions.COMMAND || FactionsMod.CONFIG.FACTION_SAFE == Config.SafeOptions.ENABLED
                )
            )
            .executes(this::run)
            .build();
    }
}
