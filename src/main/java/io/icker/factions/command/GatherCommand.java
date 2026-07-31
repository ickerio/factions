package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.FactionsMod;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import io.icker.factions.util.WorldUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;

public class GatherCommand implements Command {

    private int go(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        if (player == null) return 0;

        return execGo(player);
    }

    public int execGo(ServerPlayer player) {
        if (player.level().getServer() == null) return 0;

        ServerLevel world = WorldUtils.getWorld(FactionsMod.CONFIG.GATHER.LEVEL);

        if (world == null) {
            new Message(Component.translatable("factions.command.gather.fail.no_world"))
                    .fail()
                    .send(player, false);
            return 0;
        }

        player.teleportTo(
                world,
                FactionsMod.CONFIG.GATHER.X,
                FactionsMod.CONFIG.GATHER.Y,
                FactionsMod.CONFIG.GATHER.Z,
                new HashSet<>(),
                FactionsMod.CONFIG.GATHER.YAW,
                FactionsMod.CONFIG.GATHER.PITCH,
                false);

        new Message(Component.translatable("factions.command.gather.success"))
                .send(player, false);
        return 1;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> getNode() {
        return Commands.literal("gather")
                .requires(
                        Requires.multiple(
                                Requires.isMember(),
                                s -> FactionsMod.CONFIG.GATHER != null
                                        && FactionsMod.CONFIG.GATHER.ENABLED,
                                Requires.hasPerms("factions.gather", 0)))
                .executes(this::go)
                .build();
    }
}

