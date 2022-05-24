package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Member;
import io.icker.factions.api.persistents.Member.ChatMode;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class ChatCommand implements Command{
    private int global(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return set(context.getSource(), ChatMode.GLOBAL);
    }

    private int faction(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return set(context.getSource(), ChatMode.FACTION);
    }

    private int focus(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return set(context.getSource(), ChatMode.FOCUS);
    }

    private int set(ServerCommandSource source, ChatMode option) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        Member.get(player.getUuid()).setChatMode(option);   

        new Message("Successfully updated your chat preference").send(player, false);
        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("chat")
            .requires(Requires.hasPerms("factions.chat", 0))
            .then(CommandManager.literal("global").executes(this::global))
            .then(CommandManager.literal("faction").executes(this::faction))
            .then(CommandManager.literal("focus").executes(this::focus))
            .build();
    }

}
