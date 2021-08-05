package io.icker.factions.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.icker.factions.database.PlayerConfig;
import io.icker.factions.util.Message;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class BypassCommand implements Command<ServerCommandSource> {
    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        PlayerConfig config = PlayerConfig.get(player.getUuid());
        boolean bypass = !config.bypass;
        config.setBypass(bypass);

        new Message("Successfully toggled claim bypass")
        .filler("·")
        .add(
            new Message(bypass ? "ON" : "OFF")
            .format(bypass ? Formatting.GREEN : Formatting.RED)
        )
        .send(player, false);

        return 1;
    }
}