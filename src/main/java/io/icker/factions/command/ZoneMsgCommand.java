package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.persistents.Member;
import io.icker.factions.config.Config;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class ZoneMsgCommand implements Command {
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Member config = Member.get(player.getUuid());
        boolean zoneMsg = !config.isZoneOn();
        config.setZoneMessage(zoneMsg);

        new Message("Successfully toggled zone messages")
                .filler("·")
                .add(
                        new Message(zoneMsg ? "ON" : "OFF")
                                .format(zoneMsg ? Formatting.GREEN : Formatting.RED)
                )
                .send(player, false);

        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("zoneMessage")
            .requires(s -> Config.ZONE_MESSAGE)
            .requires(Requires.hasPerms("actions.zonemessage", 0))
            .executes(this::run)
            .build();
    }
}
