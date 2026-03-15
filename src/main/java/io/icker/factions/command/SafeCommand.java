package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.api.events.PlayerEvents;
import io.icker.factions.util.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class SafeCommand implements Command {

    private int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerEvents.OPEN_SAFE.invoker().onOpenSafe(player, Command.getUser(player).getFaction());
        return 1;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("safe")
                .requires(
                        Requires.multiple(
                                Requires.hasPerms("faction.safe", 0),
                                Requires.isMember(),
                                s -> FactionsMod.CONFIG.SAFE != null))
                .executes(this::run)
                .build();
    }
}
