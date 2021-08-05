package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.database.PlayerConfig;
import io.icker.factions.database.PlayerConfig.ChatOption;
import io.icker.factions.util.Message;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class ChatCommand {
    public static int global(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return set(context.getSource(), ChatOption.GLOBAL);
    }

    public static int faction(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return set(context.getSource(), ChatOption.FACTION);
    }

    public static int focus(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return set(context.getSource(), ChatOption.FOCUS);
    }

    private static int set(ServerCommandSource source, ChatOption option) throws CommandSyntaxException {
		ServerPlayerEntity player = source.getPlayer();
        PlayerConfig.get(player.getUuid()).setChat(option);
        
        new Message("Successfully updated your chat preference").send(player, false);
        return 1;
    }

}
